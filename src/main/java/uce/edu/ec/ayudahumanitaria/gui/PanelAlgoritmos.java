package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoAsignacion;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoMochila;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoRuta;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoRutaPonderada;
import uce.edu.ec.ayudahumanitaria.dominio.CentroAcopio;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.GestorCentros;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorPlanes;
import uce.edu.ec.ayudahumanitaria.dominio.GestorVehiculos;
import uce.edu.ec.ayudahumanitaria.dominio.PlanificadorDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.RecomendadorEnvio;
import uce.edu.ec.ayudahumanitaria.dominio.RecomendadorEnvio.ResumenCentro;
import uce.edu.ec.ayudahumanitaria.dominio.Vehiculo;
import uce.edu.ec.ayudahumanitaria.geo.EstudioVial;
import uce.edu.ec.ayudahumanitaria.geo.GeoEcuador;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Demostración de los algoritmos del curso sobre los datos reales, en una columna vertical de tarjetas.
 * Cada tarjeta explica qué hace el algoritmo y limita sus entradas a valores con sentido (según lo que
 * realmente hay: stock, necesidades y vehículos). Es de solo lectura: no modifica la base de datos.
 */
public class PanelAlgoritmos extends JPanel implements Refrescable {

    private static final String TODOS = "Todos los centros";
    private static final int MAX_NODOS_EXACTOS = 8;
    private static final double TOLERANCIA_TON = 0.05;

    private final GestorComunidades gestorComunidades;
    private final GestorVehiculos gestorVehiculos;
    private final GestorCentros gestorCentros;
    private final GestorPlanes gestorPlanes;

    private final JComboBox<Object> comboAlcance = new JComboBox<>();
    private boolean actualizandoCombo;
    private boolean limitesIniciados;
    private java.util.function.Consumer<uce.edu.ec.ayudahumanitaria.dominio.SeleccionEnvio> alAplicar;

    /** Permite que la recomendación se aplique directamente a la elección de vehículos de Planificación. */
    public void setAlAplicarRecomendacion(
            java.util.function.Consumer<uce.edu.ec.ayudahumanitaria.dominio.SeleccionEnvio> accion) {
        this.alAplicar = accion;
    }

    private final JSpinner spinnerSuministros = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 1.0, 0.5));
    private final JSpinner spinnerCapacidad = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
    private final JSpinner spinnerParadas = new JSpinner(new SpinnerNumberModel(4, 1, MAX_NODOS_EXACTOS - 1, 1));
    private final DefaultListModel<Vehiculo> modeloVehiculos = new DefaultListModel<>();
    private final JList<Vehiculo> listaVehiculos = new JList<>(modeloVehiculos);

    private final JLabel resumenSituacion = EstiloUI.textoSecundario(" ");
    private final DefaultTableModel modeloSituacion = new DefaultTableModel(
            new Object[]{"Comunidad", "Centro", "Habitantes", "Prioridad", "Necesita (t)", "Beneficio", "Benef./hab."}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTextArea areaResultados = new JTextArea();

    public PanelAlgoritmos(GestorComunidades gestorComunidades, GestorVehiculos gestorVehiculos,
                           GestorCentros gestorCentros, GestorPlanes gestorPlanes) {
        this.gestorComunidades = gestorComunidades;
        this.gestorVehiculos = gestorVehiculos;
        this.gestorCentros = gestorCentros;
        this.gestorPlanes = gestorPlanes;

        setLayout(new BorderLayout(12, 12));
        setBackground(EstiloUI.FONDO_APP);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        add(construirAlcance(), BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setOpaque(false);
        JPanel filaDatos = construirColumnaResultados();
        filaDatos.setPreferredSize(new Dimension(900, 330));
        JPanel cuadricula = construirCuadriculaAlgoritmos();
        cuerpo.add(filaDatos, BorderLayout.NORTH);
        cuerpo.add(cuadricula, BorderLayout.CENTER);
        add(cuerpo, BorderLayout.CENTER);

        comboAlcance.addActionListener(e -> {
            if (!actualizandoCombo) {
                actualizarSituacion();
            }
        });
        refrescar();
        mostrar("Elija el alcance y pulse el botón de cualquier algoritmo.\n\n"
                + "Arriba a la derecha ve qué necesita cada comunidad y cuánto hay disponible para enviar;"
                + " aquí aparece el detalle de cada cálculo.");
    }

    // ------------------------------------------------------------------ construcción de la pantalla

    private JPanel construirAlcance() {
        JPanel tarjeta = EstiloUI.tarjeta("Alcance de la demostración");
        JPanel fila = EstiloUI.fila();
        fila.add(EstiloUI.etiqueta("Centro de acopio:"));
        fila.add(comboAlcance);
        fila.add(EstiloUI.textoSecundario("Cada comunidad se asocia al centro más cercano; los límites de los campos "
                + "dependen del stock, las necesidades y los vehículos del alcance elegido."));
        tarjeta.add(fila, BorderLayout.CENTER);
        return tarjeta;
    }

    /** Cuadrícula de 3 columnas × 2 filas con una tarjeta por algoritmo (sin barras internas). */
    private JPanel construirCuadriculaAlgoritmos() {
        JPanel columna = new JPanel(new java.awt.GridLayout(2, 3, 10, 0));
        columna.setOpaque(false);
        columna.setPreferredSize(new Dimension(900, 520));

        listaVehiculos.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listaVehiculos.setVisibleRowCount(4);
        listaVehiculos.setFont(EstiloUI.FUENTE_BASE);
        listaVehiculos.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                                   boolean seleccionado, boolean foco) {
                String texto = valor instanceof Vehiculo v
                        ? String.format(Locale.US, "%s · %.1f t · %s", v.getId(), v.getCapacidadToneladas(),
                        v.getCentroNombre()) : String.valueOf(valor);
                return super.getListCellRendererComponent(lista, texto, indice, seleccionado, foco);
            }
        });

        columna.add(tarjetaAlgoritmo("1. Comunidad más necesitada", "Divide y Vencerás",
                "Parte la lista de comunidades por la mitad, busca la de mayor beneficio en cada mitad y compara "
                        + "los dos ganadores. Beneficio = habitantes × prioridad. Sirve para saber a quién atender "
                        + "primero cuando solo se puede salir una vez.",
                null, "Calcular", this::ejecutarComunidadMayorBeneficio));

        columna.add(tarjetaAlgoritmo("2. Reparto de suministros", "Greedy (voraz)",
                "Entrega el stock a las comunidades en orden de prioridad: cada una recibe lo que necesita mientras "
                        + "alcance, y la última puede recibir una parte. El tope del campo es el stock real del alcance.",
                filaConEtiqueta("Toneladas a repartir:", spinnerSuministros), "Repartir", this::ejecutarDistribucionSuministros));

        columna.add(tarjetaAlgoritmo("3. Qué comunidades caben en un vehículo", "Programación dinámica (Mochila 0/1)",
                "Elige el conjunto de comunidades que cabe en la capacidad indicada y da el mayor beneficio total, "
                        + "sin partir ninguna carga. Los pesos se redondean hacia arriba a toneladas enteras. "
                        + "El tope es la capacidad del vehículo más grande del alcance.",
                filaConEtiqueta("Capacidad (t):", spinnerCapacidad), "Seleccionar", this::ejecutarSeleccionOptima));

        columna.add(tarjetaAlgoritmo("4. Orden de visita de un viaje", "Backtracking",
                "Prueba los órdenes de visita y descarta (poda) los que ya son peores. Compara dos criterios: "
                        + "(a) menor distancia total y (b) llegar antes a las comunidades de mayor prioridad, usando "
                        + "tiempos según el estado de las vías. Máximo " + (MAX_NODOS_EXACTOS - 1)
                        + " paradas porque el cálculo crece de forma factorial.",
                filaConEtiqueta("Paradas a considerar:", spinnerParadas), "Calcular rutas", this::ejecutarRutaOptima));

        JScrollPane scrollVehiculos = new JScrollPane(listaVehiculos);
        scrollVehiculos.setPreferredSize(new Dimension(260, 78));
        columna.add(tarjetaAlgoritmo("5. Qué vehículo va a qué carga", "Branch and Bound",
                "Asigna los vehículos que usted marque a las comunidades de mayor prioridad, maximizando cuántas "
                        + "se cubren y minimizando la capacidad que se desperdicia. Marque los vehículos que "
                        + "realmente quiere enviar (máx. " + MAX_NODOS_EXACTOS + ").",
                scrollVehiculos, "Asignar vehículos", this::ejecutarAsignacionVehiculos));

        columna.add(tarjetaAlgoritmo("6. Recomendación de envío", "Greedy + datos reales",
                "Resume por centro lo que se puede enviar (necesidad limitada por el stock) y sugiere cuántos "
                        + "vehículos y viajes hacen falta, empezando por los de mayor capacidad. Es una sugerencia: "
                        + "en las pestañas Automático / Manual de más abajo usted decide.",
                null, "Recomendar envío", this::ejecutarRecomendacion));

        return columna;
    }

    private JPanel construirColumnaResultados() {
        JTable tabla = new JTable(modeloSituacion);
        EstiloUI.estilizarTabla(tabla);
        tabla.setAutoCreateRowSorter(true);
        Tablas.anchos(tabla, 150, 90, 75, 65, 80, 75, 75);
        JPanel tarjetaSituacion = EstiloUI.tarjeta("Situación: qué necesita cada comunidad y qué hay disponible");
        JPanel contenido = new JPanel(new BorderLayout(0, 6));
        contenido.setOpaque(false);
        contenido.add(resumenSituacion, BorderLayout.NORTH);
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);
        tarjetaSituacion.add(contenido, BorderLayout.CENTER);

        EstiloUI.estilizarAreaTexto(areaResultados);
        areaResultados.setEditable(false);
        areaResultados.setFont(EstiloUI.FUENTE_MONO);
        areaResultados.setLineWrap(true);
        areaResultados.setWrapStyleWord(true);
        JPanel tarjetaResultados = EstiloUI.tarjeta("Resultado del algoritmo");
        tarjetaResultados.add(new JScrollPane(areaResultados), BorderLayout.CENTER);

        JPanel fila = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        fila.setOpaque(false);
        fila.add(tarjetaSituacion);
        fila.add(tarjetaResultados);
        return fila;
    }

    /** Tarjeta vertical: título + etiqueta del algoritmo, explicación, controles opcionales y botón. */
    private JPanel tarjetaAlgoritmo(String titulo, String algoritmo, String explicacion, JComponent controles,
                                    String textoBoton, Runnable accion) {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 6));
        tarjeta.setBackground(EstiloUI.FONDO_TARJETA);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, EstiloUI.AZUL_PRIMARIO),
                        BorderFactory.createLineBorder(EstiloUI.BORDE_SUAVE, 1)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(EstiloUI.FUENTE_SUBTITULO);
        lblTitulo.setForeground(EstiloUI.TEXTO_PRINCIPAL);
        JLabel lblAlgoritmo = new JLabel(algoritmo, SwingConstants.RIGHT);
        lblAlgoritmo.setFont(EstiloUI.FUENTE_ETIQUETA);
        lblAlgoritmo.setForeground(EstiloUI.AZUL_PRIMARIO);
        lblAlgoritmo.setHorizontalAlignment(SwingConstants.LEFT);
        JPanel cabecera = new JPanel(new BorderLayout(0, 2));
        cabecera.setOpaque(false);
        cabecera.add(lblTitulo, BorderLayout.NORTH);
        cabecera.add(lblAlgoritmo, BorderLayout.SOUTH);

        JTextArea texto = new JTextArea(explicacion);
        texto.setEditable(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setOpaque(false);
        texto.setFont(EstiloUI.FUENTE_BASE);
        texto.setForeground(EstiloUI.TEXTO_SECUNDARIO);
        texto.setBorder(null);

        JButton boton = EstiloUI.botonPrimario(textoBoton);
        boton.addActionListener(e -> accion.run());
        JPanel pie = new JPanel(new BorderLayout(0, 6));
        pie.setOpaque(false);
        if (controles != null) {
            pie.add(controles, BorderLayout.CENTER);
        }
        JPanel botonera = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        botonera.setOpaque(false);
        botonera.add(boton);
        pie.add(botonera, BorderLayout.SOUTH);

        tarjeta.add(cabecera, BorderLayout.NORTH);
        tarjeta.add(texto, BorderLayout.CENTER);
        tarjeta.add(pie, BorderLayout.SOUTH);

        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setOpaque(false);
        envoltorio.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        envoltorio.add(tarjeta, BorderLayout.CENTER);
        envoltorio.setAlignmentX(LEFT_ALIGNMENT);
        return envoltorio;
    }

    private static JPanel filaConEtiqueta(String texto, JComponent campo) {
        JPanel fila = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        fila.setOpaque(false);
        fila.add(EstiloUI.etiqueta(texto));
        campo.setPreferredSize(new Dimension(90, campo.getPreferredSize().height + 4));
        fila.add(campo);
        return fila;
    }

    // ------------------------------------------------------------------ datos del alcance

    /** Comunidades del centro elegido (o todas) con su necesidad completa. */
    private Comunidad[] comunidadesDelAlcance() throws PersistenciaException {
        Comunidad[] todas = gestorComunidades.listarTodas();
        if (!(comboAlcance.getSelectedItem() instanceof CentroAcopio centro)) {
            return todas;
        }
        List<CentroAcopio> centros = gestorCentros.listar();
        List<Comunidad> propias = new ArrayList<>();
        for (Comunidad c : todas) {
            if (c.tieneUbicacion() && PlanificadorDistribucion.centroMasCercano(centros, c).getId() == centro.getId()) {
                propias.add(c);
            }
        }
        return propias.toArray(new Comunidad[0]);
    }

    private List<Vehiculo> vehiculosDelAlcance() throws PersistenciaException {
        List<Vehiculo> lista = new ArrayList<>();
        for (Vehiculo v : gestorVehiculos.listarTodos()) {
            boolean propio = !(comboAlcance.getSelectedItem() instanceof CentroAcopio centro)
                    || v.getCentroId() == centro.getId();
            if (propio && v.getCentroId() != 0) {
                lista.add(v);
            }
        }
        return lista;
    }

    private List<CentroAcopio> centrosDelAlcance() throws PersistenciaException {
        if (comboAlcance.getSelectedItem() instanceof CentroAcopio centro) {
            return List.of(centro);
        }
        return gestorCentros.listar();
    }

    private double stockDelAlcance() throws PersistenciaException {
        return centrosDelAlcance().stream().mapToDouble(CentroAcopio::getStockToneladas).sum();
    }

    private CentroAcopio centroDelAlcance(Comunidad referencia) throws PersistenciaException {
        if (comboAlcance.getSelectedItem() instanceof CentroAcopio centro) {
            return centro;
        }
        return PlanificadorDistribucion.centroMasCercano(gestorCentros.listar(), referencia);
    }

    @Override
    public void refrescar() {
        Object anterior = comboAlcance.getSelectedItem();
        actualizandoCombo = true;
        try {
            comboAlcance.removeAllItems();
            comboAlcance.addItem(TODOS);
            List<CentroAcopio> centros = gestorCentros.listar();
            centros.forEach(comboAlcance::addItem);
            if (anterior instanceof CentroAcopio previo) {
                centros.stream().filter(c -> c.getId() == previo.getId()).findFirst()
                        .ifPresent(comboAlcance::setSelectedItem);
            }
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        } finally {
            actualizandoCombo = false;
        }
        actualizarSituacion();
    }

    /** Rellena la tabla de situación y ajusta los límites de los campos a lo que realmente existe. */
    private void actualizarSituacion() {
        try {
            Comunidad[] comunidades = comunidadesDelAlcance();
            List<CentroAcopio> todosLosCentros = gestorCentros.listar();
            Map<Integer, Double> planificado = gestorPlanes.planificadoPorComunidad();
            modeloSituacion.setRowCount(0);
            double necesidad = 0;
            for (Comunidad c : comunidades) {
                double faltante = Math.max(0, c.getRecursosRequeridos() - planificado.getOrDefault(c.getId(), 0.0));
                necesidad += faltante;
                String centro = c.tieneUbicacion() ? PlanificadorDistribucion.centroMasCercano(todosLosCentros, c)
                        .getCiudad() : "—";
                modeloSituacion.addRow(new Object[]{c.getNombre(), centro, c.getHabitantes(), c.getPrioridad(),
                        String.format(Locale.US, "%.1f", faltante), String.format(Locale.US, "%.0f", c.calcularBeneficio()),
                        c.getPrioridad()});
            }
            double stock = stockDelAlcance();
            List<Vehiculo> flota = vehiculosDelAlcance();
            double capacidad = flota.stream().mapToDouble(Vehiculo::getCapacidadToneladas).sum();
            double mayor = flota.stream().mapToDouble(Vehiculo::getCapacidadToneladas).max().orElse(0);
            resumenSituacion.setText(String.format(Locale.US,
                    "Necesidad pendiente: %.1f t · Stock disponible: %.1f t · Flota: %d vehículo(s), %.1f t por viaje%s",
                    necesidad, stock, flota.size(), capacidad,
                    necesidad > stock + TOLERANCIA_TON ? " · ¡el stock no cubre toda la necesidad!" : ""));
            ajustarLimites(stock, necesidad, mayor);
            recargarVehiculos(flota);
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    private void ajustarLimites(double stock, double necesidad, double capacidadMayor) {
        double tope = Math.max(0.1, Math.round(stock * 10) / 10.0);
        SpinnerNumberModel modeloSum = (SpinnerNumberModel) spinnerSuministros.getModel();
        modeloSum.setMaximum(tope);
        double sugerido = Math.min(tope, Math.max(0.1, Math.round(Math.min(stock, necesidad) * 10) / 10.0));
        double actual = ((Number) modeloSum.getValue()).doubleValue();
        modeloSum.setValue(!limitesIniciados || actual > tope || actual < 0.1 ? sugerido : actual);

        int topeCapacidad = Math.max(1, (int) Math.ceil(capacidadMayor));
        SpinnerNumberModel modeloCap = (SpinnerNumberModel) spinnerCapacidad.getModel();
        modeloCap.setMaximum(topeCapacidad);
        int capActual = ((Number) modeloCap.getValue()).intValue();
        modeloCap.setValue(!limitesIniciados || capActual > topeCapacidad || capActual < 1 ? topeCapacidad : capActual);
        limitesIniciados = true;
    }

    private void recargarVehiculos(List<Vehiculo> flota) {
        List<String> marcados = new ArrayList<>();
        for (Vehiculo v : listaVehiculos.getSelectedValuesList()) {
            marcados.add(v.getId());
        }
        modeloVehiculos.clear();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < flota.size(); i++) {
            Vehiculo v = flota.get(i);
            modeloVehiculos.addElement(v);
            if (marcados.isEmpty() || marcados.contains(v.getId())) {
                indices.add(i);
            }
        }
        listaVehiculos.setSelectedIndices(indices.stream().mapToInt(Integer::intValue).toArray());
    }

    // ------------------------------------------------------------------ algoritmos

    private void ejecutarComunidadMayorBeneficio() {
        try {
            Comunidad[] comunidades = comunidadesDelAlcance();
            if (comunidades.length == 0) {
                mostrar("No hay comunidades en el alcance elegido.");
                return;
            }
            Comunidad ganadora = Algoritmos.comunidadMayorBeneficio(comunidades);
            StringBuilder sb = new StringBuilder("=== Divide y Vencerás: comunidad más necesitada ===\n");
            sb.append("Comunidades comparadas: ").append(comunidades.length).append("\n\n");
            sb.append(String.format(Locale.US, "Ganadora: %s%n  %d habitantes × prioridad %d = beneficio %.0f%n"
                            + "  necesita %.1f t%n", ganadora.getNombre(), ganadora.getHabitantes(), ganadora.getPrioridad(),
                    ganadora.calcularBeneficio(), ganadora.getRecursosRequeridos()));
            sb.append("\nQué significa: si solo pudiera salir un vehículo, esta es la comunidad donde más personas "
                    + "(ponderadas por urgencia) se benefician.");
            mostrar(sb.toString());
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    private void ejecutarDistribucionSuministros() {
        try {
            double suministros = leerDecimal(spinnerSuministros);
            Comunidad[] comunidades = comunidadesDelAlcance();
            if (comunidades.length == 0) {
                mostrar("No hay comunidades en el alcance elegido.");
                return;
            }
            ResultadoDistribucion resultado = Algoritmos.distribuirSuministros(comunidades, suministros);
            Map<String, Comunidad> porNombre = new HashMap<>();
            for (Comunidad c : comunidades) {
                porNombre.put(c.getNombre(), c);
            }
            StringBuilder sb = new StringBuilder("=== Greedy: reparto de suministros ===\n");
            sb.append(String.format(Locale.US, "Disponible para repartir: %.1f t. Se atiende por prioridad descendente.%n%n",
                    suministros));
            double entregado = 0;
            for (int i = 0; i < resultado.nombres.length; i++) {
                Comunidad c = porNombre.get(resultado.nombres[i]);
                double necesita = c == null ? 0 : c.getRecursosRequeridos();
                int prioridad = c == null ? 0 : c.getPrioridad();
                entregado += resultado.asignado[i];
                sb.append(String.format(Locale.US, " P%-2d %-22s necesita %6.1f t → recibe %6.1f t%s%n", prioridad,
                        resultado.nombres[i], necesita, resultado.asignado[i],
                        necesita - resultado.asignado[i] > TOLERANCIA_TON
                                ? String.format(Locale.US, "  (faltan %.1f t)", necesita - resultado.asignado[i]) : ""));
            }
            sb.append(String.format(Locale.US, "%nRepartido: %.1f t · Sobrante: %.1f t%n", entregado,
                    Math.max(0, suministros - entregado)));
            mostrar(sb.toString());
        } catch (ParseException ex) {
            mostrar("Valor no válido en el campo de toneladas.");
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    private void ejecutarSeleccionOptima() {
        try {
            int capacidad = (int) Math.round(leerDecimal(spinnerCapacidad));
            Comunidad[] comunidades = comunidadesDelAlcance();
            if (comunidades.length == 0) {
                mostrar("No hay comunidades en el alcance elegido.");
                return;
            }
            ResultadoMochila resultado = Algoritmos.resolverMochila(comunidades, capacidad);
            StringBuilder sb = new StringBuilder("=== Mochila 0/1: comunidades que caben en el vehículo ===\n");
            sb.append("Capacidad del vehículo: ").append(capacidad).append(" t\n\nSeleccionadas:\n");
            if (resultado.seleccionadas.length == 0) {
                sb.append("  (ninguna comunidad cabe completa en esta capacidad)\n");
            }
            List<String> elegidas = new ArrayList<>();
            for (Comunidad c : resultado.seleccionadas) {
                elegidas.add(c.getNombre());
                sb.append(String.format(Locale.US, "  %-22s necesita %5.1f t · beneficio %.0f (P%d)%n", c.getNombre(),
                        c.getRecursosRequeridos(), c.calcularBeneficio(), c.getPrioridad()));
            }
            sb.append("\nNo caben / no convienen:\n");
            for (Comunidad c : comunidades) {
                if (!elegidas.contains(c.getNombre())) {
                    sb.append(String.format(Locale.US, "  %-22s necesita %5.1f t · beneficio %.0f%n", c.getNombre(),
                            c.getRecursosRequeridos(), c.calcularBeneficio()));
                }
            }
            sb.append(String.format(Locale.US, "%nPeso usado: %.0f t de %d t · Beneficio total: %.0f%n",
                    (double) resultado.pesoTotal, capacidad, resultado.beneficioTotal));
            sb.append("Las comunidades que no entran necesitan otro viaje u otro vehículo.");
            mostrar(sb.toString());
        } catch (ParseException ex) {
            mostrar("Valor no válido en el campo de capacidad.");
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    private void ejecutarRutaOptima() {
        try {
            int paradas = (int) Math.round(leerDecimal(spinnerParadas));
            List<Comunidad> candidatas = new ArrayList<>();
            for (Comunidad c : comunidadesDelAlcance()) {
                if (c.tieneUbicacion()) {
                    candidatas.add(c);
                }
            }
            if (candidatas.isEmpty()) {
                mostrar("No hay comunidades con ubicación en el alcance elegido.");
                return;
            }
            candidatas.sort(Comparator.comparingInt(Comunidad::getPrioridad).reversed()
                    .thenComparing(Comparator.comparingDouble(Comunidad::calcularBeneficio).reversed()));
            CentroAcopio centro = centroDelAlcance(candidatas.get(0));
            List<Comunidad> elegidas = new ArrayList<>(candidatas.subList(0, Math.min(candidatas.size(), paradas)));

            int n = elegidas.size() + 1;
            String[] nombres = new String[n];
            double[] lat = new double[n];
            double[] lon = new double[n];
            double[] pesos = new double[n];
            nombres[0] = "CENTRO " + centro.getCiudad();
            lat[0] = centro.getLatitud();
            lon[0] = centro.getLongitud();
            for (int i = 1; i < n; i++) {
                Comunidad c = elegidas.get(i - 1);
                nombres[i] = c.getNombre();
                lat[i] = c.getLatitud();
                lon[i] = c.getLongitud();
                pesos[i] = Math.max(1, c.getPrioridad());
            }
            double[][] km = new double[n][n];
            double[][] horas = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i != j) {
                        km[i][j] = GeoEcuador.distanciaCarreteraKm(lat[i], lon[i], lat[j], lon[j]);
                        horas[i][j] = EstudioVial.horasDeManejo(km[i][j], lat[i], lon[i], lat[j], lon[j]);
                    }
                }
            }

            ResultadoRuta corta = Algoritmos.rutaOptima(nombres, km);
            ResultadoRutaPonderada prioritaria = Algoritmos.rutaOptimaPonderada(nombres, horas, pesos);

            StringBuilder sb = new StringBuilder("=== Backtracking: orden de visita ===\n");
            sb.append("Sale de ").append(centro.getNombre()).append(" hacia las ").append(elegidas.size())
                    .append(" comunidades de mayor prioridad:\n");
            for (Comunidad c : elegidas) {
                sb.append(String.format(Locale.US, "  P%-2d %-22s %s%n", c.getPrioridad(), c.getNombre(), c.getProvincia()));
            }
            sb.append("\n(a) Menor distancia total\n");
            sb.append(describirRuta(corta.mejorRuta, nombres, km, horas, pesos));
            sb.append(String.format(Locale.US, "    Nodos explorados: %d · ramas podadas: %d%n", corta.nodosExplorados,
                    corta.nodosPodados));
            sb.append("\n(b) Llegar antes a lo más prioritario (el que usa la planificación)\n");
            sb.append(describirRuta(prioritaria.mejorRuta, nombres, km, horas, pesos));
            sb.append(String.format(Locale.US, "    Nodos explorados: %d · ramas podadas: %d%n", prioritaria.nodosExplorados,
                    prioritaria.nodosPodados));
            sb.append("\nSi (a) y (b) difieren, (b) acepta recorrer algo más para atender antes a quien más lo necesita.\n"
                    + "Los tiempos usan el estudio vial (región y estado de las vías); son referenciales.");
            mostrar(sb.toString());
        } catch (ParseException ex) {
            mostrar("Valor no válido en el campo de paradas.");
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    /** Lista una ruta con km y hora de llegada acumulados en cada parada. */
    private static String describirRuta(String[] ruta, String[] nombres, double[][] km, double[][] horas,
                                        double[] pesos) {
        Map<String, Integer> indice = new HashMap<>();
        for (int i = 0; i < nombres.length; i++) {
            indice.put(nombres[i], i);
        }
        StringBuilder sb = new StringBuilder();
        double kmAcum = 0;
        double hAcum = 0;
        double ponderado = 0;
        for (int k = 1; k < ruta.length; k++) {
            int de = indice.get(ruta[k - 1]);
            int a = indice.get(ruta[k]);
            kmAcum += km[de][a];
            hAcum += horas[de][a];
            boolean regreso = k == ruta.length - 1;
            if (!regreso) {
                ponderado += pesos[a] * hAcum;
            }
            sb.append(String.format(Locale.US, "    %d. %-24s %6.1f km acum. · llega a las %4.1f h%s%n", k,
                    regreso ? "regreso al centro" : ruta[k], kmAcum, hAcum,
                    regreso || pesos[a] <= 0 ? "" : " (P" + (int) pesos[a] + ")"));
        }
        sb.append(String.format(Locale.US, "    Total: %.1f km · %.1f h · Σ(prioridad × hora de llegada) = %.1f%n", kmAcum,
                hAcum, ponderado));
        return sb.toString();
    }

    private void ejecutarAsignacionVehiculos() {
        try {
            List<Vehiculo> marcados = listaVehiculos.getSelectedValuesList();
            Comunidad[] comunidades = comunidadesDelAlcance();
            if (marcados.isEmpty()) {
                mostrar("Marque al menos un vehículo de la lista para asignarlo.");
                return;
            }
            if (comunidades.length == 0) {
                mostrar("No hay comunidades en el alcance elegido.");
                return;
            }
            List<Comunidad> ordenadas = new ArrayList<>(List.of(comunidades));
            ordenadas.sort(Comparator.comparingInt(Comunidad::getPrioridad).reversed()
                    .thenComparing(Comparator.comparingDouble(Comunidad::calcularBeneficio).reversed()));
            Comunidad[] usadas = ordenadas.subList(0, Math.min(ordenadas.size(), MAX_NODOS_EXACTOS))
                    .toArray(new Comunidad[0]);
            Vehiculo[] vehiculos = new Vehiculo[Math.min(marcados.size(), MAX_NODOS_EXACTOS)];
            for (int i = 0; i < vehiculos.length; i++) {
                vehiculos[i] = marcados.get(i).enToneladas();
            }
            ResultadoAsignacion resultado = Algoritmos.asignarVehiculosBranchAndBound(vehiculos, usadas);

            StringBuilder sb = new StringBuilder("=== Branch and Bound: vehículo ↔ comunidad ===\n");
            sb.append("Vehículos elegidos:\n");
            for (Vehiculo v : vehiculos) {
                sb.append(String.format(Locale.US, "  %-8s %5.1f t%n", v.getId(), v.getCapacidad()));
            }
            sb.append("Comunidades (las ").append(usadas.length).append(" de mayor prioridad):\n");
            for (Comunidad c : usadas) {
                sb.append(String.format(Locale.US, "  %-22s necesita %5.1f t (P%d)%n", c.getNombre(),
                        c.getRecursosRequeridos(), c.getPrioridad()));
            }
            sb.append("\n");
            if (resultado.total == 0) {
                sb.append("Ningún vehículo elegido tiene capacidad para una carga completa. Marque vehículos más "
                        + "grandes o reparta la carga en varios viajes (Planificación).\n");
            } else {
                for (int i = 0; i < resultado.total; i++) {
                    sb.append(String.format(Locale.US, "  %s → %s%n", resultado.vehiculoIds[i],
                            resultado.comunidadNombres[i]));
                }
                sb.append(String.format(Locale.US, "%nCubiertas: %d de %d · capacidad desperdiciada: %.1f t%n",
                        resultado.total, usadas.length, resultado.desperdicioTotal));
            }
            sb.append(String.format(Locale.US, "Nodos explorados: %d · ramas podadas: %d%n", resultado.nodosExplorados,
                    resultado.nodosPodados));
            sb.append("\nDemostración: no modifica la base de datos. El plan real se arma en Planificación.");
            mostrar(sb.toString());
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    private void ejecutarRecomendacion() {
        try {
            List<CentroAcopio> enAlcance = centrosDelAlcance();
            List<ResumenCentro> resumenes = new ArrayList<>();
            for (ResumenCentro r : gestorPlanes.resumenPorCentro()) {
                if (enAlcance.stream().anyMatch(c -> c.getId() == r.centro().getId())) {
                    resumenes.add(r);
                }
            }
            StringBuilder sb = new StringBuilder("=== Recomendación de envío ===\n");
            for (ResumenCentro r : resumenes) {
                sb.append(String.format(Locale.US, "%n%s — necesidad %.1f t · stock %.1f t · a enviar %.1f t%n",
                        r.centro().getNombre(), r.demandaToneladas(), r.stockToneladas(), r.enviableToneladas()));
                for (Comunidad c : r.comunidades()) {
                    sb.append(String.format(Locale.US, "    P%-2d %-22s %5.1f t%n", c.getPrioridad(), c.getNombre(),
                            c.getRecursosRequeridos()));
                }
                sb.append(String.format(Locale.US, "    Flota: %d vehículo(s), %.1f t por viaje%n", r.flota().size(),
                        r.capacidadFlotaToneladas()));
            }
            sb.append("\nSugerencia (Greedy, vehículos más grandes primero):\n");
            for (String linea : RecomendadorEnvio.recomendar(resumenes).explicacion()) {
                sb.append("  • ").append(linea).append("\n");
            }
            var recomendacion = RecomendadorEnvio.recomendar(resumenes);
            if (alAplicar != null && !recomendacion.seleccion().estaVacia()) {
                alAplicar.accept(recomendacion.seleccion());
                sb.append("\nLa recomendación ya quedó marcada en «Vehículos a enviar» (arriba): puede cambiarla "
                        + "y pulsar Simular.");
            }
            mostrar(sb.toString());
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ utilidades

    /** Lee el valor de un spinner confirmando primero lo que el usuario haya escrito (rechaza fuera de rango). */
    private static double leerDecimal(JSpinner spinner) throws ParseException {
        spinner.commitEdit();
        return ((Number) spinner.getValue()).doubleValue();
    }

    private void mostrar(String texto) {
        areaResultados.setText(texto);
        areaResultados.setCaretPosition(0);
    }
}
