package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.CentroAcopio;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.Parada;
import uce.edu.ec.ayudahumanitaria.dominio.PlanificadorDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.Viaje;
import uce.edu.ec.ayudahumanitaria.geo.CatalogoEcuador;
import uce.edu.ec.ayudahumanitaria.geo.CatalogoEcuador.Carretera;
import uce.edu.ec.ayudahumanitaria.geo.CatalogoEcuador.Ciudad;
import uce.edu.ec.ayudahumanitaria.geo.GeoEcuador;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Mapa esquemático del Ecuador continental (sin imágenes externas): red vial, ciudades de referencia,
 * centros de acopio (verde), comunidades, cobertura por centro y rutas de distribución
 * (línea de color, paradas en rojo numeradas). Permite zoom con la rueda, arrastrar para moverse,
 * doble clic para restablecer y, en modo selección, hacer clic para elegir una coordenada.
 * <p>
 * El dibujo está en {@link #dibujar} (método estático) para poder reutilizarlo al exportar el mapa a imagen
 * en los reportes PDF/Excel.
 */
public class MapaEcuadorPanel extends JPanel {

    /** Colores por centro (se repiten si hay más centros que colores). */
    public static final Color[] PALETA_CENTROS = {
            new Color(21, 101, 192), new Color(230, 81, 0), new Color(106, 27, 154), new Color(0, 131, 143),
            new Color(173, 20, 87), new Color(93, 64, 55), new Color(130, 119, 23), new Color(69, 90, 100)
    };

    private static final Color FONDO = new Color(244, 248, 252);
    private static final Color CARRETERA = new Color(208, 213, 219);
    private static final Color CIUDAD = new Color(125, 132, 141);
    private static final Color VERDE_CENTRO = new Color(30, 158, 74);
    private static final Color ROJO_PARADA = new Color(211, 47, 47);
    private static final Color COMUNIDAD = new Color(55, 71, 79);

    private static final double LON_MIN = -81.2;
    private static final double LON_MAX = -75.0;
    private static final double LAT_MIN = -5.1;
    private static final double LAT_MAX = 1.6;

    private static final Set<String> CIUDADES_PRINCIPALES = Set.of("Quito", "Guayaquil", "Cuenca", "Ambato",
            "Riobamba", "Loja", "Machala", "Esmeraldas", "Ibarra", "Tulcán", "Manta", "Portoviejo",
            "Santo Domingo", "Quevedo", "Babahoyo", "Latacunga", "Tena", "Puyo", "Macas", "Nueva Loja", "Coca",
            "Zamora", "Azogues", "Guaranda", "Santa Elena");

    /** Qué capas se dibujan. */
    public record Capas(boolean carreteras, boolean ciudades, boolean comunidades, boolean etiquetasComunidades,
                        boolean cobertura, boolean rutas) {
        public static Capas porDefecto() {
            return new Capas(true, true, true, false, false, true);
        }

        public Capas conCarreteras(boolean v) {
            return new Capas(v, ciudades, comunidades, etiquetasComunidades, cobertura, rutas);
        }

        public Capas conCiudades(boolean v) {
            return new Capas(carreteras, v, comunidades, etiquetasComunidades, cobertura, rutas);
        }

        public Capas conComunidades(boolean v) {
            return new Capas(carreteras, ciudades, v, etiquetasComunidades, cobertura, rutas);
        }

        public Capas conEtiquetas(boolean v) {
            return new Capas(carreteras, ciudades, comunidades, v, cobertura, rutas);
        }

        public Capas conCobertura(boolean v) {
            return new Capas(carreteras, ciudades, comunidades, etiquetasComunidades, v, rutas);
        }

        public Capas conRutas(boolean v) {
            return new Capas(carreteras, ciudades, comunidades, etiquetasComunidades, cobertura, v);
        }
    }

    /** Elementos geográficos a dibujar. */
    public record Datos(List<CentroAcopio> centros, List<Comunidad> comunidades, List<Viaje> viajes,
                        Viaje resaltado) {
        public static Datos vacios() {
            return new Datos(List.of(), List.of(), List.of(), null);
        }
    }

    /** Zoom y desplazamiento (en píxeles) de la vista. */
    public record Vista(double zoom, double desplazamientoX, double desplazamientoY) {
        public static Vista completa() {
            return new Vista(1.0, 0, 0);
        }
    }

    /** Punto dibujado, usado para mostrar la información al pasar el mouse. */
    public record Marcador(double x, double y, String texto) {
    }

    private Datos datos = Datos.vacios();
    private Capas capas = Capas.porDefecto();
    private Vista vista = Vista.completa();
    private List<Marcador> marcadores = List.of();

    private Consumer<double[]> alSeleccionar;
    private double[] puntoElegido;
    private Point inicioArrastre;
    private Vista vistaAlArrastrar;
    private boolean arrastrando;

    public MapaEcuadorPanel() {
        setBackground(FONDO);
        setPreferredSize(new Dimension(640, 520));
        setMinimumSize(new Dimension(320, 260));
        ToolTipManager.sharedInstance().registerComponent(this);

        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                inicioArrastre = e.getPoint();
                vistaAlArrastrar = vista;
                arrastrando = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (inicioArrastre == null) {
                    return;
                }
                int dx = e.getX() - inicioArrastre.x;
                int dy = e.getY() - inicioArrastre.y;
                if (!arrastrando && Math.abs(dx) + Math.abs(dy) > 4) {
                    arrastrando = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                if (arrastrando) {
                    vista = new Vista(vistaAlArrastrar.zoom(), vistaAlArrastrar.desplazamientoX() + dx,
                            vistaAlArrastrar.desplazamientoY() + dy);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setCursor(Cursor.getPredefinedCursor(alSeleccionar != null
                        ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
                inicioArrastre = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    resetearVista();
                } else if (alSeleccionar != null && !arrastrando) {
                    Proyeccion p = new Proyeccion(getWidth(), getHeight(), vista);
                    double[] latLon = p.aLatLon(e.getX(), e.getY());
                    puntoElegido = latLon;
                    repaint();
                    alSeleccionar.accept(latLon);
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double factor = e.getWheelRotation() < 0 ? 1.25 : 0.8;
                double nuevoZoom = Math.max(1.0, Math.min(14.0, vista.zoom() * factor));
                double w = getWidth();
                double h = getHeight();
                double bx = (e.getX() - w / 2 - vista.desplazamientoX()) / vista.zoom() + w / 2;
                double by = (e.getY() - h / 2 - vista.desplazamientoY()) / vista.zoom() + h / 2;
                double offX = e.getX() - w / 2 - (bx - w / 2) * nuevoZoom;
                double offY = e.getY() - h / 2 - (by - h / 2) * nuevoZoom;
                vista = nuevoZoom <= 1.0 ? Vista.completa() : new Vista(nuevoZoom, offX, offY);
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                setToolTipText(textoBajo(e.getPoint()));
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);
        addMouseWheelListener(raton);
    }

    // ------------------------------------------------------------------ API

    public void setDatos(List<CentroAcopio> centros, List<Comunidad> comunidades, List<Viaje> viajes) {
        this.datos = new Datos(List.copyOf(centros), List.copyOf(comunidades), List.copyOf(viajes), null);
        repaint();
    }

    public void setResaltado(Viaje viaje) {
        this.datos = new Datos(datos.centros(), datos.comunidades(), datos.viajes(), viaje);
        repaint();
    }

    public Datos getDatos() {
        return datos;
    }

    public void setCapas(Capas nuevasCapas) {
        this.capas = nuevasCapas;
        repaint();
    }

    public Capas getCapas() {
        return capas;
    }

    public void resetearVista() {
        this.vista = Vista.completa();
        repaint();
    }

    /** Activa el modo "elegir ubicación": el siguiente clic devuelve {latitud, longitud}. */
    public void activarSeleccion(Consumer<double[]> alElegir) {
        this.alSeleccionar = alElegir;
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    public void setPuntoElegido(double latitud, double longitud) {
        this.puntoElegido = new double[]{latitud, longitud};
        repaint();
    }

    /** Imagen del mapa completo (sin zoom) para incluir en reportes. */
    public static BufferedImage renderizar(Datos datos, Capas capas, int ancho, int alto, String titulo) {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        try {
            dibujar(g, ancho, alto, datos, capas, Vista.completa(), null, titulo, 1.0);
        } finally {
            g.dispose();
        }
        return imagen;
    }

    // ------------------------------------------------------------------ Swing

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            marcadores = dibujar(g, getWidth(), getHeight(), datos, capas, vista, puntoElegido, null, 1.0);
        } finally {
            g.dispose();
        }
    }

    private String textoBajo(Point p) {
        for (Marcador m : marcadores) {
            if (Math.abs(m.x() - p.x) <= 8 && Math.abs(m.y() - p.y) <= 8) {
                return m.texto();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ proyección

    /** Convierte latitud/longitud a píxeles (proyección equirrectangular ajustada al panel). */
    static final class Proyeccion {
        private final double ancho;
        private final double alto;
        private final Vista vista;
        private final double escala;
        private final double baseX;
        private final double baseY;
        private static final double K_LON = Math.cos(Math.toRadians(-1.8));

        Proyeccion(double ancho, double alto, Vista vista) {
            this.ancho = ancho;
            this.alto = alto;
            this.vista = vista;
            double margen = 28;
            double anchoUnidades = (LON_MAX - LON_MIN) * K_LON;
            double altoUnidades = LAT_MAX - LAT_MIN;
            this.escala = Math.max(1, Math.min((ancho - 2 * margen) / anchoUnidades, (alto - 2 * margen) / altoUnidades));
            this.baseX = (ancho - anchoUnidades * escala) / 2;
            this.baseY = (alto - altoUnidades * escala) / 2;
        }

        double x(double longitud) {
            double bx = baseX + (longitud - LON_MIN) * K_LON * escala;
            return (bx - ancho / 2) * vista.zoom() + ancho / 2 + vista.desplazamientoX();
        }

        double y(double latitud) {
            double by = baseY + (LAT_MAX - latitud) * escala;
            return (by - alto / 2) * vista.zoom() + alto / 2 + vista.desplazamientoY();
        }

        double[] aLatLon(double px, double py) {
            double bx = (px - ancho / 2 - vista.desplazamientoX()) / vista.zoom() + ancho / 2;
            double by = (py - alto / 2 - vista.desplazamientoY()) / vista.zoom() + alto / 2;
            double lon = (bx - baseX) / (K_LON * escala) + LON_MIN;
            double lat = LAT_MAX - (by - baseY) / escala;
            return new double[]{Math.round(lat * 10000) / 10000.0, Math.round(lon * 10000) / 10000.0};
        }

        /** Píxeles por kilómetro con el zoom actual. */
        double pxPorKm() {
            return escala * vista.zoom() / 111.32;
        }
    }

    // ------------------------------------------------------------------ dibujo

    /**
     * Dibuja el mapa completo en {@code g}.
     *
     * @param factorFuente multiplicador del tamaño de letra (útil en imágenes grandes para reportes)
     * @return puntos dibujados, para mostrar información al pasar el mouse
     */
    public static List<Marcador> dibujar(Graphics2D g, int ancho, int alto, Datos datos, Capas capas, Vista vista,
                                         double[] puntoElegido, String titulo, double factorFuente) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g.setColor(FONDO);
        g.fillRect(0, 0, ancho, alto);
        Proyeccion p = new Proyeccion(ancho, alto, vista);
        double zoom = vista.zoom();
        float f = (float) factorFuente;
        List<Marcador> marcadores = new ArrayList<>();

        // Carreteras
        if (capas.carreteras()) {
            g.setColor(CARRETERA);
            g.setStroke(new BasicStroke((float) (1.6 * f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (Carretera c : CatalogoEcuador.CARRETERAS) {
                Ciudad a = CatalogoEcuador.ciudad(c.desde());
                Ciudad b = CatalogoEcuador.ciudad(c.hasta());
                if (a != null && b != null) {
                    g.drawLine((int) p.x(a.longitud()), (int) p.y(a.latitud()),
                            (int) p.x(b.longitud()), (int) p.y(b.latitud()));
                }
            }
        }

        // Ciudades de referencia
        if (capas.ciudades()) {
            Font fuente = new Font("SansSerif", Font.PLAIN, Math.round(9 * f));
            for (Ciudad c : CatalogoEcuador.CIUDADES) {
                double x = p.x(c.longitud());
                double y = p.y(c.latitud());
                g.setColor(CIUDAD);
                g.fill(new Ellipse2D.Double(x - 2.5 * f, y - 2.5 * f, 5 * f, 5 * f));
                boolean esCentro = datos.centros().stream().anyMatch(ce -> ce.getCiudad().equalsIgnoreCase(c.nombre()));
                if (!esCentro && (zoom >= 1.9 || CIUDADES_PRINCIPALES.contains(c.nombre()))) {
                    texto(g, c.nombre(), (float) (x + 5 * f), (float) (y + 3 * f), new Color(110, 117, 126), fuente, false);
                }
            }
        }

        // Centro más cercano de cada comunidad (para cobertura y colores)
        Map<Integer, Integer> indiceCentroDeComunidad = new HashMap<>();
        for (Comunidad c : datos.comunidades()) {
            if (c.tieneUbicacion() && !datos.centros().isEmpty()) {
                CentroAcopio cercano = PlanificadorDistribucion.centroMasCercano(datos.centros(), c);
                indiceCentroDeComunidad.put(c.getId(), datos.centros().indexOf(cercano));
            }
        }

        // Cobertura: línea punteada de cada comunidad a su centro
        if (capas.cobertura()) {
            g.setStroke(new BasicStroke((float) (1.1 * f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                    new float[]{4f * f, 4f * f}, 0f));
            for (Comunidad c : datos.comunidades()) {
                Integer i = indiceCentroDeComunidad.get(c.getId());
                if (i == null || i < 0) {
                    continue;
                }
                CentroAcopio centro = datos.centros().get(i);
                g.setColor(conAlfa(colorDeCentro(i), 120));
                g.drawLine((int) p.x(centro.getLongitud()), (int) p.y(centro.getLatitud()),
                        (int) p.x(c.getLongitud()), (int) p.y(c.getLatitud()));
            }
        }

        // Rutas
        java.util.Set<Integer> idsEnRuta = new java.util.HashSet<>();
        if (capas.rutas()) {
            Viaje resaltado = datos.resaltado();
            for (Viaje v : datos.viajes()) {
                if (v.cancelado() || v == resaltado) {
                    continue;
                }
                dibujarRuta(g, p, v, datos.centros(), resaltado == null ? 1.0f : 0.35f, 3.0f * f, false, f);
                v.paradas().forEach(parada -> idsEnRuta.add(parada.comunidadId()));
            }
            if (resaltado != null) {
                dibujarRuta(g, p, resaltado, datos.centros(), 1.0f, 4.6f * f, true, f);
                resaltado.paradas().forEach(parada -> idsEnRuta.add(parada.comunidadId()));
            }
        }

        // Comunidades
        Font fuenteComunidad = new Font("SansSerif", Font.PLAIN, Math.round(10 * f));
        if (capas.comunidades()) {
            for (Comunidad c : datos.comunidades()) {
                if (!c.tieneUbicacion()) {
                    continue;
                }
                double x = p.x(c.getLongitud());
                double y = p.y(c.getLatitud());
                Integer i = indiceCentroDeComunidad.get(c.getId());
                Color color = capas.cobertura() && i != null && i >= 0 ? colorDeCentro(i) : COMUNIDAD;
                double r = 4.2 * f;
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(x - r - 1, y - r - 1, 2 * r + 2, 2 * r + 2));
                g.setColor(color);
                g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
                marcadores.add(new Marcador(x, y, "<html><b>" + escapar(c.getNombre()) + "</b><br>" + escapar(c.getProvincia())
                        + "<br>" + c.getHabitantes() + " hab. · prioridad " + c.getPrioridad()
                        + "<br>" + String.format("%.1f", c.getRecursosRequeridos()) + " t requeridas</html>"));
                if (capas.etiquetasComunidades() || (zoom >= 3 && !idsEnRuta.contains(c.getId()))) {
                    texto(g, c.getNombre(), (float) (x + r + 3 * f), (float) (y + 4 * f), COMUNIDAD, fuenteComunidad, true);
                }
            }
        }

        // Paradas del plan (rojo, numeradas) sobre las comunidades
        if (capas.rutas()) {
            Viaje resaltado = datos.resaltado();
            List<Viaje> aMarcar = resaltado != null ? List.of(resaltado) : datos.viajes();
            Font fuenteNumero = new Font("SansSerif", Font.BOLD, Math.round(9 * f));
            for (Viaje v : aMarcar) {
                if (v.cancelado()) {
                    continue;
                }
                for (Parada parada : v.paradas()) {
                    double x = p.x(parada.longitud());
                    double y = p.y(parada.latitud());
                    double r = (resaltado != null ? 8.5 : 6.5) * f;
                    g.setColor(Color.WHITE);
                    g.fill(new Ellipse2D.Double(x - r - 1.5, y - r - 1.5, 2 * r + 3, 2 * r + 3));
                    g.setColor(ROJO_PARADA);
                    g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
                    if (resaltado != null || zoom >= 2) {
                        g.setFont(fuenteNumero);
                        g.setColor(Color.WHITE);
                        String n = String.valueOf(parada.orden());
                        FontMetrics fm = g.getFontMetrics();
                        g.drawString(n, (float) (x - fm.stringWidth(n) / 2.0), (float) (y + fm.getAscent() / 2.0 - 1));
                    }
                    if (resaltado != null) {
                        texto(g, parada.comunidadNombre(), (float) (x + r + 4 * f), (float) (y + 4 * f),
                                new Color(150, 20, 20), new Font("SansSerif", Font.BOLD, Math.round(11 * f)), true);
                    }
                    marcadores.add(new Marcador(x, y, "<html><b>" + escapar(parada.comunidadNombre()) + "</b><br>Parada "
                            + parada.orden() + " · " + String.format("%.1f", parada.toneladas()) + " t<br>Llegada: "
                            + parada.llegada().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            + "</html>"));
                }
            }
        }

        // Centros de acopio
        Font fuenteCentro = new Font("SansSerif", Font.BOLD, Math.round(12 * f));
        for (int i = 0; i < datos.centros().size(); i++) {
            CentroAcopio c = datos.centros().get(i);
            double x = p.x(c.getLongitud());
            double y = p.y(c.getLatitud());
            double r = 8.5 * f;
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(x - r - 2, y - r - 2, 2 * r + 4, 2 * r + 4));
            g.setColor(VERDE_CENTRO);
            g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
            g.setColor(colorDeCentro(i));
            g.setStroke(new BasicStroke(2.2f * f));
            g.draw(new Ellipse2D.Double(x - r - 2.5 * f, y - r - 2.5 * f, 2 * r + 5 * f, 2 * r + 5 * f));
            texto(g, c.getCiudad().toUpperCase(), (float) (x + r + 6 * f), (float) (y - 6 * f), new Color(10, 80, 40),
                    fuenteCentro, true);
            marcadores.add(new Marcador(x, y, "<html><b>" + escapar(c.getNombre()) + "</b><br>" + escapar(c.getProvincia())
                    + "<br>Stock: " + String.format("%.1f", c.getStockToneladas()) + " t</html>"));
        }

        // Punto elegido en modo selección
        if (puntoElegido != null) {
            double x = p.x(puntoElegido[1]);
            double y = p.y(puntoElegido[0]);
            g.setStroke(new BasicStroke(2.2f));
            g.setColor(new Color(194, 24, 91));
            g.draw(new Ellipse2D.Double(x - 9, y - 9, 18, 18));
            g.drawLine((int) x - 14, (int) y, (int) x + 14, (int) y);
            g.drawLine((int) x, (int) y - 14, (int) x, (int) y + 14);
        }

        dibujarLeyenda(g, ancho, alto, datos, capas, p, f);
        if (titulo != null) {
            g.setFont(new Font("SansSerif", Font.BOLD, Math.round(15 * f)));
            g.setColor(EstiloUI.AZUL_PRIMARIO_OSCURO);
            g.drawString(titulo, 14 * f, 24 * f);
        }
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(207, 215, 224));
        g.drawRect(0, 0, ancho - 1, alto - 1);
        return marcadores;
    }

    private static void dibujarRuta(Graphics2D g, Proyeccion p, Viaje v, List<CentroAcopio> centros, float opacidad,
                                    float grosor, boolean conCiudades, float f) {
        int indiceCentro = 0;
        for (int i = 0; i < centros.size(); i++) {
            if (centros.get(i).getId() == v.centro().getId()) {
                indiceCentro = i;
                break;
            }
        }
        Color color = conAlfa(colorDeCentro(indiceCentro), Math.round(255 * opacidad));

        List<double[]> puntos = new ArrayList<>();
        puntos.add(new double[]{v.centro().getLatitud(), v.centro().getLongitud()});
        for (Parada parada : v.paradas()) {
            puntos.add(new double[]{parada.latitud(), parada.longitud()});
        }
        puntos.add(new double[]{v.centro().getLatitud(), v.centro().getLongitud()});

        // Línea poligonal que pasa por las ciudades del camino (esquemático)
        Path2D camino = new Path2D.Double();
        List<Ciudad> intermedias = new ArrayList<>();
        boolean primero = true;
        for (int i = 0; i < puntos.size() - 1; i++) {
            double[] a = puntos.get(i);
            double[] b = puntos.get(i + 1);
            if (primero) {
                camino.moveTo(p.x(a[1]), p.y(a[0]));
                primero = false;
            }
            for (Ciudad c : GeoEcuador.ciudadesEnElTramo(a[0], a[1], b[0], b[1], 14)) {
                camino.lineTo(p.x(c.longitud()), p.y(c.latitud()));
                intermedias.add(c);
            }
            camino.lineTo(p.x(b[1]), p.y(b[0]));
        }
        if (conCiudades) {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(grosor + 3f * f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(camino);
        }
        g.setColor(color);
        g.setStroke(new BasicStroke(grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(camino);

        if (conCiudades) {
            Font fuente = new Font("SansSerif", Font.PLAIN, Math.round(9 * f));
            for (Ciudad c : intermedias) {
                double x = p.x(c.longitud());
                double y = p.y(c.latitud());
                g.setColor(color);
                g.fill(new Ellipse2D.Double(x - 3 * f, y - 3 * f, 6 * f, 6 * f));
                texto(g, c.nombre(), (float) (x + 5 * f), (float) (y + 12 * f), new Color(60, 70, 90), fuente, true);
            }
        }
    }

    private static void dibujarLeyenda(Graphics2D g, int ancho, int alto, Datos datos, Capas capas, Proyeccion p,
                                       float f) {
        Font fuente = new Font("SansSerif", Font.PLAIN, Math.round(11 * f));
        g.setFont(fuente);
        FontMetrics fm = g.getFontMetrics();
        List<String> textos = new ArrayList<>();
        List<Color> colores = new ArrayList<>();
        textos.add("Centro de acopio");
        colores.add(VERDE_CENTRO);
        textos.add("Comunidad");
        colores.add(COMUNIDAD);
        if (capas.rutas() && !datos.viajes().isEmpty()) {
            textos.add("Parada de la ruta");
            colores.add(ROJO_PARADA);
        }
        if (capas.cobertura() || (capas.rutas() && !datos.viajes().isEmpty())) {
            for (int i = 0; i < datos.centros().size(); i++) {
                textos.add(datos.centros().get(i).getCiudad());
                colores.add(colorDeCentro(i));
            }
        }
        int ancho_ = 0;
        for (String t : textos) {
            ancho_ = Math.max(ancho_, fm.stringWidth(t));
        }
        int filaAlto = Math.round(16 * f);
        int cajaAncho = ancho_ + Math.round(34 * f);
        int cajaAlto = textos.size() * filaAlto + Math.round(10 * f);
        int x0 = Math.round(10 * f);
        int y0 = alto - cajaAlto - Math.round(10 * f);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.92f));
        g.setColor(Color.WHITE);
        g.fillRoundRect(x0, y0, cajaAncho, cajaAlto, 8, 8);
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(new Color(207, 215, 224));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x0, y0, cajaAncho, cajaAlto, 8, 8);
        for (int i = 0; i < textos.size(); i++) {
            int y = y0 + Math.round(5 * f) + i * filaAlto + filaAlto / 2;
            g.setColor(colores.get(i));
            if (i < 3 || !(capas.cobertura() || capas.rutas())) {
                g.fill(new Ellipse2D.Double(x0 + 9 * f, y - 4 * f, 8 * f, 8 * f));
            } else {
                g.setStroke(new BasicStroke(3f * f));
                g.drawLine(x0 + Math.round(7 * f), y, x0 + Math.round(19 * f), y);
            }
            g.setColor(EstiloUI.TEXTO_PRINCIPAL);
            g.setFont(fuente);
            g.drawString(textos.get(i), x0 + 26 * f, y + fm.getAscent() / 2f - 1);
        }

        // Escala (100 km)
        double px = p.pxPorKm() * 100;
        int xe = ancho - Math.round(14 * f) - (int) px;
        int ye = alto - Math.round(16 * f);
        g.setColor(new Color(60, 70, 80));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(xe, ye, xe + (int) px, ye);
        g.drawLine(xe, ye - 4, xe, ye + 4);
        g.drawLine(xe + (int) px, ye - 4, xe + (int) px, ye + 4);
        g.setFont(fuente);
        g.drawString("100 km", xe, ye - 7);
    }

    // ------------------------------------------------------------------ utilidades de dibujo

    private static Color colorDeCentro(int indice) {
        return PALETA_CENTROS[Math.floorMod(indice, PALETA_CENTROS.length)];
    }

    private static Color conAlfa(Color c, int alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alfa)));
    }

    /** Texto con un halo blanco para que se lea sobre líneas y puntos. */
    private static void texto(Graphics2D g, String s, float x, float y, Color color, Font fuente, boolean halo) {
        g.setFont(fuente);
        if (halo) {
            g.setColor(new Color(255, 255, 255, 220));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx != 0 || dy != 0) {
                        g.drawString(s, x + dx, y + dy);
                    }
                }
            }
        }
        g.setColor(color);
        g.drawString(s, x, y);
    }

    private static String escapar(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Punto de latitud/longitud como texto corto. */
    public static String formatoCoordenadas(double[] latLon) {
        return String.format(java.util.Locale.US, "%.4f, %.4f", latLon[0], latLon[1]);
    }

    /** Útil para pruebas: distancia en píxeles entre dos puntos proyectados con la vista completa. */
    static Point2D.Double proyectar(int ancho, int alto, double latitud, double longitud) {
        Proyeccion p = new Proyeccion(ancho, alto, Vista.completa());
        return new Point2D.Double(p.x(longitud), p.y(latitud));
    }
}
