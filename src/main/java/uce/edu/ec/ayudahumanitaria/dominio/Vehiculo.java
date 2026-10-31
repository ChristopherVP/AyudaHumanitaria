package uce.edu.ec.ayudahumanitaria.dominio;

public class Vehiculo {

    private String id;
    private double capacidad;
    private boolean asignado;
    private String comunidadAsignada;

    public Vehiculo(String id, double capacidad) {
        this.id = id;
        this.capacidad = capacidad;
        this.asignado = false;
        this.comunidadAsignada = null;
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

    @Override
    public String toString() {
        return id + " (capacidad=" + capacidad
                + ", asignado=" + asignado
                + ", comunidad=" + comunidadAsignada + ")";
    }
}
