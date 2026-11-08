package uce.edu.ec.ayudahumanitaria.reportes;

import uce.edu.ec.ayudahumanitaria.dominio.AnalisisAyuda;
import uce.edu.ec.ayudahumanitaria.dominio.CentroAcopio;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.Parada;
import uce.edu.ec.ayudahumanitaria.dominio.Pendiente;
import uce.edu.ec.ayudahumanitaria.dominio.PlanDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.PlanificadorDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.Vehiculo;
import uce.edu.ec.ayudahumanitaria.dominio.Viaje;
import uce.edu.ec.ayudahumanitaria.gui.Logo;
import uce.edu.ec.ayudahumanitaria.util.FormatoPlan;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Informe en PDF para registros: resumen, mapa general, centros, comunidades, flota, plan con itinerario
 * de cada viaje (con su mapa) y conclusiones. Usa {@link PdfSencillo}, sin librerías externas.
 */
public final class ExportadorPDF {

    private static final Color AZUL = new Color(15, 64, 118);
    private static final Color AZUL_CLARO = new Color(224, 236, 249);
    private static final Color GRIS_TEXTO = new Color(60, 66, 74);
    private static final Color GRIS_LINEA = new Color(207, 215, 224);
    private static final Color ZEBRA = new Color(243, 246, 250);
    private static final Color NEGRO = new Color(30, 34, 39);

    private static final double MARGEN = 40;
    private static final double ANCHO_UTIL = PdfSencillo.ANCHO - 2 * MARGEN;
    private static final double LIMITE_INFERIOR = PdfSencillo.ALTO - 50;
    private static final double TAM = 8.5;
    private static final double INTERLINEA = 11;

    private final PdfSencillo pdf = new PdfSencillo();
    private final DatosOperativos datos;
    private PdfSencillo.Pagina pagina;
    private double y;

    private ExportadorPDF(DatosOperativos datos) {
        this.datos = datos;
    }

    /** Genera el informe completo en {@code destino}. */
    public static void exportar(File destino, DatosOperativos datos) throws IOException {
        new ExportadorPDF(datos).generar(destino);
    }

    // ------------------------------------------------------------------ estructura

    private void generar(File destino) throws IOException {
        pdf.setMetadatos("Informe de distribución de ayuda humanitaria", "Ayuda Humanitaria");
        portada();
        centros();
        comunidades();
        flota();
        if (datos.plan() != null) {
            plan();
            viajes();
        }
        conclusiones();
        pies();
        try (FileOutputStream salida = new FileOutputStream(destino)) {
            pdf.guardar(salida);
        }
    }

    private void portada() throws IOException {
        nuevaPagina();
        pdf.rectanguloRelleno(pagina, 0, 0, PdfSencillo.ANCHO, 96, AZUL);
        int logo = pdf.registrarImagen(Logo.crear(128));
        pdf.dibujarImagen(pagina, logo, MARGEN, 16, 64, 64);
        pdf.texto(pagina, MARGEN + 80, 44, PdfSencillo.Fuente.NEGRITA, 20, Color.WHITE, "Ayuda Humanitaria");
        pdf.texto(pagina, MARGEN + 80, 64, PdfSencillo.Fuente.NORMAL, 11, Color.WHITE,
                "Informe de distribución · Ecuador continental");
        pdf.texto(pagina, MARGEN + 80, 80, PdfSencillo.Fuente.NORMAL, 9, new Color(200, 218, 240),
                "Generado el " + FormatoPlan.fechaHora(datos.generadoEn()));
        y = 116;

        PlanDistribucion plan = datos.plan();
        double stock = datos.centros().stream().mapToDouble(CentroAcopio::getStockToneladas).sum();
        double requerido = datos.comunidades().stream().mapToDouble(Comunidad::getRecursosRequeridos).sum();
        double entregado = datos.entregadoPorComunidad().values().stream().mapToDouble(Double::doubleValue).sum();
        List<String[]> resumen = new ArrayList<>();
        resumen.add(new String[]{"Centros de acopio", String.valueOf(datos.centros().size())});
        resumen.add(new String[]{"Comunidades registradas", String.valueOf(datos.comunidades().size())});
        resumen.add(new String[]{"Vehículos", String.valueOf(datos.vehiculos().size())});
        resumen.add(new String[]{"Stock disponible en centros", FormatoPlan.numero(stock, 1) + " t"});
        resumen.add(new String[]{"Necesidad total de las comunidades", FormatoPlan.numero(requerido, 1) + " t"});
        resumen.add(new String[]{"Entregado hasta ahora", FormatoPlan.numero(entregado, 1) + " t"});
        if (plan != null) {
            String estado = plan.id() == 0 ? "Simulación (sin guardar)" : "Plan " + plan.id();
            resumen.add(new String[]{"Plan reportado", estado});
            resumen.add(new String[]{"Viajes / toneladas / km", plan.viajes().size() + " / "
                    + FormatoPlan.numero(plan.totalToneladas(), 1) + " t / " + FormatoPlan.numero(plan.totalKm(), 0) + " km"});
            resumen.add(new String[]{"Salida programada", FormatoPlan.fechaHora(plan.salidaProgramada())});
            resumen.add(new String[]{"Fin estimado", FormatoPlan.fechaHora(plan.finEstimado())});
            resumen.add(new String[]{"Pendiente de cubrir", FormatoPlan.numero(plan.toneladasPendientes(), 1) + " t"});
        } else {
            resumen.add(new String[]{"Plan reportado", "Aún no hay un plan de distribución"});
        }
        titulo("Resumen");
        tabla(new String[]{"Concepto", "Valor"}, new double[]{0.55, 0.45}, resumen, true);

        if (datos.mapaGeneral() != null && datos.mapaGeneral().length > 0) {
            titulo("Mapa general de centros, comunidades y rutas");
            BufferedImage mapa = ImageIO.read(new ByteArrayInputStream(datos.mapaGeneral()));
            if (mapa != null) {
                imagenAjustada(mapa, 365);
            }
            texto("Línea de color: ruta del viaje (un color por centro). Círculo rojo numerado: parada. "
                    + "Mapa esquemático; las distancias son estimaciones por carretera.", PdfSencillo.Fuente.NORMAL, 8, GRIS_TEXTO);
        }
    }

    private void centros() {
        titulo("Centros de acopio");
        List<String[]> filas = new ArrayList<>();
        for (CentroAcopio c : datos.centros()) {
            long vehiculos = datos.vehiculos().stream().filter(v -> v.getCentroId() == c.getId()).count();
            long atendidas = datos.comunidades().stream().filter(Comunidad::tieneUbicacion)
                    .filter(co -> PlanificadorDistribucion.centroMasCercano(datos.centros(), co).getId() == c.getId()).count();
            filas.add(new String[]{c.getNombre(), c.getProvincia(), FormatoPlan.numero(c.getLatitud(), 3) + ", "
                    + FormatoPlan.numero(c.getLongitud(), 3), FormatoPlan.numero(c.getStockToneladas(), 1),
                    String.valueOf(vehiculos), String.valueOf(atendidas)});
        }
        tabla(new String[]{"Centro", "Provincia", "Ubicación", "Stock (t)", "Vehíc.", "Comunid."},
                new double[]{0.27, 0.2, 0.22, 0.11, 0.09, 0.11}, filas, false);
    }

    private void comunidades() {
        titulo("Comunidades y avance de entrega");
        List<String[]> filas = new ArrayList<>();
        for (Comunidad c : datos.comunidades()) {
            String centro = "—";
            String km = "—";
            if (c.tieneUbicacion() && !datos.centros().isEmpty()) {
                CentroAcopio cercano = PlanificadorDistribucion.centroMasCercano(datos.centros(), c);
                centro = cercano.getCiudad();
                km = FormatoPlan.numero(cercano.distanciaCarreteraKm(c.getLatitud(), c.getLongitud()), 0);
            }
            filas.add(new String[]{c.getNombre(), c.getProvincia(), String.valueOf(c.getHabitantes()),
                    String.valueOf(c.getPrioridad()), FormatoPlan.numero(c.getRecursosRequeridos(), 1), centro, km,
                    FormatoPlan.numero(datos.planificadoPorComunidad().getOrDefault(c.getId(), 0.0), 1),
                    FormatoPlan.numero(datos.entregadoPorComunidad().getOrDefault(c.getId(), 0.0), 1)});
        }
        tabla(new String[]{"Comunidad", "Provincia", "Habit.", "Prior.", "Req. (t)", "Centro", "Km", "Planif.", "Entreg."},
                new double[]{0.20, 0.15, 0.09, 0.07, 0.09, 0.12, 0.07, 0.10, 0.11}, filas, false);
    }

    private void flota() {
        titulo("Flota de vehículos");
        List<String[]> filas = new ArrayList<>();
        for (Vehiculo v : datos.vehiculos()) {
            filas.add(new String[]{v.getId(), FormatoPlan.numero(v.getCapacidad(), 0),
                    v.getCentroNombre() == null ? "—" : v.getCentroNombre(), v.isAsignado() ? "Asignado" : "Libre",
                    v.getComunidadAsignada() == null ? "—" : v.getComunidadAsignada()});
        }
        tabla(new String[]{"Vehículo", "Capacidad (kg)", "Centro", "Estado", "Comunidad asignada"},
                new double[]{0.16, 0.16, 0.26, 0.14, 0.28}, filas, false);
    }

    private void plan() {
        PlanDistribucion plan = datos.plan();
        titulo("Plan de distribución" + (plan.id() == 0 ? " (simulación)" : " " + plan.id()));
        List<String[]> filas = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();
        for (Viaje v : plan.viajes()) {
            filas.add(new String[]{v.id() == 0 ? "—" : String.valueOf(v.id()), v.centro().getCiudad(), v.vehiculoId(),
                    FormatoPlan.numero(v.cargaToneladas(), 1), FormatoPlan.numero(v.distanciaKm(), 0),
                    FormatoPlan.fechaHora(v.salida()), FormatoPlan.fechaHora(v.regreso()), v.estadoEn(ahora).etiqueta()});
        }
        tabla(new String[]{"Viaje", "Centro", "Vehíc.", "Carga (t)", "Km", "Salida", "Regreso", "Estado"},
                new double[]{0.07, 0.15, 0.10, 0.10, 0.08, 0.20, 0.20, 0.10}, filas, false);

        if (!plan.pendientes().isEmpty()) {
            titulo("Necesidades pendientes");
            List<String[]> pendientes = new ArrayList<>();
            for (Pendiente p : plan.pendientes()) {
                pendientes.add(new String[]{p.comunidadNombre(), p.centroNombre(), FormatoPlan.numero(p.toneladas(), 1),
                        p.motivo()});
            }
            tabla(new String[]{"Comunidad", "Centro", "Pend. (t)", "Motivo"}, new double[]{0.26, 0.20, 0.10, 0.44},
                    pendientes, false);
        }
    }

    private void viajes() throws IOException {
        PlanDistribucion plan = datos.plan();
        for (int i = 0; i < plan.viajes().size(); i++) {
            Viaje v = plan.viajes().get(i);
            asegurarEspacio(150);
            titulo("Viaje " + (v.id() == 0 ? (i + 1) : v.id()) + " · " + v.centro().getCiudad() + " · vehículo "
                    + v.vehiculoId());
            texto(String.format("Carga %s t de %s t (%s %%) · %s km · duración %s · salida %s · regreso %s",
                            FormatoPlan.numero(v.cargaToneladas(), 1), FormatoPlan.numero(v.capacidadToneladas(), 1),
                            FormatoPlan.numero(v.ocupacion() * 100, 0), FormatoPlan.numero(v.distanciaKm(), 0),
                            FormatoPlan.duracion(v.duracion()), FormatoPlan.fechaHora(v.salida()), FormatoPlan.fechaHora(v.regreso())),
                    PdfSencillo.Fuente.NORMAL, TAM, NEGRO);
            List<String[]> filas = new ArrayList<>();
            filas.add(new String[]{"Salida", v.centro().getNombre(), "", "", "", FormatoPlan.fechaHora(v.salida())});
            double latAnterior = v.centro().getLatitud();
            double lonAnterior = v.centro().getLongitud();
            for (Parada p : v.paradas()) {
                List<String> pasa = FormatoPlan.ciudadesDelTramo(latAnterior, lonAnterior, p.latitud(), p.longitud());
                String comunidad = p.comunidadNombre() + (pasa.isEmpty() ? "" : " (pasa por " + String.join(", ", pasa) + ")");
                filas.add(new String[]{String.valueOf(p.orden()), comunidad, FormatoPlan.numero(p.toneladas(), 1),
                        FormatoPlan.numero(p.kmAcumulados(), 0), FormatoPlan.fechaHora(p.llegada()),
                        FormatoPlan.fechaHora(p.salida())});
                latAnterior = p.latitud();
                lonAnterior = p.longitud();
            }
            filas.add(new String[]{"Regreso", v.centro().getNombre(), "", "", FormatoPlan.fechaHora(v.regreso()), ""});
            tabla(new String[]{"Parada", "Comunidad", "Entrega (t)", "Km acum.", "Llegada", "Salida"},
                    new double[]{0.09, 0.37, 0.11, 0.10, 0.165, 0.165}, filas, false);

            if (i < datos.mapasViaje().size() && datos.mapasViaje().get(i) != null) {
                BufferedImage mapa = ImageIO.read(new ByteArrayInputStream(datos.mapasViaje().get(i)));
                if (mapa != null) {
                    imagenAjustada(mapa, 330);
                }
            }
        }
    }

    private void conclusiones() {
        if (datos.comunidades().isEmpty()) {
            return;
        }
        AnalisisAyuda analisis = new AnalisisAyuda(datos.comunidades().toArray(new Comunidad[0]),
                datos.vehiculos().toArray(new Vehiculo[0]));
        titulo("Conclusiones");
        int n = 1;
        for (String conclusion : analisis.conclusiones()) {
            texto(n++ + ". " + conclusion, PdfSencillo.Fuente.NORMAL, 9, NEGRO);
            y += 3;
        }
        y += 6;
        texto("Notas: las distancias son estimadas (línea recta x 1.35) y los tiempos suponen 50 km/h, máximo 8 h de "
                        + "conducción diaria y descansos de 10 h; no consideran tráfico, clima ni el estado de las vías.",
                PdfSencillo.Fuente.NORMAL, 8, GRIS_TEXTO);
    }

    /** Numeración y encabezado de cada página, una vez conocido el total. */
    private void pies() {
        int total = pdf.cantidadPaginas();
        for (int i = 0; i < total; i++) {
            PdfSencillo.Pagina p = pdf.pagina(i);
            pdf.linea(p, MARGEN, PdfSencillo.ALTO - 36, PdfSencillo.ANCHO - MARGEN, PdfSencillo.ALTO - 36, 0.5, GRIS_LINEA);
            pdf.texto(p, MARGEN, PdfSencillo.ALTO - 24, PdfSencillo.Fuente.NORMAL, 8, GRIS_TEXTO,
                    "Ayuda Humanitaria · " + FormatoPlan.fechaHora(datos.generadoEn()));
            String numero = "Página " + (i + 1) + " de " + total;
            pdf.texto(p, PdfSencillo.ANCHO - MARGEN - PdfSencillo.anchoTexto(numero, PdfSencillo.Fuente.NORMAL, 8),
                    PdfSencillo.ALTO - 24, PdfSencillo.Fuente.NORMAL, 8, GRIS_TEXTO, numero);
        }
    }

    // ------------------------------------------------------------------ bloques de maquetación

    private void nuevaPagina() {
        pagina = pdf.nuevaPagina();
        y = MARGEN;
    }

    private void asegurarEspacio(double altoNecesario) {
        if (y + altoNecesario > LIMITE_INFERIOR) {
            nuevaPagina();
        }
    }

    private void titulo(String texto) {
        asegurarEspacio(40);
        y += 10;
        pdf.texto(pagina, MARGEN, y + 11, PdfSencillo.Fuente.NEGRITA, 12.5, AZUL, texto);
        y += 17;
        pdf.linea(pagina, MARGEN, y, MARGEN + ANCHO_UTIL, y, 1.2, AZUL);
        y += 8;
    }

    private void texto(String texto, PdfSencillo.Fuente fuente, double tamano, Color color) {
        for (String linea : PdfSencillo.ajustar(texto, fuente, tamano, ANCHO_UTIL)) {
            asegurarEspacio(tamano + 4);
            pdf.texto(pagina, MARGEN, y + tamano, fuente, tamano, color, linea);
            y += tamano + 3;
        }
    }

    /** Dibuja la imagen a todo el ancho útil, pasando de página si no cabe. */
    private void imagenAjustada(BufferedImage imagen, double maximo) throws IOException {
        double ancho = ANCHO_UTIL;
        double alto = ancho * imagen.getHeight() / imagen.getWidth();
        if (alto > maximo) {
            ancho = ancho * maximo / alto;
            alto = maximo;
        }
        asegurarEspacio(alto + 6);
        int id = pdf.registrarImagen(imagen);
        double x = MARGEN + (ANCHO_UTIL - ancho) / 2;
        pdf.dibujarImagen(pagina, id, x, y, ancho, alto);
        pdf.rectanguloBorde(pagina, x, y, ancho, alto, 0.6, GRIS_LINEA);
        y += alto + 6;
    }

    /**
     * Tabla con encabezado repetido en cada página. Las celdas largas se parten en varias líneas.
     *
     * @param proporciones fracción del ancho útil para cada columna (suman 1)
     * @param primeraNegrita si la primera columna va en negrita (útil en tablas concepto/valor)
     */
    private void tabla(String[] encabezados, double[] proporciones, List<String[]> filas, boolean primeraNegrita) {
        double[] anchos = new double[proporciones.length];
        for (int i = 0; i < anchos.length; i++) {
            anchos[i] = ANCHO_UTIL * proporciones[i];
        }
        encabezadoTabla(encabezados, anchos);
        int indice = 0;
        for (String[] fila : filas) {
            List<List<String>> celdas = new ArrayList<>();
            int lineasMaximas = 1;
            for (int c = 0; c < anchos.length; c++) {
                String valor = c < fila.length && fila[c] != null ? fila[c] : "";
                PdfSencillo.Fuente f = primeraNegrita && c == 0 ? PdfSencillo.Fuente.NEGRITA : PdfSencillo.Fuente.NORMAL;
                List<String> lineas = PdfSencillo.ajustar(valor, f, TAM, anchos[c] - 6);
                celdas.add(lineas);
                lineasMaximas = Math.max(lineasMaximas, lineas.size());
            }
            double alto = lineasMaximas * INTERLINEA + 4;
            if (y + alto > LIMITE_INFERIOR) {
                nuevaPagina();
                encabezadoTabla(encabezados, anchos);
            }
            if (indice % 2 == 1) {
                pdf.rectanguloRelleno(pagina, MARGEN, y, ANCHO_UTIL, alto, ZEBRA);
            }
            double x = MARGEN;
            for (int c = 0; c < anchos.length; c++) {
                PdfSencillo.Fuente f = primeraNegrita && c == 0 ? PdfSencillo.Fuente.NEGRITA : PdfSencillo.Fuente.NORMAL;
                double yl = y + 2 + TAM;
                for (String linea : celdas.get(c)) {
                    pdf.texto(pagina, x + 3, yl, f, TAM, NEGRO, linea);
                    yl += INTERLINEA;
                }
                x += anchos[c];
            }
            pdf.linea(pagina, MARGEN, y + alto, MARGEN + ANCHO_UTIL, y + alto, 0.3, GRIS_LINEA);
            y += alto;
            indice++;
        }
        if (filas.isEmpty()) {
            texto("Sin registros.", PdfSencillo.Fuente.NORMAL, TAM, GRIS_TEXTO);
        }
        y += 6;
    }

    private void encabezadoTabla(String[] encabezados, double[] anchos) {
        asegurarEspacio(2 * INTERLINEA + 8);
        double alto = INTERLINEA + 6;
        pdf.rectanguloRelleno(pagina, MARGEN, y, ANCHO_UTIL, alto, AZUL);
        double x = MARGEN;
        for (int c = 0; c < anchos.length; c++) {
            String t = c < encabezados.length ? encabezados[c] : "";
            pdf.texto(pagina, x + 3, y + 3 + TAM, PdfSencillo.Fuente.NEGRITA, TAM, Color.WHITE, t);
            x += anchos[c];
        }
        y += alto;
    }
}
