package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.GestorCentros;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorPlanes;
import uce.edu.ec.ayudahumanitaria.dominio.PlanDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.Viaje;
import uce.edu.ec.ayudahumanitaria.geo.EstudioVial;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Mapa general de Ecuador con capas activables y exportación a imagen PNG. */
public class PanelMapa extends JPanel implements Refrescable {

    private final GestorCentros gestorCentros;
    private final GestorComunidades gestorComunidades;
    private final GestorPlanes gestorPlanes;
    private final MapaEcuadorPanel mapa = new MapaEcuadorPanel();

    private final JCheckBox chkCarreteras = new JCheckBox("Red vial", true);
    private final JCheckBox chkCiudades = new JCheckBox("Ciudades", true);
    private final JCheckBox chkComunidades = new JCheckBox("Comunidades", true);
    private final JCheckBox chkEtiquetas = new JCheckBox("Nombres de comunidades", false);
    private final JCheckBox chkCobertura = new JCheckBox("Cobertura por centro", true);
    private final JCheckBox chkRutas = new JCheckBox("Rutas vigentes", true);

    public PanelMapa(GestorCentros gestorCentros, GestorComunidades gestorComunidades, GestorPlanes gestorPlanes) {
        this.gestorCentros = gestorCentros;
        this.gestorComunidades = gestorComunidades;
        this.gestorPlanes = gestorPlanes;

        setLayout(new BorderLayout(12, 12));
        setBackground(EstiloUI.FONDO_APP);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel tarjetaControles = EstiloUI.tarjeta("Capas del mapa");
        JPanel fila = EstiloUI.fila();
        List<JCheckBox> checks = List.of(chkCarreteras, chkCiudades, chkComunidades, chkEtiquetas, chkCobertura, chkRutas);
        for (JCheckBox c : checks) {
            c.setOpaque(false);
            c.setFont(EstiloUI.FUENTE_BASE);
            c.addActionListener(e -> aplicarCapas());
            fila.add(c);
        }
        JButton btnExportar = EstiloUI.botonAccion("Guardar imagen PNG");
        JButton btnRestablecer = EstiloUI.botonNeutro("Restablecer vista");
        btnExportar.addActionListener(e -> exportarPng());
        btnRestablecer.addActionListener(e -> mapa.resetearVista());
        fila.add(btnRestablecer);
        fila.add(btnExportar);
        tarjetaControles.add(fila, BorderLayout.CENTER);

        JPanel tarjetaMapa = EstiloUI.tarjeta("Ecuador: centros de acopio, comunidades y rutas "
                + "(rueda = zoom · arrastrar = mover · doble clic = restablecer)");
        tarjetaMapa.add(mapa, BorderLayout.CENTER);

        add(tarjetaControles, BorderLayout.NORTH);
        add(tarjetaMapa, BorderLayout.CENTER);
        add(construirEstudioVial(), BorderLayout.EAST);
        aplicarCapas();
        refrescar();
    }

    /** Panel lateral con el estudio referencial del estado de las vías (color de las carreteras del mapa). */
    private JPanel construirEstudioVial() {
        JTextArea texto = new JTextArea(textoEstudioVial());
        EstiloUI.estilizarAreaTexto(texto);
        texto.setEditable(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(texto);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        JPanel tarjeta = EstiloUI.tarjeta("Estudio vial: estado de las calles y carreteras");
        tarjeta.add(scroll, BorderLayout.CENTER);
        tarjeta.setPreferredSize(new java.awt.Dimension(340, 100));
        return tarjeta;
    }

    private static String textoEstudioVial() {
        StringBuilder sb = new StringBuilder();
        sb.append("COLOR DE LAS CARRETERAS EN EL MAPA\n")
                .append("  Rojo: 70 % o más de las vías estatales de la provincia en mal estado\n")
                .append("  Naranja: 50 % a 69 %\n")
                .append("  Verde: menos de 50 %\n")
                .append("  Gris: sin dato publicado\n")
                .append("  Grosor: troncal > principal > secundaria\n\n");
        sb.append("PROVINCIAS CON MÁS VÍAS EN MAL ESTADO\n");
        for (var e : EstudioVial.provinciasPorEstado()) {
            sb.append(String.format(java.util.Locale.US, "  %-18s %3.0f %%%n", e.getKey(), e.getValue() * 100));
        }
        sb.append("\nCORREDORES VIALES\n");
        for (EstudioVial.Corredor c : EstudioVial.corredores()) {
            sb.append("• ").append(c.titulo()).append(" [").append(c.tipo().etiqueta()).append(", ")
                    .append(c.region()).append("]\n  ").append(c.observacion()).append("\n");
        }
        sb.append("\nCÓMO SE USA EN LA SIMULACIÓN\n")
                .append("Velocidad media por región (Costa 58, Sierra 48, Amazonía 42 km/h), reducida hasta 30 % según ")
                .append("las vías en mal estado de la provincia (mínimo 25 km/h). Con ella se calculan las horas de ")
                .append("viaje y el orden de visita por prioridad.\n\n");
        sb.append("FUENTES\n");
        for (String f : EstudioVial.FUENTES) {
            sb.append("• ").append(f).append("\n");
        }
        sb.append("\nAVISO: es un estudio referencial con cifras publicadas; no es tránsito en tiempo real. ")
                .append("Antes de salir, confirme cierres en el ECU 911 / MTOP.");
        return sb.toString();
    }

    private MapaEcuadorPanel.Capas capasActuales() {
        return new MapaEcuadorPanel.Capas(chkCarreteras.isSelected(), chkCiudades.isSelected(),
                chkComunidades.isSelected(), chkEtiquetas.isSelected(), chkCobertura.isSelected(), chkRutas.isSelected());
    }

    private void aplicarCapas() {
        mapa.setCapas(capasActuales());
    }

    @Override
    public void refrescar() {
        try {
            mapa.setDatos(gestorCentros.listar(), List.of(gestorComunidades.listarTodas()), viajesVigentes());
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Viajes de planes no cancelados que todavía no terminaron. */
    private List<Viaje> viajesVigentes() throws PersistenciaException {
        List<Viaje> vigentes = new ArrayList<>();
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        for (PlanDistribucion plan : gestorPlanes.listarPlanes()) {
            for (Viaje v : plan.viajes()) {
                if (v.estadoEn(ahora).estaActivo()) {
                    vigentes.add(v);
                }
            }
        }
        return vigentes;
    }

    private void exportarPng() {
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new File("mapa-ayuda-humanitaria.png"));
        selector.setFileFilter(new FileNameExtensionFilter("Imagen PNG", "png"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File destino = selector.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".png")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".png");
        }
        try {
            BufferedImage imagen = MapaEcuadorPanel.renderizar(mapa.getDatos(), mapa.getCapas(), 1400, 1100,
                    "Ayuda Humanitaria — Ecuador");
            if (!ImageIO.write(imagen, "png", destino)) {
                throw new IOException("No se pudo codificar la imagen PNG.");
            }
            JOptionPane.showMessageDialog(this, "Imagen guardada en:\n" + destino.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar la imagen: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
