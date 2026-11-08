package uce.edu.ec.ayudahumanitaria.dominio;

/**
 * Comunidad beneficiaria de la ayuda humanitaria.
 * <p>
 * Unidades: {@code recursosRequeridos} en toneladas. La ubicación (latitud/longitud) se usa para
 * dibujarla en el mapa, elegir el centro de acopio más cercano y calcular rutas. Latitud y longitud
 * iguales a 0 significan "sin ubicación registrada" (0,0 cae en el océano, fuera del Ecuador).
 */
public class Comunidad {

    private int id;
    private String nombre;
    private int habitantes;
    private int prioridad;
    private double recursosRequeridos;
    private String provincia = "";
    private double latitud;
    private double longitud;

    public Comunidad(String nombre, int habitantes, int prioridad, double recursosRequeridos) {
        this.nombre = nombre;
        this.habitantes = habitantes;
        this.prioridad = prioridad;
        this.recursosRequeridos = recursosRequeridos;
    }

    public Comunidad(int id, String nombre, int habitantes, int prioridad, double recursosRequeridos) {
        this(nombre, habitantes, prioridad, recursosRequeridos);
        this.id = id;
    }

    public Comunidad(int id, String nombre, String provincia, int habitantes, int prioridad,
                     double recursosRequeridos, double latitud, double longitud) {
        this(id, nombre, habitantes, prioridad, recursosRequeridos);
        this.provincia = provincia == null ? "" : provincia;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public double calcularBeneficio() {
        return (double) habitantes * prioridad;
    }

    /** {@code true} si la comunidad tiene coordenadas registradas. */
    public boolean tieneUbicacion() {
        return latitud != 0.0 || longitud != 0.0;
    }

    /** Copia de esta comunidad con otra cantidad de recursos (no modifica el original). */
    public Comunidad conRecursos(double nuevosRecursos) {
        return new Comunidad(id, nombre, provincia, habitantes, prioridad, nuevosRecursos, latitud, longitud);
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

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia == null ? "" : provincia;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    @Override
    public String toString() {
        return nombre + " (habitantes=" + habitantes
                + ", prioridad=" + prioridad
                + ", recursos=" + recursosRequeridos
                + ", beneficio=" + calcularBeneficio() + ")";
    }
}
