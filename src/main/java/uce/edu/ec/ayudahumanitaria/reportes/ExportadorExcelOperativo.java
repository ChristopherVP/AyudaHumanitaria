package uce.edu.ec.ayudahumanitaria.reportes;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import uce.edu.ec.ayudahumanitaria.dominio.CentroAcopio;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.EstadoViaje;
import uce.edu.ec.ayudahumanitaria.dominio.Parada;
import uce.edu.ec.ayudahumanitaria.dominio.Pendiente;
import uce.edu.ec.ayudahumanitaria.dominio.PlanDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.PlanificadorDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.Vehiculo;
import uce.edu.ec.ayudahumanitaria.dominio.Viaje;
import uce.edu.ec.ayudahumanitaria.util.FormatoPlan;

import java.util.List;

/**
 * Hojas operativas del libro de Excel: centros, plan de distribución, itinerario, entregas por comunidad,
 * pendientes y mapas. Se agregan al libro que ya genera {@link ExportadorExcel}.
 */
final class ExportadorExcelOperativo {

    private static final String FUENTE = "Arial";

    private final XSSFWorkbook libro;
    private final CellStyle titulo;
    private final CellStyle subtitulo;
    private final CellStyle encabezado;
    private final CellStyle texto;
    private final CellStyle entero;
    private final CellStyle decimal;
    private final CellStyle fecha;
    private final CellStyle porcentajeEstilo;

    private ExportadorExcelOperativo(XSSFWorkbook libro) {
        this.libro = libro;
        this.titulo = estilo(fuente(true, 16, Font.COLOR_NORMAL), null, HorizontalAlignment.LEFT, null);
        this.subtitulo = estilo(fuente(false, 10, Font.COLOR_NORMAL), null, HorizontalAlignment.LEFT, null);
        this.encabezado = estilo(fuente(true, 11, IndexedColors.WHITE.getIndex()), IndexedColors.DARK_BLUE,
                HorizontalAlignment.CENTER, null);
        this.encabezado.setWrapText(true);
        this.texto = estilo(fuente(false, 10, Font.COLOR_NORMAL), null, HorizontalAlignment.LEFT, null);
        this.entero = estilo(fuente(false, 10, Font.COLOR_NORMAL), null, HorizontalAlignment.CENTER, "#,##0");
        this.decimal = estilo(fuente(false, 10, Font.COLOR_NORMAL), null, HorizontalAlignment.CENTER, "#,##0.00");
        this.porcentajeEstilo = estilo(fuente(false, 10, Font.COLOR_NORMAL), null, HorizontalAlignment.CENTER, "0.0%");
        this.fecha = estilo(fuente(false, 10, Font.COLOR_NORMAL), null, HorizontalAlignment.CENTER, null);
    }

    /** Agrega todas las hojas operativas al libro. */
    static void agregar(XSSFWorkbook libro, DatosOperativos datos, String subtituloHoja) {
        new ExportadorExcelOperativo(libro).escribir(datos, subtituloHoja);
    }

    private void escribir(DatosOperativos d, String sub) {
        hojaCentros(d, sub);
        hojaEntregas(d, sub);
        if (d.plan() != null) {
            hojaPlan(d.plan(), sub);
            hojaItinerario(d.plan(), sub);
            hojaPendientes(d.plan(), sub);
        }
        hojaMapas(d, sub);
    }

    // ------------------------------------------------------------------ hojas

    private void hojaCentros(DatosOperativos d, String sub) {
        XSSFSheet h = libro.createSheet("Centros");
        cabecera(h, "Centros de acopio", sub, 9);
        encabezados(h, 3, "ID", "Nombre", "Ciudad", "Provincia", "Latitud", "Longitud", "Stock (t)", "Vehículos",
                "Comunidades atendidas");
        int fila = 4;
        for (CentroAcopio c : d.centros()) {
            long vehiculos = d.vehiculos().stream().filter(v -> v.getCentroId() == c.getId()).count();
            long atendidas = d.comunidades().stream().filter(Comunidad::tieneUbicacion)
                    .filter(co -> PlanificadorDistribucion.centroMasCercano(d.centros(), co).getId() == c.getId()).count();
            Row r = h.createRow(fila++);
            num(r, 0, c.getId(), entero);
            txt(r, 1, c.getNombre());
            txt(r, 2, c.getCiudad());
            txt(r, 3, c.getProvincia());
            num(r, 4, c.getLatitud(), decimal);
            num(r, 5, c.getLongitud(), decimal);
            num(r, 6, c.getStockToneladas(), decimal);
            num(r, 7, vehiculos, entero);
            num(r, 8, atendidas, entero);
        }
        if (!d.centros().isEmpty()) {
            Row total = h.createRow(fila);
            txt(total, 1, "TOTAL");
            formula(total, 6, "SUM(G5:G" + fila + ")", decimal);
            formula(total, 7, "SUM(H5:H" + fila + ")", entero);
            formula(total, 8, "SUM(I5:I" + fila + ")", entero);
        }
        ancho(h, 6, 28, 18, 18, 10, 10, 10, 10, 14);
    }

    private void hojaEntregas(DatosOperativos d, String sub) {
        XSSFSheet h = libro.createSheet("Entregas");
        cabecera(h, "Avance de entregas por comunidad", sub, 11);
        encabezados(h, 3, "ID", "Comunidad", "Provincia", "Prioridad", "Requerido (t)", "Centro asignado",
                "Distancia (km)", "Planificado (t)", "Entregado (t)", "Pendiente (t)", "Avance");
        int fila = 4;
        for (Comunidad c : d.comunidades()) {
            String centro = "—";
            double km = Double.NaN;
            if (c.tieneUbicacion() && !d.centros().isEmpty()) {
                CentroAcopio cercano = PlanificadorDistribucion.centroMasCercano(d.centros(), c);
                centro = cercano.getCiudad();
                km = cercano.distanciaCarreteraKm(c.getLatitud(), c.getLongitud());
            }
            Row r = h.createRow(fila);
            int ex = fila + 1;
            num(r, 0, c.getId(), entero);
            txt(r, 1, c.getNombre());
            txt(r, 2, c.getProvincia());
            num(r, 3, c.getPrioridad(), entero);
            num(r, 4, c.getRecursosRequeridos(), decimal);
            txt(r, 5, centro);
            if (Double.isNaN(km)) {
                txt(r, 6, "—");
            } else {
                num(r, 6, Math.round(km), entero);
            }
            num(r, 7, d.planificadoPorComunidad().getOrDefault(c.getId(), 0.0), decimal);
            num(r, 8, d.entregadoPorComunidad().getOrDefault(c.getId(), 0.0), decimal);
            formula(r, 9, "MAX(E" + ex + "-H" + ex + ",0)", decimal);
            formula(r, 10, "IF(E" + ex + "=0,0,MIN(I" + ex + "/E" + ex + ",1))", porcentajeEstilo);
            fila++;
        }
        if (!d.comunidades().isEmpty()) {
            Row total = h.createRow(fila);
            txt(total, 1, "TOTAL");
            for (int col : new int[]{4, 7, 8, 9}) {
                String letra = String.valueOf((char) ('A' + col));
                formula(total, col, "SUM(" + letra + "5:" + letra + fila + ")", decimal);
            }
        }
        ancho(h, 6, 28, 18, 10, 13, 16, 13, 13, 13, 13, 10);
    }

    private void hojaPlan(PlanDistribucion plan, String sub) {
        XSSFSheet h = libro.createSheet("Plan");
        String estado = plan.id() == 0 ? "Simulación sin guardar" : "Plan " + plan.id();
        cabecera(h, "Plan de distribución — " + estado, sub, 11);
        encabezados(h, 3, "Viaje", "Centro", "Vehículo", "Carga (t)", "Capacidad (t)", "Ocupación", "Km", "Salida",
                "Regreso", "Estado", "Ruta");
        int fila = 4;
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        for (Viaje v : plan.viajes()) {
            Row r = h.createRow(fila);
            int ex = fila + 1;
            num(r, 0, v.id(), entero);
            txt(r, 1, v.centro().getCiudad());
            txt(r, 2, v.vehiculoId());
            num(r, 3, v.cargaToneladas(), decimal);
            num(r, 4, v.capacidadToneladas(), decimal);
            formula(r, 5, "IF(E" + ex + "=0,0,D" + ex + "/E" + ex + ")", porcentajeEstilo);
            num(r, 6, Math.round(v.distanciaKm()), entero);
            fechaCelda(r, 7, FormatoPlan.fechaHora(v.salida()));
            fechaCelda(r, 8, FormatoPlan.fechaHora(v.regreso()));
            EstadoViaje e = v.estadoEn(ahora);
            txt(r, 9, e.etiqueta());
            txt(r, 10, FormatoPlan.ruta(v));
            fila++;
        }
        if (!plan.viajes().isEmpty()) {
            Row total = h.createRow(fila);
            txt(total, 2, "TOTAL");
            formula(total, 3, "SUM(D5:D" + fila + ")", decimal);
            formula(total, 6, "SUM(G5:G" + fila + ")", entero);
        }
        ancho(h, 7, 14, 10, 11, 13, 10, 8, 17, 17, 12, 70);
    }

    private void hojaItinerario(PlanDistribucion plan, String sub) {
        XSSFSheet h = libro.createSheet("Itinerario");
        cabecera(h, "Itinerario por viaje (paradas con hora de llegada y salida)", sub, 11);
        encabezados(h, 3, "Viaje", "Vehículo", "Centro", "Parada", "Comunidad", "Provincia", "Entrega (t)",
                "Km tramo", "Km acumulados", "Llegada", "Salida");
        int fila = 4;
        for (Viaje v : plan.viajes()) {
            Row origen = h.createRow(fila++);
            num(origen, 0, v.id(), entero);
            txt(origen, 1, v.vehiculoId());
            txt(origen, 2, v.centro().getCiudad());
            txt(origen, 3, "Salida");
            txt(origen, 4, v.centro().getNombre());
            fechaCelda(origen, 10, FormatoPlan.fechaHora(v.salida()));
            for (Parada p : v.paradas()) {
                Row r = h.createRow(fila++);
                num(r, 0, v.id(), entero);
                txt(r, 1, v.vehiculoId());
                txt(r, 2, v.centro().getCiudad());
                num(r, 3, p.orden(), entero);
                txt(r, 4, p.comunidadNombre());
                txt(r, 5, p.provincia());
                num(r, 6, p.toneladas(), decimal);
                num(r, 7, Math.round(p.kmTramo()), entero);
                num(r, 8, Math.round(p.kmAcumulados()), entero);
                fechaCelda(r, 9, FormatoPlan.fechaHora(p.llegada()));
                fechaCelda(r, 10, FormatoPlan.fechaHora(p.salida()));
            }
            Row regreso = h.createRow(fila++);
            num(regreso, 0, v.id(), entero);
            txt(regreso, 1, v.vehiculoId());
            txt(regreso, 2, v.centro().getCiudad());
            txt(regreso, 3, "Regreso");
            txt(regreso, 4, v.centro().getNombre());
            fechaCelda(regreso, 9, FormatoPlan.fechaHora(v.regreso()));
        }
        ancho(h, 7, 12, 14, 9, 30, 18, 11, 10, 13, 17, 17);
    }

    private void hojaPendientes(PlanDistribucion plan, String sub) {
        XSSFSheet h = libro.createSheet("Pendientes");
        cabecera(h, "Necesidades que quedaron sin cubrir en este plan", sub, 4);
        encabezados(h, 3, "Comunidad", "Centro", "Pendiente (t)", "Motivo");
        int fila = 4;
        for (Pendiente p : plan.pendientes()) {
            Row r = h.createRow(fila++);
            txt(r, 0, p.comunidadNombre());
            txt(r, 1, p.centroNombre());
            num(r, 2, p.toneladas(), decimal);
            txt(r, 3, p.motivo());
        }
        if (plan.pendientes().isEmpty()) {
            txt(h.createRow(fila), 0, "Todas las necesidades quedaron cubiertas por el plan.");
        }
        ancho(h, 28, 20, 14, 80);
    }

    private void hojaMapas(DatosOperativos d, String sub) {
        XSSFSheet h = libro.createSheet("Mapa");
        cabecera(h, "Mapa de centros, comunidades y rutas", sub, 12);
        int filaActual = 3;
        if (d.mapaGeneral() != null && d.mapaGeneral().length > 0) {
            filaActual = imagen(h, d.mapaGeneral(), filaActual, 0, 12, 36) + 1;
        }
        if (d.plan() != null) {
            for (int i = 0; i < d.mapasViaje().size() && i < d.plan().viajes().size(); i++) {
                Viaje v = d.plan().viajes().get(i);
                Row r = h.createRow(filaActual++);
                Cell c = r.createCell(0);
                c.setCellValue("Viaje " + v.id() + " · " + v.centro().getCiudad() + " · vehículo " + v.vehiculoId()
                        + " · " + FormatoPlan.ruta(v));
                c.setCellStyle(subtitulo);
                filaActual = imagen(h, d.mapasViaje().get(i), filaActual, 0, 12, 30) + 1;
            }
        }
    }

    /** Inserta un PNG ocupando {@code columnas} x {@code filas} celdas y devuelve la fila siguiente a la imagen. */
    private int imagen(XSSFSheet h, byte[] png, int fila, int col, int columnas, int filas) {
        int idx = libro.addPicture(png, Workbook.PICTURE_TYPE_PNG);
        XSSFDrawing dibujo = h.getDrawingPatriarch() != null ? h.getDrawingPatriarch() : h.createDrawingPatriarch();
        XSSFClientAnchor anclaje = new XSSFClientAnchor(0, 0, 0, 0, col, fila, col + columnas, fila + filas);
        dibujo.createPicture(anclaje, idx);
        return fila + filas;
    }

    // ------------------------------------------------------------------ utilidades

    private void cabecera(XSSFSheet h, String texto, String sub, int columnas) {
        Row t = h.createRow(0);
        Cell c = t.createCell(0);
        c.setCellValue(texto);
        c.setCellStyle(titulo);
        t.setHeightInPoints(26);
        Row s = h.createRow(1);
        Cell cs = s.createCell(0);
        cs.setCellValue(sub);
        cs.setCellStyle(subtitulo);
        if (columnas > 1) {
            h.addMergedRegion(new CellRangeAddress(0, 0, 0, columnas - 1));
            h.addMergedRegion(new CellRangeAddress(1, 1, 0, columnas - 1));
        }
        h.setDisplayGridlines(false);
    }

    private void encabezados(XSSFSheet h, int fila, String... titulos) {
        Row r = h.createRow(fila);
        for (int j = 0; j < titulos.length; j++) {
            Cell c = r.createCell(j);
            c.setCellValue(titulos[j]);
            c.setCellStyle(encabezado);
        }
        r.setHeightInPoints(32);
        h.createFreezePane(0, fila + 1);
    }

    private void txt(Row r, int col, String valor) {
        Cell c = r.createCell(col);
        c.setCellValue(valor);
        c.setCellStyle(texto);
    }

    private void num(Row r, int col, double valor, CellStyle estilo) {
        Cell c = r.createCell(col);
        c.setCellValue(valor);
        c.setCellStyle(estilo);
    }

    private void fechaCelda(Row r, int col, String valor) {
        Cell c = r.createCell(col);
        c.setCellValue(valor);
        c.setCellStyle(fecha);
    }

    private void formula(Row r, int col, String f, CellStyle estilo) {
        Cell c = r.createCell(col);
        c.setCellFormula(f);
        c.setCellStyle(estilo);
    }

    private static void ancho(XSSFSheet h, int... anchosEnCaracteres) {
        for (int i = 0; i < anchosEnCaracteres.length; i++) {
            h.setColumnWidth(i, Math.min(255, anchosEnCaracteres[i] + 2) * 256);
        }
    }

    private Font fuente(boolean negrita, int tam, short color) {
        Font f = libro.createFont();
        f.setFontName(FUENTE);
        f.setBold(negrita);
        f.setFontHeightInPoints((short) tam);
        f.setColor(color);
        return f;
    }

    private CellStyle estilo(Font fuente, IndexedColors relleno, HorizontalAlignment alineacion, String formato) {
        CellStyle s = libro.createCellStyle();
        s.setFont(fuente);
        s.setAlignment(alineacion);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        if (relleno != null) {
            s.setFillForegroundColor(relleno.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (formato != null) {
            s.setDataFormat(libro.createDataFormat().getFormat(formato));
        }
        s.setBorderBottom(BorderStyle.HAIR);
        return s;
    }
}
