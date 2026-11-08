package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.geo.GeoEcuador;

/**
 * Ciudad y provincia derivadas de unas coordenadas. Evita que el usuario las escriba
 * (y se equivoque): el punto elegido en el mapa es la única fuente de verdad.
 */
public record UbicacionAutomatica(String ciudad, String provincia) {

    /** @throws IllegalArgumentException si el punto no está en el Ecuador continental. */
    public static UbicacionAutomatica de(double latitud, double longitud) {
        if (!GeoEcuador.estaEnEcuadorContinental(latitud, longitud)) {
            throw new IllegalArgumentException("El punto elegido está fuera del Ecuador continental.");
        }
        return new UbicacionAutomatica(GeoEcuador.ciudadMasCercana(latitud, longitud).nombre(),
                GeoEcuador.provinciaDe(latitud, longitud).nombre());
    }

    public String resumen() {
        return ciudad + " · " + provincia;
    }
}
