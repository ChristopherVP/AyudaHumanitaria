package uce.edu.ec.ayudahumanitaria.dominio;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Viaje de un vehículo desde su centro, a través de una o varias comunidades, y de regreso al centro.
 * Es inmutable. El estado almacenado solo distingue "cancelado"; para el resto se usa {@link #estadoEn}.
 */
public record Viaje(
        int id,
        CentroAcopio centro,
        String vehiculoId,
        double capacidadToneladas,
        double cargaToneladas,
        List<Parada> paradas,
        double distanciaKm,
        LocalDateTime salida,
        LocalDateTime regreso,
        boolean cancelado) {

    public Viaje {
        paradas = List.copyOf(paradas);
    }

    public Viaje conId(int nuevoId) {
        return new Viaje(nuevoId, centro, vehiculoId, capacidadToneladas, cargaToneladas, paradas,
                distanciaKm, salida, regreso, cancelado);
    }

    public Viaje comoCancelado() {
        return new Viaje(id, centro, vehiculoId, capacidadToneladas, cargaToneladas, paradas,
                distanciaKm, salida, regreso, true);
    }

    public EstadoViaje estadoEn(LocalDateTime ahora) {
        return cancelado ? EstadoViaje.CANCELADO : EstadoViaje.segunReloj(salida, regreso, ahora);
    }

    public Duration duracion() {
        return Duration.between(salida, regreso);
    }

    /** Porcentaje de la capacidad del vehículo que se utiliza. */
    public double ocupacion() {
        return capacidadToneladas > 0 ? cargaToneladas / capacidadToneladas : 0;
    }

    /** Ruta en texto: "Centro → Comunidad 1 → Comunidad 2 → Centro". */
    public String rutaTexto() {
        String nombreCentro = centro == null ? "Centro" : centro.getNombre();
        String intermedias = paradas.stream().map(Parada::comunidadNombre).collect(Collectors.joining(" → "));
        return nombreCentro + " → " + intermedias + " → " + nombreCentro;
    }

    /** Nombres de las comunidades visitadas separados por coma. */
    public String comunidadesTexto() {
        return paradas.stream().map(Parada::comunidadNombre).collect(Collectors.joining(", "));
    }
}
