package uce.edu.ec.ayudahumanitaria.dominio;

import java.time.LocalDateTime;

/** Estado de un viaje de distribución respecto al reloj real. */
public enum EstadoViaje {
    PLANIFICADO("Planificado"),
    EN_RUTA("En ruta"),
    COMPLETADO("Completado"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoViaje(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Estado que corresponde a un viaje no cancelado según la hora actual. */
    public static EstadoViaje segunReloj(LocalDateTime salida, LocalDateTime regreso, LocalDateTime ahora) {
        if (ahora.isBefore(salida)) {
            return PLANIFICADO;
        }
        return ahora.isBefore(regreso) ? EN_RUTA : COMPLETADO;
    }

    /** {@code true} si el viaje todavía ocupa al vehículo (planificado o en ruta). */
    public boolean estaActivo() {
        return this == PLANIFICADO || this == EN_RUTA;
    }

    public static EstadoViaje desdeTexto(String texto) {
        try {
            return EstadoViaje.valueOf(texto);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return PLANIFICADO;
        }
    }
}
