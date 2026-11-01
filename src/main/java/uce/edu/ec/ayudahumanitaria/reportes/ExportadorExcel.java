package uce.edu.ec.ayudahumanitaria.reportes;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;
import uce.edu.ec.ayudahumanitaria.dominio.AnalisisAyuda;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.EstadisticaDescriptiva.Resumen;
import uce.edu.ec.ayudahumanitaria.dominio.Vehiculo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera un libro de Excel profesional con los datos actuales de la base de datos:
 * hojas de datos, estadística descriptiva con fórmulas vivas, gráficos y conclusiones.
 * El libro se vuelve a generar completo cada vez, a partir de lo que haya registrado en ese momento.
 */
public final class ExportadorExcel {

    private static final String FUENTE = "Arial";
    private static final int FILA_ENCABEZADO = 3;   // índice base 0 (fila 4 de Excel)
    private static final int PRIMERA_FILA = 4;      // índice base 0 (fila 5 de Excel)

    private ExportadorExcel() {
    }

    // ------------------------------------------------------------------ estilos

    private static final class Estilos {
        final CellStyle titulo;
        final CellStyle subtitulo;
        final CellStyle encabezado;
        final CellStyle texto;
        final CellStyle textoZ;
        final CellStyle centro;
        final CellStyle centroZ;
        final CellStyle entero;
        final CellStyle enteroZ;
        final CellStyle decimal;
        final CellStyle decimalZ;
        final CellStyle porcentaje;
        final CellStyle porcentajeZ;
        final CellStyle negrita;
        final CellStyle parrafo;
        final CellStyle numeroConclusion;
        final CellStyle nota;

        Estilos(XSSFWorkbook wb) {
            titulo = crear(wb, fuente(wb, true, 16, 31, 58, 110), null, HorizontalAlignment.LEFT, null, false);
            subtitulo = crear(wb, fuente(wb, false, 10, 85, 85, 85, true), null, HorizontalAlignment.LEFT, null, false);
            encabezado = crear(wb, fuente(wb, true, 11, 255, 255, 255), new byte[]{31, 58, 110},
                    HorizontalAlignment.CENTER, null, true);
            encabezado.setWrapText(true);
            texto = crear(wb, fuente(wb, false, 10, 17, 17, 17), null, HorizontalAlignment.LEFT, null, true);
            textoZ = crear(wb, fuente(wb, false, 10, 17, 17, 17), new byte[]{(byte) 243, (byte) 245, (byte) 248},
                    HorizontalAlignment.LEFT, null, true);
            centro = crear(wb, fuente(wb, false, 10, 17, 17, 17), null, HorizontalAlignment.CENTER, null, true);
            centroZ = crear(wb, fuente(wb, false, 10, 17, 17, 17), new byte[]{(byte) 243, (byte) 245, (byte) 248},
                    HorizontalAlignment.CENTER, null, true);
            entero = crear(wb, fuente(wb, false, 10, 17, 17, 17), null, HorizontalAlignment.CENTER, "#,##0", true);
            enteroZ = crear(wb, fuente(wb, false, 10, 17, 17, 17), new byte[]{(byte) 243, (byte) 245, (byte) 248},
                    HorizontalAlignment.CENTER, "#,##0", true);
            decimal = crear(wb, fuente(wb, false, 10, 17, 17, 17), null, HorizontalAlignment.CENTER, "#,##0.00", true);
            decimalZ = crear(wb, fuente(wb, false, 10, 17, 17, 17), new byte[]{(byte) 243, (byte) 245, (byte) 248},
                    HorizontalAlignment.CENTER, "#,##0.00", true);
            porcentaje = crear(wb, fuente(wb, false, 10, 17, 17, 17), null, HorizontalAlignment.CENTER, "0.0%", true);
            porcentajeZ = crear(wb, fuente(wb, false, 10, 17, 17, 17), new byte[]{(byte) 243, (byte) 245, (byte) 248},
                    HorizontalAlignment.CENTER, "0.0%", true);
            negrita = crear(wb, fuente(wb, true, 10, 17, 17, 17), null, HorizontalAlignment.LEFT, null, true);
            parrafo = crear(wb, fuente(wb, false, 11, 17, 17, 17), null, HorizontalAlignment.LEFT, null, true);
            parrafo.setWrapText(true);
            parrafo.setVerticalAlignment(VerticalAlignment.CENTER);
            numeroConclusion = crear(wb, fuente(wb, true, 12, 31, 58, 110), null, HorizontalAlignment.CENTER, null, true);
            numeroConclusion.setVerticalAlignment(VerticalAlignment.CENTER);
            nota = crear(wb, fuente(wb, false, 9, 85, 85, 85, true), null, HorizontalAlignment.LEFT, null, false);
        }

        private static XSSFFont fuente(XSSFWorkbook wb, boolean negrita, int tam, int r, int g, int b) {
            return fuente(wb, negrita, tam, r, g, b, false);
        }

        private static XSSFFont fuente(XSSFWorkbook wb, boolean negrita, int tam, int r, int g, int b, boolean cursiva) {
            XSSFFont f = wb.createFont();
            f.setFontName(FUENTE);
            f.setBold(negrita);
            f.setItalic(cursiva);
            f.setFontHeightInPoints((short) tam);
            f.setColor(new XSSFColor(new byte[]{(byte) r, (byte) g, (byte) b}, null));
            return f;
        }

        private static CellStyle crear(XSSFWorkbook wb, XSSFFont fuente, byte[] relleno, HorizontalAlignment h,
                                       String formato, boolean borde) {
            org.apache.poi.xssf.usermodel.XSSFCellStyle s = wb.createCellStyle();
            s.setFont(fuente);
            s.setAlignment(h);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            if (relleno != null) {
                s.setFillForegroundColor(new XSSFColor(relleno, null));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            if (formato != null) {
                s.setDataFormat(wb.createDataFormat().getFormat(formato));
            }
            if (borde) {
                s.setBorderBottom(BorderStyle.THIN);
                s.setBorderTop(BorderStyle.THIN);
                s.setBorderLeft(BorderStyle.THIN);
                s.setBorderRight(BorderStyle.THIN);
                short gris = IndexedColors.GREY_40_PERCENT.getIndex();
                s.setBottomBorderColor(gris);
                s.setTopBorderColor(gris);
                s.setLeftBorderColor(gris);
                s.setRightBorderColor(gris);
            }
            return s;
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static XSSFCell celda(XSSFSheet hoja, int fila, int col) {
        XSSFRow r = hoja.getRow(fila) != null ? hoja.getRow(fila) : hoja.createRow(fila);
        return r.getCell(col) != null ? r.getCell(col) : r.createCell(col);
    }

    private static void texto(XSSFSheet hoja, int fila, int col, String valor, CellStyle estilo) {
        XSSFCell c = celda(hoja, fila, col);
        c.setCellValue(valor);
        c.setCellStyle(estilo);
    }

    private static void numero(XSSFSheet hoja, int fila, int col, double valor, CellStyle estilo) {
        XSSFCell c = celda(hoja, fila, col);
        c.setCellValue(valor);
        c.setCellStyle(estilo);
    }

    /** Fórmula con su resultado numérico en caché (se ve bien aun sin recalcular, p. ej. en Vista protegida). */
    private static void formula(XSSFSheet hoja, int fila, int col, String f, double cache, CellStyle estilo) {
        XSSFCell c = celda(hoja, fila, col);
        c.setCellFormula(f);
        if (!Double.isNaN(cache) && !Double.isInfinite(cache)) {
            c.getCTCell().setV(Double.toString(cache));
        }
        c.setCellStyle(estilo);
    }

    private static void formulaTexto(XSSFSheet hoja, int fila, int col, String f, String cache, CellStyle estilo) {
        XSSFCell c = celda(hoja, fila, col);
        c.setCellFormula(f);
        if (cache != null) {
            c.getCTCell().setT(STCellType.STR);
            c.getCTCell().setV(cache);
        }
        c.setCellStyle(estilo);
    }

    private static void titulo(XSSFSheet hoja, Estilos e, String titulo, String subtitulo, int columnas) {
        texto(hoja, 0, 0, titulo, e.titulo);
        texto(hoja, 1, 0, subtitulo, e.subtitulo);
        hoja.getRow(0).setHeightInPoints(26);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, columnas - 1));
        hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, columnas - 1));
        hoja.setDisplayGridlines(false);
    }

    private static void encabezados(XSSFSheet hoja, Estilos e, int fila, String... titulos) {
        for (int j = 0; j < titulos.length; j++) {
            texto(hoja, fila, j, titulos[j], e.encabezado);
        }
        hoja.getRow(fila).setHeightInPoints(32);
    }

    private static String rango(String hoja, String col, int n) {
        int ultima = Math.max(PRIMERA_FILA + 1, PRIMERA_FILA + n);
        return hoja + "!$" + col + "$" + (PRIMERA_FILA + 1) + ":$" + col + "$" + ultima;
    }

    private static String nivel(int prioridad) {
        return prioridad >= 8 ? "Alta" : prioridad >= 5 ? "Media" : "Baja";
    }

    // ------------------------------------------------------------------ exportación

    public static void exportar(File destino, Comunidad[] comunidades, Vehiculo[] vehiculos) throws IOException {
        if (comunidades == null || comunidades.length == 0) {
            throw new IllegalStateException("No hay comunidades registradas para generar el reporte.");
        }
        Vehiculo[] flota = vehiculos == null ? new Vehiculo[0] : vehiculos;
        AnalisisAyuda a = new AnalisisAyuda(comunidades, flota);
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String sub = "Sistema Inteligente de Distribución de Ayuda Humanitaria · Generado el " + fecha;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Estilos e = new Estilos(wb);
            XSSFSheet resumen = wb.createSheet("Resumen");
            XSSFSheet hojaC = wb.createSheet("Comunidades");
            XSSFSheet hojaV = wb.createSheet("Vehiculos");
            XSSFSheet hojaE = wb.createSheet("Estadisticas");
            XSSFSheet hojaK = wb.createSheet("Conclusiones");

            escribirComunidades(hojaC, e, a, sub);
            escribirVehiculos(hojaV, e, a, sub);
            escribirEstadisticas(hojaE, e, a, sub);
            escribirResumen(resumen, e, a, sub);
            escribirConclusiones(hojaK, e, a, sub);
            agregarGraficos(resumen, hojaC, comunidades.length);

            wb.setActiveSheet(0);
            wb.setForceFormulaRecalculation(true);
            try (FileOutputStream salida = new FileOutputStream(destino)) {
                wb.write(salida);
            }
        }
    }

    // ------------------------------------------------------------------ hojas

    private static void escribirComunidades(XSSFSheet h, Estilos e, AnalisisAyuda a, String sub) {
        Comunidad[] cs = a.getComunidades();
        int n = cs.length;
        titulo(h, e, "Comunidades registradas", sub, 8);
        encabezados(h, e, FILA_ENCABEZADO, "ID", "Comunidad", "Habitantes", "Prioridad (1-10)",
                "Recursos requeridos (t)", "Beneficio (habitantes × prioridad)", "% del beneficio total", "Nivel de prioridad");
        int ult = PRIMERA_FILA + n;
        for (int i = 0; i < n; i++) {
            Comunidad c = cs[i];
            int fila = PRIMERA_FILA + i;
            int ex = fila + 1;
            boolean z = i % 2 == 1;
            numero(h, fila, 0, c.getId(), z ? e.centroZ : e.centro);
            texto(h, fila, 1, c.getNombre(), z ? e.textoZ : e.texto);
            numero(h, fila, 2, c.getHabitantes(), z ? e.enteroZ : e.entero);
            numero(h, fila, 3, c.getPrioridad(), z ? e.centroZ : e.centro);
            numero(h, fila, 4, c.getRecursosRequeridos(), z ? e.decimalZ : e.decimal);
            formula(h, fila, 5, "C" + ex + "*D" + ex, c.calcularBeneficio(), z ? e.enteroZ : e.entero);
            formula(h, fila, 6, "IFERROR(F" + ex + "/SUM($F$5:$F$" + ult + "),0)",
                    a.beneficioTotal > 0 ? c.calcularBeneficio() / a.beneficioTotal : 0, z ? e.porcentajeZ : e.porcentaje);
            formulaTexto(h, fila, 7, "IF(D" + ex + ">=8,\"Alta\",IF(D" + ex + ">=5,\"Media\",\"Baja\"))",
                    nivel(c.getPrioridad()), z ? e.centroZ : e.centro);
        }
        int totalFila = PRIMERA_FILA + n;
        texto(h, totalFila, 1, "TOTAL", e.negrita);
        formula(h, totalFila, 2, "SUM(C5:C" + ult + ")", a.habitantes.suma, e.entero);
        formula(h, totalFila, 4, "SUM(E5:E" + ult + ")", a.recursosTotalesTon, e.decimal);
        formula(h, totalFila, 5, "SUM(F5:F" + ult + ")", a.beneficioTotal, e.entero);
        formula(h, totalFila, 6, "SUM(G5:G" + ult + ")", 1.0, e.porcentaje);
        h.setAutoFilter(new CellRangeAddress(FILA_ENCABEZADO, ult - 1, 0, 7));
        h.createFreezePane(2, PRIMERA_FILA);
        int[] anchos = {6, 26, 14, 14, 18, 24, 18, 16};
        for (int j = 0; j < anchos.length; j++) {
            h.setColumnWidth(j, anchos[j] * 256);
        }
    }

    private static void escribirVehiculos(XSSFSheet h, Estilos e, AnalisisAyuda a, String sub) {
        Vehiculo[] vs = a.getVehiculos();
        titulo(h, e, "Flota de vehículos", sub, 5);
        encabezados(h, e, FILA_ENCABEZADO, "ID", "Capacidad (kg)", "Capacidad (t)", "Asignado", "Comunidad asignada");
        for (int i = 0; i < vs.length; i++) {
            Vehiculo v = vs[i];
            int fila = PRIMERA_FILA + i;
            int ex = fila + 1;
            boolean z = i % 2 == 1;
            texto(h, fila, 0, v.getId(), z ? e.textoZ : e.texto);
            numero(h, fila, 1, v.getCapacidad(), z ? e.enteroZ : e.entero);
            formula(h, fila, 2, "B" + ex + "/1000", v.getCapacidad() / 1000.0, z ? e.decimalZ : e.decimal);
            texto(h, fila, 3, v.isAsignado() ? "Sí" : "No", z ? e.centroZ : e.centro);
            texto(h, fila, 4, v.getComunidadAsignada() == null ? "—" : v.getComunidadAsignada(), z ? e.textoZ : e.texto);
        }
        if (vs.length == 0) {
            texto(h, PRIMERA_FILA, 0, "No hay vehículos registrados.", e.nota);
        } else {
            int ult = PRIMERA_FILA + vs.length;
            int total = ult;
            texto(h, total, 0, "TOTAL", e.negrita);
            formula(h, total, 1, "SUM(B5:B" + ult + ")", a.capacidadTotalTon * 1000, e.entero);
            formula(h, total, 2, "SUM(C5:C" + ult + ")", a.capacidadTotalTon, e.decimal);
            h.setAutoFilter(new CellRangeAddress(FILA_ENCABEZADO, ult - 1, 0, 4));
        }
        h.createFreezePane(0, PRIMERA_FILA);
        int[] anchos = {14, 16, 16, 12, 28};
        for (int j = 0; j < anchos.length; j++) {
            h.setColumnWidth(j, anchos[j] * 256);
        }
    }

    private static void escribirEstadisticas(XSSFSheet h, Estilos e, AnalisisAyuda a, String sub) {
        int nc = a.getComunidades().length;
        int nv = a.getVehiculos().length;
        titulo(h, e, "Estadística descriptiva", sub, 12);
        encabezados(h, e, FILA_ENCABEZADO, "Variable", "n", "Suma", "Media", "Mediana", "Desv. estándar",
                "Mínimo", "Q1", "Q3", "Máximo", "Rango", "Coef. variación (%)");

        String[] nombres = {"Habitantes", "Prioridad (1-10)", "Recursos requeridos (t)", "Beneficio",
                "Capacidad de vehículos (kg)"};
        String[] rangos = {rango("Comunidades", "C", nc), rango("Comunidades", "D", nc),
                rango("Comunidades", "E", nc), rango("Comunidades", "F", nc), rango("Vehiculos", "B", nv)};
        Resumen[] datos = {a.habitantes, a.prioridad, a.recursos, a.beneficio, a.capacidadKg};

        for (int i = 0; i < nombres.length; i++) {
            int fila = PRIMERA_FILA + i;
            int ex = fila + 1;
            String r = rangos[i];
            Resumen d = datos[i];
            boolean z = i % 2 == 1;
            CellStyle num = z ? e.decimalZ : e.decimal;
            texto(h, fila, 0, nombres[i], z ? e.textoZ : e.texto);
            formula(h, fila, 1, "COUNT(" + r + ")", d.n, z ? e.centroZ : e.centro);
            formula(h, fila, 2, "SUM(" + r + ")", d.suma, num);
            formula(h, fila, 3, "IFERROR(AVERAGE(" + r + "),\"n/d\")", d.n > 0 ? d.media : Double.NaN, num);
            formula(h, fila, 4, "IFERROR(MEDIAN(" + r + "),\"n/d\")", d.n > 0 ? d.mediana : Double.NaN, num);
            formula(h, fila, 5, "IFERROR(STDEV(" + r + "),\"n/d\")", d.desviacion, num);
            formula(h, fila, 6, "IF(COUNT(" + r + ")=0,\"n/d\",MIN(" + r + "))", d.n > 0 ? d.minimo : Double.NaN, num);
            formula(h, fila, 7, "IFERROR(QUARTILE(" + r + ",1),\"n/d\")", d.n > 0 ? d.q1 : Double.NaN, num);
            formula(h, fila, 8, "IFERROR(QUARTILE(" + r + ",3),\"n/d\")", d.n > 0 ? d.q3 : Double.NaN, num);
            formula(h, fila, 9, "IF(COUNT(" + r + ")=0,\"n/d\",MAX(" + r + "))", d.n > 0 ? d.maximo : Double.NaN, num);
            formula(h, fila, 10, "IFERROR(J" + ex + "-G" + ex + ",\"n/d\")", d.n > 0 ? d.rango : Double.NaN, num);
            formula(h, fila, 11, "IFERROR(F" + ex + "/D" + ex + "*100,\"n/d\")", d.coeficienteVariacion, num);
        }

        int fila = PRIMERA_FILA + nombres.length + 2;
        texto(h, fila, 0, "Correlación de Pearson: habitantes vs recursos requeridos", e.negrita);
        h.addMergedRegion(new CellRangeAddress(fila, fila, 0, 3));
        double r = a.correlacionHabitantesRecursos;
        formula(h, fila, 4, "IFERROR(CORREL(" + rango("Comunidades", "C", nc) + "," + rango("Comunidades", "E", nc)
                + "),\"n/d\")", r, e.decimal);
        String lectura = Double.isNaN(r) ? "Datos insuficientes"
                : (Math.abs(r) >= 0.7 ? "Fuerte" : Math.abs(r) >= 0.4 ? "Moderada" : "Débil") + (r >= 0 ? " positiva" : " negativa");
        formulaTexto(h, fila, 5, "IF(ISNUMBER(E" + (fila + 1) + "),IF(ABS(E" + (fila + 1) + ")>=0.7,\"Fuerte\",IF(ABS(E"
                + (fila + 1) + ")>=0.4,\"Moderada\",\"Débil\"))&IF(E" + (fila + 1) + ">=0,\" positiva\",\" negativa\"),"
                + "\"Datos insuficientes\")", lectura, e.centro);
        h.addMergedRegion(new CellRangeAddress(fila, fila, 5, 6));
        texto(h, fila + 2, 0, "Q1 y Q3: primer y tercer cuartil (método de Excel). Desviación estándar muestral (n-1). "
                + "Coef. de variación = desviación / media × 100.", e.nota);
        h.addMergedRegion(new CellRangeAddress(fila + 2, fila + 2, 0, 11));

        h.setColumnWidth(0, 30 * 256);
        for (int j = 1; j < 12; j++) {
            h.setColumnWidth(j, 15 * 256);
        }
        h.createFreezePane(1, PRIMERA_FILA);
    }

    private static void escribirResumen(XSSFSheet h, Estilos e, AnalisisAyuda a, String sub) {
        int nc = a.getComunidades().length;
        int nv = a.getVehiculos().length;
        titulo(h, e, "Resumen ejecutivo", sub, 8);
        encabezados(h, e, FILA_ENCABEZADO, "Indicador", "Valor");

        String habit = rango("Comunidades", "C", nc);
        String recur = rango("Comunidades", "E", nc);
        String benef = rango("Comunidades", "F", nc);
        String nombr = rango("Comunidades", "B", nc);
        String capac = rango("Vehiculos", "C", nv);
        String asig = rango("Vehiculos", "D", nv);
        String idv = rango("Vehiculos", "A", nv);

        int f = PRIMERA_FILA;
        Object[][] filas = {
                {"Comunidades registradas", "COUNTA(" + nombr + ")", (double) nc, e.entero},
                {"Habitantes totales", "SUM(" + habit + ")", a.habitantes.suma, e.entero},
                {"Recursos requeridos totales (t)", "SUM(" + recur + ")", a.recursosTotalesTon, e.decimal},
                {"Beneficio total (habitantes × prioridad)", "SUM(" + benef + ")", a.beneficioTotal, e.entero},
                {"Comunidad con mayor beneficio", "INDEX(" + nombr + ",MATCH(MAX(" + benef + ")," + benef + ",0))",
                        a.mayorBeneficio == null ? "—" : a.mayorBeneficio.getNombre(), e.centro},
                {"Vehículos registrados", "COUNTA(" + idv + ")", (double) nv, e.entero},
                {"Capacidad total de la flota (t)", "SUM(" + capac + ")", a.capacidadTotalTon, e.decimal},
                {"Cobertura de la demanda", "IFERROR(B" + (f + 7) + "/B" + (f + 3) + ",0)", a.coberturaPorcentaje / 100.0,
                        e.porcentaje},
                {"Déficit de recursos (t)", "MAX(0,B" + (f + 3) + "-B" + (f + 7) + ")", a.deficitTon, e.decimal},
                {"Vehículos asignados", "COUNTIF(" + asig + ",\"Sí\")", (double) a.vehiculosAsignados, e.entero},
                {"Comunidades con prioridad alta (≥ 8)", "COUNTIF(" + rango("Comunidades", "D", nc) + ",\">=8\")",
                        (double) a.comunidadesPrioridadAlta, e.entero},
        };
        for (int i = 0; i < filas.length; i++) {
            int fila = f + i;
            boolean z = i % 2 == 1;
            texto(h, fila, 0, (String) filas[i][0], z ? e.textoZ : e.texto);
            CellStyle estilo = (CellStyle) filas[i][3];
            if (filas[i][2] instanceof String) {
                formulaTexto(h, fila, 1, (String) filas[i][1], (String) filas[i][2], e.centro);
            } else {
                formula(h, fila, 1, (String) filas[i][1], (Double) filas[i][2], estilo);
            }
        }
        texto(h, f + filas.length + 1, 0, "Las cifras de esta hoja son fórmulas: se actualizan si cambias los datos de las hojas "
                + "Comunidades o Vehiculos. Revisa la hoja Conclusiones para la interpretación.", e.nota);
        h.addMergedRegion(new CellRangeAddress(f + filas.length + 1, f + filas.length + 1, 0, 1));
        h.getRow(f + filas.length + 1).setHeightInPoints(40);
        h.setColumnWidth(0, 42 * 256);
        h.setColumnWidth(1, 24 * 256);
        h.setColumnWidth(2, 3 * 256);
    }

    private static void escribirConclusiones(XSSFSheet h, Estilos e, AnalisisAyuda a, String sub) {
        titulo(h, e, "Conclusiones del análisis", sub, 2);
        encabezados(h, e, FILA_ENCABEZADO, "N.º", "Conclusión");
        List<String> lista = a.conclusiones();
        for (int i = 0; i < lista.size(); i++) {
            int fila = PRIMERA_FILA + i;
            numero(h, fila, 0, i + 1, e.numeroConclusion);
            texto(h, fila, 1, lista.get(i), e.parrafo);
            int lineas = (int) Math.ceil(lista.get(i).length() / 105.0);
            h.getRow(fila).setHeightInPoints(Math.max(32, lineas * 16 + 8));
        }
        texto(h, PRIMERA_FILA + lista.size() + 1, 0, "Conclusiones generadas automáticamente a partir de los datos registrados "
                + "en el momento de la exportación.", e.nota);
        h.addMergedRegion(new CellRangeAddress(PRIMERA_FILA + lista.size() + 1, PRIMERA_FILA + lista.size() + 1, 0, 1));
        h.setColumnWidth(0, 8 * 256);
        h.setColumnWidth(1, 110 * 256);
    }

    // ------------------------------------------------------------------ gráficos

    private static void agregarGraficos(XSSFSheet resumen, XSSFSheet datos, int n) {
        int ult = PRIMERA_FILA + n - 1;
        grafico(resumen, datos, "Beneficio por comunidad", "Beneficio", ult, 5, 3, 3, 11, 19);
        grafico(resumen, datos, "Recursos requeridos por comunidad (t)", "Toneladas", ult, 4, 3, 20, 11, 36);
    }

    private static void grafico(XSSFSheet destino, XSSFSheet datos, String titulo, String serie, int ultimaFila,
                                int colValores, int c1, int f1, int c2, int f2) {
        XSSFDrawing dibujo = destino.createDrawingPatriarch();
        XSSFClientAnchor ancla = dibujo.createAnchor(0, 0, 0, 0, c1, f1, c2, f2);
        XSSFChart grafico = dibujo.createChart(ancla);
        grafico.setTitleText(titulo);
        grafico.setTitleOverlay(false);

        XDDFCategoryAxis ejeX = grafico.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis ejeY = grafico.createValueAxis(AxisPosition.LEFT);
        ejeY.setCrosses(org.apache.poi.xddf.usermodel.chart.AxisCrosses.AUTO_ZERO);

        XDDFDataSource<String> categorias = XDDFDataSourcesFactory.fromStringCellRange(datos,
                new CellRangeAddress(PRIMERA_FILA, ultimaFila, 1, 1));
        XDDFNumericalDataSource<Double> valores = XDDFDataSourcesFactory.fromNumericCellRange(datos,
                new CellRangeAddress(PRIMERA_FILA, ultimaFila, colValores, colValores));

        XDDFBarChartData data = (XDDFBarChartData) grafico.createData(ChartTypes.BAR, ejeX, ejeY);
        data.setBarDirection(BarDirection.COL);
        XDDFBarChartData.Series s = (XDDFBarChartData.Series) data.addSeries(categorias, valores);
        s.setTitle(serie, null);
        grafico.plot(data);
    }
}
