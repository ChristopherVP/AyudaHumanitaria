package uce.edu.ec.ayudahumanitaria.persistencia;

import uce.edu.ec.ayudahumanitaria.dominio.CentroAcopio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CentroDAO {

    /** Inserta y devuelve el centro con su id generado. */
    public CentroAcopio insertar(CentroAcopio c) throws PersistenciaException {
        String sql = "INSERT INTO CENTRO (NOMBRE, CIUDAD, PROVINCIA, LATITUD, LONGITUD, STOCK_TON) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = ConexionBD.obtenerConexion()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getCiudad());
            ps.setString(3, c.getProvincia());
            ps.setDouble(4, c.getLatitud());
            ps.setDouble(5, c.getLongitud());
            ps.setDouble(6, c.getStockToneladas());
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    return c.conId(llaves.getInt(1));
                }
            }
            return c;
        } catch (SQLException ex) {
            if (ex.getSQLState() != null && ex.getSQLState().startsWith("23")) {
                throw new PersistenciaException("Ya existe un centro con el nombre " + c.getNombre() + ".", ex);
            }
            throw new PersistenciaException("No se pudo registrar el centro.", ex);
        }
    }

    public void actualizar(CentroAcopio c) throws PersistenciaException {
        String sql = "UPDATE CENTRO SET NOMBRE = ?, CIUDAD = ?, PROVINCIA = ?, LATITUD = ?, LONGITUD = ?, "
                + "STOCK_TON = ? WHERE ID = ?";
        try (PreparedStatement ps = ConexionBD.obtenerConexion().prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getCiudad());
            ps.setString(3, c.getProvincia());
            ps.setDouble(4, c.getLatitud());
            ps.setDouble(5, c.getLongitud());
            ps.setDouble(6, c.getStockToneladas());
            ps.setInt(7, c.getId());
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("No existe un centro con ID " + c.getId() + ".");
            }
        } catch (SQLException ex) {
            if (ex.getSQLState() != null && ex.getSQLState().startsWith("23")) {
                throw new PersistenciaException("Ya existe un centro con el nombre " + c.getNombre() + ".", ex);
            }
            throw new PersistenciaException("No se pudo actualizar el centro.", ex);
        }
    }

    public void eliminar(int id) throws PersistenciaException {
        try (PreparedStatement ps = ConexionBD.obtenerConexion().prepareStatement("DELETE FROM CENTRO WHERE ID = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo eliminar el centro.", ex);
        }
    }

    /** Suma (o resta, si es negativo) toneladas al stock sin permitir que quede por debajo de cero. */
    public void ajustarStock(int id, double deltaToneladas) throws PersistenciaException {
        String sql = "UPDATE CENTRO SET STOCK_TON = STOCK_TON + ? WHERE ID = ? AND STOCK_TON + ? >= 0";
        try (PreparedStatement ps = ConexionBD.obtenerConexion().prepareStatement(sql)) {
            ps.setDouble(1, deltaToneladas);
            ps.setInt(2, id);
            ps.setDouble(3, deltaToneladas);
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("No se pudo ajustar el stock: el centro no existe o el stock quedaría negativo.");
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo ajustar el stock del centro.", ex);
        }
    }

    public List<CentroAcopio> listarTodos() throws PersistenciaException {
        List<CentroAcopio> resultado = new ArrayList<>();
        try (Statement st = ConexionBD.obtenerConexion().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM CENTRO ORDER BY ID")) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo obtener el listado de centros.", ex);
        }
        return resultado;
    }

    public CentroAcopio buscarPorId(int id) throws PersistenciaException {
        try (PreparedStatement ps = ConexionBD.obtenerConexion().prepareStatement("SELECT * FROM CENTRO WHERE ID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo consultar el centro.", ex);
        }
    }

    public int contarVehiculos(int centroId) throws PersistenciaException {
        try (PreparedStatement ps = ConexionBD.obtenerConexion()
                .prepareStatement("SELECT COUNT(*) FROM VEHICULO WHERE CENTRO_ID = ?")) {
            ps.setInt(1, centroId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo contar los vehículos del centro.", ex);
        }
    }

    private CentroAcopio mapear(ResultSet rs) throws SQLException {
        return new CentroAcopio(rs.getInt("ID"), rs.getString("NOMBRE"), rs.getString("CIUDAD"),
                rs.getString("PROVINCIA"), rs.getDouble("LATITUD"), rs.getDouble("LONGITUD"),
                rs.getDouble("STOCK_TON"));
    }
}
