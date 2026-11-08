package uce.edu.ec.ayudahumanitaria.gui;

import javax.swing.ImageIcon;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Taskbar;
import java.awt.Window;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Logo del proyecto dibujado con Java2D (sin archivos de imagen): un marcador de mapa con un corazón,
 * que representa "llevar ayuda a un lugar", sobre una ruta punteada. Se genera a cualquier tamaño, por
 * lo que sirve para el ícono de la ventana (en lugar de la taza de Java), el splash y los reportes.
 */
public final class Logo {

    private static final Color AZUL_OSCURO = new Color(15, 64, 118);
    private static final Color AZUL = new Color(25, 90, 158);
    private static final Color VERDE = new Color(67, 197, 158);
    private static final Color CORAZON = new Color(228, 87, 46);
    private static final int[] TAMANOS_ICONO = {16, 24, 32, 48, 64, 128, 256};

    private Logo() {
    }

    /** Dibuja el logo en una imagen cuadrada con fondo transparente. */
    public static BufferedImage crear(int tamano) {
        int lado = Math.max(8, tamano);
        BufferedImage imagen = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.scale(lado / 100.0, lado / 100.0); // se dibuja en un lienzo de 100 x 100

            g.setPaint(new GradientPaint(0, 0, AZUL, 100, 100, AZUL_OSCURO));
            g.fill(new RoundRectangle2D.Double(0, 0, 100, 100, 24, 24));

            // Ruta punteada
            g.setColor(VERDE);
            g.setStroke(new BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                    new float[]{0.1f, 8f}, 0f));
            Path2D ruta = new Path2D.Double();
            ruta.moveTo(13, 86);
            ruta.curveTo(30, 66, 56, 98, 87, 72);
            g.draw(ruta);
            g.setStroke(new BasicStroke(1f));
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(8.5, 81.5, 9, 9));

            // Sombra del marcador
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
            g.setColor(Color.BLACK);
            g.fill(new Ellipse2D.Double(38, 79, 24, 7));
            g.setComposite(AlphaComposite.SrcOver);

            // Marcador (gota)
            Area marcador = new Area(new Ellipse2D.Double(26, 14, 48, 48));
            Path2D punta = new Path2D.Double();
            punta.moveTo(30.5, 52);
            punta.lineTo(69.5, 52);
            punta.lineTo(50, 83);
            punta.closePath();
            marcador.add(new Area(punta));
            g.setColor(Color.WHITE);
            g.fill(marcador);

            // Corazón
            Area corazon = new Area(new Ellipse2D.Double(36.5, 26, 14, 14));
            corazon.add(new Area(new Ellipse2D.Double(49.5, 26, 14, 14)));
            Path2D base = new Path2D.Double();
            base.moveTo(37.0, 34);
            base.lineTo(63.0, 34);
            base.lineTo(50, 52);
            base.closePath();
            corazon.add(new Area(base));
            g.setColor(CORAZON);
            g.fill(corazon);
        } finally {
            g.dispose();
        }
        return imagen;
    }

    /** Imágenes del ícono en varios tamaños (Windows elige la que necesita para barra de tareas, Alt+Tab, etc.). */
    public static List<Image> iconos() {
        List<Image> lista = new ArrayList<>();
        for (int tamano : TAMANOS_ICONO) {
            lista.add(crear(tamano));
        }
        return lista;
    }

    public static ImageIcon icono(int tamano) {
        return new ImageIcon(crear(tamano));
    }

    /** Asigna el logo como ícono de la ventana y, si el sistema lo permite, de la barra de tareas/dock. */
    public static void aplicarA(Window ventana) {
        ventana.setIconImages(iconos());
        instalarIconoDeAplicacion();
    }

    /** Ícono de la aplicación en la barra de tareas/dock (macOS y algunos entornos). Es opcional. */
    public static void instalarIconoDeAplicacion() {
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar barra = Taskbar.getTaskbar();
                if (barra.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    barra.setIconImage(crear(256));
                }
            }
        } catch (UnsupportedOperationException | SecurityException ex) {
            // El sistema no permite cambiar el ícono de la aplicación: se mantiene el de la ventana.
        }
    }
}
