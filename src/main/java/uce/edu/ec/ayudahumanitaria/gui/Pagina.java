package uce.edu.ec.ayudahumanitaria.gui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Página con desplazamiento vertical: si la ventana es más baja que {@code alturaMinima}, aparece la barra
 * para deslizar hacia abajo; si es más alta, el contenido se estira y no deja huecos.
 */
public final class Pagina {

    private Pagina() {
    }

    public static JScrollPane de(JComponent contenido, int alturaMinima) {
        if (alturaMinima <= 0) {
            throw new IllegalArgumentException("La altura mínima debe ser positiva.");
        }
        Contenedor contenedor = new Contenedor(contenido, alturaMinima);
        JScrollPane scroll = new JScrollPane(contenedor, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getViewport().setBackground(EstiloUI.FONDO_APP);
        return scroll;
    }

    private static final class Contenedor extends JPanel implements Scrollable {
        private final int alturaMinima;

        Contenedor(JComponent contenido, int alturaMinima) {
            super(new BorderLayout());
            this.alturaMinima = alturaMinima;
            setBackground(EstiloUI.FONDO_APP);
            add(contenido, BorderLayout.CENTER);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width, Math.max(d.height, alturaMinima));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(visible.height - 40, 20);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() >= getPreferredSize().height;
        }
    }
}
