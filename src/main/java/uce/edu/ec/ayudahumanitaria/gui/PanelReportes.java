package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos;
import uce.edu.ec.ayudahumanitaria.dominio.AnalisisAyuda;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorVehiculos;
import uce.edu.ec.ayudahumanitaria.dominio.Vehiculo;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.reportes.ExportadorExcel;

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
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public class PanelReportes extends JPanel {

    private final GestorComunidades gestorComunidades;
    private final GestorVehiculos gestorVehiculos;

    private final JTextArea areaResultados = new JTextArea();

    public PanelReportes(GestorComunidades gestorComunidades, GestorVehiculos gestorVehiculos) {
        this.gestorComunidades = gestorComunidades;
        this.gestorVehiculos = gestorVehiculos;

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

        JPanel tarjetaEstadistica = EstiloUI.tarjeta("Estadística y exportación a Excel");
        JPanel filaEstadistica = EstiloUI.fila();

        JButton btnEstadistica = EstiloUI.botonPrimario("Ver estadísticas y conclusiones");
        btnEstadistica.addActionListener(e -> mostrarEstadisticas());
        filaEstadistica.add(btnEstadistica);

        JButton btnExcel = EstiloUI.botonAccion("Generar Excel");
        btnExcel.addActionListener(e -> generarExcel());
        filaEstadistica.add(btnExcel);

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

    private void generarExcel() {
        Comunidad[] comunidades;
        Vehiculo[] vehiculos;
        try {
            comunidades = gestorComunidades.listarTodas();
            vehiculos = gestorVehiculos.listarTodos();
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Generar Excel", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (comunidades.length == 0) {
            JOptionPane.showMessageDialog(this, "No hay comunidades registradas para generar el reporte.",
                    "Generar Excel", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar reporte en Excel");
        selector.setFileFilter(new FileNameExtensionFilter("Libro de Excel (*.xlsx)", "xlsx"));
        String marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        selector.setSelectedFile(new File("AyudaHumanitaria_" + marca + ".xlsx"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destino = selector.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".xlsx")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".xlsx");
        }
        if (destino.exists() && JOptionPane.showConfirmDialog(this, "El archivo ya existe. ¿Deseas reemplazarlo?",
                "Generar Excel", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            ExportadorExcel.exportar(destino, comunidades, vehiculos);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo. Si lo tienes abierto en Excel, ciérralo "
                    + "e inténtalo de nuevo.\n" + ex.getMessage(), "Generar Excel", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int abrir = JOptionPane.showConfirmDialog(this, "Excel generado correctamente:\n" + destino.getAbsolutePath()
                + "\n\n¿Deseas abrirlo ahora?", "Generar Excel", JOptionPane.YES_NO_OPTION);
        if (abrir == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(destino);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo abrir el archivo automáticamente.",
                        "Generar Excel", JOptionPane.WARNING_MESSAGE);
            }
        }
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
                    .append(" | capacidad=").append(v.getCapacidad())
                    .append(" | asignado=").append(v.isAsignado())
                    .append(" | comunidad=").append(v.getComunidadAsignada())
                    .append("\n");
        }
        areaResultados.setText(sb.toString());
        areaResultados.setCaretPosition(0);
    }
}
