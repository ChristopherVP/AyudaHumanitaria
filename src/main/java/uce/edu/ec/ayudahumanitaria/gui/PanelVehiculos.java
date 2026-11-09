package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.CentroAcopio;
import uce.edu.ec.ayudahumanitaria.dominio.GestorCentros;
import uce.edu.ec.ayudahumanitaria.dominio.GestorVehiculos;
import uce.edu.ec.ayudahumanitaria.dominio.Vehiculo;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

public class PanelVehiculos extends JPanel implements Refrescable {

    private final GestorVehiculos gestor;
    private final GestorCentros gestorCentros;
    private final Runnable alCambiar;
    private final JComboBox<CentroAcopio> comboCentro = new JComboBox<>();

    private final JTextField campoId = EstiloUI.campoTexto(10);
    private final JTextField campoCapacidad = EstiloUI.campoTexto(10);
    private final JTextField campoBusquedaId = EstiloUI.campoTexto(10);

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Capacidad (kg)", "Asignado", "Comunidad asignada", "Centro"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);

    private final JButton btnRegistrar = EstiloUI.botonPrimario("Registrar");
    private final JButton btnModificar = EstiloUI.botonAccion("Modificar seleccionado");
    private final JButton btnEliminar = EstiloUI.botonPeligro("Eliminar seleccionado");
    private final JButton btnNuevo = EstiloUI.botonNeutro("Nuevo");

    private boolean modoEdicion = false;
    private final String[] valoresOriginales = new String[2];
    private boolean actualizandoFormulario = false;

    public PanelVehiculos(GestorVehiculos gestor, GestorCentros gestorCentros, Runnable alCambiar) {
        this.gestor = gestor;
        this.gestorCentros = gestorCentros;
        this.alCambiar = alCambiar;

        setLayout(new BorderLayout(12, 12));
        setBackground(EstiloUI.FONDO_APP);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel panelSuperior = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        panelSuperior.setOpaque(false);

        JPanel tarjetaFormulario = construirTarjetaFormulario();
        JPanel tarjetaBusqueda = construirTarjetaBusqueda();
        panelSuperior.add(tarjetaFormulario);
        panelSuperior.add(tarjetaBusqueda);

        EstiloUI.estilizarTabla(tabla);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createLineBorder(EstiloUI.BORDE_SUAVE, 1));
        JPanel tarjetaTabla = EstiloUI.tarjeta("Vehículos registrados");
        tarjetaTabla.add(scrollTabla, BorderLayout.CENTER);
        tarjetaTabla.add(EstiloUI.textoSecundario("«Asignado» significa que el vehículo tiene un viaje de un plan "
                + "CONFIRMADO (pestaña Logística); cambia solo según el reloj: planificado/en ruta = asignado, "
                + "completado = libre."), BorderLayout.NORTH);
        tarjetaTabla.setPreferredSize(new java.awt.Dimension(900, 420));

        add(panelSuperior, BorderLayout.NORTH);
        add(tarjetaTabla, BorderLayout.CENTER);

        btnRegistrar.addActionListener(e -> registrar());
        btnModificar.addActionListener(e -> modificar());
        btnEliminar.addActionListener(e -> eliminar());
        btnNuevo.addActionListener(e -> volverModoRegistro());

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccionEnFormulario();
            }
        });

        DocumentListener detectorCambios = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                verificarCambios();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                verificarCambios();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                verificarCambios();
            }
        };
        campoCapacidad.getDocument().addDocumentListener(detectorCambios);
        comboCentro.addActionListener(e -> verificarCambios());

        volverModoRegistro();
        refrescar();
    }

    private JPanel construirTarjetaFormulario() {
        JPanel tarjeta = EstiloUI.tarjeta("Datos del vehículo");

        JPanel fila1 = EstiloUI.fila();
        fila1.add(EstiloUI.etiqueta("ID:"));
        fila1.add(campoId);
        fila1.add(EstiloUI.etiqueta("Capacidad (kg):"));
        fila1.add(campoCapacidad);
        JPanel fila2 = EstiloUI.fila();
        fila2.add(EstiloUI.etiqueta("Centro de acopio:"));
        fila2.add(comboCentro);
        JPanel campos = new JPanel(new java.awt.GridLayout(0, 1, 0, 0));
        campos.setOpaque(false);
        campos.add(fila1);
        campos.add(fila2);

        JPanel acciones = EstiloUI.fila();
        acciones.add(btnRegistrar);
        acciones.add(btnModificar);
        acciones.add(btnEliminar);
        acciones.add(btnNuevo);

        JPanel contenido = new JPanel(new BorderLayout(0, 4));
        contenido.setOpaque(false);
        JPanel pila = new JPanel(new java.awt.GridLayout(0, 1, 0, 0));
        pila.setOpaque(false);
        pila.add(campos);
        pila.add(acciones);
        contenido.add(pila, BorderLayout.NORTH);

        tarjeta.add(contenido, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel construirTarjetaBusqueda() {
        JPanel tarjeta = EstiloUI.tarjeta("Buscar vehículos");

        JPanel fila = EstiloUI.fila();
        fila.add(EstiloUI.etiqueta("ID:"));
        fila.add(campoBusquedaId);
        JButton btnBuscar = EstiloUI.botonNeutro("Buscar");
        JButton btnListarTodos = EstiloUI.botonNeutro("Listar todos");
        fila.add(btnBuscar);
        fila.add(btnListarTodos);

        btnBuscar.addActionListener(e -> buscar());
        btnListarTodos.addActionListener(e -> cargarTabla());

        tarjeta.add(fila, BorderLayout.CENTER);
        return tarjeta;
    }

    private void registrar() {
        try {
            CentroAcopio centro = (CentroAcopio) comboCentro.getSelectedItem();
            if (centro == null) {
                throw new ValidationException("Registre primero un centro de acopio.");
            }
            gestor.registrar(campoId.getText(), Double.parseDouble(campoCapacidad.getText().trim().replace(',', '.')), centro.getId());
            JOptionPane.showMessageDialog(this, "Vehículo registrado correctamente.");
            volverModoRegistro();
            cambio();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La capacidad debe ser numérica.",
                    "Error de formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modificar() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un vehículo de la tabla.");
            return;
        }

        try {
            String id = modeloTabla.getValueAt(fila, 0).toString();
            boolean asignado = (boolean) modeloTabla.getValueAt(fila, 2);
            Object comunidadAsignada = modeloTabla.getValueAt(fila, 3);

            CentroAcopio centro = (CentroAcopio) comboCentro.getSelectedItem();
            if (centro == null) {
                throw new ValidationException("Seleccione un centro de acopio.");
            }
            Vehiculo v = new Vehiculo(id, Double.parseDouble(campoCapacidad.getText().trim().replace(',', '.')), centro.getId());
            v.setAsignado(asignado);
            v.setComunidadAsignada(comunidadAsignada == null ? null : comunidadAsignada.toString());

            gestor.modificar(v);
            JOptionPane.showMessageDialog(this, "Vehículo actualizado correctamente.");
            volverModoRegistro();
            cambio();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La capacidad debe ser numérica.",
                    "Error de formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un vehículo de la tabla.");
            return;
        }
        try {
            String id = modeloTabla.getValueAt(fila, 0).toString();
            gestor.eliminar(id);
            volverModoRegistro();
            cambio();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buscar() {
        try {
            llenarTabla(gestor.buscar(campoBusquedaId.getText()));
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void cargarTabla() {
        try {
            llenarTabla(gestor.listarTodos());
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void llenarTabla(Vehiculo[] vehiculos) {
        modeloTabla.setRowCount(0);
        for (Vehiculo v : vehiculos) {
            modeloTabla.addRow(new Object[]{
                    v.getId(), v.getCapacidad() == Math.rint(v.getCapacidad()) ? (Object) (long) v.getCapacidad() : (Object) v.getCapacidad(), v.isAsignado(), v.getComunidadAsignada(), v.getCentroNombre()
            });
        }
    }

    private void cargarSeleccionEnFormulario() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            return;
        }

        actualizandoFormulario = true;
        valoresOriginales[0] = modeloTabla.getValueAt(fila, 1).toString();
        valoresOriginales[1] = String.valueOf(modeloTabla.getValueAt(fila, 4));
        for (int i = 0; i < comboCentro.getItemCount(); i++) {
            if (comboCentro.getItemAt(i).getNombre().equals(valoresOriginales[1])) {
                comboCentro.setSelectedIndex(i);
            }
        }

        campoId.setText(modeloTabla.getValueAt(fila, 0).toString());
        campoId.setEditable(false);
        campoCapacidad.setText(valoresOriginales[0]);
        actualizandoFormulario = false;

        modoEdicion = true;
        btnRegistrar.setVisible(false);
        btnModificar.setEnabled(false);
        btnEliminar.setEnabled(true);
    }

    private void verificarCambios() {
        if (actualizandoFormulario || !modoEdicion) {
            return;
        }
        CentroAcopio centro = (CentroAcopio) comboCentro.getSelectedItem();
        boolean cambio = !campoCapacidad.getText().equals(valoresOriginales[0])
                || (centro != null && !centro.getNombre().equals(valoresOriginales[1]));
        btnModificar.setEnabled(cambio);
    }

    private void volverModoRegistro() {
        modoEdicion = false;
        tabla.clearSelection();
        limpiarFormulario();
        campoId.setEditable(true);
        btnRegistrar.setVisible(true);
        btnModificar.setEnabled(false);
        btnEliminar.setEnabled(false);
    }

    private void cambio() {
        refrescar();
        alCambiar.run();
    }

    @Override
    public void refrescar() {
        try {
            Object anterior = comboCentro.getSelectedItem();
            List<CentroAcopio> centros = gestorCentros.listar();
            comboCentro.removeAllItems();
            centros.forEach(comboCentro::addItem);
            if (anterior != null) {
                comboCentro.setSelectedItem(anterior);
            }
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
        cargarTabla();
    }

    private void limpiarFormulario() {
        actualizandoFormulario = true;
        campoId.setText("");
        campoCapacidad.setText("");
        actualizandoFormulario = false;
    }
}
