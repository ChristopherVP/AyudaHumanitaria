package uce.edu.ec.ayudahumanitaria.gui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Pestaña de logística, de arriba abajo:
 * 1) Algoritmos (situación y herramientas de análisis), 2) sub-pestañas Automático / Manual con su guía paso a paso,
 * 3) Resultado (viajes, itinerario, mapa). Un vehículo en viaje confirmado queda bloqueado para ambos procesos.
 */
public class PanelLogistica extends JPanel {

    private final JTabbedPane modos = new JTabbedPane();

    public PanelLogistica(PanelPlanificacion planificacion, PanelAlgoritmos algoritmos) {
        setLayout(new BorderLayout(0, 0));
        setBackground(EstiloUI.FONDO_APP);

        modos.addTab("Automático (los algoritmos deciden)", planificacion.seccionAutomatico());
        modos.addTab("Manual (usted elige los vehículos)", planificacion.seccionManual());
        modos.setFont(EstiloUI.FUENTE_BASE.deriveFont(java.awt.Font.BOLD));
        modos.setPreferredSize(new Dimension(900, 640));

        JPanel central = new JPanel(new BorderLayout());
        central.setOpaque(false);
        central.add(encabezado("2. Planifique el envío: elija un modo y siga los pasos"), BorderLayout.NORTH);
        central.add(modos, BorderLayout.CENTER);
        JPanel borde = new JPanel(new BorderLayout());
        borde.setOpaque(false);
        borde.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        borde.add(central, BorderLayout.CENTER);

        JPanel arriba = new JPanel(new BorderLayout());
        arriba.setOpaque(false);
        arriba.add(encabezado("1. Algoritmos: comprenda la situación y pruebe cada técnica"), BorderLayout.NORTH);
        arriba.add(algoritmos, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        abajo.setBorder(BorderFactory.createEmptyBorder(0, 14, 14, 14));
        abajo.add(encabezado("3. Resultado: viajes, itinerario y mapa"), BorderLayout.NORTH);
        abajo.add(planificacion.seccionResultados(), BorderLayout.CENTER);

        JPanel medio = new JPanel(new BorderLayout());
        medio.setOpaque(false);
        medio.add(borde, BorderLayout.NORTH);
        medio.add(abajo, BorderLayout.CENTER);

        add(arriba, BorderLayout.NORTH);
        add(medio, BorderLayout.CENTER);

        // Una recomendación de las tarjetas se carga en la tabla manual y se muestra esa pestaña.
        algoritmos.setAlAplicarRecomendacion(seleccion -> {
            planificacion.aplicarSeleccion(seleccion);
            modos.setSelectedIndex(1);
        });
    }

    private static JLabel encabezado(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(EstiloUI.FUENTE_TITULO);
        l.setForeground(EstiloUI.AZUL_PRIMARIO);
        l.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 0));
        return l;
    }
}
