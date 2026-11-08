package uce.edu.ec.ayudahumanitaria.reportes;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Escritor mínimo de PDF (sin librerías externas): páginas A4 con texto Helvetica, líneas, rectángulos
 * e imágenes RGB comprimidas. Las coordenadas se dan en puntos desde la esquina superior izquierda.
 * <p>
 * Solo admite el juego de caracteres Windows-1252 (español incluido); lo demás se reemplaza por "?".
 */
public final class PdfSencillo {

    public static final double ANCHO = 595.28;
    public static final double ALTO = 841.89;

    private static final Charset WIN_ANSI = Charset.forName("windows-1252");

    public enum Fuente {
        NORMAL("F1", "Helvetica"), NEGRITA("F2", "Helvetica-Bold");

        private final String recurso;
        private final String nombreBase;

        Fuente(String recurso, String nombreBase) {
            this.recurso = recurso;
            this.nombreBase = nombreBase;
        }
    }

    /** Anchos Helvetica (1/1000 em) para los caracteres ASCII 32..126. */
    private static final int[] ANCHOS_NORMAL = {
            278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
            1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
            333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
            556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584};
    private static final int[] ANCHOS_NEGRITA = {
            278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611,
            975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556,
            333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611,
            611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584};

    /** Una página del documento. */
    public static final class Pagina {
        private final StringBuilder contenido = new StringBuilder();
        private final Map<String, Integer> imagenesUsadas = new LinkedHashMap<>();
    }

    private record ImagenPdf(byte[] datos, int ancho, int alto) {
    }

    private final List<Pagina> paginas = new ArrayList<>();
    private final List<ImagenPdf> imagenes = new ArrayList<>();
    private String titulo = "Documento";
    private String autor = "";

    public void setMetadatos(String titulo, String autor) {
        this.titulo = titulo == null ? "" : titulo;
        this.autor = autor == null ? "" : autor;
    }

    public Pagina nuevaPagina() {
        Pagina p = new Pagina();
        paginas.add(p);
        return p;
    }

    public int cantidadPaginas() {
        return paginas.size();
    }

    public Pagina pagina(int indice) {
        return paginas.get(indice);
    }

    // ------------------------------------------------------------------ dibujo

    public void texto(Pagina p, double x, double y, Fuente fuente, double tamano, Color color, String texto) {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        p.contenido.append("BT ").append(color(color, false)).append('/').append(fuente.recurso).append(' ')
                .append(num(tamano)).append(" Tf ").append(num(x)).append(' ').append(num(ALTO - y)).append(" Td (")
                .append(escapar(texto)).append(") Tj ET\n");
    }

    public void linea(Pagina p, double x1, double y1, double x2, double y2, double grosor, Color color) {
        p.contenido.append(color(color, true)).append(num(grosor)).append(" w ").append(num(x1)).append(' ')
                .append(num(ALTO - y1)).append(" m ").append(num(x2)).append(' ').append(num(ALTO - y2)).append(" l S\n");
    }

    public void rectanguloRelleno(Pagina p, double x, double y, double ancho, double alto, Color color) {
        p.contenido.append(color(color, false)).append(num(x)).append(' ').append(num(ALTO - y - alto)).append(' ')
                .append(num(ancho)).append(' ').append(num(alto)).append(" re f\n");
    }

    public void rectanguloBorde(Pagina p, double x, double y, double ancho, double alto, double grosor, Color color) {
        p.contenido.append(color(color, true)).append(num(grosor)).append(" w ").append(num(x)).append(' ')
                .append(num(ALTO - y - alto)).append(' ').append(num(ancho)).append(' ').append(num(alto)).append(" re S\n");
    }

    /** Registra una imagen una sola vez y devuelve su identificador para dibujarla en varias páginas. */
    public int registrarImagen(BufferedImage imagen) throws IOException {
        int w = imagen.getWidth();
        int h = imagen.getHeight();
        byte[] rgb = new byte[w * h * 3];
        int k = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = imagen.getRGB(x, y);
                int alfa = (argb >>> 24) & 0xFF;
                int r = mezclarConBlanco((argb >> 16) & 0xFF, alfa);
                int g = mezclarConBlanco((argb >> 8) & 0xFF, alfa);
                int b = mezclarConBlanco(argb & 0xFF, alfa);
                rgb[k++] = (byte) r;
                rgb[k++] = (byte) g;
                rgb[k++] = (byte) b;
            }
        }
        ByteArrayOutputStream comprimido = new ByteArrayOutputStream();
        try (DeflaterOutputStream def = new DeflaterOutputStream(comprimido, new Deflater(Deflater.BEST_COMPRESSION))) {
            def.write(rgb);
        }
        imagenes.add(new ImagenPdf(comprimido.toByteArray(), w, h));
        return imagenes.size() - 1;
    }

    public void dibujarImagen(Pagina p, int idImagen, double x, double y, double ancho, double alto) {
        String nombre = "Im" + idImagen;
        p.imagenesUsadas.put(nombre, idImagen);
        p.contenido.append("q ").append(num(ancho)).append(" 0 0 ").append(num(alto)).append(' ').append(num(x))
                .append(' ').append(num(ALTO - y - alto)).append(" cm /").append(nombre).append(" Do Q\n");
    }

    private static int mezclarConBlanco(int canal, int alfa) {
        return (canal * alfa + 255 * (255 - alfa)) / 255;
    }

    // ------------------------------------------------------------------ medidas de texto

    public static double anchoTexto(String texto, Fuente fuente, double tamano) {
        int[] tabla = fuente == Fuente.NEGRITA ? ANCHOS_NEGRITA : ANCHOS_NORMAL;
        double suma = 0;
        for (char c : texto.toCharArray()) {
            suma += anchoCaracter(c, tabla);
        }
        return suma * tamano / 1000.0;
    }

    private static int anchoCaracter(char c, int[] tabla) {
        if (c >= 32 && c <= 126) {
            return tabla[c - 32];
        }
        String base = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
        char b = base.isEmpty() ? '?' : base.charAt(0);
        return b >= 32 && b <= 126 ? tabla[b - 32] : 556;
    }

    /** Parte el texto en líneas que caben en {@code anchoMaximo}. Las palabras más largas se cortan. */
    public static List<String> ajustar(String texto, Fuente fuente, double tamano, double anchoMaximo) {
        List<String> lineas = new ArrayList<>();
        if (texto == null) {
            return lineas;
        }
        for (String parrafo : texto.split("\n", -1)) {
            StringBuilder actual = new StringBuilder();
            for (String palabra : parrafo.split(" ")) {
                String candidata = actual.length() == 0 ? palabra : actual + " " + palabra;
                if (anchoTexto(candidata, fuente, tamano) <= anchoMaximo) {
                    actual = new StringBuilder(candidata);
                    continue;
                }
                if (actual.length() > 0) {
                    lineas.add(actual.toString());
                    actual = new StringBuilder();
                }
                String resto = palabra;
                while (anchoTexto(resto, fuente, tamano) > anchoMaximo && resto.length() > 1) {
                    int corte = resto.length() - 1;
                    while (corte > 1 && anchoTexto(resto.substring(0, corte), fuente, tamano) > anchoMaximo) {
                        corte--;
                    }
                    lineas.add(resto.substring(0, corte));
                    resto = resto.substring(corte);
                }
                actual.append(resto);
            }
            lineas.add(actual.toString());
        }
        return lineas;
    }

    // ------------------------------------------------------------------ salida

    public void guardar(OutputStream salida) throws IOException {
        if (paginas.isEmpty()) {
            nuevaPagina();
        }
        ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();
        List<Integer> posiciones = new ArrayList<>();
        escribir(cuerpo, "%PDF-1.4\n%âãÏÓ\n");

        int primeraImagen = 5;
        int primeraPagina = primeraImagen + imagenes.size();
        List<Integer> idsPagina = new ArrayList<>();
        for (int i = 0; i < paginas.size(); i++) {
            idsPagina.add(primeraPagina + i * 2);
        }
        int idInfo = primeraPagina + paginas.size() * 2;

        objeto(cuerpo, posiciones, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder hijos = new StringBuilder();
        for (int id : idsPagina) {
            hijos.append(id).append(" 0 R ");
        }
        objeto(cuerpo, posiciones, 2, "<< /Type /Pages /Kids [" + hijos + "] /Count " + paginas.size() + " >>");
        for (Fuente f : Fuente.values()) {
            objeto(cuerpo, posiciones, f == Fuente.NORMAL ? 3 : 4, "<< /Type /Font /Subtype /Type1 /BaseFont /"
                    + f.nombreBase + " /Encoding /WinAnsiEncoding >>");
        }
        for (int i = 0; i < imagenes.size(); i++) {
            ImagenPdf img = imagenes.get(i);
            iniciarObjeto(cuerpo, posiciones, primeraImagen + i);
            escribir(cuerpo, "<< /Type /XObject /Subtype /Image /Width " + img.ancho() + " /Height " + img.alto()
                    + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length " + img.datos().length
                    + " >>\nstream\n");
            cuerpo.write(img.datos());
            escribir(cuerpo, "\nendstream\nendobj\n");
        }
        for (int i = 0; i < paginas.size(); i++) {
            Pagina p = paginas.get(i);
            byte[] contenido = p.contenido.toString().getBytes(WIN_ANSI);
            int idPagina = idsPagina.get(i);
            int idContenido = idPagina + 1;
            StringBuilder xobjects = new StringBuilder();
            for (Map.Entry<String, Integer> e : p.imagenesUsadas.entrySet()) {
                xobjects.append('/').append(e.getKey()).append(' ').append(primeraImagen + e.getValue()).append(" 0 R ");
            }
            objeto(cuerpo, posiciones, idPagina, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + num(ANCHO) + ' '
                    + num(ALTO) + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> /XObject << " + xobjects
                    + ">> >> /Contents " + idContenido + " 0 R >>");
            iniciarObjeto(cuerpo, posiciones, idContenido);
            escribir(cuerpo, "<< /Length " + contenido.length + " >>\nstream\n");
            cuerpo.write(contenido);
            escribir(cuerpo, "\nendstream\nendobj\n");
        }
        objeto(cuerpo, posiciones, idInfo, "<< /Title (" + escapar(titulo) + ") /Author (" + escapar(autor)
                + ") /Producer (Ayuda Humanitaria) >>");

        int inicioXref = cuerpo.size();
        int total = idInfo + 1;
        escribir(cuerpo, "xref\n0 " + total + "\n0000000000 65535 f \n");
        // Las posiciones se guardaron por número de objeto.
        for (int id = 1; id < total; id++) {
            escribir(cuerpo, String.format(Locale.US, "%010d 00000 n \n", posiciones.get(id - 1)));
        }
        escribir(cuerpo, "trailer\n<< /Size " + total + " /Root 1 0 R /Info " + idInfo + " 0 R >>\nstartxref\n"
                + inicioXref + "\n%%EOF\n");
        cuerpo.writeTo(salida);
    }

    // Se escriben los objetos en orden creciente de número; "posiciones" es la lista de desplazamientos en ese orden.
    private void iniciarObjeto(ByteArrayOutputStream cuerpo, List<Integer> posiciones, int id) throws IOException {
        if (posiciones.size() != id - 1) {
            throw new IOException("Los objetos del PDF deben escribirse en orden (esperado " + (posiciones.size() + 1)
                    + ", recibido " + id + ").");
        }
        posiciones.add(cuerpo.size());
        escribir(cuerpo, id + " 0 obj\n");
    }

    private void objeto(ByteArrayOutputStream cuerpo, List<Integer> posiciones, int id, String contenido)
            throws IOException {
        iniciarObjeto(cuerpo, posiciones, id);
        escribir(cuerpo, contenido + "\nendobj\n");
    }

    private static void escribir(ByteArrayOutputStream salida, String texto) throws IOException {
        salida.write(texto.getBytes(WIN_ANSI));
    }

    private static String color(Color c, boolean trazo) {
        return num(c.getRed() / 255.0) + " " + num(c.getGreen() / 255.0) + " " + num(c.getBlue() / 255.0)
                + (trazo ? " RG " : " rg ");
    }

    private static String num(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }

    /** Escapa paréntesis y barras, y reemplaza lo que Windows-1252 no puede representar. */
    static String escapar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            char r = c;
            switch (c) {
                case '→' -> {
                    sb.append("->");
                    continue;
                }
                case '≤' -> {
                    sb.append("<=");
                    continue;
                }
                case '≥' -> {
                    sb.append(">=");
                    continue;
                }
                case '\n', '\r', '\t' -> r = ' ';
                default -> {
                }
            }
            if (r > 255 && !WIN_ANSI.newEncoder().canEncode(r)) {
                r = '?';
            }
            if (r == '(' || r == ')' || r == '\\') {
                sb.append('\\');
            }
            sb.append(r);
        }
        return sb.toString();
    }
}
