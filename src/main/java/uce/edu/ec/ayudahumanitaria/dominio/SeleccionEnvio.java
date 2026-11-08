package uce.edu.ec.ayudahumanitaria.dominio;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Decisión del usuario: qué vehículos salen y cuántos viajes puede hacer cada uno.
 * Un vehículo ausente (o con 0 viajes) no sale. Es inmutable.
 *
 * @param viajesPorVehiculo id de vehículo → viajes máximos (1 a {@link #MAX_VIAJES_POR_VEHICULO})
 */
public record SeleccionEnvio(Map<String, Integer> viajesPorVehiculo) {

    public static final int MAX_VIAJES_POR_VEHICULO = 5;

    public SeleccionEnvio {
        Map<String, Integer> copia = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : viajesPorVehiculo.entrySet()) {
            int viajes = e.getValue() == null ? 0 : e.getValue();
            if (viajes < 0 || viajes > MAX_VIAJES_POR_VEHICULO) {
                throw new IllegalArgumentException("Los viajes de " + e.getKey() + " deben estar entre 0 y "
                        + MAX_VIAJES_POR_VEHICULO + ".");
            }
            if (viajes > 0) {
                copia.put(e.getKey(), viajes);
            }
        }
        viajesPorVehiculo = Collections.unmodifiableMap(copia);
    }

    public int viajesDe(String vehiculoId) {
        return viajesPorVehiculo.getOrDefault(vehiculoId, 0);
    }

    public boolean incluye(String vehiculoId) {
        return viajesDe(vehiculoId) > 0;
    }

    public int totalVehiculos() {
        return viajesPorVehiculo.size();
    }

    public int totalViajes() {
        return viajesPorVehiculo.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean estaVacia() {
        return viajesPorVehiculo.isEmpty();
    }
}
