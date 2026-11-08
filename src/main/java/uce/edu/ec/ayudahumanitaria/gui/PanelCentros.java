package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.CentroAcopio;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.GestorCentros;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Administración de los centros de acopio (origen de la distribución). */
public class PanelCentros extends JPanel implements Refrescable {

    private final GestorCentros gestor;
    private final GestorComunidades gestorComunidades;
    private final Runnable alCambiar;

    private final JTextField campoNombre = EstiloUI.campoTexto(16);
    private final JTextField campoDireccion = EstiloUI.campoTexto(28);
    private final javax.swing.JLabel etiquetaUbicacion = EstiloUI.etiqueta("Elija el punto en el mapa");
    private final JTextField campoLatitud = EstiloUI.campoTexto(8);
    private final JTextField campoLongitud = EstiloUI.campoTexto(8);
    private final JTextField campoStock = EstiloUI.campoTexto(8);
    private final JTextField campoReabastecer = EstiloUI.campoTexto(6);

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Dirección", "Ciudad", "Provincia", "Stock (t)"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final MapaEcuadorPanel mapa = new MapaEcuadorPanel();

    private final JButton btnRegistrar = EstiloUI.botonPrimario("Registrar");
    private final JButton btnModificar = EstiloUI.botonAccion("Guardar cambios");
    private final JButton btnEliminar = EstiloUI.botonPeligro("Eliminar");
    private final JButton btnNuevo = EstiloUI.botonNeutro("Nuevo");
    private final JButton btnMapa = EstiloUI.botonNeutro("Elegir en el mapa…");
    private final JButton btnReabastecer = EstiloUI.botonAccion("Reabastecer");

    private List<CentroAcopio> centros = List.of();
    private CentroAcopio seleccionado;

    public PanelCentros(GestorCentros gestor, GestorComunidades gestorComunidades, Runnable alCambiar) {
        this.gestor = gestor;
        this.gestorComunidades = gestorComunidades;
        this.alCambiar = alCambiar;

        setLayout(new BorderLayout(12, 12));
        setBackground(EstiloUI.FONDO_APP);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        EstiloUI.estilizarTabla(tabla);
        Tablas.anchos(tabla, 35, 160, 200, 90, 110, 70);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(EstiloUI.BORDE_SUAVE, 1));
        JPanel tarjetaTabla = EstiloUI.tarjeta("Centros de acopio");
        tarjetaTabla.add(scroll, BorderLayout.CENTER);

        JPanel tarjetaMapa = EstiloUI.tarjeta("Ubicación");
        tarjetaMapa.add(mapa, BorderLayout.CENTER);

        javax.swing.JSplitPane centro = new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT,
                tarjetaTabla, tarjetaMapa);
        centro.setResizeWeight(0.62);
        centro.setBorder(null);
        centro.setOpaque(false);

        add(construirFormulario(), BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);

        btnRegistrar.addActionListener(e -> registrar());
        btnModificar.addActionListener(e -> modificar());
        btnEliminar.addActionListener(e -> eliminar());
        btnNuevo.addActionListener(e -> nuevo());
        btnMapa.addActionListener(e -> elegirEnMapa());
        btnReabastecer.addActionListener(e -> reabastecer());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccion();
            }
        });
        nuevo();
        refrescar();
    }

    private JPanel construirFormulario() {
        JPanel tarjeta = EstiloUI.tarjeta("Datos del centro");
        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        agregar(campos, g, 0, 0, "Nombre:", campoNombre);
        agregar(campos, g, 2, 0, "Dirección:", campoDireccion);
        agregar(campos, g, 0, 1, "Latitud:", campoLatitud);
        agregar(campos, g, 2, 1, "Longitud:", campoLongitud);
        g.gridx = 4;
        g.gridy = 1;
        campos.add(btnMapa, g);
        agregar(campos, g, 0, 2, "Stock inicial (t):", campoStock);
        agregar(campos, g, 2, 2, "Ciudad · Provincia:", etiquetaUbicacion);

        JPanel acciones = EstiloUI.fila();
        acciones.add(btnRegistrar);
        acciones.add(btnModificar);
        acciones.add(btnEliminar);
        acciones.add(btnNuevo);
        acciones.add(EstiloUI.etiqueta("   Ingresar (t):"));
        acciones.add(campoReabastecer);
        acciones.add(btnReabastecer);

        JPanel alineado = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        alineado.setOpaque(false);
        alineado.add(campos);
        JPanel contenido = new JPanel(new BorderLayout(0, 6));
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

    // ------------------------------------------------------------------ acciones

    private void registrar() {
        try {
            double lat = numero(campoLatitud, "latitud");
            double lon = numero(campoLongitud, "longitud");
            UbicacionAutomatica u = ubicacion(lat, lon);
            gestor.registrar(campoNombre.getText(), campoDireccion.getText(), u.ciudad(), u.provincia(), lat, lon,
                    numero(campoStock, "stock"));
            aviso("Centro registrado correctamente.");
            nuevo();
            cambio();
        } catch (ValidationException | PersistenciaException | IllegalArgumentException ex) {
            error(ex);
        }
    }

    private void modificar() {
        if (seleccionado == null) {
            aviso("Seleccione un centro de la tabla.");
            return;
        }
        try {
            double lat = numero(campoLatitud, "latitud");
            double lon = numero(campoLongitud, "longitud");
            UbicacionAutomatica u = ubicacion(lat, lon);
            gestor.modificar(seleccionado, campoNombre.getText(), campoDireccion.getText(), u.ciudad(), u.provincia(),
                    lat, lon, numero(campoStock, "stock"));
            aviso("Centro actualizado correctamente.");
            nuevo();
            cambio();
        } catch (ValidationException | PersistenciaException | IllegalArgumentException ex) {
            error(ex);
        }
    }

    private void eliminar() {
        if (seleccionado == null) {
            aviso("Seleccione un centro de la tabla.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "¿Eliminar el centro " + seleccionado.getNombre() + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            gestor.eliminar(seleccionado.getId());
            nuevo();
            cambio();
        } catch (ValidationException | PersistenciaException ex) {
            error(ex);
        }
    }

    private void reabastecer() {
        if (seleccionado == null) {
            aviso("Seleccione un centro de la tabla.");
            return;
        }
        try {
            gestor.reabastecer(seleccionado, numero(campoReabastecer, "toneladas a ingresar"));
            campoReabastecer.setText("");
            cambio();
        } catch (ValidationException | PersistenciaException | NumberFormatException ex) {
            error(ex);
        }
    }

    private void elegirEnMapa() {
        double[] actual = null;
        try {
            actual = new double[]{parse(campoLatitud), parse(campoLongitud)};
        } catch (NumberFormatException ignorada) {
            // Sin coordenadas válidas todavía: se abre el mapa sin punto previo (no es un error).
        }
        try {
            DialogoSeleccionUbicacion.elegir(this, centros, List.of(gestorComunidades.listarTodas()), actual)
                    .ifPresent(p -> {
                        campoLatitud.setText(String.format(Locale.US, "%.4f", p[0]));
                        campoLongitud.setText(String.format(Locale.US, "%.4f", p[1]));
                        mostrarUbicacion(p[0], p[1]);
                    });
        } catch (PersistenciaException ex) {
            error(ex);
        }
    }

    private void nuevo() {
        seleccionado = null;
        tabla.clearSelection();
        campoNombre.setText("");
        campoDireccion.setText("");
        etiquetaUbicacion.setText("Elija el punto en el mapa");
        campoLatitud.setText("");
        campoLongitud.setText("");
        campoStock.setText("");
        btnRegistrar.setVisible(true);
        btnModificar.setEnabled(false);
        btnEliminar.setEnabled(false);
        btnReabastecer.setEnabled(false);
    }

    private void cargarSeleccion() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= centros.size()) {
            return;
        }
        seleccionado = centros.get(tabla.convertRowIndexToModel(fila));
        campoNombre.setText(seleccionado.getNombre());
        campoDireccion.setText(seleccionado.getDireccion());
        etiquetaUbicacion.setText(seleccionado.getCiudad() + " · " + seleccionado.getProvincia());
        campoLatitud.setText(String.format(Locale.US, "%.4f", seleccionado.getLatitud()));
        campoLongitud.setText(String.format(Locale.US, "%.4f", seleccionado.getLongitud()));
        campoStock.setText(String.format(Locale.US, "%.1f", seleccionado.getStockToneladas()));
        btnRegistrar.setVisible(false);
        btnModificar.setEnabled(true);
        btnEliminar.setEnabled(true);
        btnReabastecer.setEnabled(true);
    }

    private void cambio() {
        refrescar();
        alCambiar.run();
    }

    @Override
    public void refrescar() {
        try {
            centros = new ArrayList<>(gestor.listar());
            modelo.setRowCount(0);
            for (CentroAcopio c : centros) {
                modelo.addRow(new Object[]{c.getId(), c.getNombre(), c.getDireccion(), c.getCiudad(), c.getProvincia(),
                        String.format(Locale.US, "%.1f", c.getStockToneladas())});
            }
            mapa.setDatos(centros, List.of(gestorComunidades.listarTodas()), List.of());
        } catch (PersistenciaException ex) {
            error(ex);
        }
        // Importante: aquí NO se limpia el formulario; si no, el temporizador o el cambio de pestaña
        // borrarían lo que el usuario está escribiendo.
    }

    private static UbicacionAutomatica ubicacion(double lat, double lon) {
        return UbicacionAutomatica.de(lat, lon);
    }

    private void mostrarUbicacion(double lat, double lon) {
        try {
            etiquetaUbicacion.setText(ubicacion(lat, lon).resumen());
        } catch (IllegalArgumentException ex) {
            etiquetaUbicacion.setText(ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static double parse(JTextField campo) {
        return Double.parseDouble(campo.getText().trim().replace(',', '.'));
    }

    private static double numero(JTextField campo, String nombre) {
        try {
            return parse(campo);
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("El campo " + nombre + " debe ser numérico.");
        }
    }

    private void aviso(String texto) {
        JOptionPane.showMessageDialog(this, texto);
    }

    private void error(Exception ex) {
        boolean validacion = ex instanceof ValidationException || ex instanceof IllegalArgumentException;
        JOptionPane.showMessageDialog(this, ex.getMessage(), validacion ? "Validación" : "Error de base de datos",
                validacion ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }
}
