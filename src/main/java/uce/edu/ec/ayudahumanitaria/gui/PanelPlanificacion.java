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

    private final JSpinner selectorAuto = crearSelectorSalida();
    private final JSpinner selectorManual = crearSelectorSalida();
    private final JLabel resumen = EstiloUI.textoSecundario("Calcule un plan en la pestaña Automático o Manual.");
    private final JButton btnCalcularAuto = EstiloUI.botonPrimario("Calcular plan automático");
    private final JButton btnRapida = EstiloUI.botonAccion("Simulación rápida (salida en 30 min)");
    private final JButton btnCalcularManual = EstiloUI.botonPrimario("Calcular plan con mis vehículos");
    private final JButton btnRecomendar = EstiloUI.botonAccion("Sugerir con algoritmos (Greedy)");
    private final JButton btnTodos = EstiloUI.botonNeutro("Marcar todos los libres");
    private final JButton btnNinguno = EstiloUI.botonNeutro("Quitar todos");
    private final JButton btnConfirmar = EstiloUI.botonAccion("Confirmar plan");
    private final JButton btnCancelarPlan = EstiloUI.botonPeligro("Cancelar plan seleccionado");
    private final JButton btnActualizar = EstiloUI.botonNeutro("Actualizar estados");
    private final JComboBox<Object> comboCentrosAuto = crearComboCentros();
    private final JComboBox<Object> comboCentrosManual = crearComboCentros();
    private final JLabel enViajeAuto = EstiloUI.textoSecundario(" ");
    private final JLabel enViajeManual = EstiloUI.textoSecundario(" ");
    /** Vehículos con un viaje confirmado vigente → hora de regreso. Bloqueados para cualquier proceso. */
    private Map<String, LocalDateTime> enViaje = Map.of();

    private final DefaultTableModel modeloAuto = new DefaultTableModel(
            new Object[]{"Vehículo", "Centro", "Capacidad (t)", "Viajes", "Por qué lo eligieron los algoritmos"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tablaAuto = new JTable(modeloAuto);
    private JPanel seccionAutomatico;
    private JPanel seccionManual;
    private JPanel seccionResultados;

    /** Elección del usuario (id de vehículo → viajes). Sobrevive a los refrescos automáticos. */
    private final Map<String, Integer> eleccion = new LinkedHashMap<>();
    private List<ResumenCentro> resumenes = List.of();
    private boolean cargandoFlota;
    /** Motivo de cada vehículo sugerido por los algoritmos (vacío para los elegidos a mano). */
    private Map<String, String> motivos = Map.of();
    private final DefaultTableModel modeloFlota = new DefaultTableModel(
            new Object[]{"Enviar", "Vehículo", "Centro", "Capacidad (t)", "Viajes", "Estado / por qué"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            // Un vehículo en viaje confirmado no se puede elegir (ni cambiar sus viajes).
            return (columna == 0 || columna == 4) && !enViaje.containsKey(String.valueOf(getValueAt(fila, 1)));
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

        // Este panel no se muestra por sí solo: PanelLogistica ubica sus tres secciones.
        seccionAutomatico = construirAutomatico();
        seccionManual = construirManual();
        seccionResultados = construirResultados();

        btnCalcularAuto.addActionListener(e -> calcularAutomatico());
        btnCalcularManual.addActionListener(e -> calcularManual());
        btnConfirmar.addActionListener(e -> confirmar());
        btnCancelarPlan.addActionListener(e -> cancelarPlan());
        btnActualizar.addActionListener(e -> refrescar());
        btnRecomendar.addActionListener(e -> sugerirParaManual());
        btnRapida.addActionListener(e -> simulacionRapida());
        btnTodos.addActionListener(e -> marcarTodos(true));
        btnNinguno.addActionListener(e -> marcarTodos(false));
        modeloFlota.addTableModelListener(e -> {
            if (!cargandoFlota) {
                leerEleccionDeLaTabla();
                pintarSituacion();
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

    public JPanel seccionAutomatico() {
        return seccionAutomatico;
    }

    public JPanel seccionManual() {
        return seccionManual;
    }

    public JPanel seccionResultados() {
        return seccionResultados;
    }

    private static JSpinner crearSelectorSalida() {
        Date inicial = Date.from(LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
                .atZone(ZoneId.systemDefault()).toInstant());
        JSpinner selector = new JSpinner(new SpinnerDateModel(inicial, null, null, java.util.Calendar.HOUR_OF_DAY));
        selector.setEditor(new JSpinner.DateEditor(selector, "dd/MM/yyyy HH:mm"));
        selector.setFont(EstiloUI.FUENTE_BASE);
        return selector;
    }

    private static JComboBox<Object> crearComboCentros() {
        JComboBox<Object> combo = new JComboBox<>();
        combo.setPreferredSize(new java.awt.Dimension(360, combo.getPreferredSize().height));
        combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> lista, Object valor,
                                                                   int indice, boolean sel, boolean foco) {
                String texto = valor instanceof ResumenCentro r ? r.centro().getNombre() + " (solo este centro)"
                        : String.valueOf(valor);
                return super.getListCellRendererComponent(lista, texto, indice, sel, foco);
            }
        });
        return combo;
    }

    /** Cuadro de texto con los pasos numerados, para que el usuario sepa qué hacer en cada pestaña. */
    private static JTextArea pasos(String texto) {
        JTextArea area = new JTextArea(texto);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(EstiloUI.FUENTE_BASE);
        area.setForeground(EstiloUI.TEXTO_SECUNDARIO);
        area.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        return area;
    }

    private JPanel construirAutomatico() {
        JPanel tarjeta = EstiloUI.tarjeta("Modo automático: los algoritmos deciden vehículos, centros, cargas y rutas");
        JTextArea guia = pasos("""
                PASO A PASO
                1. Revise arriba la situación (qué necesita cada comunidad y cuánto stock hay) y, si quiere, pruebe cada algoritmo en las tarjetas.
                2. Elija qué centros participan. «Todos» = cada comunidad la atiende su centro más cercano (Greedy por distancia).
                3. Elija la fecha y hora de salida (no puede estar en el pasado).
                4. Pulse «Calcular plan automático». El sistema ejecuta, en este orden:
                   · Greedy: reparte el stock de cada centro a sus comunidades por prioridad.
                   · Mochila 0/1: arma cargas de comunidades que caben en un vehículo con el mayor beneficio.
                   · Branch and Bound: asigna cada carga al vehículo libre que menos capacidad desperdicia.
                   · Backtracking: ordena las paradas para llegar antes a las comunidades de mayor prioridad.
                5. Abajo verá qué vehículos usó y por qué. Si un vehículo hace 2 o más viajes, regresa al centro, se prepara y sale otra vez.
                6. Revise los viajes, el itinerario y el mapa en «Resultado» (al final de esta página) y pulse «Confirmar plan». Desde ese momento los vehículos quedan EN VIAJE y no se pueden usar en el modo manual ni en otro plan hasta que regresen.""");

        JPanel controles = EstiloUI.fila();
        controles.add(EstiloUI.etiqueta("Centros que participan:"));
        controles.add(comboCentrosAuto);
        controles.add(EstiloUI.etiqueta("Salida:"));
        controles.add(selectorAuto);
        controles.add(btnCalcularAuto);
        controles.add(btnRapida);

        EstiloUI.estilizarTabla(tablaAuto);
        Tablas.anchos(tablaAuto, 90, 120, 100, 70, 640);
        JScrollPane scroll = new JScrollPane(tablaAuto);
        scroll.setPreferredSize(new java.awt.Dimension(900, 130));

        JPanel norte = new JPanel(new BorderLayout(0, 4));
        norte.setOpaque(false);
        norte.add(guia, BorderLayout.NORTH);
        norte.add(controles, BorderLayout.CENTER);
        norte.add(enViajeAuto, BorderLayout.SOUTH);
        JPanel contenido = new JPanel(new BorderLayout(0, 8));
        contenido.setOpaque(false);
        contenido.add(norte, BorderLayout.NORTH);
        contenido.add(scroll, BorderLayout.CENTER);
        tarjeta.add(contenido, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel construirManual() {
        JPanel tarjeta = EstiloUI.tarjeta("Modo manual: usted elige qué vehículos salen y cuántos viajes hacen");
        JTextArea guia = pasos("""
                PASO A PASO
                1. En la tabla, marque «Enviar» en los vehículos que quiere mandar. Los vehículos EN VIAJE aparecen bloqueados: ya fueron asignados en un plan confirmado.
                2. Elija cuántos viajes hará cada uno (1 a 5). Con 2 o más, el vehículo regresa al centro y sale de nuevo.
                3. (Opcional) Pulse «Sugerir con algoritmos» para que Greedy marque los vehículos más grandes que cubren la carga; luego puede cambiarlos.
                4. Compare en la segunda tabla lo que necesita cada centro con la capacidad que eligió: avisa si falta o si no hay vehículos.
                5. Elija la fecha y hora de salida y pulse «Calcular plan con mis vehículos». Solo se usan los que marcó:
                   · Greedy reparte el stock por prioridad, Mochila 0/1 arma las cargas,
                   · Branch and Bound asigna cada carga a uno de SUS vehículos y Backtracking ordena las paradas por prioridad.
                6. Revise los viajes, el itinerario y el mapa en «Resultado» (al final) y pulse «Confirmar plan». Los vehículos pasan a EN VIAJE y no podrán usarse en el modo automático hasta que regresen.""");

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
            casilla.setEnabled(modeloFlota.isCellEditable(fila, 0));
            casilla.setBackground(sel ? t.getSelectionBackground() : (fila % 2 == 0 ? java.awt.Color.WHITE
                    : EstiloUI.FILA_ALTERNA));
            return casilla;
        });
        tablaFlota.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new javax.swing.JCheckBox()));
        tablaFlota.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(viajes));

        JPanel tablas = new JPanel(new java.awt.GridLayout(2, 1, 0, 8));
        tablas.setOpaque(false);
        tablas.add(new JScrollPane(tablaFlota));
        tablas.add(new JScrollPane(tablaSituacion));
        tablas.setPreferredSize(new java.awt.Dimension(900, 400));

        JPanel acciones = EstiloUI.fila();
        acciones.add(btnTodos);
        acciones.add(btnNinguno);
        acciones.add(EstiloUI.etiqueta("  Sugerir solo para:"));
        acciones.add(comboCentrosManual);
        acciones.add(btnRecomendar);
        JPanel calculo = EstiloUI.fila();
        calculo.add(EstiloUI.etiqueta("Salida:"));
        calculo.add(selectorManual);
        calculo.add(btnCalcularManual);

        JPanel norte = new JPanel(new BorderLayout(0, 4));
        norte.setOpaque(false);
        norte.add(guia, BorderLayout.NORTH);
        norte.add(acciones, BorderLayout.CENTER);
        norte.add(enViajeManual, BorderLayout.SOUTH);
        JPanel contenido = new JPanel(new BorderLayout(0, 8));
        contenido.setOpaque(false);
        contenido.add(norte, BorderLayout.NORTH);
        contenido.add(tablas, BorderLayout.CENTER);
        contenido.add(calculo, BorderLayout.SOUTH);
        tarjeta.add(contenido, BorderLayout.CENTER);
        return tarjeta;
    }

    /** Resultado común de ambos modos: confirmar, viajes, itinerario y mapa. */
    private JPanel construirResultados() {
        JPanel tarjeta = EstiloUI.tarjeta("Resultado del plan (aquí se revisa y se confirma)");
        JPanel fila = EstiloUI.fila();
        fila.add(btnConfirmar);
        fila.add(btnCancelarPlan);
        fila.add(btnActualizar);
        JPanel contenido = new JPanel(new BorderLayout(0, 4));
        contenido.setOpaque(false);
        contenido.add(fila, BorderLayout.NORTH);
        contenido.add(resumen, BorderLayout.SOUTH);
        tarjeta.add(contenido, BorderLayout.CENTER);

        JPanel todo = new JPanel(new BorderLayout(0, 12));
        todo.setOpaque(false);
        todo.add(tarjeta, BorderLayout.NORTH);
        todo.add(construirCentro(), BorderLayout.CENTER);
        return todo;
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

    private static LocalDateTime salidaDe(JSpinner selector) {
        return LocalDateTime.ofInstant(((Date) selector.getValue()).toInstant(), ZoneId.systemDefault());
    }

    private void mostrarSimulacion(PlanDistribucion plan) {
        mostrarPlan(plan);
        tablaHistorial.clearSelection();
        btnConfirmar.setEnabled(!plan.estaVacio());
        btnCancelarPlan.setEnabled(false);
        if (plan.estaVacio()) {
            JOptionPane.showMessageDialog(this, "No hay nada que distribuir con los datos actuales "
                    + "(revise stock de los centros, vehículos libres y necesidades pendientes).");
        }
    }

    private void avisarValidacion(ValidationException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
    }

    private void avisarBD(PersistenciaException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
    }

    /** Modo automático: los algoritmos eligen vehículos y centros. */
    private void calcularAutomatico() {
        try {
            GestorPlanes.ResultadoRapido r = gestorPlanes.simularAutomatico(salidaDe(selectorAuto),
                    centroElegido(comboCentrosAuto));
            llenarTablaAuto(r.recomendacion());
            mostrarSimulacion(r.plan());
            anteponerABitacora("MODO AUTOMÁTICO", r.recomendacion().explicacion());
            resumen.setText(resumen.getText() + "  → revise y pulse «Confirmar plan».");
        } catch (ValidationException ex) {
            avisarValidacion(ex);
        } catch (PersistenciaException ex) {
            avisarBD(ex);
        }
    }

    /** Modo manual: solo se usan los vehículos y viajes que marcó el usuario. */
    private void calcularManual() {
        try {
            leerEleccionDeLaTabla();
            PlanDistribucion plan = gestorPlanes.simular(salidaDe(selectorManual), new SeleccionEnvio(eleccion));
            mostrarSimulacion(plan);
            anteponerABitacora("MODO MANUAL", List.of(eleccion.size() + " vehículo(s) elegido(s), "
                    + new SeleccionEnvio(eleccion).totalViajes() + " viaje(s) como máximo."));
            resumen.setText(resumen.getText() + "  → revise y pulse «Confirmar plan».");
            llenarFlota();
        } catch (ValidationException ex) {
            avisarValidacion(ex);
        } catch (PersistenciaException ex) {
            avisarBD(ex);
        }
    }

    /** Sugerencia Greedy para el modo manual: solo marca la tabla; el usuario decide. */
    private void sugerirParaManual() {
        try {
            RecomendadorEnvio.Recomendacion r = gestorPlanes.recomendar(centroElegido(comboCentrosManual));
            aplicarEleccion(r.seleccion(), r.motivos());
            JOptionPane.showMessageDialog(this, String.join("\n\n", r.explicacion())
                            + "\n\nEs solo una sugerencia: puede cambiar vehículos y viajes antes de calcular.",
                    "Sugerencia (Greedy)", JOptionPane.INFORMATION_MESSAGE);
        } catch (PersistenciaException ex) {
            avisarBD(ex);
        }
    }

    /** Automático con salida en 30 minutos y todos los centros; no guarda nada hasta confirmar. */
    private void simulacionRapida() {
        try {
            GestorPlanes.ResultadoRapido r = gestorPlanes.simulacionRapida();
            selectorAuto.setValue(Date.from(r.plan().salidaProgramada().atZone(ZoneId.systemDefault()).toInstant()));
            llenarTablaAuto(r.recomendacion());
            mostrarSimulacion(r.plan());
            anteponerABitacora("SIMULACIÓN RÁPIDA (salida en 30 min)", r.recomendacion().explicacion());
        } catch (ValidationException ex) {
            avisarValidacion(ex);
        } catch (PersistenciaException ex) {
            avisarBD(ex);
        }
    }

    private void anteponerABitacora(String titulo, List<String> lineas) {
        areaBitacora.setText(titulo + "\n" + String.join("\n", lineas) + "\n\n" + areaBitacora.getText());
        areaBitacora.setCaretPosition(0);
    }

    private void llenarTablaAuto(RecomendadorEnvio.Recomendacion r) {
        modeloAuto.setRowCount(0);
        for (ResumenCentro rc : resumenes) {
            for (Vehiculo v : rc.flota()) {
                int viajes = r.seleccion().viajesDe(v.getId());
                if (viajes > 0) {
                    modeloAuto.addRow(new Object[]{v.getId(), rc.centro().getCiudad(),
                            FormatoPlan.numero(v.getCapacidadToneladas(), 1), viajes,
                            r.motivos().getOrDefault(v.getId(), "")});
                }
            }
        }
    }

    // ------------------------------------------------------------------ elección de vehículos

    /** Marca en la tabla manual la elección recibida (p. ej. desde la recomendación de las tarjetas de algoritmos). */
    public void aplicarSeleccion(SeleccionEnvio seleccion) {
        aplicarEleccion(seleccion, Map.of());
    }

    /** Id del centro elegido en el combo, o {@code null} si participan todos. */
    private static Integer centroElegido(JComboBox<Object> combo) {
        return combo.getSelectedItem() instanceof ResumenCentro r ? r.centro().getId() : null;
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

    private void actualizarCombosCentros() {
        for (JComboBox<Object> combo : List.of(comboCentrosAuto, comboCentrosManual)) {
            Integer anterior = centroElegido(combo);
            combo.removeAllItems();
            combo.addItem("Todos (cada comunidad va al centro más cercano)");
            for (ResumenCentro r : resumenes) {
                combo.addItem(r);
            }
            if (anterior != null) {
                resumenes.stream().filter(r -> r.centro().getId() == anterior).findFirst()
                        .ifPresent(combo::setSelectedItem);
            }
        }
    }

    private String textoEnViaje() {
        if (enViaje.isEmpty()) {
            return "Todos los vehículos están libres.";
        }
        StringBuilder sb = new StringBuilder("En viaje (bloqueados hasta regresar): ");
        enViaje.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> sb.append(e.getKey())
                .append(" (regresa ").append(FormatoPlan.fechaHora(e.getValue())).append(")  "));
        return sb.toString();
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
                        viajes > 0 ? motivos.getOrDefault(v.getId(), "Elegido por usted") : "Libre"});
            }
            for (Vehiculo v : r.ocupados()) {
                modeloFlota.addRow(new Object[]{false, v.getId(), r.centro().getCiudad(),
                        FormatoPlan.numero(v.getCapacidadToneladas(), 1), 1,
                        "EN VIAJE hasta " + FormatoPlan.fechaHora(enViaje.get(v.getId())) + " (bloqueado)"});
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
            enViaje = gestorPlanes.vehiculosEnViaje();
            resumenes = gestorPlanes.resumenPorCentro();
            eleccion.keySet().removeIf(enViaje::containsKey);
            actualizarCombosCentros();
            enViajeAuto.setText(textoEnViaje());
            enViajeManual.setText(textoEnViaje());
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
