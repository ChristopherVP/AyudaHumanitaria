package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.CentroAcopio;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.GestorCentros;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorPlanes;
import uce.edu.ec.ayudahumanitaria.dominio.PlanificadorDistribucion;
import uce.edu.ec.ayudahumanitaria.geo.CatalogoEcuador;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Administración de comunidades: datos, ubicación en el mapa, centro que la atiende y avance de entrega. */
public class PanelComunidades extends JPanel implements Refrescable {

    private final GestorComunidades gestor;
    private final GestorCentros gestorCentros;
    private final GestorPlanes gestorPlanes;
    private final Runnable alCambiar;

    private final JTextField campoNombre = EstiloUI.campoTexto(16);
    private final JComboBox<String> comboProvincia = new JComboBox<>(CatalogoEcuador.nombresProvincias());
    private final JTextField campoHabitantes = EstiloUI.campoTexto(8);
    private final JTextField campoPrioridad = EstiloUI.campoTexto(4);
    private final JTextField campoRecursos = EstiloUI.campoTexto(8);
    private final JTextField campoLatitud = EstiloUI.campoTexto(8);
    private final JTextField campoLongitud = EstiloUI.campoTexto(8);
    private final JTextField campoBusqueda = EstiloUI.campoTexto(12);
    private final JTextField campoBusquedaId = EstiloUI.campoTexto(6);

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Provincia", "Habitantes", "Prioridad", "Req. (t)", "Beneficio",
                    "Centro asignado", "Dist. (km)", "Planif. (t)", "Entreg. (t)"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columna) {
            return switch (columna) {
                case 0, 3, 4 -> Integer.class;
                case 6 -> Double.class;
                default -> String.class;
            };
        }
    };
    private final JTable tabla = new JTable(modeloTabla);
    private final MapaEcuadorPanel mapa = new MapaEcuadorPanel();

    private final JButton btnRegistrar = EstiloUI.botonPrimario("Registrar");
    private final JButton btnModificar = EstiloUI.botonAccion("Modificar seleccionada");
    private final JButton btnEliminar = EstiloUI.botonPeligro("Eliminar seleccionada");
    private final JButton btnNuevo = EstiloUI.botonNeutro("Nuevo");
    private final JButton btnUbicar = EstiloUI.botonNeutro("Elegir en el mapa…");

    private List<CentroAcopio> centros = List.of();
    private Comunidad[] mostradas = new Comunidad[0];
    private Comunidad seleccionada;

    public PanelComunidades(GestorComunidades gestor, GestorCentros gestorCentros, GestorPlanes gestorPlanes,
                            Runnable alCambiar) {
        this.gestor = gestor;
        this.gestorCentros = gestorCentros;
        this.gestorPlanes = gestorPlanes;
        this.alCambiar = alCambiar;

        setLayout(new BorderLayout(12, 12));
        setBackground(EstiloUI.FONDO_APP);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel superior = new JPanel(new BorderLayout(0, 12));
        superior.setOpaque(false);
        superior.add(construirFormulario(), BorderLayout.NORTH);
        superior.add(construirBusqueda(), BorderLayout.CENTER);

        EstiloUI.estilizarTabla(tabla);
        tabla.setAutoCreateRowSorter(true);
        Tablas.anchos(tabla, 35, 170, 110, 75, 65, 65, 75, 110, 65, 65, 65);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(EstiloUI.BORDE_SUAVE, 1));
        JPanel tarjetaTabla = EstiloUI.tarjeta("Comunidades registradas");
        tarjetaTabla.add(scroll, BorderLayout.CENTER);

        JPanel tarjetaMapa = EstiloUI.tarjeta("Mapa (la comunidad elegida se resalta)");
        tarjetaMapa.add(mapa, BorderLayout.CENTER);

        JPanel izquierda = new JPanel(new BorderLayout(0, 12));
        izquierda.setOpaque(false);
        izquierda.add(superior, BorderLayout.NORTH);
        izquierda.add(tarjetaTabla, BorderLayout.CENTER);

        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, tarjetaMapa);
        division.setResizeWeight(0.68);
        division.setBorder(null);
        division.setOpaque(false);

        add(division, BorderLayout.CENTER);

        btnRegistrar.addActionListener(e -> registrar());
        btnModificar.addActionListener(e -> modificar());
        btnEliminar.addActionListener(e -> eliminar());
        btnNuevo.addActionListener(e -> volverModoRegistro());
        btnUbicar.addActionListener(e -> elegirEnMapa());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccionEnFormulario();
            }
        });

        volverModoRegistro();
        refrescar();
    }

    private JPanel construirFormulario() {
        JPanel tarjeta = EstiloUI.tarjeta("Datos de la comunidad");
        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        agregar(campos, g, 0, 0, "Nombre:", campoNombre);
        agregar(campos, g, 2, 0, "Provincia:", comboProvincia);
        agregar(campos, g, 0, 1, "Habitantes:", campoHabitantes);
        agregar(campos, g, 2, 1, "Prioridad (1-10):", campoPrioridad);
        agregar(campos, g, 0, 2, "Recursos req. (t):", campoRecursos);
        g.gridx = 2;
        g.gridy = 2;
        g.gridwidth = 2;
        campos.add(btnUbicar, g);
        g.gridwidth = 1;
        agregar(campos, g, 0, 3, "Latitud:", campoLatitud);
        agregar(campos, g, 2, 3, "Longitud:", campoLongitud);

        JPanel acciones = EstiloUI.fila();
        acciones.add(btnRegistrar);
        acciones.add(btnModificar);
        acciones.add(btnEliminar);
        acciones.add(btnNuevo);

        JPanel alineado = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        alineado.setOpaque(false);
        alineado.add(campos);
        JPanel contenido = new JPanel(new BorderLayout(0, 8));
        contenido.setOpaque(false);
        contenido.add(alineado, BorderLayout.NORTH);
        contenido.add(acciones, BorderLayout.SOUTH);
        tarjeta.add(contenido, BorderLayout.CENTER);
        return tarjeta;
    }

    private static void agregar(JPanel panel, GridBagConstraints g, int x, int y, String texto,
                                java.awt.Component campo) {
        g.gridx = x;
        g.gridy = y;
        panel.add(EstiloUI.etiqueta(texto), g);
        g.gridx = x + 1;
        panel.add(campo, g);
    }

    private JPanel construirBusqueda() {
        JPanel tarjeta = EstiloUI.tarjeta("Buscar comunidades");
        JPanel fila = EstiloUI.fila();
        fila.add(EstiloUI.etiqueta("Nombre:"));
        fila.add(campoBusqueda);
        JButton buscar = EstiloUI.botonNeutro("Buscar");
        JButton todas = EstiloUI.botonNeutro("Listar todas");
        fila.add(buscar);
        fila.add(todas);
        fila.add(EstiloUI.etiqueta("ID:"));
        fila.add(campoBusquedaId);
        JButton porId = EstiloUI.botonNeutro("Buscar por ID");
        fila.add(porId);
        buscar.addActionListener(e -> buscar());
        todas.addActionListener(e -> refrescar());
        porId.addActionListener(e -> buscarPorId());
        tarjeta.add(fila, BorderLayout.CENTER);
        return tarjeta;
    }

    // ------------------------------------------------------------------ acciones

    private void registrar() {
        try {
            gestor.registrar(campoNombre.getText(), (String) comboProvincia.getSelectedItem(),
                    entero(campoHabitantes, "habitantes"), entero(campoPrioridad, "prioridad"),
                    decimal(campoRecursos, "recursos"), decimal(campoLatitud, "latitud"),
                    decimal(campoLongitud, "longitud"));
            JOptionPane.showMessageDialog(this, "Comunidad registrada correctamente.");
            volverModoRegistro();
            cambio();
        } catch (ValidationException | PersistenciaException | NumberFormatException ex) {
            error(ex);
        }
    }

    private void modificar() {
        if (seleccionada == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una comunidad de la tabla.");
            return;
        }
        try {
            Comunidad c = new Comunidad(seleccionada.getId(), campoNombre.getText(),
                    (String) comboProvincia.getSelectedItem(), entero(campoHabitantes, "habitantes"),
                    entero(campoPrioridad, "prioridad"), decimal(campoRecursos, "recursos"),
                    decimal(campoLatitud, "latitud"), decimal(campoLongitud, "longitud"));
            gestor.modificar(c);
            JOptionPane.showMessageDialog(this, "Comunidad actualizada correctamente.");
            volverModoRegistro();
            cambio();
        } catch (ValidationException | PersistenciaException | NumberFormatException ex) {
            error(ex);
        }
    }

    private void eliminar() {
        if (seleccionada == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una comunidad de la tabla.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "¿Eliminar la comunidad " + seleccionada.getNombre() + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            gestor.eliminar(seleccionada.getId());
            volverModoRegistro();
            cambio();
        } catch (ValidationException | PersistenciaException ex) {
            error(ex);
        }
    }

    private void buscar() {
        try {
            llenarTabla(gestor.buscar(campoBusqueda.getText()));
        } catch (PersistenciaException ex) {
            error(ex);
        }
    }

    private void buscarPorId() {
        try {
            int id = Integer.parseInt(campoBusquedaId.getText().trim());
            Comunidad encontrada = gestor.buscarPorId(id);
            if (encontrada == null) {
                JOptionPane.showMessageDialog(this, "No se encontró una comunidad con ID " + id + ".");
                return;
            }
            llenarTabla(new Comunidad[]{encontrada});
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese un ID numérico.");
        } catch (PersistenciaException ex) {
            error(ex);
        }
    }

    private void elegirEnMapa() {
        double[] actual = null;
        try {
            actual = new double[]{decimalSinMensaje(campoLatitud), decimalSinMensaje(campoLongitud)};
        } catch (NumberFormatException ignorada) {
            // Aún no hay coordenadas escritas: el mapa se abre sin punto previo (no es un error).
        }
        DialogoSeleccionUbicacion.elegir(this, centros, List.of(mostradas), actual).ifPresent(p -> {
            campoLatitud.setText(String.format(Locale.US, "%.4f", p[0]));
            campoLongitud.setText(String.format(Locale.US, "%.4f", p[1]));
        });
    }

    private void cambio() {
        refrescar();
        alCambiar.run();
    }

    @Override
    public void refrescar() {
        try {
            centros = gestorCentros.listar();
            llenarTabla(gestor.listarTodas());
        } catch (PersistenciaException ex) {
            error(ex);
        }
    }

    private void llenarTabla(Comunidad[] comunidades) {
        try {
            Map<Integer, Double> planificado = gestorPlanes.planificadoPorComunidad();
            Map<Integer, Double> entregado = gestorPlanes.entregadoPorComunidad();
            mostradas = comunidades;
            modeloTabla.setRowCount(0);
            for (Comunidad c : comunidades) {
                String centro = "—";
                String distancia = "—";
                if (c.tieneUbicacion() && !centros.isEmpty()) {
                    CentroAcopio cercano = PlanificadorDistribucion.centroMasCercano(centros, c);
                    centro = cercano.getCiudad();
                    distancia = String.format(Locale.US, "%.0f", cercano.distanciaCarreteraKm(c.getLatitud(), c.getLongitud()));
                }
                modeloTabla.addRow(new Object[]{c.getId(), c.getNombre(), c.getProvincia(), c.getHabitantes(),
                        c.getPrioridad(), String.format(Locale.US, "%.1f", c.getRecursosRequeridos()),
                        c.calcularBeneficio(), centro, distancia,
                        String.format(Locale.US, "%.1f", planificado.getOrDefault(c.getId(), 0.0)),
                        String.format(Locale.US, "%.1f", entregado.getOrDefault(c.getId(), 0.0))});
            }
            mapa.setDatos(centros, List.of(comunidades), List.of());
        } catch (PersistenciaException ex) {
            error(ex);
        }
    }

    private void cargarSeleccionEnFormulario() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        int id = (int) modeloTabla.getValueAt(tabla.convertRowIndexToModel(fila), 0);
        seleccionada = null;
        for (Comunidad c : mostradas) {
            if (c.getId() == id) {
                seleccionada = c;
            }
        }
        if (seleccionada == null) {
            return;
        }
        campoNombre.setText(seleccionada.getNombre());
        comboProvincia.setSelectedItem(seleccionada.getProvincia());
        campoHabitantes.setText(String.valueOf(seleccionada.getHabitantes()));
        campoPrioridad.setText(String.valueOf(seleccionada.getPrioridad()));
        campoRecursos.setText(String.format(Locale.US, "%.1f", seleccionada.getRecursosRequeridos()));
        campoLatitud.setText(String.format(Locale.US, "%.4f", seleccionada.getLatitud()));
        campoLongitud.setText(String.format(Locale.US, "%.4f", seleccionada.getLongitud()));
        btnRegistrar.setVisible(false);
        btnModificar.setEnabled(true);
        btnEliminar.setEnabled(true);
        mapa.setPuntoElegido(seleccionada.getLatitud(), seleccionada.getLongitud());
    }

    private void volverModoRegistro() {
        seleccionada = null;
        tabla.clearSelection();
        campoNombre.setText("");
        campoHabitantes.setText("");
        campoPrioridad.setText("");
        campoRecursos.setText("");
        campoLatitud.setText("");
        campoLongitud.setText("");
        btnRegistrar.setVisible(true);
        btnModificar.setEnabled(false);
        btnEliminar.setEnabled(false);
    }

    // ------------------------------------------------------------------ utilidades

    private static int entero(JTextField campo, String nombre) {
        try {
            return Integer.parseInt(campo.getText().trim());
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("El campo " + nombre + " debe ser un número entero.");
        }
    }

    private static double decimalSinMensaje(JTextField campo) {
        return Double.parseDouble(campo.getText().trim().replace(',', '.'));
    }

    private static double decimal(JTextField campo, String nombre) {
        try {
            return decimalSinMensaje(campo);
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("El campo " + nombre + " debe ser numérico.");
        }
    }

    private void error(Exception ex) {
        boolean validacion = ex instanceof ValidationException || ex instanceof NumberFormatException;
        JOptionPane.showMessageDialog(this, ex.getMessage(), validacion ? "Validación" : "Error de base de datos",
                validacion ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }
}
