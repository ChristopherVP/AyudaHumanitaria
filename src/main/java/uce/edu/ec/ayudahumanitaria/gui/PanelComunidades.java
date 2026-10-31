package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class PanelComunidades extends JPanel {

    private final GestorComunidades gestor;

    private final JTextField campoNombre = EstiloUI.campoTexto(15);
    private final JTextField campoHabitantes = EstiloUI.campoTexto(8);
    private final JTextField campoPrioridad = EstiloUI.campoTexto(4);
    private final JTextField campoRecursos = EstiloUI.campoTexto(8);
    private final JTextField campoBusqueda = EstiloUI.campoTexto(12);
    private final JTextField campoBusquedaId = EstiloUI.campoTexto(6);

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Habitantes", "Prioridad", "Recursos", "Beneficio"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);

    private final JButton btnRegistrar = EstiloUI.botonPrimario("Registrar");
    private final JButton btnModificar = EstiloUI.botonAccion("Modificar seleccionada");
    private final JButton btnEliminar = EstiloUI.botonPeligro("Eliminar seleccionada");
    private final JButton btnNuevo = EstiloUI.botonNeutro("Nuevo");

    private boolean modoEdicion = false;
    private final String[] valoresOriginales = new String[4];
    private boolean actualizandoFormulario = false;

    public PanelComunidades(GestorComunidades gestor) {
        this.gestor = gestor;

        setLayout(new BorderLayout(12, 12));
        setBackground(EstiloUI.FONDO_APP);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel panelSuperior = new JPanel();
        panelSuperior.setLayout(new BoxLayout(panelSuperior, BoxLayout.Y_AXIS));
        panelSuperior.setOpaque(false);

        JPanel tarjetaFormulario = construirTarjetaFormulario();
        JPanel tarjetaBusqueda = construirTarjetaBusqueda();
        tarjetaFormulario.setAlignmentX(Component.LEFT_ALIGNMENT);
        tarjetaBusqueda.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelSuperior.add(tarjetaFormulario);
        panelSuperior.add(Box.createVerticalStrut(12));
        panelSuperior.add(tarjetaBusqueda);

        EstiloUI.estilizarTabla(tabla);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createLineBorder(EstiloUI.BORDE_SUAVE, 1));
        JPanel tarjetaTabla = EstiloUI.tarjeta("Comunidades registradas");
        tarjetaTabla.add(scrollTabla, BorderLayout.CENTER);

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
        campoNombre.getDocument().addDocumentListener(detectorCambios);
        campoHabitantes.getDocument().addDocumentListener(detectorCambios);
        campoPrioridad.getDocument().addDocumentListener(detectorCambios);
        campoRecursos.getDocument().addDocumentListener(detectorCambios);

        volverModoRegistro();
        cargarTabla();
    }

    private JPanel construirTarjetaFormulario() {
        JPanel tarjeta = EstiloUI.tarjeta("Datos de la comunidad");

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        campos.add(EstiloUI.etiqueta("Nombre:"), gbc);
        gbc.gridx = 1;
        campos.add(campoNombre, gbc);
        gbc.gridx = 2;
        campos.add(EstiloUI.etiqueta("Habitantes:"), gbc);
        gbc.gridx = 3;
        campos.add(campoHabitantes, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        campos.add(EstiloUI.etiqueta("Prioridad (1-10):"), gbc);
        gbc.gridx = 1;
        campos.add(campoPrioridad, gbc);
        gbc.gridx = 2;
        campos.add(EstiloUI.etiqueta("Recursos req. (ton):"), gbc);
        gbc.gridx = 3;
        campos.add(campoRecursos, gbc);

        JPanel acciones = EstiloUI.fila();
        acciones.add(btnRegistrar);
        acciones.add(btnModificar);
        acciones.add(btnEliminar);
        acciones.add(btnNuevo);

        JPanel contenido = new JPanel(new BorderLayout(0, 8));
        contenido.setOpaque(false);
        contenido.add(campos, BorderLayout.NORTH);
        contenido.add(acciones, BorderLayout.SOUTH);

        tarjeta.add(contenido, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel construirTarjetaBusqueda() {
        JPanel tarjeta = EstiloUI.tarjeta("Buscar comunidades");

        JPanel fila = EstiloUI.fila();
        fila.add(EstiloUI.etiqueta("Nombre:"));
        fila.add(campoBusqueda);
        JButton btnBuscar = EstiloUI.botonNeutro("Buscar");
        JButton btnListarTodas = EstiloUI.botonNeutro("Listar todas");
        fila.add(btnBuscar);
        fila.add(btnListarTodas);

        fila.add(EstiloUI.etiqueta("ID:"));
        fila.add(campoBusquedaId);
        JButton btnBuscarId = EstiloUI.botonNeutro("Buscar por ID");
        fila.add(btnBuscarId);

        btnBuscar.addActionListener(e -> buscar());
        btnListarTodas.addActionListener(e -> cargarTabla());
        btnBuscarId.addActionListener(e -> buscarPorId());

        tarjeta.add(fila, BorderLayout.CENTER);
        return tarjeta;
    }

    private void registrar() {
        try {
            gestor.registrar(
                    campoNombre.getText(),
                    Integer.parseInt(campoHabitantes.getText()),
                    Integer.parseInt(campoPrioridad.getText()),
                    Double.parseDouble(campoRecursos.getText())
            );
            JOptionPane.showMessageDialog(this, "Comunidad registrada correctamente.");
            volverModoRegistro();
            cargarTabla();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Habitantes, prioridad y recursos deben ser numéricos.",
                    "Error de formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modificar() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una comunidad de la tabla.");
            return;
        }

        try {
            int id = (int) modeloTabla.getValueAt(fila, 0);
            Comunidad c = new Comunidad(
                    id,
                    campoNombre.getText(),
                    Integer.parseInt(campoHabitantes.getText()),
                    Integer.parseInt(campoPrioridad.getText()),
                    Double.parseDouble(campoRecursos.getText())
            );
            gestor.modificar(c);
            JOptionPane.showMessageDialog(this, "Comunidad actualizada correctamente.");
            volverModoRegistro();
            cargarTabla();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Habitantes, prioridad y recursos deben ser numéricos.",
                    "Error de formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una comunidad de la tabla.");
            return;
        }

        try {
            int id = (int) modeloTabla.getValueAt(fila, 0);
            gestor.eliminar(id);
            volverModoRegistro();
            cargarTabla();
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buscar() {
        try {
            llenarTabla(gestor.buscar(campoBusqueda.getText()));
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buscarPorId() {
        try {
            int id = Integer.parseInt(campoBusquedaId.getText());
            Comunidad encontrada = gestor.buscarPorId(id);

            if (encontrada == null) {
                JOptionPane.showMessageDialog(this, "No se encontró una comunidad con ID " + id + ".");
                return;
            }

            llenarTabla(new Comunidad[]{encontrada});
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese un ID numérico.");
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void cargarTabla() {
        try {
            llenarTabla(gestor.listarTodas());
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void llenarTabla(Comunidad[] comunidades) {
        modeloTabla.setRowCount(0);
        for (Comunidad c : comunidades) {
            modeloTabla.addRow(new Object[]{
                    c.getId(), c.getNombre(), c.getHabitantes(),
                    c.getPrioridad(), c.getRecursosRequeridos(), c.calcularBeneficio()
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
        valoresOriginales[1] = modeloTabla.getValueAt(fila, 2).toString();
        valoresOriginales[2] = modeloTabla.getValueAt(fila, 3).toString();
        valoresOriginales[3] = modeloTabla.getValueAt(fila, 4).toString();

        campoNombre.setText(valoresOriginales[0]);
        campoHabitantes.setText(valoresOriginales[1]);
        campoPrioridad.setText(valoresOriginales[2]);
        campoRecursos.setText(valoresOriginales[3]);
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
        boolean cambio = !campoNombre.getText().equals(valoresOriginales[0])
                || !campoHabitantes.getText().equals(valoresOriginales[1])
                || !campoPrioridad.getText().equals(valoresOriginales[2])
                || !campoRecursos.getText().equals(valoresOriginales[3]);
        btnModificar.setEnabled(cambio);
    }

    private void volverModoRegistro() {
        modoEdicion = false;
        tabla.clearSelection();
        limpiarFormulario();
        btnRegistrar.setVisible(true);
        btnModificar.setEnabled(false);
        btnEliminar.setEnabled(false);
    }

    private void limpiarFormulario() {
        actualizandoFormulario = true;
        campoNombre.setText("");
        campoHabitantes.setText("");
        campoPrioridad.setText("");
        campoRecursos.setText("");
        actualizandoFormulario = false;
    }
}
