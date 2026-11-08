package uce.edu.ec.ayudahumanitaria.dominio;

/**
 * Supuestos operativos de la simulación (todos configurables). Los valores por defecto representan
 * transporte pesado por carretera en el Ecuador (sierra y Amazonía).
 *
 * @param velocidadPromedioKmH     velocidad media de circulación, incluyendo curvas y tráfico
 * @param maxHorasConduccionDia    horas máximas de conducción antes de que el conductor deba descansar
 * @param descansoHoras            duración del descanso (pernocta) cuando se alcanza el máximo diario
 * @param servicioBaseMinutos      tiempo fijo de descarga en cada comunidad
 * @param servicioMinutosPorTon    tiempo adicional de descarga por tonelada entregada
 * @param preparacionHoras         tiempo que necesita un vehículo en el centro entre un viaje y el siguiente
 * @param maxParadasPorViaje       paradas máximas por viaje (el backtracking crece de forma factorial)
 */
public record ParametrosSimulacion(
        double velocidadPromedioKmH,
        double maxHorasConduccionDia,
        double descansoHoras,
        double servicioBaseMinutos,
        double servicioMinutosPorTon,
        double preparacionHoras,
        int maxParadasPorViaje) {

    public ParametrosSimulacion {
        if (velocidadPromedioKmH <= 0 || maxHorasConduccionDia <= 0 || descansoHoras < 0
                || servicioBaseMinutos < 0 || servicioMinutosPorTon < 0 || preparacionHoras < 0
                || maxParadasPorViaje < 1 || maxParadasPorViaje > 8) {
            throw new IllegalArgumentException("Parámetros de simulación fuera de rango.");
        }
    }

    public static ParametrosSimulacion porDefecto() {
        return new ParametrosSimulacion(50.0, 8.0, 10.0, 30.0, 10.0, 2.0, 7);
    }
}
