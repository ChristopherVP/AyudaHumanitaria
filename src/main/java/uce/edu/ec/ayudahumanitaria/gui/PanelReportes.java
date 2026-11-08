package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos;
import uce.edu.ec.ayudahumanitaria.dominio.AnalisisAyuda;
import uce.edu.ec.ayudahumanitaria.dominio.CentroAcopio;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.GestorCentros;
import uce.edu.ec.ayudahumanitaria.dominio.GestorPlanes;
import uce.edu.ec.ayudahumanitaria.dominio.PlanDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.Viaje;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorVehiculos;
import uce.edu.ec.ayudahumanitaria.dominio.Vehiculo;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.reportes.DatosOperativos;
import uce.edu.ec.ayudahumanitaria.reportes.ExportadorExcel;
import uce.edu.ec.ayudahumanitaria.reportes.ExportadorPDF;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import javax.imageio.ImageIO;
import javax.swing.SwingWorker;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class PanelReportes extends JPanel {

    private final GestorComunidades gestorComunidades;
    private final GestorVehiculos gestorVehiculos;
    private final GestorCentros gestorCentros;
    private final GestorPlanes gestorPlanes;
    private final Supplier<PlanDistribucion> planElegido;
    private final JButton btnExcel = EstiloUI.botonAccion("Generar Excel");
    private final JButton btnPdf = EstiloUI.botonAccion("Generar PDF con mapas");

    private final JTextArea areaResultados = new JTextArea();

    /**
     * @param planElegido plan que se reporta (el que se está viendo en Planificación); puede devolver {@code null},
     *                    en cuyo caso se usa el plan guardado más reciente
     */
    public PanelReportes(GestorComunidades gestorComunidades, GestorVehiculos gestorVehiculos,
                         GestorCentros gestorCentros, GestorPlanes gestorPlanes, Supplier<PlanDistribucion> planElegido) {
        this.gestorComunidades = gestorComunidades;
        this.gestorVehiculos = gestorVehiculos;
        this.gestorCentros = gestorCentros;
        this.gestorPlanes = gestorPlanes;
        this.planElegido = planElegido;

        setLayout(new BorderLayout(12, 12));
        setBackground(EstiloUI.FONDO_APP);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel panelSecciones = new JPanel();
        panelSecciones.setLayout(new BoxLayout(panelSecciones, BoxLayout.Y_AXIS));
        panelSecciones.setOpaque(false);

        JPanel tarjetaComunidades = EstiloUI.tarjeta("Reportes de comunidades");
        JPanel filaComunidades = EstiloUI.fila();

        JButton btnBeneficio = EstiloUI.botonPrimario("Por beneficio");
        btnBeneficio.addActionListener(e -> reporteComunidadesPorBeneficio());
        filaComunidades.add(btnBeneficio);

        JButton btnPrioridad = EstiloUI.botonPrimario("Por prioridad");
        btnPrioridad.addActionListener(e -> reporteComunidadesPorPrioridad());
        filaComunidades.add(btnPrioridad);

        JButton btnRecursos = EstiloUI.botonPrimario("Por recursos requeridos");
        btnRecursos.addActionListener(e -> reporteComunidadesPorRecursos());
        filaComunidades.add(btnRecursos);

        JButton btnNombreComunidad = EstiloUI.botonPrimario("Por nombre");
        btnNombreComunidad.addActionListener(e -> reporteComunidadesPorNombre());
        filaComunidades.add(btnNombreComunidad);

        tarjetaComunidades.add(filaComunidades, BorderLayout.CENTER);

        JPanel tarjetaVehiculos = EstiloUI.tarjeta("Reportes de vehículos");
        JPanel filaVehiculos = EstiloUI.fila();

        JButton btnCapacidad = EstiloUI.botonAccion("Por capacidad");
        btnCapacidad.addActionListener(e -> reporteVehiculosPorCapacidad());
        filaVehiculos.add(btnCapacidad);

        JButton btnIdVehiculo = EstiloUI.botonAccion("Por identificador");
        btnIdVehiculo.addActionListener(e -> reporteVehiculosPorId());
        filaVehiculos.add(btnIdVehiculo);

        tarjetaVehiculos.add(filaVehiculos, BorderLayout.CENTER);

        JPanel tarjetaEstadistica = EstiloUI.tarjeta("Estadística y exportación (Excel y PDF)");
        JPanel filaEstadistica = EstiloUI.fila();

        JButton btnEstadistica = EstiloUI.botonPrimario("Ver estadísticas y conclusiones");
        btnEstadistica.addActionListener(e -> mostrarEstadisticas());
        filaEstadistica.add(btnEstadistica);

        btnExcel.addActionListener(e -> exportar(true));
        filaEstadistica.add(btnExcel);
        btnPdf.addActionListener(e -> exportar(false));
        filaEstadistica.add(btnPdf);

        tarjetaEstadistica.add(filaEstadistica, BorderLayout.CENTER);

        tarjetaComunidades.setAlignmentX(Component.LEFT_ALIGNMENT);
        tarjetaVehiculos.setAlignmentX(Component.LEFT_ALIGNMENT);
        tarjetaEstadistica.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelSecciones.add(tarjetaComunidades);
        panelSecciones.add(Box.createVerticalStrut(12));
        panelSecciones.add(tarjetaVehiculos);
        panelSecciones.add(Box.createVerticalStrut(12));
        panelSecciones.add(tarjetaEstadistica);

        EstiloUI.estilizarAreaTexto(areaResultados);
        areaResultados.setEditable(false);
        areaResultados.setFont(EstiloUI.FUENTE_MONO);

        JScrollPane scrollResultados = new JScrollPane(areaResultados);
        scrollResultados.setBorder(BorderFactory.createEmptyBorder());
        JPanel tarjetaResultados = EstiloUI.tarjeta("Resultados");
        tarjetaResultados.add(scrollResultados, BorderLayout.CENTER);

        add(panelSecciones, BorderLayout.NORTH);
        add(tarjetaResultados, BorderLayout.CENTER);
    }

    private void mostrarEstadisticas() {
        try {
            Comunidad[] comunidades = gestorComunidades.listarTodas();
            if (comunidades.length == 0) {
                areaResultados.setText("No hay comunidades registradas. Registra al menos una para ver estadísticas.");
                return;
            }
            AnalisisAyuda analisis = new AnalisisAyuda(comunidades, gestorVehiculos.listarTodos());
            areaResultados.setText(analisis.reporteTexto());
            areaResultados.setCaretPosition(0);
        } catch (PersistenciaException ex) {
            areaResultados.setText(ex.getMessage());
        }
    }

    /** Pide el destino, arma los datos (y renderiza los mapas) en segundo plano y genera el archivo. */
    private void exportar(boolean excel) {
        String extension = excel ? "xlsx" : "pdf";
        String nombre = excel ? "Excel" : "PDF";
        try {
            if (gestorComunidades.listarTodas().length == 0) {
                JOptionPane.showMessageDialog(this, "No hay comunidades registradas para generar el reporte.",
                        "Generar " + nombre, JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Generar " + nombre, JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar reporte en " + nombre);
        selector.setFileFilter(new FileNameExtensionFilter(excel ? "Libro de Excel (*.xlsx)" : "Documento PDF (*.pdf)",
                extension));
        String marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        selector.setSelectedFile(new File("AyudaHumanitaria_" + marca + "." + extension));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File elegido = selector.getSelectedFile();
        File destino = elegido.getName().toLowerCase().endsWith("." + extension)
                ? elegido : new File(elegido.getParentFile(), elegido.getName() + "." + extension);
        if (destino.exists() && JOptionPane.showConfirmDialog(this, "El archivo ya existe. ¿Deseas reemplazarlo?",
                "Generar " + nombre, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        btnExcel.setEnabled(false);
        btnPdf.setEnabled(false);
        areaResultados.setText("Generando " + nombre + " (se están dibujando los mapas de cada viaje)...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DatosOperativos datos = recolectarDatos();
                if (excel) {
                    ExportadorExcel.exportar(destino, datos.comunidades().toArray(new Comunidad[0]),
                            datos.vehiculos().toArray(new Vehiculo[0]), datos);
                } else {
                    ExportadorPDF.exportar(destino, datos);
                }
                return null;
            }

            @Override
            protected void done() {
                btnExcel.setEnabled(true);
                btnPdf.setEnabled(true);
                try {
                    get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause() == null ? ex : ex.getCause();
                    areaResultados.setText("No se pudo generar el " + nombre + ": " + causa.getMessage());
                    JOptionPane.showMessageDialog(PanelReportes.this, "No se pudo guardar el archivo. Si lo tienes abierto, "
                                    + "ciérralo e inténtalo de nuevo.\n" + causa.getMessage(), "Generar " + nombre,
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                areaResultados.setText(nombre + " generado en:\n" + destino.getAbsolutePath());
                int abrir = JOptionPane.showConfirmDialog(PanelReportes.this, nombre + " generado correctamente:\n"
                                + destino.getAbsolutePath() + "\n\n¿Deseas abrirlo ahora?", "Generar " + nombre,
                        JOptionPane.YES_NO_OPTION);
                if (abrir == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().open(destino);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(PanelReportes.this, "No se pudo abrir el archivo automáticamente.",
                                "Generar " + nombre, JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    /** Lee la base y dibuja el mapa general y el de cada viaje del plan elegido. */
    private DatosOperativos recolectarDatos() throws Exception {
        List<CentroAcopio> centros = gestorCentros.listar();
        List<Comunidad> comunidades = List.of(gestorComunidades.listarTodas());
        List<Vehiculo> vehiculos = List.of(gestorVehiculos.listarTodos());
        PlanDistribucion plan = planElegido.get();
        if (plan == null) {
            List<PlanDistribucion> guardados = gestorPlanes.listarPlanes();
            plan = guardados.isEmpty() ? null : guardados.get(0);
        }
        List<Viaje> viajes = plan == null ? List.of() : plan.viajes();

        MapaEcuadorPanel.Capas capas = MapaEcuadorPanel.Capas.porDefecto();
        byte[] general = png(MapaEcuadorPanel.renderizar(new MapaEcuadorPanel.Datos(centros, comunidades, viajes, null),
                capas.conCobertura(viajes.isEmpty()), 1300, 1000, "Centros de acopio, comunidades y rutas"));
        List<byte[]> porViaje = new ArrayList<>();
        for (Viaje v : viajes) {
            porViaje.add(png(MapaEcuadorPanel.renderizar(new MapaEcuadorPanel.Datos(centros, comunidades, viajes, v),
                    capas, 1000, 720, null)));
        }
        return new DatosOperativos(LocalDateTime.now(), centros, comunidades, vehiculos, plan,
                gestorPlanes.planificadoPorComunidad(), gestorPlanes.entregadoPorComunidad(), general, porViaje);
    }

    private static byte[] png(java.awt.image.BufferedImage imagen) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        if (!ImageIO.write(imagen, "png", salida)) {
            throw new IOException("No se pudo codificar la imagen del mapa.");
        }
        return salida.toByteArray();
    }

    private void reporteComunidadesPorBeneficio() {
        try {
            Comunidad[] comunidades = gestorComunidades.listarTodas();
            Algoritmos.quickSort(comunidades, Comparator.comparingDouble(Comunidad::calcularBeneficio).reversed());
            mostrarComunidades("Reporte de comunidades por beneficio", comunidades);
        } catch (PersistenciaException ex) {
            areaResultados.setText(ex.getMessage());
        }
    }

    private void reporteComunidadesPorPrioridad() {
        try {
            Comunidad[] comunidades = gestorComunidades.listarTodas();
            Algoritmos.mergeSort(comunidades, Comparator.comparingInt(Comunidad::getPrioridad).reversed());
            mostrarComunidades("Reporte de comunidades por prioridad", comunidades);
        } catch (PersistenciaException ex) {
            areaResultados.setText(ex.getMessage());
        }
    }

    private void reporteComunidadesPorRecursos() {
        try {
            Comunidad[] comunidades = gestorComunidades.listarTodas();
            Algoritmos.insertionSort(comunidades, Comparator.comparingDouble(Comunidad::getRecursosRequeridos));
            mostrarComunidades("Reporte de comunidades por recursos requeridos", comunidades);
        } catch (PersistenciaException ex) {
            areaResultados.setText(ex.getMessage());
        }
    }

    private void reporteComunidadesPorNombre() {
        try {
            Comunidad[] comunidades = gestorComunidades.listarTodas();
            Algoritmos.bubbleSort(comunidades, Comparator.comparing(Comunidad::getNombre));
            mostrarComunidades("Reporte de comunidades por nombre", comunidades);
        } catch (PersistenciaException ex) {
            areaResultados.setText(ex.getMessage());
        }
    }

    private void reporteVehiculosPorCapacidad() {
        try {
            Vehiculo[] vehiculos = gestorVehiculos.listarTodos();
            Algoritmos.selectionSort(vehiculos, Comparator.comparingDouble(Vehiculo::getCapacidad).reversed());
            mostrarVehiculos("Reporte de vehículos por capacidad", vehiculos);
        } catch (PersistenciaException ex) {
            areaResultados.setText(ex.getMessage());
        }
    }

    private void reporteVehiculosPorId() {
        try {
            Vehiculo[] vehiculos = gestorVehiculos.listarTodos();
            Algoritmos.shellSort(vehiculos, Comparator.comparing(Vehiculo::getId));
            mostrarVehiculos("Reporte de vehículos por identificador", vehiculos);
        } catch (PersistenciaException ex) {
            areaResultados.setText(ex.getMessage());
        }
    }

    private void mostrarComunidades(String titulo, Comunidad[] comunidades) {
        StringBuilder sb = new StringBuilder("=== " + titulo + " ===\n");
        for (Comunidad c : comunidades) {
            sb.append(" - ").append(c.getNombre())
                    .append(" | habitantes=").append(c.getHabitantes())
                    .append(" | prioridad=").append(c.getPrioridad())
                    .append(" | recursos=").append(c.getRecursosRequeridos())
                    .append(" | beneficio=").append(c.calcularBeneficio())
                    .append("\n");
        }
        areaResultados.setText(sb.toString());
        areaResultados.setCaretPosition(0);
    }

    private void mostrarVehiculos(String titulo, Vehiculo[] vehiculos) {
        StringBuilder sb = new StringBuilder("=== " + titulo + " ===\n");
        for (Vehiculo v : vehiculos) {
            sb.append(" - ").append(v.getId())
                    .append(" | capacidad=").append(v.getCapacidad()).append(" kg")
                    .append(" | centro=").append(v.getCentroNombre())
                    .append(" | asignado=").append(v.isAsignado())
                    .append(" | comunidad=").append(v.getComunidadAsignada())
                    .append("\n");
        }
        areaResultados.setText(sb.toString());
        areaResultados.setCaretPosition(0);
    }
}
