package uce.edu.ec.ayudahumanitaria.dominio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resume qué necesita cada centro y propone qué vehículos enviar. Usa <b>Greedy</b>: asigna cada comunidad al
 * centro más cercano, limita lo enviable por el stock y toma los vehículos de mayor capacidad hasta cubrir la
 * carga; si no alcanzan, reparte viajes adicionales entre ellos. Es solo una sugerencia: el usuario decide.
 */
public final class RecomendadorEnvio {

    private static final double TOLERANCIA_TON = 0.05;
    /** Viajes extra que se sugieren por vehículo como máximo. */
    private static final int MAX_VIAJES_SUGERIDOS = 3;

    private RecomendadorEnvio() {
    }

    /** Situación de un centro: lo que necesitan sus comunidades, lo que tiene y lo que puede mover su flota. */
    public record ResumenCentro(CentroAcopio centro, List<Comunidad> comunidades, double demandaToneladas,
                                double stockToneladas, double enviableToneladas, List<Vehiculo> flota) {
        public double capacidadFlotaToneladas() {
            return flota.stream().mapToDouble(Vehiculo::getCapacidadToneladas).sum();
        }
    }

    /**
     * @param seleccion   vehículos y viajes sugeridos
     * @param explicacion una línea por centro
     * @param motivos     id de vehículo → por qué se sugiere (para mostrarlo junto a cada vehículo)
     */
    public record Recomendacion(SeleccionEnvio seleccion, List<String> explicacion, Map<String, String> motivos) {
    }

    /** Solo el resumen del centro indicado (o todos si {@code centroId} es {@code null}). */
    public static List<ResumenCentro> filtrarPorCentro(List<ResumenCentro> resumenes, Integer centroId) {
        if (centroId == null) {
            return resumenes;
        }
        return resumenes.stream().filter(r -> r.centro().getId() == centroId).toList();
    }

    /** Un resumen por centro, con las necesidades pendientes (ya descontado lo planificado) de sus comunidades. */
    public static List<ResumenCentro> resumenPorCentro(List<CentroAcopio> centros, Comunidad[] comunidades,
                                                       Map<Integer, Double> yaPlanificado, Vehiculo[] vehiculos) {
        Map<Integer, List<Comunidad>> porCentro = new LinkedHashMap<>();
        for (CentroAcopio c : centros) {
            porCentro.put(c.getId(), new ArrayList<>());
        }
        for (Comunidad c : comunidades) {
            double faltante = c.getRecursosRequeridos() - yaPlanificado.getOrDefault(c.getId(), 0.0);
            if (faltante <= TOLERANCIA_TON || !c.tieneUbicacion()) {
                continue;
            }
            CentroAcopio centro = PlanificadorDistribucion.centroMasCercano(centros, c);
            if (centro != null) {
                porCentro.get(centro.getId()).add(c.conRecursos(faltante));
            }
        }
        List<ResumenCentro> resumenes = new ArrayList<>();
        for (CentroAcopio centro : centros) {
            List<Comunidad> necesitadas = porCentro.get(centro.getId());
            necesitadas.sort(Comparator.comparingInt(Comunidad::getPrioridad).reversed()
                    .thenComparing(Comparator.comparingDouble(Comunidad::calcularBeneficio).reversed()));
            double demanda = necesitadas.stream().mapToDouble(Comunidad::getRecursosRequeridos).sum();
            List<Vehiculo> flota = new ArrayList<>();
            for (Vehiculo v : vehiculos) {
                if (v.getCentroId() == centro.getId() && v.getCapacidadToneladas() > TOLERANCIA_TON) {
                    flota.add(v);
                }
            }
            resumenes.add(new ResumenCentro(centro, List.copyOf(necesitadas), demanda, centro.getStockToneladas(),
                    Math.min(demanda, centro.getStockToneladas()), List.copyOf(flota)));
        }
        return resumenes;
    }

    /** Propone vehículos y viajes por centro para mover lo enviable. */
    public static Recomendacion recomendar(List<ResumenCentro> resumenes) {
        Map<String, Integer> viajes = new LinkedHashMap<>();
        List<String> explicacion = new ArrayList<>();
        Map<String, String> motivos = new LinkedHashMap<>();
        for (ResumenCentro r : resumenes) {
            if (r.enviableToneladas() <= TOLERANCIA_TON) {
                continue;
            }
            if (r.flota().isEmpty()) {
                explicacion.add(String.format(Locale.US, "%s: hay %.1f t por enviar pero el centro no tiene vehículos.",
                        r.centro().getNombre(), r.enviableToneladas()));
                continue;
            }
            List<Vehiculo> porCapacidad = new ArrayList<>(r.flota());
            porCapacidad.sort(Comparator.comparingDouble(Vehiculo::getCapacidadToneladas).reversed()
                    .thenComparing(Vehiculo::getId));
            Map<String, Integer> locales = new LinkedHashMap<>();
            double cubierto = 0;
            for (Vehiculo v : porCapacidad) {
                if (cubierto >= r.enviableToneladas() - TOLERANCIA_TON) {
                    break;
                }
                locales.put(v.getId(), 1);
                cubierto += v.getCapacidadToneladas();
                motivos.put(v.getId(), String.format(Locale.US,
                        "Mayor capacidad libre de %s (%.1f t): con él se cubren %.1f de %.1f t a enviar",
                        r.centro().getCiudad(), v.getCapacidadToneladas(), Math.min(cubierto, r.enviableToneladas()),
                        r.enviableToneladas()));
            }
            // Si no alcanza con un viaje por vehículo, se agregan viajes en rondas (greedy por capacidad).
            boolean progreso = true;
            while (cubierto < r.enviableToneladas() - TOLERANCIA_TON && progreso) {
                progreso = false;
                for (Vehiculo v : porCapacidad) {
                    int actuales = locales.getOrDefault(v.getId(), 0);
                    if (cubierto >= r.enviableToneladas() - TOLERANCIA_TON) {
                        break;
                    }
                    if (actuales < MAX_VIAJES_SUGERIDOS) {
                        locales.put(v.getId(), actuales + 1);
                        cubierto += v.getCapacidadToneladas();
                        motivos.put(v.getId(), String.format(Locale.US,
                                "La flota de %s no alcanza en un viaje: repite hasta %d viaje(s) para cubrir %.1f t",
                                r.centro().getCiudad(), actuales + 1, r.enviableToneladas()));
                        progreso = true;
                    }
                }
            }
            viajes.putAll(locales);
            int totalViajes = locales.values().stream().mapToInt(Integer::intValue).sum();
            explicacion.add(String.format(Locale.US,
                    "%s: enviar %.1f t (necesidad %.1f t, stock %.1f t) → %d vehículo(s), %d viaje(s), capacidad %.1f t%s.",
                    r.centro().getNombre(), r.enviableToneladas(), r.demandaToneladas(), r.stockToneladas(),
                    locales.size(), totalViajes, cubierto,
                    cubierto < r.enviableToneladas() - TOLERANCIA_TON ? " (insuficiente: quedará un remanente)" : ""));
        }
        if (explicacion.isEmpty()) {
            explicacion.add("No hay necesidades pendientes con stock disponible: no hace falta enviar vehículos.");
        }
        return new Recomendacion(new SeleccionEnvio(viajes), List.copyOf(explicacion), Map.copyOf(motivos));
    }
}
