package uce.edu.ec.ayudahumanitaria.geo;

import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos;
import uce.edu.ec.ayudahumanitaria.geo.CatalogoEcuador.Ciudad;
import uce.edu.ec.ayudahumanitaria.geo.CatalogoEcuador.Provincia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Cálculos geográficos sin dependencias de interfaz ni de base de datos:
 * distancias, validación de coordenadas y ciudades cercanas a un tramo.
 */
public final class GeoEcuador {

    /** Radio medio de la Tierra en kilómetros. */
    private static final double RADIO_TIERRA_KM = 6371.0088;

    /**
     * Factor que convierte la distancia en línea recta en distancia estimada por carretera.
     * En el Ecuador, por el relieve andino, la carretera suele medir entre 1.2 y 1.6 veces la línea recta.
     */
    public static final double FACTOR_CARRETERA = 1.35;

    /** Límites del Ecuador continental (con un pequeño margen). */
    public static final double LAT_MIN = -5.05;
    public static final double LAT_MAX = 1.55;
    public static final double LON_MIN = -81.15;
    public static final double LON_MAX = -75.05;

    /**
     * Contorno aproximado y algo holgado del Ecuador continental, en pares {latitud, longitud},
     * recorrido en sentido horario desde la costa de Esmeraldas.
     */
    private static final double[][] CONTORNO = {
            {1.55, -79.05}, {1.55, -78.50}, {1.05, -77.95}, {0.95, -77.45}, {0.45, -76.95},
            {0.40, -76.55}, {0.08, -75.60}, {0.00, -75.05}, {-0.30, -75.05}, {-1.60, -75.10},
            {-2.00, -75.60}, {-2.40, -76.40}, {-2.50, -76.85}, {-3.00, -77.90}, {-3.50, -78.20},
            {-4.20, -78.40}, {-4.55, -78.60}, {-5.05, -78.90}, {-5.05, -79.20}, {-4.60, -79.45},
            {-4.50, -80.45}, {-3.50, -80.35}, {-3.00, -80.45}, {-2.60, -80.55}, {-2.30, -81.15},
            {-1.30, -81.05}, {-0.90, -81.05}, {-0.40, -80.70}, {0.10, -80.25}, {0.60, -80.05},
            {1.00, -79.80}
    };

    private GeoEcuador() {
    }

    /** Distancia en línea recta (círculo máximo) entre dos puntos, en km. */
    public static double distanciaRectaKm(double lat1, double lon1, double lat2, double lon2) {
        double f1 = Math.toRadians(lat1);
        double f2 = Math.toRadians(lat2);
        double df = f2 - f1;
        double dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(df / 2) * Math.sin(df / 2)
                + Math.cos(f1) * Math.cos(f2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * RADIO_TIERRA_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /** Distancia estimada por carretera entre dos puntos, en km. */
    public static double distanciaCarreteraKm(double lat1, double lon1, double lat2, double lon2) {
        return distanciaRectaKm(lat1, lon1, lat2, lon2) * FACTOR_CARRETERA;
    }

    /** {@code true} si el punto cae dentro del contorno aproximado del Ecuador continental. */
    public static boolean estaEnEcuadorContinental(double latitud, double longitud) {
        if (Double.isNaN(latitud) || Double.isNaN(longitud)
                || latitud < LAT_MIN || latitud > LAT_MAX || longitud < LON_MIN || longitud > LON_MAX) {
            return false;
        }
        boolean dentro = false;
        int n = CONTORNO.length;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double latI = CONTORNO[i][0];
            double lonI = CONTORNO[i][1];
            double latJ = CONTORNO[j][0];
            double lonJ = CONTORNO[j][1];
            boolean cruza = (latI > latitud) != (latJ > latitud)
                    && longitud < (lonJ - lonI) * (latitud - latI) / (latJ - latI) + lonI;
            if (cruza) {
                dentro = !dentro;
            }
        }
        return dentro;
    }

    /**
     * Indica si el punto es coherente con la provincia indicada (está dentro de su radio de referencia).
     * Evita, por ejemplo, registrar una comunidad "de Guayas" con coordenadas de la Amazonía.
     */
    public static boolean esCoherenteConProvincia(String nombreProvincia, double latitud, double longitud) {
        Provincia p = CatalogoEcuador.provincia(nombreProvincia);
        if (p == null) {
            return false;
        }
        return distanciaRectaKm(p.latitud(), p.longitud(), latitud, longitud) <= p.radioKm();
    }

    /** Ciudad del catálogo más cercana a un punto (nunca {@code null}). */
    public static Ciudad ciudadMasCercana(double latitud, double longitud) {
        Ciudad mejor = null;
        double mejorDistancia = Double.MAX_VALUE;
        for (Ciudad c : CatalogoEcuador.CIUDADES) {
            double d = distanciaRectaKm(latitud, longitud, c.latitud(), c.longitud());
            if (d < mejorDistancia) {
                mejorDistancia = d;
                mejor = c;
            }
        }
        return mejor;
    }

    /**
     * Ciudades del catálogo que quedan cerca de la línea recta entre dos puntos, ordenadas desde el origen
     * hacia el destino. Sirve para mostrar "pasa por ..." en el mapa esquemático.
     *
     * @param anchoKm distancia máxima perpendicular a la línea para considerar que la ciudad "está en el camino"
     */
    public static List<Ciudad> ciudadesEnElTramo(double lat1, double lon1, double lat2, double lon2, double anchoKm) {
        double kmPorGradoLat = 111.32;
        double kmPorGradoLon = 111.32 * Math.cos(Math.toRadians((lat1 + lat2) / 2));
        double dx = (lon2 - lon1) * kmPorGradoLon;
        double dy = (lat2 - lat1) * kmPorGradoLat;
        double largo2 = dx * dx + dy * dy;
        List<Ciudad> candidatas = new ArrayList<>();
        if (largo2 < 1e-9) {
            return candidatas;
        }
        double largo = Math.sqrt(largo2);
        double margen = Math.min(0.5, 8.0 / largo);

        int indice = 0;
        List<double[]> encontradas = new ArrayList<>();
        for (Ciudad c : CatalogoEcuador.CIUDADES) {
            double px = (c.longitud() - lon1) * kmPorGradoLon;
            double py = (c.latitud() - lat1) * kmPorGradoLat;
            double t = (px * dx + py * dy) / largo2;
            if (t <= margen || t >= 1 - margen) {
                indice++;
                continue;
            }
            double perpendicular = Math.abs(px * dy - py * dx) / largo;
            if (perpendicular <= anchoKm) {
                encontradas.add(new double[]{indice, t});
            }
            indice++;
        }
        if (encontradas.isEmpty()) {
            return candidatas;
        }
        double[][] arreglo = encontradas.toArray(new double[0][]);
        Algoritmos.quickSort(arreglo, Comparator.comparingDouble((double[] par) -> par[1]));
        for (double[] par : arreglo) {
            candidatas.add(CatalogoEcuador.CIUDADES.get((int) par[0]));
        }
        return candidatas;
    }
}
