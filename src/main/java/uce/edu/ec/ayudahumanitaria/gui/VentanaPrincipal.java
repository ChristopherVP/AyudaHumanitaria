package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.GestorCentros;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorPlanes;
import uce.edu.ec.ayudahumanitaria.dominio.GestorVehiculos;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/** Ventana principal: una pestaña por área (mapa, centros, comunidades, vehículos, planificación, etc.). */
public class VentanaPrincipal extends JFrame {

    static {
        EstiloUI.aplicarTema();
    }

    private static final int SEGUNDOS_ENTRE_ACTUALIZACIONES = 60;

    private final GestorPlanes gestorPlanes;
    private final List<Refrescable> refrescables = new ArrayList<>();
    private final JTabbedPane pestanas = new JTabbedPane();

    public VentanaPrincipal(GestorCentros gestorCentros, GestorComunidades gestorComunidades,
                            GestorVehiculos gestorVehiculos, GestorPlanes gestorPlanes) {
        this.gestorPlanes = gestorPlanes;
        setTitle("Sistema Inteligente de Distribución de Ayuda Humanitaria");
        Logo.aplicarA(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);

        getContentPane().setBackground(EstiloUI.FONDO_APP);
        setLayout(new BorderLayout());

        add(construirEncabezado(), BorderLayout.NORTH);
        add(construirPestanas(gestorCentros, gestorComunidades, gestorVehiculos), BorderLayout.CENTER);
        add(construirPiePagina(), BorderLayout.SOUTH);

        // Al cambiar de pestaña se vuelve a leer la base, así siempre se ve lo último.
        pestanas.addChangeListener(e -> {
            Component actual = pestanas.getSelectedComponent();
            if (actual instanceof Refrescable r) {
                r.refrescar();
            }
        });
        // Los estados (planificado / en ruta / completado) dependen del reloj: se revisan cada minuto.
        Timer reloj = new Timer(SEGUNDOS_ENTRE_ACTUALIZACIONES * 1000, e -> actualizarPorReloj());
        reloj.setRepeats(true);
        reloj.start();
    }

    private void actualizarPorReloj() {
        try {
            gestorPlanes.sincronizar();
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Component actual = pestanas.getSelectedComponent();
        if (actual instanceof Refrescable r) {
            r.refrescar();
        }
    }

    /** Refresca todas las pestañas (se llama cuando una pestaña cambia datos que otras muestran). */
    private void refrescarTodo() {
        refrescables.forEach(Refrescable::refrescar);
    }

    private JPanel construirEncabezado() {
        JPanel encabezado = new JPanel(new BorderLayout(14, 0));
        encabezado.setBackground(EstiloUI.AZUL_PRIMARIO_OSCURO);
        encabezado.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel icono = new JLabel(Logo.icono(48));

        JLabel titulo = new JLabel("Sistema Inteligente de Distribución de Ayuda Humanitaria");
        titulo.setFont(EstiloUI.FUENTE_TITULO);
        titulo.setForeground(Color.WHITE);

        JLabel subtitulo = new JLabel("Centros de acopio, comunidades, vehículos, rutas y reportes del Ecuador continental");
        subtitulo.setFont(EstiloUI.FUENTE_BASE);
        subtitulo.setForeground(new Color(214, 227, 242));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(subtitulo);

        encabezado.add(icono, BorderLayout.WEST);
        encabezado.add(textos, BorderLayout.CENTER);
        return encabezado;
    }

    private JTabbedPane construirPestanas(GestorCentros gestorCentros, GestorComunidades gestorComunidades,
                                          GestorVehiculos gestorVehiculos) {
        pestanas.setFont(EstiloUI.FUENTE_SUBTITULO);
        pestanas.setBackground(EstiloUI.FONDO_APP);

        PanelMapa panelMapa = new PanelMapa(gestorCentros, gestorComunidades, gestorPlanes);
        PanelCentros panelCentros = new PanelCentros(gestorCentros, gestorComunidades, this::refrescarTodo);
        PanelComunidades panelComunidades = new PanelComunidades(gestorComunidades, gestorCentros, gestorPlanes,
                this::refrescarTodo);
        PanelVehiculos panelVehiculos = new PanelVehiculos(gestorVehiculos, gestorCentros, this::refrescarTodo);
        PanelPlanificacion panelPlanificacion = new PanelPlanificacion(gestorPlanes, gestorCentros, gestorComunidades,
                this::refrescarTodo);
        PanelAlgoritmos panelAlgoritmos = new PanelAlgoritmos(gestorComunidades, gestorVehiculos, gestorCentros, gestorPlanes);
        PanelReportes panelReportes = new PanelReportes(gestorComunidades, gestorVehiculos, gestorCentros, gestorPlanes,
                panelPlanificacion::getPlanMostrado);

        refrescables.add(panelMapa);
        refrescables.add(panelCentros);
        refrescables.add(panelComunidades);
        refrescables.add(panelVehiculos);
        refrescables.add(panelPlanificacion);
        refrescables.add(panelAlgoritmos);
        PanelLogistica panelLogistica = new PanelLogistica(panelPlanificacion, panelAlgoritmos);

        pestanas.addTab("Mapa", Pagina.de(panelMapa, 700));
        pestanas.addTab("Centros", Pagina.de(panelCentros, 780));
        pestanas.addTab("Comunidades", Pagina.de(panelComunidades, 860));
        pestanas.addTab("Vehículos", Pagina.de(panelVehiculos, 720));
        pestanas.addTab("Logística", Pagina.de(panelLogistica, 2200));
        pestanas.addTab("Reportes", Pagina.de(panelReportes, 620));
        pestanas.addTab("Ayuda", new PanelAyuda());
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
