package uce.edu.ec.ayudahumanitaria.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Utilidades visuales centralizadas para toda la aplicación: paleta de
 * colores, tipografías y fábricas de componentes con estilo homogéneo.
 * <p>
 * Esta clase pertenece exclusivamente a la capa de presentación (GUI):
 * no contiene ni conoce reglas de negocio, persistencia ni validaciones.
 */
public final class EstiloUI {

    private EstiloUI() {
    }

    // ---------- Paleta de colores institucional ----------
    public static final Color AZUL_PRIMARIO = new Color(25, 90, 158);
    public static final Color AZUL_PRIMARIO_OSCURO = new Color(15, 64, 118);
    public static final Color AZUL_PRIMARIO_CLARO = new Color(224, 236, 249);
    public static final Color VERDE_ACCION = new Color(43, 138, 106);
    public static final Color VERDE_ACCION_OSCURO = new Color(28, 110, 83);
    public static final Color ROJO_PELIGRO = new Color(190, 58, 54);
    public static final Color ROJO_PELIGRO_OSCURO = new Color(155, 40, 37);
    public static final Color GRIS_NEUTRO = new Color(109, 119, 131);
    public static final Color GRIS_NEUTRO_OSCURO = new Color(84, 92, 102);
    public static final Color FONDO_APP = new Color(240, 243, 247);
    public static final Color FONDO_TARJETA = Color.WHITE;
    public static final Color BORDE_SUAVE = new Color(207, 215, 224);
    public static final Color TEXTO_PRINCIPAL = new Color(30, 34, 39);
    public static final Color TEXTO_SECUNDARIO = new Color(102, 112, 123);
    public static final Color FILA_ALTERNA = new Color(237, 242, 247);

    public static final Font FUENTE_TITULO = new Font("SansSerif", Font.BOLD, 19);
    public static final Font FUENTE_SUBTITULO = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FUENTE_BASE = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FUENTE_ETIQUETA = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FUENTE_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    /**
     * Aplica el tema visual de la aplicación. Debe invocarse una única vez,
     * antes de construir cualquier componente Swing.
     */
    public static void aplicarTema() {
        try {
            MetalLookAndFeel.setCurrentTheme(new TemaAyudaHumanitaria());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException ex) {
            // Si el tema personalizado no está disponible, se conserva el L&F por defecto.
        }
        UIManager.put("Table.alternateRowColor", FILA_ALTERNA);
    }

    // ---------- Fábrica de botones ----------
    public static JButton botonPrimario(String texto) {
        return crearBoton(texto, AZUL_PRIMARIO, AZUL_PRIMARIO_OSCURO);
    }

    public static JButton botonAccion(String texto) {
        return crearBoton(texto, VERDE_ACCION, VERDE_ACCION_OSCURO);
    }

    public static JButton botonPeligro(String texto) {
        return crearBoton(texto, ROJO_PELIGRO, ROJO_PELIGRO_OSCURO);
    }

    public static JButton botonNeutro(String texto) {
        return crearBoton(texto, GRIS_NEUTRO, GRIS_NEUTRO_OSCURO);
    }

    private static JButton crearBoton(String texto, Color base, Color oscuro) {
        JButton boton = new JButton(texto);
        boton.setFont(FUENTE_ETIQUETA);
        boton.setForeground(Color.WHITE);
        boton.setBackground(base);
        boton.setOpaque(true);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (boton.isEnabled()) {
                    boton.setBackground(oscuro);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (boton.isEnabled()) {
                    boton.setBackground(base);
                }
            }
        });
        return boton;
    }

    // ---------- Campos y etiquetas ----------
    public static JTextField campoTexto(int columnas) {
        JTextField campo = new JTextField(columnas);
        campo.setFont(FUENTE_BASE);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE_SUAVE, 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return campo;
    }

    public static JLabel etiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(FUENTE_ETIQUETA);
        etiqueta.setForeground(TEXTO_PRINCIPAL);
        return etiqueta;
    }

    public static JLabel textoSecundario(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(FUENTE_BASE);
        etiqueta.setForeground(TEXTO_SECUNDARIO);
        return etiqueta;
    }

    // ---------- Contenedores ----------

    /** Panel tipo "tarjeta": fondo blanco, borde suave y título opcional. */
    public static JPanel tarjeta(String titulo) {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 10));
        tarjeta.setBackground(FONDO_TARJETA);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE_SUAVE, 1, true),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        if (titulo != null && !titulo.isEmpty()) {
            JLabel lblTitulo = new JLabel(titulo);
            lblTitulo.setFont(FUENTE_SUBTITULO);
            lblTitulo.setForeground(AZUL_PRIMARIO_OSCURO);
            tarjeta.add(lblTitulo, BorderLayout.NORTH);
        }
        return tarjeta;
    }

    /** Fila horizontal transparente, útil para agrupar etiquetas/campos/botones. */
    public static JPanel fila() {
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        fila.setOpaque(false);
        return fila;
    }

    // ---------- Tablas ----------
    public static void estilizarTabla(JTable tabla) {
        tabla.setFont(FUENTE_BASE);
        tabla.setRowHeight(26);
        tabla.setSelectionBackground(AZUL_PRIMARIO_CLARO);
        tabla.setSelectionForeground(TEXTO_PRINCIPAL);
        tabla.setGridColor(new Color(228, 232, 237));
        tabla.setShowGrid(true);
        tabla.setIntercellSpacing(new Dimension(1, 1));
        tabla.setFillsViewportHeight(true);

        JTableHeader encabezado = tabla.getTableHeader();
        encabezado.setFont(FUENTE_ETIQUETA);
        encabezado.setBackground(AZUL_PRIMARIO);
        encabezado.setForeground(Color.WHITE);
        encabezado.setPreferredSize(new Dimension(100, 32));
        encabezado.setReorderingAllowed(false);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? FONDO_TARJETA : FILA_ALTERNA);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        };
        renderer.setHorizontalAlignment(SwingConstants.LEFT);

        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    public static void estilizarAreaTexto(JTextArea area) {
        area.setFont(FUENTE_MONO);
        area.setBackground(FONDO_TARJETA);
        area.setForeground(TEXTO_PRINCIPAL);
        area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
    }

    /** Tema Metal personalizado con la paleta institucional del sistema. */
    private static final class TemaAyudaHumanitaria extends DefaultMetalTheme {
        @Override
        public String getName() {
            return "AyudaHumanitaria";
        }

        @Override
        protected ColorUIResource getPrimary1() {
            return new ColorUIResource(AZUL_PRIMARIO_OSCURO);
        }

        @Override
        protected ColorUIResource getPrimary2() {
            return new ColorUIResource(AZUL_PRIMARIO);
        }

        @Override
        protected ColorUIResource getPrimary3() {
            return new ColorUIResource(AZUL_PRIMARIO_CLARO);
        }

        @Override
        protected ColorUIResource getSecondary1() {
            return new ColorUIResource(new Color(200, 206, 214));
        }

        @Override
        protected ColorUIResource getSecondary2() {
            return new ColorUIResource(new Color(224, 228, 233));
        }

        @Override
        protected ColorUIResource getSecondary3() {
            return new ColorUIResource(FONDO_APP);
        }

        @Override
        public FontUIResource getControlTextFont() {
            return new FontUIResource(FUENTE_BASE);
        }

        @Override
        public FontUIResource getSystemTextFont() {
            return new FontUIResource(FUENTE_BASE);
        }

        @Override
        public FontUIResource getUserTextFont() {
            return new FontUIResource(FUENTE_BASE);
        }

        @Override
        public FontUIResource getMenuTextFont() {
            return new FontUIResource(FUENTE_BASE);
        }

        @Override
        public FontUIResource getWindowTitleFont() {
            return new FontUIResource(FUENTE_SUBTITULO);
        }

        @Override
        public FontUIResource getSubTextFont() {
            return new FontUIResource(new Font("SansSerif", Font.PLAIN, 11));
        }
    }
}
