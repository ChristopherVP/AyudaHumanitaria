package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.EstadoViaje;
import uce.edu.ec.ayudahumanitaria.dominio.GestorCentros;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorPlanes;
import uce.edu.ec.ayudahumanitaria.dominio.Pendiente;
import uce.edu.ec.ayudahumanitaria.dominio.PlanDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.RecomendadorEnvio;
import uce.edu.ec.ayudahumanitaria.dominio.RecomendadorEnvio.ResumenCentro;
import uce.edu.ec.ayudahumanitaria.dominio.SeleccionEnvio;
import uce.edu.ec.ayudahumanitaria.dominio.Vehiculo;
import uce.edu.ec.ayudahumanitaria.dominio.Viaje;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.util.FormatoPlan;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Simulación y registro de la distribución con fechas reales: se elige la hora de salida, se calcula el plan
 * (reparto, grupos, vehículos y rutas), se revisa en la tabla/mapa y, si es correcto, se confirma.
 */
public class PanelPlanificacion extends JPanel implements Refrescable {

    private final GestorPlanes gestorPlanes;
    private final GestorCentros gestorCentros;
    private final GestorComunidades gestorComunidades;
    private final Runnable alCambiar;

    private final JSpinner selectorSalida;
    private final JLabel resumen = EstiloUI.textoSecundario("Elija la fecha y hora de salida y pulse «Simular».");
    private final JButton btnSimular = EstiloUI.botonPrimario("Simular");
    private final JButton btnConfirmar = EstiloUI.botonAccion("Confirmar plan");
    private final JButton btnCancelarPlan = EstiloUI.botonPeligro("Cancelar plan seleccionado");
    private final JButton btnActualizar = EstiloUI.botonNeutro("Actualizar estados");
    private final JButton btnRecomendar = EstiloUI.botonAccion("Recomendar vehículos");
    private final JButton btnRapida = EstiloUI.botonAccion("Simulación rápida");
    private final JButton btnTodos = EstiloUI.botonNeutro("Marcar todos");
    private final JButton btnNinguno = EstiloUI.botonNeutro("Quitar todos");

    /** Elección del usuario (id de vehículo → viajes). Sobrevive a los refrescos automáticos. */
    private final Map<String, Integer> eleccion = new LinkedHashMap<>();
    private List<ResumenCentro> resumenes = List.of();
    private boolean cargandoFlota;
    private boolean ajustandoModo;
    /** Motivo de cada vehículo sugerido por los algoritmos (vacío para los elegidos a mano). */
    private Map<String, String> motivos = Map.of();
    private final JComboBox<String> comboModo = new JComboBox<>(new String[]{
            "Automático (los algoritmos eligen)",
            "Manual (yo elijo)"});
    private final JComboBox<Object> comboCentros = new JComboBox<>();

    private final DefaultTableModel modeloFlota = new DefaultTableModel(
            new Object[]{"Enviar", "Vehículo", "Centro", "Capacidad (t)", "Viajes", "Por qué (algoritmo)"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return columna == 0 || columna == 4;
        }

        @Override
        public Class<?> getColumnClass(int columna) {
            return switch (columna) {
                case 0 -> Boolean.class;
                case 4 -> Integer.class;
                default -> String.class;
            };
        }
    };
    private final JTable tablaFlota = new JTable(modeloFlota);
    private final DefaultTableModel modeloSituacion = new DefaultTableModel(
            new Object[]{"Centro", "Comunidades que atiende (necesidad)", "Necesidad (t)", "Stock (t)",
                    "A enviar (t)", "Capacidad elegida (t)", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tablaSituacion = new JTable(modeloSituacion);

    private final DefaultTableModel modeloViajes = new DefaultTableModel(
            new Object[]{"#", "Centro", "Vehículo", "Viaje", "Carga (t)", "Ocup.", "Km", "Salida", "Regreso", "Estado", "Comunidades"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tablaViajes = new JTable(modeloViajes);
    private final DefaultTableModel modeloPendientes = new DefaultTableModel(
            new Object[]{"Comunidad", "Centro", "Pendiente (t)", "Motivo"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final DefaultTableModel modeloHistorial = new DefaultTableModel(
            new Object[]{"Plan", "Creado", "Salida", "Fin estimado", "Viajes", "Toneladas", "Km", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tablaHistorial = new JTable(modeloHistorial);
    private final JTextArea areaItinerario = new JTextArea();
    private final JTextArea areaBitacora = new JTextArea();
    private final MapaEcuadorPanel mapa = new MapaEcuadorPanel();

    private PlanDistribucion planMostrado;
    private List<PlanDistribucion> historial = new ArrayList<>();
    private boolean cargandoHistorial;

    public PanelPlanificacion(GestorPlanes gestorPlanes, GestorCentros gestorCentros,
                              GestorComunidades gestorComunidades, Runnable alCambiar) {
        this.gestorPlanes = gestorPlanes;
        this.gestorCentros = gestorCentros;
        this.gestorComunidades = gestorComunidades;
        this.alCambiar = alCambiar;

        setLayout(new BorderLayout(12, 12));
        setBackground(EstiloUI.FONDO_APP);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        Date inicial = Date.from(LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
                .atZone(ZoneId.systemDefault()).toInstant());
        selectorSalida = new JSpinner(new SpinnerDateModel(inicial, null, null, java.util.Calendar.HOUR_OF_DAY));
        selectorSalida.setEditor(new JSpinner.DateEditor(selectorSalida, "dd/MM/yyyy HH:mm"));
        selectorSalida.setFont(EstiloUI.FUENTE_BASE);

        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.setOpaque(false);
        norte.add(construirEleccionVehiculos(), BorderLayout.CENTER);
        norte.add(construirCabecera(), BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);
        add(construirCentro(), BorderLayout.CENTER);

        btnSimular.addActionListener(e -> simular());
        btnConfirmar.addActionListener(e -> confirmar());
        btnCancelarPlan.addActionListener(e -> cancelarPlan());
        btnActualizar.addActionListener(e -> refrescar());
        btnRecomendar.addActionListener(e -> recomendar());
        btnRapida.addActionListener(e -> simulacionRapida());
        btnTodos.addActionListener(e -> marcarTodos(true));
        btnNinguno.addActionListener(e -> marcarTodos(false));
        modeloFlota.addTableModelListener(e -> {
            if (!cargandoFlota) {
                leerEleccionDeLaTabla();
                pintarSituacion();
                pasarAManual();
            }
        });
        tablaViajes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarViajeSeleccionado();
            }
        });
        tablaHistorial.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !cargandoHistorial) {
                mostrarPlanDelHistorial();
            }
        });
        btnConfirmar.setEnabled(false);
        btnCancelarPlan.setEnabled(false);
        refrescar();
    }

    private JPanel construirCabecera() {
        JPanel tarjeta = EstiloUI.tarjeta("2) Fecha de salida y simulación");
        JPanel filaModo = EstiloUI.fila();
        filaModo.add(EstiloUI.etiqueta("Modo:"));
        filaModo.add(comboModo);
        filaModo.add(EstiloUI.etiqueta("Centros que participan:"));
        comboCentros.setPreferredSize(new java.awt.Dimension(340, comboCentros.getPreferredSize().height));
        comboCentros.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> lista, Object valor,
                                                                   int indice, boolean sel, boolean foco) {
                String texto = valor instanceof ResumenCentro r ? r.centro().getNombre() + " (solo este centro)"
                        : String.valueOf(valor);
                return super.getListCellRendererComponent(lista, texto, indice, sel, foco);
            }
        });
        filaModo.add(comboCentros);
        JPanel fila = EstiloUI.fila();
        fila.add(EstiloUI.etiqueta("Salida (fecha y hora):"));
        fila.add(selectorSalida);
        fila.add(btnSimular);
        fila.add(btnConfirmar);
        fila.add(btnCancelarPlan);
        fila.add(btnActualizar);
        JPanel contenido = new JPanel(new BorderLayout(0, 4));
        contenido.setOpaque(false);
        JPanel controles = new JPanel(new java.awt.GridLayout(0, 1, 0, 0));
        controles.setOpaque(false);
        controles.add(filaModo);
        controles.add(fila);
        contenido.add(controles, BorderLayout.NORTH);
        contenido.add(resumen, BorderLayout.SOUTH);
        tarjeta.add(contenido, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel construirEleccionVehiculos() {
        JPanel tarjeta = EstiloUI.tarjeta("1) Elija qué vehículos envía cada centro y cuántos viajes hace cada uno");
        EstiloUI.estilizarTabla(tablaFlota);
        EstiloUI.estilizarTabla(tablaSituacion);
        Tablas.anchos(tablaFlota, 55, 90, 110, 90, 60, 520);
        Tablas.anchos(tablaSituacion, 100, 480, 90, 80, 90, 130, 230);
        JComboBox<Integer> viajes = new JComboBox<>();
        for (int i = 1; i <= SeleccionEnvio.MAX_VIAJES_POR_VEHICULO; i++) {
            viajes.addItem(i);
        }
        javax.swing.JCheckBox casilla = new javax.swing.JCheckBox();
        casilla.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tablaFlota.getColumnModel().getColumn(0).setCellRenderer((t, valor, sel, foco, fila, col) -> {
            casilla.setSelected(Boolean.TRUE.equals(valor));
            casilla.setBackground(sel ? t.getSelectionBackground() : (fila % 2 == 0 ? java.awt.Color.WHITE
                    : EstiloUI.FILA_ALTERNA));
            return casilla;
        });
        tablaFlota.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new javax.swing.JCheckBox()));
        tablaFlota.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(viajes));

        JScrollPane scrollFlota = new JScrollPane(tablaFlota);
        JScrollPane scrollSituacion = new JScrollPane(tablaSituacion);
        JPanel division = new JPanel(new java.awt.GridLayout(2, 1, 0, 8));
        division.setOpaque(false);
        division.add(scrollFlota);
        division.add(scrollSituacion);
        division.setPreferredSize(new java.awt.Dimension(900, 400));

        JPanel botones = EstiloUI.fila();
        botones.add(btnTodos);
        botones.add(btnNinguno);
        botones.add(btnRecomendar);
        botones.add(btnRapida);
        botones.add(EstiloUI.textoSecundario("  Un vehículo con 2 o más viajes vuelve al centro, recarga y sale de nuevo."));

        JPanel contenido = new JPanel(new BorderLayout(0, 6));
        contenido.setOpaque(false);
        contenido.add(botones, BorderLayout.NORTH);
        contenido.add(division, BorderLayout.CENTER);
        tarjeta.add(contenido, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel construirCentro() {
        EstiloUI.estilizarTabla(tablaViajes);
        Tablas.anchos(tablaViajes, 35, 110, 80, 60, 70, 60, 60, 130, 130, 95, 380);
        Tablas.anchos(tablaHistorial, 45, 115, 115, 115, 55, 80, 60, 90);
        EstiloUI.estilizarTabla(tablaHistorial);
        JTable tablaPendientes = new JTable(modeloPendientes);
        EstiloUI.estilizarTabla(tablaPendientes);
        EstiloUI.estilizarAreaTexto(areaItinerario);
        EstiloUI.estilizarAreaTexto(areaBitacora);
        areaItinerario.setEditable(false);
        areaBitacora.setEditable(false);
        areaItinerario.setFont(EstiloUI.FUENTE_MONO);
        areaBitacora.setFont(EstiloUI.FUENTE_MONO);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Itinerario del viaje", new JScrollPane(areaItinerario));
        pestanas.addTab("Pendientes", new JScrollPane(tablaPendientes));
        pestanas.addTab("Bitácora de algoritmos", new JScrollPane(areaBitacora));
        pestanas.addTab("Historial de planes", new JScrollPane(tablaHistorial));

        JPanel tarjetaViajes = EstiloUI.tarjeta("Viajes del plan (seleccione uno para ver su itinerario y su ruta)");
        tarjetaViajes.add(new JScrollPane(tablaViajes), BorderLayout.CENTER);
        tarjetaViajes.setPreferredSize(new java.awt.Dimension(900, 330));

        JPanel tarjetaMapa = EstiloUI.tarjeta("Ruta del viaje seleccionado");
        tarjetaMapa.add(mapa, BorderLayout.CENTER);

        JPanel fila = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        fila.setOpaque(false);
        fila.add(pestanas);
        fila.add(tarjetaMapa);
        fila.setPreferredSize(new java.awt.Dimension(900, 420));

        JPanel contenedor = new JPanel(new BorderLayout(0, 12));
        contenedor.setOpaque(false);
        contenedor.add(tarjetaViajes, BorderLayout.NORTH);
        contenedor.add(fila, BorderLayout.CENTER);
        return contenedor;
    }

    // ------------------------------------------------------------------ acciones

    private void simular() {
        try {
            Date fecha = (Date) selectorSalida.getValue();
            LocalDateTime salida = LocalDateTime.ofInstant(fecha.toInstant(), ZoneId.systemDefault());
            PlanDistribucion plan;
            if (esAutomatico()) {
                GestorPlanes.ResultadoRapido r = gestorPlanes.simularAutomatico(salida, centroElegido());
                aplicarEleccion(r.recomendacion().seleccion(), r.recomendacion().motivos());
                plan = r.plan();
            } else {
                leerEleccionDeLaTabla();
                plan = gestorPlanes.simular(salida, new SeleccionEnvio(eleccion));
            }
            mostrarPlan(plan);
            tablaHistorial.clearSelection();
            btnConfirmar.setEnabled(!plan.estaVacio());
            btnCancelarPlan.setEnabled(false);
            if (plan.estaVacio()) {
                JOptionPane.showMessageDialog(this, "No hay nada que distribuir con los datos actuales "
                        + "(revise stock de los centros, vehículos libres y necesidades pendientes).");
            }
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recomendar() {
        try {
            RecomendadorEnvio.Recomendacion r = gestorPlanes.recomendar(centroElegido());
            aplicarEleccion(r.seleccion(), r.motivos());
            JOptionPane.showMessageDialog(this, String.join("\n\n", r.explicacion())
                            + "\n\nEs solo una sugerencia: puede cambiar vehículos y viajes antes de simular.",
                    "Recomendación (Greedy)", JOptionPane.INFORMATION_MESSAGE);
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Recomienda vehículos y simula de una vez; no guarda nada hasta que el usuario confirme. */
    private void simulacionRapida() {
        try {
            GestorPlanes.ResultadoRapido r = gestorPlanes.simulacionRapida();
            aplicarEleccion(r.recomendacion().seleccion(), r.recomendacion().motivos());
            selectorSalida.setValue(Date.from(r.plan().salidaProgramada().atZone(ZoneId.systemDefault()).toInstant()));
            mostrarPlan(r.plan());
            tablaHistorial.clearSelection();
            btnConfirmar.setEnabled(!r.plan().estaVacio());
            btnCancelarPlan.setEnabled(false);
            areaBitacora.setText("SIMULACIÓN RÁPIDA (salida en 30 min, vehículos sugeridos por Greedy)\n"
                    + String.join("\n", r.recomendacion().explicacion()) + "\n\n" + areaBitacora.getText());
            areaBitacora.setCaretPosition(0);
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------ elección de vehículos

    /** Marca en la tabla la elección recibida (p. ej. desde la recomendación de la sección de algoritmos). */
    public void aplicarSeleccion(SeleccionEnvio seleccion) {
        aplicarEleccion(seleccion, Map.of());
    }

    private boolean esAutomatico() {
        return comboModo.getSelectedIndex() == 0;
    }

    /** Id del centro elegido, o {@code null} si participan todos (cada comunidad va al centro más cercano). */
    private Integer centroElegido() {
        return comboCentros.getSelectedItem() instanceof ResumenCentro r ? r.centro().getId() : null;
    }

    /** Si el usuario toca la tabla, deja de ser automático: sus cambios mandan. */
    private void pasarAManual() {
        if (!ajustandoModo && esAutomatico()) {
            ajustandoModo = true;
            comboModo.setSelectedIndex(1);
            motivos = Map.of();
            ajustandoModo = false;
        }
    }

    private void marcarTodos(boolean marcar) {
        eleccion.clear();
        if (marcar) {
            for (ResumenCentro r : resumenes) {
                for (Vehiculo v : r.flota()) {
                    eleccion.put(v.getId(), 1);
                }
            }
        }
        motivos = Map.of();
        llenarFlota();
        pintarSituacion();
        pasarAManual();
    }

    private void aplicarEleccion(SeleccionEnvio seleccion, Map<String, String> nuevosMotivos) {
        motivos = nuevosMotivos;
        eleccion.clear();
        eleccion.putAll(seleccion.viajesPorVehiculo());
        llenarFlota();
        pintarSituacion();
    }

    private void leerEleccionDeLaTabla() {
        eleccion.clear();
        for (int i = 0; i < modeloFlota.getRowCount(); i++) {
            if (Boolean.TRUE.equals(modeloFlota.getValueAt(i, 0))) {
                Object viajes = modeloFlota.getValueAt(i, 4);
                eleccion.put((String) modeloFlota.getValueAt(i, 1), viajes instanceof Integer n ? n : 1);
            }
        }
    }

    private void actualizarComboCentros() {
        Integer anterior = centroElegido();
        ajustandoModo = true;
        comboCentros.removeAllItems();
        comboCentros.addItem("Todos (cada comunidad va al centro más cercano)");
        for (ResumenCentro r : resumenes) {
            comboCentros.addItem(r);
        }
        if (anterior != null) {
            resumenes.stream().filter(r -> r.centro().getId() == anterior).findFirst()
                    .ifPresent(comboCentros::setSelectedItem);
        }
        ajustandoModo = false;
    }

    private void llenarFlota() {
        if (tablaFlota.isEditing()) {
            return;
        }
        cargandoFlota = true;
        modeloFlota.setRowCount(0);
        for (ResumenCentro r : resumenes) {
            for (Vehiculo v : r.flota()) {
                int viajes = eleccion.getOrDefault(v.getId(), 0);
                modeloFlota.addRow(new Object[]{viajes > 0, v.getId(), r.centro().getCiudad(),
                        FormatoPlan.numero(v.getCapacidadToneladas(), 1), Math.max(1, viajes),
                        viajes > 0 ? motivos.getOrDefault(v.getId(), "Elegido por usted") : ""});
            }
        }
        cargandoFlota = false;
    }

    private void pintarSituacion() {
        modeloSituacion.setRowCount(0);
        for (ResumenCentro r : resumenes) {
            double elegida = 0;
            for (Vehiculo v : r.flota()) {
                elegida += v.getCapacidadToneladas() * eleccion.getOrDefault(v.getId(), 0);
            }
            modeloSituacion.addRow(new Object[]{r.centro().getCiudad(), textoComunidades(r),
                    FormatoPlan.numero(r.demandaToneladas(), 1), FormatoPlan.numero(r.stockToneladas(), 1),
                    FormatoPlan.numero(r.enviableToneladas(), 1), FormatoPlan.numero(elegida, 1),
                    estadoEleccion(r, elegida)});
        }
    }

    private static String textoComunidades(ResumenCentro r) {
        if (r.comunidades().isEmpty()) {
            return "Sin necesidades pendientes";
        }
        StringBuilder sb = new StringBuilder();
        for (Comunidad c : r.comunidades()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(String.format(Locale.US, "%s %.1f t (P%d)", c.getNombre(), c.getRecursosRequeridos(),
                    c.getPrioridad()));
        }
        return sb.toString();
    }

    private static String estadoEleccion(ResumenCentro r, double capacidadElegida) {
        if (r.enviableToneladas() <= 0.05) {
            return "Nada que enviar";
        }
        if (capacidadElegida <= 0) {
            return "Sin vehículos elegidos";
        }
        double falta = r.enviableToneladas() - capacidadElegida;
        return falta > 0.05 ? String.format(Locale.US, "Faltan %.1f t: agregue vehículos o viajes", falta)
                : "Cubre la carga";
    }

    private void confirmar() {
        if (planMostrado == null || planMostrado.id() != 0) {
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "Se registrarán " + planMostrado.viajes().size() + " viajes y se descontarán "
                        + FormatoPlan.numero(planMostrado.totalToneladas(), 1) + " t del stock de los centros.\n¿Confirmar el plan?",
                "Confirmar plan", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            PlanDistribucion guardado = gestorPlanes.confirmar(planMostrado);
            JOptionPane.showMessageDialog(this, "Plan " + guardado.id() + " confirmado.");
            alCambiar.run();
            refrescar();
            seleccionarPlanEnHistorial(guardado.id());
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelarPlan() {
        if (planMostrado == null || planMostrado.id() == 0) {
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "Se cancelarán los viajes no completados del plan " + planMostrado.id()
                        + " y se devolverá su stock a los centros.\n¿Continuar?", "Cancelar plan", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            int id = planMostrado.id();
            gestorPlanes.cancelar(id);
            alCambiar.run();
            refrescar();
            seleccionarPlanEnHistorial(id);
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refrescar() {
        try {
            historial = new ArrayList<>(gestorPlanes.listarPlanes());
            resumenes = gestorPlanes.resumenPorCentro();
            actualizarComboCentros();
            eleccion.keySet().removeIf(id -> resumenes.stream().noneMatch(
                    r -> r.flota().stream().anyMatch(v -> v.getId().equals(id))));
            llenarFlota();
            pintarSituacion();
            cargandoHistorial = true;
            modeloHistorial.setRowCount(0);
            LocalDateTime ahora = LocalDateTime.now();
            for (PlanDistribucion p : historial) {
                modeloHistorial.addRow(new Object[]{p.id(), FormatoPlan.fechaHora(p.creadoEn()),
                        FormatoPlan.fechaHora(p.salidaProgramada()), FormatoPlan.fechaHora(p.finEstimado()),
                        p.viajes().size(), FormatoPlan.numero(p.totalToneladas(), 1),
                        FormatoPlan.numero(p.totalKm(), 0), estadoDelPlan(p, ahora)});
            }
            cargandoHistorial = false;
            if (planMostrado != null && planMostrado.id() != 0) {
                int id = planMostrado.id();
                historial.stream().filter(p -> p.id() == id).findFirst().ifPresent(this::mostrarPlan);
            } else if (planMostrado == null && !historial.isEmpty()) {
                mostrarPlan(historial.get(0));
                btnCancelarPlan.setEnabled(!historial.get(0).estaCancelado());
            }
        } catch (PersistenciaException ex) {
            cargandoHistorial = false;
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String estadoDelPlan(PlanDistribucion plan, LocalDateTime ahora) {
        if (plan.estaCancelado()) {
            return EstadoViaje.CANCELADO.etiqueta();
        }
        boolean activo = plan.viajes().stream().anyMatch(v -> v.estadoEn(ahora).estaActivo());
        boolean enRuta = plan.viajes().stream().anyMatch(v -> v.estadoEn(ahora) == EstadoViaje.EN_RUTA);
        if (enRuta) {
            return EstadoViaje.EN_RUTA.etiqueta();
        }
        return activo ? EstadoViaje.PLANIFICADO.etiqueta() : EstadoViaje.COMPLETADO.etiqueta();
    }

    private void seleccionarPlanEnHistorial(int id) {
        for (int i = 0; i < historial.size(); i++) {
            if (historial.get(i).id() == id) {
                tablaHistorial.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    private void mostrarPlanDelHistorial() {
        int fila = tablaHistorial.getSelectedRow();
        if (fila < 0 || fila >= historial.size()) {
            return;
        }
        PlanDistribucion plan = historial.get(fila);
        mostrarPlan(plan);
        btnConfirmar.setEnabled(false);
        btnCancelarPlan.setEnabled(!plan.estaCancelado());
    }

    // ------------------------------------------------------------------ presentación

    /** Plan que se está viendo (simulado o guardado); lo usan los reportes. */
    public PlanDistribucion getPlanMostrado() {
        return planMostrado;
    }

    private void mostrarPlan(PlanDistribucion plan) {
        planMostrado = plan;
        LocalDateTime ahora = LocalDateTime.now();
        modeloViajes.setRowCount(0);
        List<String> numeracion = FormatoPlan.numeracionDeViajes(plan);
        for (int i = 0; i < plan.viajes().size(); i++) {
            Viaje v = plan.viajes().get(i);
            modeloViajes.addRow(new Object[]{v.id() == 0 ? "—" : String.valueOf(v.id()), v.centro().getCiudad(),
                    v.vehiculoId(), numeracion.get(i), FormatoPlan.numero(v.cargaToneladas(), 1),
                    FormatoPlan.numero(v.ocupacion() * 100, 0) + " %", FormatoPlan.numero(v.distanciaKm(), 0),
                    FormatoPlan.fechaHora(v.salida()), FormatoPlan.fechaHora(v.regreso()),
                    v.estadoEn(ahora).etiqueta(), FormatoPlan.ruta(v)});
        }
        modeloPendientes.setRowCount(0);
        for (Pendiente p : plan.pendientes()) {
            modeloPendientes.addRow(new Object[]{p.comunidadNombre(), p.centroNombre(),
                    FormatoPlan.numero(p.toneladas(), 1), p.motivo()});
        }
        areaBitacora.setText(String.join("\n", plan.bitacora()) + "\n\n" + textoEstadisticas(plan));
        areaBitacora.setCaretPosition(0);
        areaItinerario.setText(plan.estaVacio() ? "Sin viajes." : "Seleccione un viaje de la tabla para ver su itinerario.");

        String estado = plan.id() == 0 ? "SIMULACIÓN (sin guardar)" : "Plan " + plan.id();
        resumen.setText(String.format("%s · %d viajes · %s t · %s km · pendiente %s t · termina %s",
                estado, plan.viajes().size(), FormatoPlan.numero(plan.totalToneladas(), 1),
                FormatoPlan.numero(plan.totalKm(), 0), FormatoPlan.numero(plan.toneladasPendientes(), 1),
                FormatoPlan.fechaHora(plan.finEstimado())));
        actualizarMapa(null);
        if (!plan.viajes().isEmpty()) {
            tablaViajes.setRowSelectionInterval(0, 0);
        }
    }

    private static String textoEstadisticas(PlanDistribucion plan) {
        PlanDistribucion.EstadisticasAlgoritmos e = plan.estadisticasAlgoritmos();
        return "Backtracking: " + e.nodosBacktracking() + " nodos, " + e.podasBacktracking() + " podas\n"
                + "Branch and Bound: " + e.nodosBranchAndBound() + " nodos, " + e.podasBranchAndBound() + " podas";
    }

    private void mostrarViajeSeleccionado() {
        int fila = tablaViajes.getSelectedRow();
        if (planMostrado == null || fila < 0 || fila >= planMostrado.viajes().size()) {
            return;
        }
        Viaje v = planMostrado.viajes().get(fila);
        areaItinerario.setText(FormatoPlan.itinerario(v));
        areaItinerario.setCaretPosition(0);
        actualizarMapa(v);
    }

    private void actualizarMapa(Viaje resaltado) {
        try {
            List<Viaje> viajes = planMostrado == null ? List.of() : planMostrado.viajes();
            mapa.setDatos(gestorCentros.listar(), List.of(gestorComunidades.listarTodas()), viajes);
            mapa.setResaltado(resaltado);
        } catch (PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }
}
