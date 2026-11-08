package uce.edu.ec.ayudahumanitaria.dominio;

import java.time.LocalDateTime;

/**
 * Entrega a una comunidad dentro de un viaje.
 *
 * @param comunidadId  identificador de la comunidad (0 si ya fue eliminada del sistema)
 * @param orden        posición en la ruta (1 = primera parada)
 * @param toneladas    carga entregada
 * @param kmTramo      distancia desde la parada anterior (o desde el centro)
 * @param kmAcumulados distancia recorrida desde la salida
 * @param llegada      hora estimada de llegada
 * @param salida       hora estimada de salida (llegada + tiempo de descarga)
 */
public record Parada(
        int comunidadId,
        String comunidadNombre,
        String provincia,
        double latitud,
        double longitud,
        int orden,
        double toneladas,
        double kmTramo,
        double kmAcumulados,
        LocalDateTime llegada,
        LocalDateTime salida) {
}
