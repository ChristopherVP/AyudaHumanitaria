package uce.edu.ec.ayudahumanitaria.persistencia;

import uce.edu.ec.ayudahumanitaria.dominio.CentroAcopio;
import uce.edu.ec.ayudahumanitaria.dominio.EstadoViaje;
import uce.edu.ec.ayudahumanitaria.dominio.Parada;
import uce.edu.ec.ayudahumanitaria.dominio.Pendiente;
import uce.edu.ec.ayudahumanitaria.dominio.PlanDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.Viaje;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistencia de planes de distribución, viajes y paradas (historial de registros). */
public class PlanDAO {

    private static final double TOLERANCIA = 0.06;

    // ------------------------------------------------------------------ guardar / cancelar

    /**
     * Guarda el plan completo en una sola transacción: valida disponibilidad de vehículos y necesidades,
     * descuenta el stock de cada centro y registra viajes y paradas. Si algo falla, no queda nada guardado.
     */
    public PlanDistribucion guardar(PlanDistribucion plan) throws PersistenciaException {
        Connection con = ConexionBD.obtenerConexion();
        try {
            con.setAutoCommit(false);
            try {
                int planId = insertarPlan(con, plan);
                List<Viaje> guardados = new ArrayList<>();
                Map<Integer, Double> aDescontar = new LinkedHashMap<>();
                for (Viaje v : plan.viajes()) {
                    validarVehiculoLibre(con, v);
                    for (Parada p : v.paradas()) {
                        validarNecesidad(con, p);
                    }
                    int viajeId = insertarViaje(con, planId, v);
                    for (Parada p : v.paradas()) {
                        insertarParada(con, viajeId, p);
                    }
                    guardados.add(v.conId(viajeId));
                    aDescontar.merge(v.centro().getId(), v.cargaToneladas(), Double::sum);
                }
                for (Map.Entry<Integer, Double> e : aDescontar.entrySet()) {
                    descontarStock(con, e.getKey(), e.getValue());
                }
                con.commit();
                return plan.conId(planId, guardados);
            } catch (SQLException | PersistenciaException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo guardar el plan de distribución.", ex);
        }
    }

    /** Cancela un plan que aún no ha salido: devuelve el stock a los centros y marca sus viajes como cancelados. */
    public void cancelar(int planId, LocalDateTime ahora) throws PersistenciaException {
        Connection con = ConexionBD.obtenerConexion();
        try {
            con.setAutoCommit(false);
            try {
                Map<Integer, Double> aDevolver = new HashMap<>();
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT CENTRO_ID, CARGA_TON, SALIDA FROM VIAJE WHERE PLAN_ID = ? AND ESTADO <> 'CANCELADO'")) {
                    ps.setInt(1, planId);
                    try (ResultSet rs = ps.executeQuery()) {
                        boolean alguno = false;
                        while (rs.next()) {
                            alguno = true;
                            if (!rs.getTimestamp("SALIDA").toLocalDateTime().isAfter(ahora)) {
                                throw new PersistenciaException(
                                        "El plan ya inició (hay viajes en ruta o completados): no se puede cancelar.");
                            }
                            int centroId = rs.getInt("CENTRO_ID");
                            if (centroId > 0) {
                                aDevolver.merge(centroId, rs.getDouble("CARGA_TON"), Double::sum);
                            }
                        }
                        if (!alguno) {
                            throw new PersistenciaException("El plan no existe o ya estaba cancelado.");
                        }
                    }
                }
                for (Map.Entry<Integer, Double> e : aDevolver.entrySet()) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE CENTRO SET STOCK_TON = STOCK_TON + ? WHERE ID = ?")) {
                        ps.setDouble(1, e.getValue());
                        ps.setInt(2, e.getKey());
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE VIAJE SET ESTADO = 'CANCELADO' WHERE PLAN_ID = ?")) {
                    ps.setInt(1, planId);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException | PersistenciaException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo cancelar el plan.", ex);
        }
    }

    private int insertarPlan(Connection con, PlanDistribucion plan) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO PLAN (CREADO_EN, SALIDA, BITACORA, PENDIENTES) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(plan.creadoEn()));
            ps.setTimestamp(2, Timestamp.valueOf(plan.salidaProgramada()));
            ps.setString(3, String.join("\n", plan.bitacora()));
            ps.setString(4, serializarPendientes(plan.pendientes()));
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                llaves.next();
                return llaves.getInt(1);
            }
        }
    }

    private int insertarViaje(Connection con, int planId, Viaje v) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO VIAJE (PLAN_ID, CENTRO_ID, CENTRO_NOMBRE, CENTRO_LAT, CENTRO_LON, VEHICULO_ID, "
                        + "VEHICULO_ETIQUETA, CAPACIDAD_TON, CARGA_TON, DISTANCIA_KM, SALIDA, REGRESO, ESTADO) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, planId);
            ps.setInt(2, v.centro().getId());
            ps.setString(3, v.centro().getNombre());
            ps.setDouble(4, v.centro().getLatitud());
            ps.setDouble(5, v.centro().getLongitud());
            ps.setString(6, v.vehiculoId());
            ps.setString(7, v.vehiculoId());
            ps.setDouble(8, v.capacidadToneladas());
            ps.setDouble(9, v.cargaToneladas());
            ps.setDouble(10, v.distanciaKm());
            ps.setTimestamp(11, Timestamp.valueOf(v.salida()));
            ps.setTimestamp(12, Timestamp.valueOf(v.regreso()));
            ps.setString(13, EstadoViaje.PLANIFICADO.name());
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                llaves.next();
                return llaves.getInt(1);
            }
        }
    }

    private void insertarParada(Connection con, int viajeId, Parada p) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO PARADA (VIAJE_ID, COMUNIDAD_ID, COMUNIDAD_NOMBRE, PROVINCIA, LATITUD, LONGITUD, ORDEN, "
                        + "TONELADAS, KM_TRAMO, KM_ACUMULADOS, LLEGADA, SALIDA) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, viajeId);
            if (p.comunidadId() > 0) {
                ps.setInt(2, p.comunidadId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, p.comunidadNombre());
            ps.setString(4, p.provincia() == null ? "" : p.provincia());
            ps.setDouble(5, p.latitud());
            ps.setDouble(6, p.longitud());
            ps.setInt(7, p.orden());
            ps.setDouble(8, p.toneladas());
            ps.setDouble(9, p.kmTramo());
            ps.setDouble(10, p.kmAcumulados());
            ps.setTimestamp(11, Timestamp.valueOf(p.llegada()));
            ps.setTimestamp(12, Timestamp.valueOf(p.salida()));
            ps.executeUpdate();
        }
    }

    private void validarVehiculoLibre(Connection con, Viaje v) throws SQLException, PersistenciaException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM VIAJE WHERE VEHICULO_ID = ? AND ESTADO <> 'CANCELADO' "
                        + "AND SALIDA < ? AND REGRESO > ?")) {
            ps.setString(1, v.vehiculoId());
            ps.setTimestamp(2, Timestamp.valueOf(v.regreso()));
            ps.setTimestamp(3, Timestamp.valueOf(v.salida()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    throw new PersistenciaException("El vehículo " + v.vehiculoId()
                            + " ya tiene otro viaje en ese horario. Vuelva a simular.");
                }
            }
        }
    }

    private void validarNecesidad(Connection con, Parada p) throws SQLException, PersistenciaException {
        if (p.comunidadId() <= 0) {
            return;
        }
        double requerido;
        try (PreparedStatement ps = con.prepareStatement("SELECT RECURSOS_REQUERIDOS FROM COMUNIDAD WHERE ID = ?")) {
            ps.setInt(1, p.comunidadId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new PersistenciaException("La comunidad " + p.comunidadNombre()
                            + " ya no existe. Vuelva a simular.");
                }
                requerido = rs.getDouble(1);
            }
        }
        double planificado = 0;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COALESCE(SUM(P.TONELADAS), 0) FROM PARADA P JOIN VIAJE V ON V.ID = P.VIAJE_ID "
                        + "WHERE P.COMUNIDAD_ID = ? AND V.ESTADO <> 'CANCELADO'")) {
            ps.setInt(1, p.comunidadId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                planificado = rs.getDouble(1);
            }
        }
        if (planificado + p.toneladas() > requerido + TOLERANCIA) {
            throw new PersistenciaException("La comunidad " + p.comunidadNombre()
                    + " ya tiene cubierta su necesidad con otro plan. Vuelva a simular.");
        }
    }

    private void descontarStock(Connection con, int centroId, double toneladas) throws SQLException, PersistenciaException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE CENTRO SET STOCK_TON = STOCK_TON - ? WHERE ID = ? AND STOCK_TON >= ?")) {
            ps.setDouble(1, toneladas);
            ps.setInt(2, centroId);
            ps.setDouble(3, toneladas - 1e-9);
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("El stock del centro cambió y ya no alcanza para este plan. "
                        + "Vuelva a simular.");
            }
        }
    }

    // ------------------------------------------------------------------ estados y consultas de apoyo

    /** Actualiza el estado de los viajes según el reloj real y refleja el uso de cada vehículo. */
    public void sincronizarEstados(LocalDateTime ahora) throws PersistenciaException {
        Connection con = ConexionBD.obtenerConexion();
        Timestamp t = Timestamp.valueOf(ahora);
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE VIAJE SET ESTADO = 'COMPLETADO' WHERE ESTADO IN ('PLANIFICADO','EN_RUTA') AND REGRESO <= ?")) {
                ps.setTimestamp(1, t);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE VIAJE SET ESTADO = 'EN_RUTA' WHERE ESTADO = 'PLANIFICADO' AND SALIDA <= ? AND REGRESO > ?")) {
                ps.setTimestamp(1, t);
                ps.setTimestamp(2, t);
                ps.executeUpdate();
            }
            try (Statement st = con.createStatement()) {
                st.executeUpdate("UPDATE VEHICULO SET ASIGNADO = FALSE, COMUNIDAD_ASIGNADA = NULL");
            }
            Map<String, String> etiquetas = etiquetasPorVehiculo(con);
            for (Map.Entry<String, String> e : etiquetas.entrySet()) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE VEHICULO SET ASIGNADO = TRUE, COMUNIDAD_ASIGNADA = ? WHERE ID = ?")) {
                    ps.setString(1, e.getValue());
                    ps.setString(2, e.getKey());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo actualizar el estado de los viajes.", ex);
        }
    }

    /**
     * Primer viaje vigente (el más próximo en el tiempo) de cada vehículo y sus destinos en orden.
     * Los destinos se unen en Java (no con LISTAGG/STRING_AGG) para que funcione igual en H2 y PostgreSQL.
     */
    private Map<String, String> etiquetasPorVehiculo(Connection con) throws SQLException {
        Map<String, Integer> primerViaje = new LinkedHashMap<>();
        Map<String, List<String>> destinos = new LinkedHashMap<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT V.ID, V.VEHICULO_ID, P.COMUNIDAD_NOMBRE FROM VIAJE V "
                             + "LEFT JOIN PARADA P ON P.VIAJE_ID = V.ID "
                             + "WHERE V.ESTADO IN ('PLANIFICADO','EN_RUTA') AND V.VEHICULO_ID IS NOT NULL "
                             + "ORDER BY V.SALIDA, V.ID, P.ORDEN")) {
            while (rs.next()) {
                String vehiculo = rs.getString("VEHICULO_ID");
                int viaje = rs.getInt("ID");
                primerViaje.putIfAbsent(vehiculo, viaje);
                List<String> lista = destinos.computeIfAbsent(vehiculo, k -> new ArrayList<>());
                String comunidad = rs.getString("COMUNIDAD_NOMBRE");
                if (primerViaje.get(vehiculo) == viaje && comunidad != null) {
                    lista.add(comunidad);
                }
            }
        }
        Map<String, String> etiquetas = new LinkedHashMap<>();
        destinos.forEach((vehiculo, lista) -> {
            String texto = String.join(", ", lista);
            etiquetas.put(vehiculo, texto.length() > 95 ? texto.substring(0, 92) + "..." : texto);
        });
        return etiquetas;
    }

    /** Toneladas ya comprometidas por comunidad en viajes no cancelados. */
    public Map<Integer, Double> toneladasPlanificadas() throws PersistenciaException {
        return sumarPorComunidad("V.ESTADO <> 'CANCELADO'");
    }

    /** Toneladas ya entregadas (viajes completados) por comunidad. */
    public Map<Integer, Double> toneladasEntregadas() throws PersistenciaException {
        return sumarPorComunidad("V.ESTADO = 'COMPLETADO'");
    }

    private Map<Integer, Double> sumarPorComunidad(String condicion) throws PersistenciaException {
        Map<Integer, Double> resultado = new HashMap<>();
        String sql = "SELECT P.COMUNIDAD_ID, SUM(P.TONELADAS) FROM PARADA P JOIN VIAJE V ON V.ID = P.VIAJE_ID "
                + "WHERE P.COMUNIDAD_ID IS NOT NULL AND " + condicion + " GROUP BY P.COMUNIDAD_ID";
        try (Statement st = ConexionBD.obtenerConexion().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.put(rs.getInt(1), rs.getDouble(2));
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo consultar las entregas.", ex);
        }
        return resultado;
    }

    /** Hora en que cada vehículo termina su último viaje vigente (sin incluir el tiempo de preparación). */
    public Map<String, LocalDateTime> regresoDeViajesVigentes() throws PersistenciaException {
        Map<String, LocalDateTime> resultado = new HashMap<>();
        String sql = "SELECT VEHICULO_ID, MAX(REGRESO) FROM VIAJE WHERE ESTADO IN ('PLANIFICADO','EN_RUTA') "
                + "AND VEHICULO_ID IS NOT NULL GROUP BY VEHICULO_ID";
        try (Statement st = ConexionBD.obtenerConexion().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.put(rs.getString(1), rs.getTimestamp(2).toLocalDateTime());
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo consultar la disponibilidad de vehículos.", ex);
        }
        return resultado;
    }

    public int contarViajesVigentesDeComunidad(int comunidadId) throws PersistenciaException {
        return contar("SELECT COUNT(DISTINCT V.ID) FROM VIAJE V JOIN PARADA P ON P.VIAJE_ID = V.ID "
                + "WHERE P.COMUNIDAD_ID = ? AND V.ESTADO IN ('PLANIFICADO','EN_RUTA')", comunidadId);
    }

    public int contarViajesVigentesDeVehiculo(String vehiculoId) throws PersistenciaException {
        return contar("SELECT COUNT(*) FROM VIAJE WHERE VEHICULO_ID = ? AND ESTADO IN ('PLANIFICADO','EN_RUTA')",
                vehiculoId);
    }

    public int contarViajesVigentesDeCentro(int centroId) throws PersistenciaException {
        return contar("SELECT COUNT(*) FROM VIAJE WHERE CENTRO_ID = ? AND ESTADO IN ('PLANIFICADO','EN_RUTA')",
                centroId);
    }

    private int contar(String sql, Object parametro) throws PersistenciaException {
        try (PreparedStatement ps = ConexionBD.obtenerConexion().prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo consultar los viajes vigentes.", ex);
        }
    }

    // ------------------------------------------------------------------ lectura de planes

    /** Todos los planes guardados, del más reciente al más antiguo. */
    public List<PlanDistribucion> listarPlanes() throws PersistenciaException {
        List<PlanDistribucion> planes = new ArrayList<>();
        try (Statement st = ConexionBD.obtenerConexion().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM PLAN ORDER BY ID DESC")) {
            List<Object[]> cabeceras = new ArrayList<>();
            while (rs.next()) {
                cabeceras.add(new Object[]{rs.getInt("ID"), rs.getTimestamp("CREADO_EN").toLocalDateTime(),
                        rs.getTimestamp("SALIDA").toLocalDateTime(), rs.getString("BITACORA"),
                        rs.getString("PENDIENTES")});
            }
            for (Object[] c : cabeceras) {
                int id = (Integer) c[0];
                planes.add(new PlanDistribucion(id, (LocalDateTime) c[1], (LocalDateTime) c[2], cargarViajes(id),
                        deserializarPendientes((String) c[4]), c[3] == null || ((String) c[3]).isEmpty()
                        ? List.of() : Arrays.asList(((String) c[3]).split("\n")),
                        PlanDistribucion.EstadisticasAlgoritmos.vacias()));
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo obtener el listado de planes.", ex);
        }
        return planes;
    }

    private List<Viaje> cargarViajes(int planId) throws SQLException, PersistenciaException {
        List<Viaje> viajes = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtenerConexion()
                .prepareStatement("SELECT V.*, C.CIUDAD AS CENTRO_CIUDAD FROM VIAJE V LEFT JOIN CENTRO C ON C.ID = V.CENTRO_ID "
                        + "WHERE V.PLAN_ID = ? ORDER BY V.SALIDA, V.ID")) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int viajeId = rs.getInt("ID");
                    CentroAcopio centro = new CentroAcopio(rs.getInt("CENTRO_ID"), rs.getString("CENTRO_NOMBRE"),
                            ciudadDelCentro(rs), "", rs.getDouble("CENTRO_LAT"), rs.getDouble("CENTRO_LON"), 0);
                    viajes.add(new Viaje(viajeId, centro, rs.getString("VEHICULO_ETIQUETA"),
                            rs.getDouble("CAPACIDAD_TON"), rs.getDouble("CARGA_TON"), cargarParadas(viajeId),
                            rs.getDouble("DISTANCIA_KM"), rs.getTimestamp("SALIDA").toLocalDateTime(),
                            rs.getTimestamp("REGRESO").toLocalDateTime(),
                            "CANCELADO".equals(rs.getString("ESTADO"))));
                }
            }
        }
        return viajes;
    }

    /** Ciudad del centro (si el centro fue eliminado, se usa el nombre guardado en el viaje). */
    private static String ciudadDelCentro(ResultSet rs) throws SQLException {
        String ciudad = rs.getString("CENTRO_CIUDAD");
        return ciudad == null || ciudad.isBlank() ? rs.getString("CENTRO_NOMBRE") : ciudad;
    }

    private List<Parada> cargarParadas(int viajeId) throws SQLException, PersistenciaException {
        List<Parada> paradas = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtenerConexion()
                .prepareStatement("SELECT * FROM PARADA WHERE VIAJE_ID = ? ORDER BY ORDEN")) {
            ps.setInt(1, viajeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    paradas.add(new Parada(rs.getInt("COMUNIDAD_ID"), rs.getString("COMUNIDAD_NOMBRE"),
                            rs.getString("PROVINCIA"), rs.getDouble("LATITUD"), rs.getDouble("LONGITUD"),
                            rs.getInt("ORDEN"), rs.getDouble("TONELADAS"), rs.getDouble("KM_TRAMO"),
                            rs.getDouble("KM_ACUMULADOS"), rs.getTimestamp("LLEGADA").toLocalDateTime(),
                            rs.getTimestamp("SALIDA").toLocalDateTime()));
                }
            }
        }
        return paradas;
    }

    // ------------------------------------------------------------------ pendientes (texto)

    private static String serializarPendientes(List<Pendiente> pendientes) {
        StringBuilder sb = new StringBuilder();
        for (Pendiente p : pendientes) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(p.comunidadId()).append('\t').append(p.comunidadNombre()).append('\t')
                    .append(p.centroNombre()).append('\t').append(p.toneladas()).append('\t').append(p.motivo());
        }
        return sb.toString();
    }

    private static List<Pendiente> deserializarPendientes(String texto) {
        List<Pendiente> lista = new ArrayList<>();
        if (texto == null || texto.isEmpty()) {
            return lista;
        }
        for (String linea : texto.split("\n")) {
            String[] c = linea.split("\t", -1);
            if (c.length == 5) {
                try {
                    lista.add(new Pendiente(Integer.parseInt(c[0]), c[1], c[2], Double.parseDouble(c[3]), c[4]));
                } catch (NumberFormatException ignorada) {
                    // Línea dañada: se omite (solo es información de apoyo del historial).
                }
            }
        }
        return lista;
    }
}