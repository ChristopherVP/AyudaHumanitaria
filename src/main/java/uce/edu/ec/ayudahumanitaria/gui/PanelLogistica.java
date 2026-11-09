package uce.edu.ec.ayudahumanitaria.gui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Pestaña única de logística: arriba se decide y simula el envío (vehículos, viajes, fechas) y debajo están los
 * algoritmos que respaldan esas decisiones (situación, reparto, mochila, rutas, asignación, recomendación).
 */
public class PanelLogistica extends JPanel {

    public PanelLogistica(PanelPlanificacion planificacion, PanelAlgoritmos algoritmos) {
        setLayout(new BorderLayout(0, 6));
        setBackground(EstiloUI.FONDO_APP);

        JLabel titulo = encabezado("Herramientas de análisis (algoritmos): comprenda la situación y apoye su decisión");
        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setOpaque(false);
        inferior.add(titulo, BorderLayout.NORTH);
        inferior.add(algoritmos, BorderLayout.CENTER);

        add(planificacion, BorderLayout.NORTH);
        add(inferior, BorderLayout.CENTER);
        algoritmos.setAlAplicarRecomendacion(planificacion::aplicarSeleccion);
    }

    private static JLabel encabezado(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(EstiloUI.FUENTE_TITULO);
        l.setForeground(EstiloUI.AZUL_PRIMARIO);
        l.setBorder(BorderFactory.createEmptyBorder(10, 16, 0, 0));
        return l;
    }
}
