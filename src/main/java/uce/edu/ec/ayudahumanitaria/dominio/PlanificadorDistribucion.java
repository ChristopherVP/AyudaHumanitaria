package uce.edu.ec.ayudahumanitaria.dominio;

import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoAsignacion;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoMochila;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoRuta;
import uce.edu.ec.ayudahumanitaria.dominio.PlanDistribucion.EstadisticasAlgoritmos;
import uce.edu.ec.ayudahumanitaria.geo.GeoEcuador;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Planifica una distribución completa con fechas y horas reales usando <b>solo</b> los algoritmos del proyecto:
 * <ol>
 *   <li><b>Greedy (voraz):</b> cada comunidad queda a cargo del centro de acopio más cercano, y el stock de
 *       cada centro se reparte por prioridad descendente.</li>
 *   <li><b>Divide y Vencerás:</b> identifica la comunidad de mayor beneficio de cada centro.</li>
 *   <li><b>Programación dinámica (mochila 0/1):</b> arma los grupos de comunidades que caben en un vehículo
 *       maximizando el beneficio.</li>
 *   <li><b>Branch and Bound:</b> asigna vehículos a grupos minimizando la capacidad desperdiciada.</li>
 *   <li><b>Backtracking:</b> encuentra el orden de visita de menor distancia dentro de cada viaje.</li>
 * </ol>
 * La clase no usa base de datos ni interfaz: recibe los datos y devuelve un {@link PlanDistribucion}
 * inmutable, por lo que es fácil de probar.
 */
public final class PlanificadorDistribucion {

    /** Diferencias menores a 50 kg se consideran ruido numérico. */
    private static final double TOLERANCIA_TON = 0.05;
    /** Grupos que se ofrecen a Branch and Bound en cada oleada (limita el tamaño del árbol de búsqueda). */
    private static final int GRUPOS_POR_OLEADA = 6;
    private static final int MAX_OLEADAS = 40;
    private static final String ETIQUETA_CENTRO = "CENTRO";

    private PlanificadorDistribucion() {
    }

    // ------------------------------------------------------------------ tipos internos

    private record Carga(Comunidad comunidad, double toneladas) {
    }

    private record Grupo(List<Carga> cargas, double toneladas) {
        double beneficio() {
            return cargas.stream().mapToDouble(c -> c.comunidad().calcularBeneficio()).sum();
        }
    }

    private static final class Contadores {
        long nodosBacktracking;
        long podasBacktracking;
        long nodosBranchAndBound;
        long podasBranchAndBound;
    }

    // ------------------------------------------------------------------ API pública

    /**
     * @param centros       centros de acopio con su stock actual
     * @param comunidades   todas las comunidades registradas
     * @param yaPlanificado toneladas ya comprometidas por comunidad (id → toneladas) en planes vigentes
     * @param vehiculos     flota completa (cada vehículo indica su centro)
     * @param libreDesde    hora desde la que cada vehículo (id) queda libre; los ausentes están libres ya
     * @param salida        fecha y hora de salida elegida
     * @param ahora         hora actual (se guarda como fecha de creación del plan)
     */
    public static PlanDistribucion planificar(List<CentroAcopio> centros, Comunidad[] comunidades,
                                              Map<Integer, Double> yaPlanificado, Vehiculo[] vehiculos,
                                              Map<String, LocalDateTime> libreDesde, LocalDateTime salida,
                                              LocalDateTime ahora, ParametrosSimulacion parametros) {
        List<Pendiente> pendientes = new ArrayList<>();
        List<String> bitacora = new ArrayList<>();
        List<Viaje> viajes = new ArrayList<>();
        Contadores contadores = new Contadores();

        // 1) Greedy: cada comunidad con necesidad pendiente va al centro más cercano.
        Map<Integer, List<Comunidad>> porCentro = new LinkedHashMap<>();
        for (Comunidad c : comunidades) {
            double faltante = c.getRecursosRequeridos() - yaPlanificado.getOrDefault(c.getId(), 0.0);
            if (faltante <= TOLERANCIA_TON) {
                continue;
            }
            if (!c.tieneUbicacion()) {
                pendientes.add(new Pendiente(c.getId(), c.getNombre(), "—", faltante, "Sin ubicación registrada"));
                continue;
            }
            CentroAcopio centro = centroMasCercano(centros, c);
            if (centro == null) {
                pendientes.add(new Pendiente(c.getId(), c.getNombre(), "—", faltante, "No hay centros de acopio"));
                continue;
            }
            porCentro.computeIfAbsent(centro.getId(), k -> new ArrayList<>()).add(c.conRecursos(faltante));
        }

        if (porCentro.isEmpty()) {
            bitacora.add("No hay necesidades pendientes que planificar.");
        }

        for (CentroAcopio centro : centros) {
            List<Comunidad> necesidades = porCentro.get(centro.getId());
            if (necesidades == null) {
                continue;
            }
            List<Vehiculo> flota = new ArrayList<>();
            for (Vehiculo v : vehiculos) {
                if (v.getCentroId() == centro.getId() && v.getCapacidadToneladas() > TOLERANCIA_TON) {
                    flota.add(v);
                }
            }
            planificarCentro(centro, necesidades, flota, libreDesde, salida, parametros,
                    viajes, pendientes, bitacora, contadores);
        }

        Viaje[] ordenados = viajes.toArray(new Viaje[0]);
        Algoritmos.mergeSort(ordenados, Comparator.comparing(Viaje::salida)
                .thenComparing(v -> v.centro().getNombre()).thenComparing(Viaje::vehiculoId));

        EstadisticasAlgoritmos estadisticas = new EstadisticasAlgoritmos(contadores.nodosBacktracking,
                contadores.podasBacktracking, contadores.nodosBranchAndBound, contadores.podasBranchAndBound);
        return new PlanDistribucion(0, ahora, salida, Arrays.asList(ordenados), pendientes, bitacora, estadisticas);
    }

    /** Centro de acopio más cercano por carretera (empates: el de menor id). {@code null} si no hay centros. */
    public static CentroAcopio centroMasCercano(List<CentroAcopio> centros, Comunidad comunidad) {
        CentroAcopio mejor = null;
        double mejorDistancia = Double.MAX_VALUE;
        for (CentroAcopio centro : centros) {
            double d = centro.distanciaCarreteraKm(comunidad.getLatitud(), comunidad.getLongitud());
            if (d < mejorDistancia - 1e-9 || (Math.abs(d - mejorDistancia) <= 1e-9 && mejor != null
                    && centro.getId() < mejor.getId())) {
                mejorDistancia = d;
                mejor = centro;
            }
        }
        return mejor;
    }

    // ------------------------------------------------------------------ planificación por centro

    private static void planificarCentro(CentroAcopio centro, List<Comunidad> necesidades, List<Vehiculo> flota,
                                         Map<String, LocalDateTime> libreDesde, LocalDateTime salida,
                                         ParametrosSimulacion p, List<Viaje> viajes, List<Pendiente> pendientes,
                                         List<String> bitacora, Contadores contadores) {
        Comunidad[] ordenadas = necesidades.toArray(new Comunidad[0]);
        Algoritmos.quickSort(ordenadas, Comparator.comparingInt(Comunidad::getPrioridad).reversed()
                .thenComparing(Comparator.comparingDouble(Comunidad::calcularBeneficio).reversed())
                .thenComparingInt(Comunidad::getId));

        double demanda = 0;
        for (Comunidad c : ordenadas) {
            demanda += c.getRecursosRequeridos();
        }
        Comunidad critica = Algoritmos.comunidadMayorBeneficio(ordenadas);
        bitacora.add(String.format(Locale.US,
                "[%s] Greedy: atiende %d comunidad(es) por cercanía; demanda %.2f t, stock %.2f t. "
                        + "Divide y Vencerás: la comunidad de mayor beneficio es %s.",
                centro.getNombre(), ordenadas.length, demanda, centro.getStockToneladas(),
                critica == null ? "—" : critica.getNombre()));

        if (flota.isEmpty()) {
            for (Comunidad c : ordenadas) {
                pendientes.add(new Pendiente(c.getId(), c.getNombre(), centro.getNombre(),
                        c.getRecursosRequeridos(), "El centro no tiene vehículos"));
            }
            bitacora.add("[" + centro.getNombre() + "] Sin vehículos registrados: no se generan viajes.");
            return;
        }

        // 2) Greedy: reparto del stock por prioridad (las comunidades viajan al algoritmo con su id como nombre).
        Comunidad[] paraGreedy = new Comunidad[ordenadas.length];
        for (int i = 0; i < ordenadas.length; i++) {
            paraGreedy[i] = clave(ordenadas[i], ordenadas[i].getRecursosRequeridos());
        }
        ResultadoDistribucion reparto = Algoritmos.distribuirSuministros(paraGreedy, centro.getStockToneladas());
        Map<Integer, Double> asignado = new HashMap<>();
        for (int i = 0; i < reparto.nombres.length; i++) {
            asignado.put(Integer.parseInt(reparto.nombres[i]), reparto.asignado[i]);
        }

        double maxCapacidad = 0;
        for (Vehiculo v : flota) {
            maxCapacidad = Math.max(maxCapacidad, v.getCapacidadToneladas());
        }

        // Cargas por comunidad; si una carga excede el vehículo más grande se parte en viajes de carga completa.
        List<Grupo> grupos = new ArrayList<>();
        List<Carga> sueltas = new ArrayList<>();
        for (Comunidad c : ordenadas) {
            double recibido = asignado.getOrDefault(c.getId(), 0.0);
            double faltante = c.getRecursosRequeridos();
            if (recibido <= TOLERANCIA_TON) {
                pendientes.add(new Pendiente(c.getId(), c.getNombre(), centro.getNombre(), faltante,
                        "Sin stock suficiente en el centro"));
                continue;
            }
            if (faltante - recibido > TOLERANCIA_TON) {
                pendientes.add(new Pendiente(c.getId(), c.getNombre(), centro.getNombre(), faltante - recibido,
                        "Stock insuficiente: entrega parcial"));
            }
            double restante = recibido;
            while (restante > maxCapacidad + 1e-9) {
                grupos.add(new Grupo(List.of(new Carga(c, maxCapacidad)), maxCapacidad));
                restante -= maxCapacidad;
            }
            if (restante > TOLERANCIA_TON) {
                sueltas.add(new Carga(c, restante));
            }
        }

        // 3) Mochila 0/1: forma grupos de comunidades que caben en un vehículo (resolución de 0.1 t).
        int capacidadUnidades = (int) Math.floor(maxCapacidad * 10 + 1e-9);
        List<Carga> restantes = new ArrayList<>(sueltas);
        while (!restantes.isEmpty()) {
            List<Carga> candidatas = new ArrayList<>();
            for (Carga carga : new ArrayList<>(restantes)) {
                if (unidades(carga.toneladas()) > capacidadUnidades) {
                    grupos.add(new Grupo(List.of(carga), carga.toneladas()));
                    restantes.remove(carga);
                } else {
                    candidatas.add(carga);
                }
            }
            if (candidatas.isEmpty()) {
                break;
            }

            Comunidad[] escaladas = new Comunidad[candidatas.size()];
            Map<Integer, Carga> porId = new HashMap<>();
            for (int i = 0; i < candidatas.size(); i++) {
                Carga carga = candidatas.get(i);
                escaladas[i] = clave(carga.comunidad(), unidades(carga.toneladas()));
                porId.put(carga.comunidad().getId(), carga);
            }
            ResultadoMochila mochila = Algoritmos.resolverMochila(escaladas, capacidadUnidades);

            List<Carga> elegidas = new ArrayList<>();
            for (Comunidad seleccionada : mochila.seleccionadas) {
                elegidas.add(porId.get(seleccionada.getId()));
            }
            if (elegidas.isEmpty()) {
                elegidas.add(candidatas.get(0));
            }
            if (elegidas.size() > p.maxParadasPorViaje()) {
                Carga[] arreglo = elegidas.toArray(new Carga[0]);
                Algoritmos.quickSort(arreglo,
                        Comparator.comparingDouble((Carga carga) -> carga.comunidad().calcularBeneficio()).reversed());
                elegidas = new ArrayList<>(Arrays.asList(arreglo).subList(0, p.maxParadasPorViaje()));
            }
            restantes.removeAll(elegidas);
            double suma = 0;
            for (Carga carga : elegidas) {
                suma += carga.toneladas();
            }
            grupos.add(new Grupo(elegidas, suma));
        }

        Grupo[] arregloGrupos = grupos.toArray(new Grupo[0]);
        Algoritmos.quickSort(arregloGrupos, Comparator.comparingDouble(Grupo::beneficio).reversed());
        List<Grupo> cola = new ArrayList<>(Arrays.asList(arregloGrupos));
        bitacora.add(String.format(Locale.US,
                "[%s] Mochila: %d grupo(s) de entrega formados para vehículos de hasta %.1f t.",
                centro.getNombre(), cola.size(), maxCapacidad));

        // 4) Branch and Bound por oleadas: asigna vehículos a grupos minimizando el desperdicio de capacidad.
        Map<String, LocalDateTime> libre = new HashMap<>();
        for (Vehiculo v : flota) {
            LocalDateTime desde = libreDesde.getOrDefault(v.getId(), salida);
            libre.put(v.getId(), desde.isAfter(salida) ? desde : salida);
        }
        Vehiculo[] flotaEnToneladas = new Vehiculo[flota.size()];
        for (int i = 0; i < flota.size(); i++) {
            flotaEnToneladas[i] = flota.get(i).enToneladas();
        }

        int oleada = 0;
        while (!cola.isEmpty() && oleada < MAX_OLEADAS) {
            oleada++;
            int n = Math.min(GRUPOS_POR_OLEADA, cola.size());
            List<Grupo> lote = new ArrayList<>(cola.subList(0, n));
            Comunidad[] seudoComunidades = new Comunidad[n];
            for (int i = 0; i < n; i++) {
                seudoComunidades[i] = new Comunidad(i, "G" + i, 1, 1, lote.get(i).toneladas());
            }
            ResultadoAsignacion asignacion = Algoritmos.asignarVehiculosBranchAndBound(flotaEnToneladas,
                    seudoComunidades);
            contadores.nodosBranchAndBound += asignacion.nodosExplorados;
            contadores.podasBranchAndBound += asignacion.nodosPodados;

            if (asignacion.total == 0) {
                for (Grupo g : lote) {
                    for (Carga carga : g.cargas()) {
                        pendientes.add(new Pendiente(carga.comunidad().getId(), carga.comunidad().getNombre(),
                                centro.getNombre(), carga.toneladas(),
                                "Ningún vehículo del centro tiene capacidad suficiente"));
                    }
                }
                quitarPorIdentidad(cola, lote);
                continue;
            }

            List<Grupo> atendidos = new ArrayList<>();
            for (int k = 0; k < asignacion.total; k++) {
                Grupo grupo = lote.get(Integer.parseInt(asignacion.comunidadNombres[k].substring(1)));
                Vehiculo vehiculo = buscarVehiculo(flota, asignacion.vehiculoIds[k]);
                Viaje viaje = construirViaje(centro, vehiculo, grupo, libre.get(vehiculo.getId()), p, contadores);
                viajes.add(viaje);
                libre.put(vehiculo.getId(), viaje.regreso().plusSeconds(Math.round(p.preparacionHoras() * 3600)));
                atendidos.add(grupo);
                bitacora.add(String.format(Locale.US,
                        "[%s] Branch and Bound: %s (%.1f t) ← %s (%.2f t). Backtracking: %s, %.1f km.",
                        centro.getNombre(), vehiculo.getId(), vehiculo.getCapacidadToneladas(),
                        viaje.comunidadesTexto(), grupo.toneladas(), viaje.rutaTexto(), viaje.distanciaKm()));
            }
            quitarPorIdentidad(cola, atendidos);
        }

        for (Grupo g : cola) {
            for (Carga carga : g.cargas()) {
                pendientes.add(new Pendiente(carga.comunidad().getId(), carga.comunidad().getNombre(),
                        centro.getNombre(), carga.toneladas(), "No alcanzaron las oleadas de vehículos"));
            }
        }
    }

    // ------------------------------------------------------------------ ruta y cronograma de un viaje

    private static Viaje construirViaje(CentroAcopio centro, Vehiculo vehiculo, Grupo grupo,
                                        LocalDateTime salida, ParametrosSimulacion p, Contadores contadores) {
        List<Carga> cargas = grupo.cargas();
        int n = cargas.size();
        String[] nombres = new String[n + 1];
        double[] lat = new double[n + 1];
        double[] lon = new double[n + 1];
        nombres[0] = ETIQUETA_CENTRO;
        lat[0] = centro.getLatitud();
        lon[0] = centro.getLongitud();
        Map<String, Carga> porEtiqueta = new HashMap<>();
        for (int i = 0; i < n; i++) {
            Comunidad c = cargas.get(i).comunidad();
            nombres[i + 1] = String.valueOf(c.getId());
            lat[i + 1] = c.getLatitud();
            lon[i + 1] = c.getLongitud();
            porEtiqueta.put(nombres[i + 1], cargas.get(i));
        }

        double[][] distancias = new double[n + 1][n + 1];
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                distancias[i][j] = i == j ? 0.0 : GeoEcuador.distanciaCarreteraKm(lat[i], lon[i], lat[j], lon[j]);
            }
        }

        ResultadoRuta ruta = Algoritmos.rutaOptima(nombres, distancias);
        contadores.nodosBacktracking += ruta.nodosExplorados;
        contadores.podasBacktracking += ruta.nodosPodados;

        Reloj reloj = new Reloj(salida, p);
        double kmAcumulados = 0;
        double latPrevia = centro.getLatitud();
        double lonPrevia = centro.getLongitud();
        List<Parada> paradas = new ArrayList<>();
        for (int i = 1; i < ruta.mejorRuta.length - 1; i++) {
            Carga carga = porEtiqueta.get(ruta.mejorRuta[i]);
            Comunidad c = carga.comunidad();
            double km = GeoEcuador.distanciaCarreteraKm(latPrevia, lonPrevia, c.getLatitud(), c.getLongitud());
            reloj.conducir(km / p.velocidadPromedioKmH());
            LocalDateTime llegada = reloj.ahora();
            reloj.esperarMinutos(p.servicioBaseMinutos() + p.servicioMinutosPorTon() * carga.toneladas());
            kmAcumulados += km;
            paradas.add(new Parada(c.getId(), c.getNombre(), c.getProvincia(), c.getLatitud(), c.getLongitud(),
                    i, carga.toneladas(), km, kmAcumulados, llegada, reloj.ahora()));
            latPrevia = c.getLatitud();
            lonPrevia = c.getLongitud();
        }
        double kmRegreso = GeoEcuador.distanciaCarreteraKm(latPrevia, lonPrevia, centro.getLatitud(),
                centro.getLongitud());
        reloj.conducir(kmRegreso / p.velocidadPromedioKmH());
        kmAcumulados += kmRegreso;

        return new Viaje(0, centro, vehiculo.getId(), vehiculo.getCapacidadToneladas(), grupo.toneladas(),
                paradas, kmAcumulados, salida, reloj.ahora(), false);
    }

    /**
     * Reloj de un viaje: avanza con la conducción respetando el máximo de horas por jornada
     * (al alcanzarlo, el conductor pernocta el tiempo de descanso configurado).
     */
    private static final class Reloj {
        private final ParametrosSimulacion p;
        private LocalDateTime ahora;
        private double horasConducidasHoy;

        Reloj(LocalDateTime inicio, ParametrosSimulacion parametros) {
            this.ahora = inicio;
            this.p = parametros;
        }

        LocalDateTime ahora() {
            return ahora;
        }

        void conducir(double horas) {
            double restante = horas;
            while (restante > 1e-9) {
                double disponible = p.maxHorasConduccionDia() - horasConducidasHoy;
                if (disponible <= 1e-9) {
                    ahora = ahora.plusSeconds(Math.round(p.descansoHoras() * 3600));
                    horasConducidasHoy = 0;
                    continue;
                }
                double usar = Math.min(disponible, restante);
                ahora = ahora.plusSeconds(Math.round(usar * 3600));
                horasConducidasHoy += usar;
                restante -= usar;
            }
        }

        void esperarMinutos(double minutos) {
            ahora = ahora.plusSeconds(Math.round(minutos * 60));
        }
    }

    // ------------------------------------------------------------------ utilidades

    /** Copia que viaja a los algoritmos: el nombre es el id (único) y los recursos son los indicados. */
    private static Comunidad clave(Comunidad c, double recursos) {
        return new Comunidad(c.getId(), String.valueOf(c.getId()), c.getProvincia(), c.getHabitantes(),
                c.getPrioridad(), recursos, c.getLatitud(), c.getLongitud());
    }

    /** Toneladas expresadas en décimas de tonelada, redondeadas hacia arriba (sin error de coma flotante). */
    private static int unidades(double toneladas) {
        return (int) Math.ceil(toneladas * 10 - 1e-9);
    }

    /** Quita de la lista los elementos que sean la misma instancia (los grupos iguales por valor no se confunden). */
    private static <T> void quitarPorIdentidad(List<T> lista, List<T> aQuitar) {
        lista.removeIf(elemento -> aQuitar.stream().anyMatch(x -> x == elemento));
    }

    private static Vehiculo buscarVehiculo(List<Vehiculo> flota, String id) {
        for (Vehiculo v : flota) {
            if (v.getId().equals(id)) {
                return v;
            }
        }
        throw new IllegalStateException("Vehículo no encontrado: " + id);
    }
}
