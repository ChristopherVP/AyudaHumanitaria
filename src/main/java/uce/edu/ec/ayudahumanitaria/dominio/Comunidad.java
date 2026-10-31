package uce.edu.ec.ayudahumanitaria.dominio;

public class Comunidad {

    private int id;
    private String nombre;
    private int habitantes;
    private int prioridad;
    private double recursosRequeridos;

    public Comunidad(String nombre, int habitantes, int prioridad, double recursosRequeridos) {
        this.nombre = nombre;
        this.habitantes = habitantes;
        this.prioridad = prioridad;
        this.recursosRequeridos = recursosRequeridos;
    }

    public Comunidad(int id, String nombre, int habitantes, int prioridad, double recursosRequeridos) {
        this.id = id;
        this.nombre = nombre;
        this.habitantes = habitantes;
        this.prioridad = prioridad;
        this.recursosRequeridos = recursosRequeridos;
    }

    public double calcularBeneficio() {
        return (double) habitantes * prioridad;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getHabitantes() {
        return habitantes;
    }

    public void setHabitantes(int habitantes) {
        this.habitantes = habitantes;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }

    public double getRecursosRequeridos() {
        return recursosRequeridos;
    }

    public void setRecursosRequeridos(double recursosRequeridos) {
        this.recursosRequeridos = recursosRequeridos;
    }

    @Override
    public String toString() {
        return nombre + " (habitantes=" + habitantes
                + ", prioridad=" + prioridad
                + ", recursos=" + recursosRequeridos
                + ", beneficio=" + calcularBeneficio() + ")";
    }
}
