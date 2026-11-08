package uce.edu.ec.ayudahumanitaria.dominio;

/**
 * Vehículo de carga. La capacidad se registra en kilogramos; los algoritmos de distribución
 * trabajan en toneladas, por eso existe {@link #getCapacidadToneladas()}.
 * <p>
 * Cada vehículo pertenece a un centro de acopio ({@code centroId}); 0 significa "sin centro".
 */
public class Vehiculo {

    public static final double KG_POR_TONELADA = 1000.0;

    private String id;
    private double capacidad;
    private boolean asignado;
    private String comunidadAsignada;
    private int centroId;
    private String centroNombre = "";

    public Vehiculo(String id, double capacidad) {
        this.id = id;
        this.capacidad = capacidad;
        this.asignado = false;
        this.comunidadAsignada = null;
    }

    public Vehiculo(String id, double capacidad, int centroId) {
        this(id, capacidad);
        this.centroId = centroId;
    }

    /** Copia de este vehículo expresando la capacidad en toneladas (para los algoritmos). */
    public Vehiculo enToneladas() {
        Vehiculo copia = new Vehiculo(id, getCapacidadToneladas(), centroId);
        copia.asignado = asignado;
        copia.comunidadAsignada = comunidadAsignada;
        copia.centroNombre = centroNombre;
        return copia;
    }

    public double getCapacidadToneladas() {
        return capacidad / KG_POR_TONELADA;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(double capacidad) {
        this.capacidad = capacidad;
    }

    public boolean isAsignado() {
        return asignado;
    }

    public void setAsignado(boolean asignado) {
        this.asignado = asignado;
    }

    public String getComunidadAsignada() {
        return comunidadAsignada;
    }

    public void setComunidadAsignada(String comunidadAsignada) {
        this.comunidadAsignada = comunidadAsignada;
    }

    public int getCentroId() {
        return centroId;
    }

    public void setCentroId(int centroId) {
        this.centroId = centroId;
    }

    public String getCentroNombre() {
        return centroNombre;
    }

    public void setCentroNombre(String centroNombre) {
        this.centroNombre = centroNombre == null ? "" : centroNombre;
    }

    @Override
    public String toString() {
        return id + " (capacidad=" + capacidad
                + ", asignado=" + asignado
                + ", comunidad=" + comunidadAsignada + ")";
    }
}
