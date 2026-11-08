package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.GestorCentros;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorPlanes;
import uce.edu.ec.ayudahumanitaria.dominio.PlanDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.Viaje;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
        aplicarCapas();
        refrescar();
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
