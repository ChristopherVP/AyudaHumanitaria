package uce.edu.ec.ayudahumanitaria.dominio;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;

public class Algoritmos {

    private Algoritmos() {
    }

    public static <T> void bubbleSort(T[] arreglo, Comparator<T> comparador) {
        int n = arreglo.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (comparador.compare(arreglo[j], arreglo[j + 1]) > 0) {
                    T temp = arreglo[j];
                    arreglo[j] = arreglo[j + 1];
                    arreglo[j + 1] = temp;
                }
            }
        }
    }

    public static <T> void insertionSort(T[] arreglo, Comparator<T> comparador) {
        int n = arreglo.length;
        for (int i = 1; i < n; i++) {
            T clave = arreglo[i];
            int j = i - 1;
            while (j >= 0 && comparador.compare(arreglo[j], clave) > 0) {
                arreglo[j + 1] = arreglo[j];
                j--;
            }
            arreglo[j + 1] = clave;
        }
    }

    public static <T> void selectionSort(T[] arreglo, Comparator<T> comparador) {
        int n = arreglo.length;
        for (int i = 0; i < n - 1; i++) {
            int indiceMinimo = i;
            for (int j = i + 1; j < n; j++) {
                if (comparador.compare(arreglo[j], arreglo[indiceMinimo]) < 0) {
                    indiceMinimo = j;
                }
            }
            T temp = arreglo[indiceMinimo];
            arreglo[indiceMinimo] = arreglo[i];
            arreglo[i] = temp;
        }
    }

    public static <T> void shellSort(T[] arreglo, Comparator<T> comparador) {
        int n = arreglo.length;
        for (int salto = n / 2; salto > 0; salto /= 2) {
            for (int i = salto; i < n; i++) {
                T temp = arreglo[i];
                int j = i;
                while (j >= salto && comparador.compare(arreglo[j - salto], temp) > 0) {
                    arreglo[j] = arreglo[j - salto];
                    j -= salto;
                }
                arreglo[j] = temp;
            }
        }
    }

    public static <T> void mergeSort(T[] arreglo, Comparator<T> comparador) {
        if (arreglo.length > 1) {
            mergeSort(arreglo, 0, arreglo.length - 1, comparador);
        }
    }

    private static <T> void mergeSort(T[] arreglo, int izq, int der, Comparator<T> comparador) {
        if (izq < der) {
            int medio = (izq + der) / 2;
            mergeSort(arreglo, izq, medio, comparador);
            mergeSort(arreglo, medio + 1, der, comparador);
            combinar(arreglo, izq, medio, der, comparador);
        }
    }

    private static <T> void combinar(T[] arreglo, int izq, int medio, int der, Comparator<T> comparador) {
        T[] parteIzquierda = Arrays.copyOfRange(arreglo, izq, medio + 1);
        T[] parteDerecha = Arrays.copyOfRange(arreglo, medio + 1, der + 1);

        int i = 0;
        int j = 0;
        int k = izq;

        while (i < parteIzquierda.length && j < parteDerecha.length) {
            if (comparador.compare(parteIzquierda[i], parteDerecha[j]) <= 0) {
                arreglo[k++] = parteIzquierda[i++];
            } else {
                arreglo[k++] = parteDerecha[j++];
            }
        }
        while (i < parteIzquierda.length) {
            arreglo[k++] = parteIzquierda[i++];
        }
        while (j < parteDerecha.length) {
            arreglo[k++] = parteDerecha[j++];
        }
    }

    public static <T> void quickSort(T[] arreglo, Comparator<T> comparador) {
        if (arreglo.length > 1) {
            quickSort(arreglo, 0, arreglo.length - 1, comparador);
        }
    }

    private static <T> void quickSort(T[] arreglo, int bajo, int alto, Comparator<T> comparador) {
        if (bajo < alto) {
            int p = particionar(arreglo, bajo, alto, comparador);
            quickSort(arreglo, bajo, p - 1, comparador);
            quickSort(arreglo, p + 1, alto, comparador);
        }
    }

    private static <T> int particionar(T[] arreglo, int bajo, int alto, Comparator<T> comparador) {
        T pivote = arreglo[alto];
        int i = bajo - 1;
        for (int j = bajo; j < alto; j++) {
            if (comparador.compare(arreglo[j], pivote) <= 0) {
                i++;
                T temp = arreglo[i];
                arreglo[i] = arreglo[j];
                arreglo[j] = temp;
            }
        }
        T temp = arreglo[i + 1];
        arreglo[i + 1] = arreglo[alto];
        arreglo[alto] = temp;
        return i + 1;
    }

    public static <T> int busquedaSecuencial(T[] arreglo, Predicate<T> coincide) {
        for (int i = 0; i < arreglo.length; i++) {
            if (coincide.test(arreglo[i])) {
                return i;
            }
        }
        return -1;
    }

    public static <T> int busquedaBinariaIterativa(T[] arregloOrdenado, T valor, Comparator<T> comparador) {
        int l = 0;
        int r = arregloOrdenado.length - 1;
        while (l <= r) {
            int m = (l + r) / 2;
            int cmp = comparador.compare(arregloOrdenado[m], valor);
            if (cmp == 0) {
                return m;
            } else if (cmp < 0) {
                l = m + 1;
            } else {
                r = m - 1;
            }
        }
        return -1;
    }

    public static <T> int busquedaBinariaRecursiva(T[] arregloOrdenado, T valor, Comparator<T> comparador) {
        return busquedaBinariaRecursiva(arregloOrdenado, valor, comparador, 0, arregloOrdenado.length - 1);
    }

    private static <T> int busquedaBinariaRecursiva(T[] arregloOrdenado, T valor, Comparator<T> comparador,
                                                      int l, int r) {
        if (l > r) {
            return -1;
        }
        int m = (l + r) / 2;
        int cmp = comparador.compare(arregloOrdenado[m], valor);
        if (cmp == 0) {
            return m;
        }
        return cmp < 0
                ? busquedaBinariaRecursiva(arregloOrdenado, valor, comparador, m + 1, r)
                : busquedaBinariaRecursiva(arregloOrdenado, valor, comparador, l, m - 1);
    }

    public static Comunidad comunidadMayorBeneficio(Comunidad[] comunidades) {
        if (comunidades == null || comunidades.length == 0) {
            return null;
        }
        return mayorBeneficio(comunidades, 0, comunidades.length - 1);
    }

    private static Comunidad mayorBeneficio(Comunidad[] comunidades, int inicio, int fin) {
        if (inicio == fin) {
            return comunidades[inicio];
        }
        int medio = (inicio + fin) / 2;
        Comunidad izquierda = mayorBeneficio(comunidades, inicio, medio);
        Comunidad derecha = mayorBeneficio(comunidades, medio + 1, fin);
        return izquierda.calcularBeneficio() >= derecha.calcularBeneficio() ? izquierda : derecha;
    }

    public static class ResultadoDistribucion {
        public String[] nombres;
        public double[] asignado;
    }

    public static ResultadoDistribucion distribuirSuministros(Comunidad[] comunidades, double disponibles) {
        Comunidad[] ordenadas = Arrays.copyOf(comunidades, comunidades.length);
        quickSort(ordenadas, Comparator.comparingInt(Comunidad::getPrioridad).reversed());

        ResultadoDistribucion resultado = new ResultadoDistribucion();
        resultado.nombres = new String[ordenadas.length];
        resultado.asignado = new double[ordenadas.length];

        double restante = disponibles;
        for (int i = 0; i < ordenadas.length; i++) {
            Comunidad c = ordenadas[i];
            resultado.nombres[i] = c.getNombre();
            if (restante <= 0) {
                resultado.asignado[i] = 0.0;
                continue;
            }
            double asignadoActual = Math.min(c.getRecursosRequeridos(), restante);
            resultado.asignado[i] = asignadoActual;
            restante -= asignadoActual;
        }
        return resultado;
    }

    public static class ResultadoMochila {
        public Comunidad[] seleccionadas;
        public double beneficioTotal;
        public int pesoTotal;
    }

    public static ResultadoMochila resolverMochila(Comunidad[] comunidades, int capacidad) {
        int n = comunidades.length;
        int[] pesos = new int[n];
        double[] valores = new double[n];

        for (int i = 0; i < n; i++) {
            pesos[i] = (int) Math.round(comunidades[i].getRecursosRequeridos());
            valores[i] = comunidades[i].calcularBeneficio();
        }

        double[][] dp = new double[n + 1][capacidad + 1];

        for (int i = 1; i <= n; i++) {
            for (int w = 0; w <= capacidad; w++) {
                if (pesos[i - 1] <= w) {
                    double incluir = dp[i - 1][w - pesos[i - 1]] + valores[i - 1];
                    double excluir = dp[i - 1][w];
                    dp[i][w] = Math.max(incluir, excluir);
                } else {
                    dp[i][w] = dp[i - 1][w];
                }
            }
        }

        Comunidad[] seleccionadasTemp = new Comunidad[n];
        int contador = 0;
        int w = capacidad;

        for (int i = n; i > 0; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                seleccionadasTemp[contador++] = comunidades[i - 1];
                w -= pesos[i - 1];
            }
        }

        ResultadoMochila resultado = new ResultadoMochila();
        resultado.seleccionadas = Arrays.copyOf(seleccionadasTemp, contador);
        resultado.beneficioTotal = dp[n][capacidad];
        resultado.pesoTotal = capacidad - w;
        return resultado;
    }


    public static class ResultadoRuta {
        public String[] mejorRuta;
        public double mejorDistancia = Double.MAX_VALUE;
        public int nodosExplorados;
        public int nodosPodados;
    }

    public static ResultadoRuta rutaOptima(String[] nombres, double[][] distancias) {
        ResultadoRuta resultado = new ResultadoRuta();
        boolean[] visitado = new boolean[nombres.length];
        String[] rutaActual = new String[nombres.length + 1];

        rutaActual[0] = nombres[0];
        visitado[0] = true;

        backtrack(nombres, distancias, visitado, rutaActual, 1, 0.0, resultado);
        return resultado;
    }

    private static void backtrack(String[] nombres, double[][] distancias, boolean[] visitado,
                                   String[] rutaActual, int nivel, double distanciaAcumulada,
                                   ResultadoRuta resultado) {

        resultado.nodosExplorados++;

        if (distanciaAcumulada >= resultado.mejorDistancia) {
            resultado.nodosPodados++;
            return;
        }

        boolean todasVisitadas = true;
        for (int i = 1; i < visitado.length; i++) {
            if (!visitado[i]) {
                todasVisitadas = false;
                break;
            }
        }

        if (todasVisitadas) {
            int actual = indiceDe(nombres, rutaActual[nivel - 1]);
            double distanciaTotal = distanciaAcumulada + distancias[actual][0];

            if (distanciaTotal < resultado.mejorDistancia) {
                resultado.mejorDistancia = distanciaTotal;
                String[] rutaCompleta = Arrays.copyOf(rutaActual, nivel + 1);
                rutaCompleta[nivel] = nombres[0];
                resultado.mejorRuta = rutaCompleta;
            }
            return;
        }

        int actual = indiceDe(nombres, rutaActual[nivel - 1]);
        for (int i = 1; i < nombres.length; i++) {
            if (visitado[i]) {
                continue;
            }
            double distancia = distancias[actual][i];

            visitado[i] = true;
            rutaActual[nivel] = nombres[i];

            backtrack(nombres, distancias, visitado, rutaActual, nivel + 1,
                    distanciaAcumulada + distancia, resultado);

            visitado[i] = false;
        }
    }

    private static int indiceDe(String[] nombres, String valor) {
        for (int i = 0; i < nombres.length; i++) {
            if (nombres[i].equals(valor)) {
                return i;
            }
        }
        return -1;
    }

    public static class ResultadoAsignacion {
        public String[] vehiculoIds;
        public String[] comunidadNombres;
        public int total;
        public double desperdicioTotal;
        public int nodosExplorados;
        public int nodosPodados;
    }

    public static ResultadoAsignacion asignarVehiculosBranchAndBound(Vehiculo[] vehiculos, Comunidad[] comunidades) {
        Comunidad[] ordenadas = Arrays.copyOf(comunidades, comunidades.length);
        quickSort(ordenadas, Comparator.comparingDouble(Comunidad::getRecursosRequeridos).reversed());

        boolean[] usados = new boolean[vehiculos.length];
        int[] asignacionActual = new int[ordenadas.length];
        int[] mejorAsignacion = new int[ordenadas.length];
        Arrays.fill(asignacionActual, -1);
        Arrays.fill(mejorAsignacion, -1);

        MejorSolucionAsignacion mejor = new MejorSolucionAsignacion();
        mejor.coberturaMaxima = -1;
        mejor.desperdicioMinimo = Double.MAX_VALUE;

        ramificarAsignacion(vehiculos, ordenadas, usados, asignacionActual, 0, 0, 0.0,
                mejor, mejorAsignacion);

        ResultadoAsignacion resultado = new ResultadoAsignacion();
        resultado.vehiculoIds = new String[ordenadas.length];
        resultado.comunidadNombres = new String[ordenadas.length];

        int contador = 0;
        for (int i = 0; i < ordenadas.length; i++) {
            if (mejorAsignacion[i] != -1) {
                resultado.vehiculoIds[contador] = vehiculos[mejorAsignacion[i]].getId();
                resultado.comunidadNombres[contador] = ordenadas[i].getNombre();
                contador++;
            }
        }

        resultado.vehiculoIds = Arrays.copyOf(resultado.vehiculoIds, contador);
        resultado.comunidadNombres = Arrays.copyOf(resultado.comunidadNombres, contador);
        resultado.total = contador;
        resultado.desperdicioTotal = mejor.coberturaMaxima == -1 ? 0.0 : mejor.desperdicioMinimo;
        resultado.nodosExplorados = mejor.nodosExplorados;
        resultado.nodosPodados = mejor.nodosPodados;
        return resultado;
    }

    private static class MejorSolucionAsignacion {
        int coberturaMaxima;
        double desperdicioMinimo;
        int nodosExplorados;
        int nodosPodados;
    }

    private static void ramificarAsignacion(Vehiculo[] vehiculos, Comunidad[] comunidades, boolean[] usados,
                                             int[] asignacionActual, int indice, int coberturaActual,
                                             double desperdicioAcumulado, MejorSolucionAsignacion mejor,
                                             int[] mejorAsignacion) {

        mejor.nodosExplorados++;

        if (indice == comunidades.length) {
            if (esMejorAsignacion(coberturaActual, desperdicioAcumulado, mejor)) {
                mejor.coberturaMaxima = coberturaActual;
                mejor.desperdicioMinimo = desperdicioAcumulado;
                System.arraycopy(asignacionActual, 0, mejorAsignacion, 0, asignacionActual.length);
            }
            return;
        }

        int vehiculosDisponibles = contarNoUsados(usados);
        int comunidadesRestantes = comunidades.length - indice;
        int coberturaMaximaAlcanzable = coberturaActual + Math.min(vehiculosDisponibles, comunidadesRestantes);

        if (mejor.coberturaMaxima != -1 && coberturaMaximaAlcanzable < mejor.coberturaMaxima) {
            mejor.nodosPodados++;
            return;
        }
        if (mejor.coberturaMaxima != -1 && coberturaMaximaAlcanzable == mejor.coberturaMaxima
                && desperdicioAcumulado >= mejor.desperdicioMinimo) {
            mejor.nodosPodados++;
            return;
        }

        Comunidad comunidad = comunidades[indice];

        for (int i = 0; i < vehiculos.length; i++) {
            if (usados[i]) {
                continue;
            }
            if (vehiculos[i].getCapacidad() >= comunidad.getRecursosRequeridos()) {
                double desperdicio = vehiculos[i].getCapacidad() - comunidad.getRecursosRequeridos();

                usados[i] = true;
                asignacionActual[indice] = i;

                ramificarAsignacion(vehiculos, comunidades, usados, asignacionActual, indice + 1,
                        coberturaActual + 1, desperdicioAcumulado + desperdicio, mejor, mejorAsignacion);

                usados[i] = false;
                asignacionActual[indice] = -1;
            }
        }

        asignacionActual[indice] = -1;
        ramificarAsignacion(vehiculos, comunidades, usados, asignacionActual, indice + 1,
                coberturaActual, desperdicioAcumulado, mejor, mejorAsignacion);
    }

    private static boolean esMejorAsignacion(int cobertura, double desperdicio, MejorSolucionAsignacion mejor) {
        if (mejor.coberturaMaxima == -1) {
            return true;
        }
        if (cobertura > mejor.coberturaMaxima) {
            return true;
        }
        return cobertura == mejor.coberturaMaxima && desperdicio < mejor.desperdicioMinimo;
    }

    private static int contarNoUsados(boolean[] usados) {
        int contador = 0;
        for (boolean usado : usados) {
            if (!usado) {
                contador++;
            }
        }
        return contador;
    }
}
