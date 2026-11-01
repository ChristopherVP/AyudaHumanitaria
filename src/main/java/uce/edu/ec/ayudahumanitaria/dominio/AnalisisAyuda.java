package uce.edu.ec.ayudahumanitaria.dominio;

import uce.edu.ec.ayudahumanitaria.dominio.EstadisticaDescriptiva.Resumen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Análisis estadístico de los datos registrados (comunidades y vehículos) y generación
 * automática de conclusiones. No depende de la interfaz ni de la base de datos: recibe
 * los arreglos que entregan los gestores.
 * <p>
 * Unidades: los recursos requeridos están en toneladas y la capacidad de los vehículos en kg.
 */
public class AnalisisAyuda {

    public static final int PRIORIDAD_ALTA = 8;
    private static final double KG_POR_TONELADA = 1000.0;

    private final Comunidad[] comunidades;
    private final Vehiculo[] vehiculos;

    public final Resumen habitantes;
    public final Resumen prioridad;
    public final Resumen recursos;
    public final Resumen beneficio;
    public final Resumen capacidadKg;

    public final double correlacionHabitantesRecursos;
    public final double beneficioTotal;
    public final double recursosTotalesTon;
    public final double capacidadTotalTon;
    public final double coberturaPorcentaje;
    public final double deficitTon;
    public final int vehiculosAsignados;
    public final int comunidadesPrioridadAlta;
    public final Comunidad mayorBeneficio;

    public AnalisisAyuda(Comunidad[] comunidades, Vehiculo[] vehiculos) {
        this.comunidades = comunidades == null ? new Comunidad[0] : comunidades;
        this.vehiculos = vehiculos == null ? new Vehiculo[0] : vehiculos;

        int n = this.comunidades.length;
        double[] h = new double[n];
        double[] p = new double[n];
        double[] r = new double[n];
        double[] b = new double[n];
        double sumaBeneficio = 0;
        double sumaRecursos = 0;
        int altas = 0;
        for (int i = 0; i < n; i++) {
            Comunidad c = this.comunidades[i];
            h[i] = c.getHabitantes();
            p[i] = c.getPrioridad();
            r[i] = c.getRecursosRequeridos();
            b[i] = c.calcularBeneficio();
            sumaBeneficio += b[i];
            sumaRecursos += r[i];
            if (c.getPrioridad() >= PRIORIDAD_ALTA) {
                altas++;
            }
        }

        int m = this.vehiculos.length;
        double[] cap = new double[m];
        double sumaCapacidadKg = 0;
        int asignados = 0;
        for (int i = 0; i < m; i++) {
            cap[i] = this.vehiculos[i].getCapacidad();
            sumaCapacidadKg += cap[i];
            if (this.vehiculos[i].isAsignado()) {
                asignados++;
            }
        }

        habitantes = EstadisticaDescriptiva.resumir(h);
        prioridad = EstadisticaDescriptiva.resumir(p);
        recursos = EstadisticaDescriptiva.resumir(r);
        beneficio = EstadisticaDescriptiva.resumir(b);
        capacidadKg = EstadisticaDescriptiva.resumir(cap);
        correlacionHabitantesRecursos = EstadisticaDescriptiva.correlacion(h, r);

        beneficioTotal = sumaBeneficio;
        recursosTotalesTon = sumaRecursos;
        capacidadTotalTon = sumaCapacidadKg / KG_POR_TONELADA;
        coberturaPorcentaje = sumaRecursos > 0 ? capacidadTotalTon / sumaRecursos * 100.0 : 0;
        deficitTon = Math.max(0, sumaRecursos - capacidadTotalTon);
        vehiculosAsignados = asignados;
        comunidadesPrioridadAlta = altas;
        mayorBeneficio = Algoritmos.comunidadMayorBeneficio(this.comunidades);
    }

    public Comunidad[] getComunidades() {
        return comunidades;
    }

    public Vehiculo[] getVehiculos() {
        return vehiculos;
    }

    // ------------------------------------------------------------------ conclusiones

    public List<String> conclusiones() {
        List<String> lista = new ArrayList<>();
        int n = comunidades.length;
        if (n == 0) {
            lista.add("No hay comunidades registradas, por lo que no se pueden generar conclusiones.");
            return lista;
        }

        lista.add(String.format(Locale.US,
                "Alcance. Se analizaron %d comunidad(es) que suman %,.0f habitantes; el promedio es de %,.0f habitantes "
                        + "por comunidad y la mediana de %,.0f.",
                n, habitantes.suma, habitantes.media, habitantes.mediana));

        if (habitantes.tieneDispersion() && !Double.isNaN(habitantes.coeficienteVariacion)) {
            double cv = habitantes.coeficienteVariacion;
            String nivel = cv < 15 ? "baja" : cv < 35 ? "moderada" : "alta";
            String lectura = cv < 15 ? "las comunidades tienen tamaños similares"
                    : cv < 35 ? "existe cierta diferencia de tamaño entre comunidades"
                    : "hay comunidades de tamaños muy dispares, por lo que un reparto uniforme sería injusto";
            lista.add(String.format(Locale.US,
                    "Variabilidad de la población. El coeficiente de variación es %.1f%% (variabilidad %s): %s.",
                    cv, nivel, lectura));
        }

        if (mayorBeneficio != null && beneficioTotal > 0) {
            double participacion = mayorBeneficio.calcularBeneficio() / beneficioTotal * 100.0;
            lista.add(String.format(Locale.US,
                    "Mayor necesidad. %s es la comunidad con mayor beneficio (habitantes x prioridad = %,.0f) y concentra "
                            + "el %.1f%% del beneficio total, por lo que debe atenderse primero.",
                    mayorBeneficio.getNombre(), mayorBeneficio.calcularBeneficio(), participacion));
        }

        lista.add(String.format(Locale.US,
                "Prioridades. %d de %d comunidad(es) (%.0f%%) tienen prioridad alta (%d o más sobre 10); la prioridad "
                        + "promedio es %.1f.",
                comunidadesPrioridadAlta, n, comunidadesPrioridadAlta * 100.0 / n, PRIORIDAD_ALTA, prioridad.media));

        if (Double.isNaN(correlacionHabitantesRecursos)) {
            lista.add("Relación población-recursos. Se necesitan al menos 3 comunidades con datos no constantes para "
                    + "calcular la correlación entre habitantes y recursos requeridos.");
        } else {
            double r = correlacionHabitantesRecursos;
            double fuerza = Math.abs(r);
            String intensidad = fuerza >= 0.7 ? "fuerte" : fuerza >= 0.4 ? "moderada" : "débil";
            String sentido = r >= 0 ? "positiva" : "negativa";
            String lectura = fuerza < 0.4
                    ? "la demanda de recursos no depende principalmente del número de habitantes"
                    : r >= 0 ? "a mayor población, mayores son los recursos requeridos"
                    : "a mayor población, menores son los recursos requeridos";
            lista.add(String.format(Locale.US,
                    "Relación población-recursos. La correlación de Pearson entre habitantes y recursos requeridos es "
                            + "r = %.2f (%s %s): %s.", r, intensidad, sentido, lectura));
        }

        if (vehiculos.length == 0) {
            lista.add(String.format(Locale.US,
                    "Capacidad logística. No hay vehículos registrados: no se puede cubrir ninguna parte de la demanda "
                            + "de %.1f toneladas.", recursosTotalesTon));
        } else if (deficitTon > 0) {
            int adicionales = (int) Math.ceil(deficitTon * KG_POR_TONELADA / capacidadKg.media);
            lista.add(String.format(Locale.US,
                    "Capacidad logística. La flota (%.2f t) cubre solo el %.1f%% de los %.1f t requeridos; existe un "
                            + "déficit de %.1f t. Con la capacidad promedio actual se necesitarían unos %d vehículo(s) "
                            + "adicional(es) para atender toda la demanda.",
                    capacidadTotalTon, coberturaPorcentaje, recursosTotalesTon, deficitTon, adicionales));
        } else {
            lista.add(String.format(Locale.US,
                    "Capacidad logística. La flota (%.2f t) cubre el %.1f%% de los %.1f t requeridos, con una holgura de "
                            + "%.1f t.",
                    capacidadTotalTon, coberturaPorcentaje, recursosTotalesTon, capacidadTotalTon - recursosTotalesTon));
        }

        if (vehiculos.length > 0) {
            lista.add(String.format(Locale.US,
                    "Uso de la flota. %d de %d vehículo(s) (%.0f%%) están asignados a una comunidad; %d siguen disponibles.",
                    vehiculosAsignados, vehiculos.length, vehiculosAsignados * 100.0 / vehiculos.length,
                    vehiculos.length - vehiculosAsignados));
        }

        StringBuilder recomendacion = new StringBuilder("Recomendación. Atender primero ");
        recomendacion.append(mayorBeneficio != null ? mayorBeneficio.getNombre() : "las comunidades de mayor beneficio");
        if (deficitTon > 0 && vehiculos.length > 0) {
            recomendacion.append(" y usar el algoritmo de la mochila para elegir qué comunidades cubrir con la capacidad "
                    + "disponible, o bien ampliar la flota para cerrar el déficit.");
        } else if (vehiculos.length == 0) {
            recomendacion.append(" y registrar vehículos antes de planificar la distribución.");
        } else {
            recomendacion.append(" y distribuir los suministros por orden de prioridad; la capacidad actual es suficiente.");
        }
        lista.add(recomendacion.toString());
        return lista;
    }

    // ------------------------------------------------------------------ texto para pantalla

    public String reporteTexto() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Estadística descriptiva ===\n");
        agregarFila(sb, "Habitantes", habitantes, "%,.0f");
        agregarFila(sb, "Prioridad (1-10)", prioridad, "%,.2f");
        agregarFila(sb, "Recursos requeridos (t)", recursos, "%,.2f");
        agregarFila(sb, "Beneficio", beneficio, "%,.0f");
        if (vehiculos.length > 0) {
            agregarFila(sb, "Capacidad vehículos (kg)", capacidadKg, "%,.0f");
        }
        sb.append("\n=== Indicadores ===\n");
        sb.append(String.format(Locale.US, " - Beneficio total: %,.0f%n", beneficioTotal));
        sb.append(String.format(Locale.US, " - Recursos requeridos: %.2f t | Capacidad de la flota: %.2f t%n",
                recursosTotalesTon, capacidadTotalTon));
        sb.append(String.format(Locale.US, " - Cobertura de la demanda: %.1f%% | Déficit: %.2f t%n",
                coberturaPorcentaje, deficitTon));
        sb.append("\n=== Conclusiones ===\n");
        int i = 1;
        for (String c : conclusiones()) {
            sb.append(i++).append(". ").append(c).append("\n\n");
        }
        return sb.toString();
    }

    private void agregarFila(StringBuilder sb, String nombre, Resumen r, String formato) {
        sb.append(" - ").append(nombre).append(": n=").append(r.n);
        sb.append(String.format(Locale.US, " | media=" + formato + " | mediana=" + formato, r.media, r.mediana));
        if (r.tieneDispersion()) {
            sb.append(String.format(Locale.US, " | desv.=" + formato, r.desviacion));
        }
        sb.append(String.format(Locale.US, " | mín=" + formato + " | máx=" + formato + "%n", r.minimo, r.maximo));
    }
}
