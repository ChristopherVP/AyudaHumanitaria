package uce.edu.ec.ayudahumanitaria.dominio;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de planificar una distribución: viajes con su cronograma real, necesidades que quedaron
 * pendientes y una bitácora de las decisiones tomadas por los algoritmos. Es inmutable.
 *
 * @param id                 0 mientras es solo una simulación; se asigna al confirmarla (guardarla)
 * @param salidaProgramada   fecha y hora de salida elegida por el usuario
 * @param estadisticasAlgoritmos nodos explorados/podados por backtracking y branch and bound
 */
public record PlanDistribucion(
        int id,
        LocalDateTime creadoEn,
        LocalDateTime salidaProgramada,
        List<Viaje> viajes,
        List<Pendiente> pendientes,
        List<String> bitacora,
        EstadisticasAlgoritmos estadisticasAlgoritmos) {

    /** Contadores de trabajo de los algoritmos de búsqueda exhaustiva. */
    public record EstadisticasAlgoritmos(long nodosBacktracking, long podasBacktracking,
                                         long nodosBranchAndBound, long podasBranchAndBound) {
        public static EstadisticasAlgoritmos vacias() {
            return new EstadisticasAlgoritmos(0, 0, 0, 0);
        }
    }

    public PlanDistribucion {
        viajes = List.copyOf(viajes);
        pendientes = List.copyOf(pendientes);
        bitacora = List.copyOf(bitacora);
    }

    public PlanDistribucion conId(int nuevoId, List<Viaje> viajesGuardados) {
        return new PlanDistribucion(nuevoId, creadoEn, salidaProgramada, viajesGuardados, pendientes, bitacora,
                estadisticasAlgoritmos);
    }

    public PlanDistribucion conViajes(List<Viaje> nuevosViajes) {
        return new PlanDistribucion(id, creadoEn, salidaProgramada, nuevosViajes, pendientes, bitacora,
                estadisticasAlgoritmos);
    }

    public boolean estaVacio() {
        return viajes.isEmpty();
    }

    public double totalKm() {
        return viajes.stream().mapToDouble(Viaje::distanciaKm).sum();
    }

    public double totalToneladas() {
        return viajes.stream().mapToDouble(Viaje::cargaToneladas).sum();
    }

    public double toneladasPendientes() {
        return pendientes.stream().mapToDouble(Pendiente::toneladas).sum();
    }

    /** Hora estimada en que el último vehículo regresa a su centro (o la salida si no hay viajes). */
    public LocalDateTime finEstimado() {
        return viajes.stream().map(Viaje::regreso).max(LocalDateTime::compareTo).orElse(salidaProgramada);
    }

    /** Un plan está cancelado cuando todos sus viajes lo están. */
    public boolean estaCancelado() {
        return !viajes.isEmpty() && viajes.stream().allMatch(Viaje::cancelado);
    }
}
