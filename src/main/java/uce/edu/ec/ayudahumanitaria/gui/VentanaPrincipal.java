package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorVehiculos;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

public class VentanaPrincipal extends JFrame {

    static {
        EstiloUI.aplicarTema();
    }

    public VentanaPrincipal(GestorComunidades gestorComunidades, GestorVehiculos gestorVehiculos) {
        setTitle("Sistema Inteligente de Distribución de Ayuda Humanitaria");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        getContentPane().setBackground(EstiloUI.FONDO_APP);
        setLayout(new BorderLayout());

        add(construirEncabezado(), BorderLayout.NORTH);
        add(construirPestanas(gestorComunidades, gestorVehiculos), BorderLayout.CENTER);
        add(construirPiePagina(), BorderLayout.SOUTH);
    }

    private JPanel construirEncabezado() {
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setBackground(EstiloUI.AZUL_PRIMARIO_OSCURO);
        encabezado.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel titulo = new JLabel("Sistema Inteligente de Distribución de Ayuda Humanitaria");
        titulo.setFont(EstiloUI.FUENTE_TITULO);
        titulo.setForeground(Color.WHITE);

        JLabel subtitulo = new JLabel("Gestión de comunidades, vehículos y algoritmos de distribución");
        subtitulo.setFont(EstiloUI.FUENTE_BASE);
        subtitulo.setForeground(new Color(214, 227, 242));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        titulo.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        subtitulo.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(subtitulo);

        encabezado.add(textos, BorderLayout.WEST);
        return encabezado;
    }

    private JTabbedPane construirPestanas(GestorComunidades gestorComunidades, GestorVehiculos gestorVehiculos) {
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setFont(EstiloUI.FUENTE_SUBTITULO);
        pestanas.setBackground(EstiloUI.FONDO_APP);

        PanelComunidades panelComunidades = new PanelComunidades(gestorComunidades);
        PanelVehiculos panelVehiculos = new PanelVehiculos(gestorVehiculos);

        pestanas.addTab("Comunidades", panelComunidades);
        pestanas.addTab("Vehículos", panelVehiculos);
        pestanas.addTab("Distribución", new PanelAlgoritmos(gestorComunidades, gestorVehiculos, panelVehiculos));
        pestanas.addTab("Reportes", new PanelReportes(gestorComunidades, gestorVehiculos));

        return pestanas;
    }

    private JPanel construirPiePagina() {
        JPanel pie = new JPanel(new BorderLayout());
        pie.setBackground(EstiloUI.AZUL_PRIMARIO_CLARO);
        pie.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloUI.BORDE_SUAVE),
                BorderFactory.createEmptyBorder(6, 20, 6, 20)));

        JLabel estado = new JLabel("Universidad Central del Ecuador · Facultad de Ingeniería y Ciencias Aplicadas");
        estado.setFont(new Font("SansSerif", Font.PLAIN, 11));
        estado.setForeground(EstiloUI.TEXTO_SECUNDARIO);
        pie.add(estado, BorderLayout.WEST);

        return pie;
    }
}
