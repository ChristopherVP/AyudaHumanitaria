package uce.edu.ec.ayudahumanitaria.dominio;

import java.util.Comparator;

/**
 * Estadística descriptiva básica sobre un conjunto de datos numéricos.
 * Usa el MergeSort propio del proyecto (Algoritmos) para ordenar y calcula los
 * cuantiles con el mismo método que Excel (QUARTILE / QUARTILE.INC), de modo que
 * los resultados coincidan con las fórmulas del libro exportado.
 */
public final class EstadisticaDescriptiva {

    private EstadisticaDescriptiva() {
    }

    public static class Resumen {
        public int n;
        public double suma;
        public double media;
        public double mediana;
        /** Desviación estándar muestral (n-1). NaN si hay menos de 2 datos. */
        public double desviacion = Double.NaN;
        public double minimo;
        public double q1;
        public double q3;
        public double maximo;
        public double rango;
        /** Coeficiente de variación en %. NaN si no se puede calcular. */
        public double coeficienteVariacion = Double.NaN;

        public boolean tieneDispersion() {
            return !Double.isNaN(desviacion);
        }
    }

    public static Resumen resumir(double[] datos) {
        Resumen r = new Resumen();
        r.n = datos == null ? 0 : datos.length;
        if (r.n == 0) {
            return r;
        }

        Double[] ordenados = new Double[r.n];
        for (int i = 0; i < r.n; i++) {
            ordenados[i] = datos[i];
            r.suma += datos[i];
        }
        Algoritmos.mergeSort(ordenados, Comparator.naturalOrder());

        double[] v = new double[r.n];
        for (int i = 0; i < r.n; i++) {
            v[i] = ordenados[i];
        }

        r.media = r.suma / r.n;
        r.minimo = v[0];
        r.maximo = v[r.n - 1];
        r.rango = r.maximo - r.minimo;
        r.mediana = cuantil(v, 0.5);
        r.q1 = cuantil(v, 0.25);
        r.q3 = cuantil(v, 0.75);

        if (r.n >= 2) {
            double acumulado = 0;
            for (double d : datos) {
                acumulado += (d - r.media) * (d - r.media);
            }
            r.desviacion = Math.sqrt(acumulado / (r.n - 1));
            if (r.media != 0) {
                r.coeficienteVariacion = r.desviacion / Math.abs(r.media) * 100.0;
            }
        }
        return r;
    }

    /** Cuantil por interpolación lineal (equivale a QUARTILE.INC / PERCENTILE.INC de Excel). */
    private static double cuantil(double[] ordenado, double p) {
        double posicion = (ordenado.length - 1) * p;
        int inferior = (int) Math.floor(posicion);
        int superior = (int) Math.ceil(posicion);
        return ordenado[inferior] + (posicion - inferior) * (ordenado[superior] - ordenado[inferior]);
    }

    /** Coeficiente de correlación de Pearson. NaN si hay menos de 3 pares o una variable es constante. */
    public static double correlacion(double[] x, double[] y) {
        if (x == null || y == null || x.length != y.length || x.length < 3) {
            return Double.NaN;
        }
        int n = x.length;
        double mx = 0;
        double my = 0;
        for (int i = 0; i < n; i++) {
            mx += x[i];
            my += y[i];
        }
        mx /= n;
        my /= n;

        double sxy = 0;
        double sxx = 0;
        double syy = 0;
        for (int i = 0; i < n; i++) {
            sxy += (x[i] - mx) * (y[i] - my);
            sxx += (x[i] - mx) * (x[i] - mx);
            syy += (y[i] - my) * (y[i] - my);
        }
        if (sxx == 0 || syy == 0) {
            return Double.NaN;
        }
        return sxy / Math.sqrt(sxx * syy);
    }
}
