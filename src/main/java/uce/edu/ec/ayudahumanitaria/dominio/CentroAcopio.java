package uce.edu.ec.ayudahumanitaria.dominio;

import uce.edu.ec.ayudahumanitaria.geo.GeoEcuador;

/**
 * Centro de acopio y distribución: punto de origen de los vehículos y del stock de ayuda.
 * Es inmutable: para "cambiar" un valor se crea una copia ({@link #conStock(double)}, {@link #conId(int)}).
 * El stock se expresa en toneladas.
 */
public final class CentroAcopio {

    private final int id;
    private final String nombre;
    private final String ciudad;
    private final String provincia;
    private final double latitud;
    private final double longitud;
    private final double stockToneladas;

    public CentroAcopio(int id, String nombre, String ciudad, String provincia,
                        double latitud, double longitud, double stockToneladas) {
        this.id = id;
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.provincia = provincia;
        this.latitud = latitud;
        this.longitud = longitud;
        this.stockToneladas = stockToneladas;
    }

    public CentroAcopio(String nombre, String ciudad, String provincia,
                        double latitud, double longitud, double stockToneladas) {
        this(0, nombre, ciudad, provincia, latitud, longitud, stockToneladas);
    }

    public CentroAcopio conStock(double nuevoStock) {
        return new CentroAcopio(id, nombre, ciudad, provincia, latitud, longitud, nuevoStock);
    }

    public CentroAcopio conId(int nuevoId) {
        return new CentroAcopio(nuevoId, nombre, ciudad, provincia, latitud, longitud, stockToneladas);
    }

    /** Distancia estimada por carretera (km) desde este centro hasta un punto. */
    public double distanciaCarreteraKm(double lat, double lon) {
        return GeoEcuador.distanciaCarreteraKm(latitud, longitud, lat, lon);
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getProvincia() {
        return provincia;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public double getStockToneladas() {
        return stockToneladas;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
