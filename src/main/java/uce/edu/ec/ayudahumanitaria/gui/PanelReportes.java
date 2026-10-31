package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorVehiculos;
import uce.edu.ec.ayudahumanitaria.dominio.Vehiculo;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
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

        tarjetaComunidades.setAlignmentX(Component.LEFT_ALIGNMENT);
        tarjetaVehiculos.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelSecciones.add(tarjetaComunidades);
        panelSecciones.add(Box.createVerticalStrut(12));
        panelSecciones.add(tarjetaVehiculos);

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
