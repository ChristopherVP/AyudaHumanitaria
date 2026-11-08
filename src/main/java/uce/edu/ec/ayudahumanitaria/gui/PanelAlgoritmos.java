package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoAsignacion;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoMochila;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoRuta;
import uce.edu.ec.ayudahumanitaria.dominio.CentroAcopio;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.GestorCentros;
import uce.edu.ec.ayudahumanitaria.dominio.PlanificadorDistribucion;
import uce.edu.ec.ayudahumanitaria.geo.GeoEcuador;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorVehiculos;
import uce.edu.ec.ayudahumanitaria.dominio.Vehiculo;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Demostración de los algoritmos del curso sobre los datos reales. Se puede limitar a un centro de acopio
 * (comunidades que ese centro atiende). Es de solo lectura: no modifica la base de datos.
 */
public class PanelAlgoritmos extends JPanel implements Refrescable {

    private final GestorComunidades gestorComunidades;
    private final GestorVehiculos gestorVehiculos;
    private final GestorCentros gestorCentros;
    private final JComboBox<Object> comboAlcance = new JComboBox<>();
    private static final String TODOS = "Todos los centros";
    private static final int MAX_NODOS_EXACTOS = 8;

    private final JTextField campoSuministros = EstiloUI.campoTexto(8);
    private final JTextField campoCapacidadMochila = EstiloUI.campoTexto(8);

    private final JTextArea areaResultados = new JTextArea();

    public PanelAlgoritmos(GestorComunidades gestorComunidades, GestorVehiculos gestorVehiculos,
                           GestorCentros gestorCentros) {
        this.gestorComunidades = gestorComunidades;
        this.gestorVehiculos = gestorVehiculos;
        this.gestorCentros = gestorCentros;

        setLayout(new BorderLayout(12, 12));
        setBackground(EstiloUI.FONDO_APP);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel panelSecciones = new JPanel(new GridLayout(0, 1, 0, 8));
        JPanel alcance = construirTarjetaCompacta();
        alcance.add(EstiloUI.etiqueta("Alcance de la demostración:"));
        alcance.add(comboAlcance);
        panelSecciones.add(alcance);
        panelSecciones.setOpaque(false);

        panelSecciones.add(crearSeccionSimple(
                "Comunidad con mayor necesidad de ayuda",
                "Identifica la comunidad con el mayor beneficio (habitantes x prioridad).",
                "Calcular",
                e -> ejecutarComunidadMayorBeneficio()
        ));

        panelSecciones.add(crearSeccionConCampo(
                "Distribución de suministros (Greedy)",
                "Suministros disponibles (ton):", campoSuministros,
                "Distribuir suministros",
                e -> ejecutarDistribucionSuministros()
        ));

        panelSecciones.add(crearSeccionConCampo(
                "Selección óptima de comunidades (Mochila)",
                "Capacidad total del envío (ton):", campoCapacidadMochila,
                "Seleccionar comunidades óptimas",
                e -> ejecutarSeleccionOptima()
        ));

        panelSecciones.add(crearSeccionSimple(
                "Ruta óptima de distribución",
                "Calcula la ruta de menor distancia entre el centro y las comunidades (Backtracking).",
                "Calcular ruta",
                e -> ejecutarRutaOptima()
        ));

        panelSecciones.add(crearSeccionSimple(
                "Asignación de vehículos a comunidades",
                "Asigna vehículos a comunidades minimizando el desperdicio de capacidad (Branch and Bound).",
                "Asignar",
                e -> ejecutarAsignacionVehiculos()
        ));

        EstiloUI.estilizarAreaTexto(areaResultados);
        areaResultados.setEditable(false);
        areaResultados.setFont(EstiloUI.FUENTE_MONO);

        JScrollPane scrollResultados = new JScrollPane(areaResultados);
        scrollResultados.setBorder(BorderFactory.createEmptyBorder());
        JPanel tarjetaResultados = EstiloUI.tarjeta("Resultados");
        tarjetaResultados.add(scrollResultados, BorderLayout.CENTER);

        add(panelSecciones, BorderLayout.NORTH);
        refrescar();
        add(tarjetaResultados, BorderLayout.CENTER);
    }

    private JPanel crearSeccionSimple(String titulo, String descripcion, String textoBoton, ActionListener accion) {
        JPanel tarjeta = construirTarjetaCompacta();
        tarjeta.setToolTipText(descripcion);

        JLabel lblTitulo = EstiloUI.etiqueta(titulo);
        lblTitulo.setToolTipText(descripcion);

        JButton boton = EstiloUI.botonPrimario(textoBoton);
        boton.setToolTipText(descripcion);
        boton.addActionListener(accion);

        tarjeta.add(lblTitulo);
        tarjeta.add(boton);
        return tarjeta;
    }

    private JPanel crearSeccionConCampo(String titulo, String etiquetaCampo, JTextField campo,
                                        String textoBoton, ActionListener accion) {
        JPanel tarjeta = construirTarjetaCompacta();

        JLabel lblTitulo = EstiloUI.etiqueta(titulo);
        JButton boton = EstiloUI.botonPrimario(textoBoton);
        boton.addActionListener(accion);

        tarjeta.add(lblTitulo);
        tarjeta.add(EstiloUI.textoSecundario(etiquetaCampo));
        tarjeta.add(campo);
        tarjeta.add(boton);
        return tarjeta;
    }

    /** Tarjeta compacta de una sola fila: franja azul a la izquierda + contenido en línea. */
    private JPanel construirTarjetaCompacta() {
        JPanel tarjeta = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 14, 8));
        tarjeta.setBackground(EstiloUI.FONDO_TARJETA);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, EstiloUI.AZUL_PRIMARIO),
                        BorderFactory.createLineBorder(EstiloUI.BORDE_SUAVE, 1)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return tarjeta;
    }

    private void ejecutarComunidadMayorBeneficio() {
        try {
            Comunidad[] comunidades = comunidadesDelAlcance();
            if (comunidades.length == 0) {
                mostrar("No hay comunidades en el alcance elegido.");
                return;
            }

            Comunidad resultado = Algoritmos.comunidadMayorBeneficio(comunidades);

            mostrar("=== Comunidad con mayor necesidad de ayuda ===\n"
                    + "Comunidad seleccionada: " + resultado.getNombre() + "\n"
                    + "Beneficio = habitantes x prioridad = " + resultado.calcularBeneficio());
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    private void ejecutarDistribucionSuministros() {
        try {
            double suministros = Double.parseDouble(campoSuministros.getText().trim().replace(',', '.'));
            if (suministros <= 0) {
                mostrar("Los suministros deben ser mayores que 0.");
                return;
            }
            Comunidad[] comunidades = comunidadesDelAlcance();

            ResultadoDistribucion resultado = Algoritmos.distribuirSuministros(comunidades, suministros);

            StringBuilder sb = new StringBuilder("=== Distribución de suministros ===\n");
            sb.append("Suministros disponibles: ").append(suministros).append("\n");
            sb.append("Orden de asignación por prioridad descendente:\n");

            double restante = suministros;
            for (int i = 0; i < resultado.nombres.length; i++) {
                sb.append(" - ").append(resultado.nombres[i])
                        .append(": asignado ").append(resultado.asignado[i]).append("\n");
                restante -= resultado.asignado[i];
            }
            sb.append("Suministros sobrantes: ").append(Math.max(restante, 0));

            mostrar(sb.toString());
        } catch (NumberFormatException ex) {
            mostrar("Ingrese una cantidad numérica válida de suministros.");
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    private void ejecutarSeleccionOptima() {
        try {
            int capacidad = Integer.parseInt(campoCapacidadMochila.getText().trim());
            if (capacidad <= 0) {
                mostrar("La capacidad debe ser mayor que 0.");
                return;
            }
            Comunidad[] comunidades = comunidadesDelAlcance();

            ResultadoMochila resultado = Algoritmos.resolverMochila(comunidades, capacidad);

            StringBuilder sb = new StringBuilder("=== Selección óptima de comunidades ===\n");
            sb.append("Capacidad: ").append(capacidad).append(" toneladas\n");
            sb.append("Comunidades seleccionadas:\n");

            for (Comunidad c : resultado.seleccionadas) {
                sb.append(" - ").append(c.getNombre())
                        .append(" (peso=").append(c.getRecursosRequeridos())
                        .append(", beneficio=").append(c.calcularBeneficio())
                        .append(")\n");
            }

            sb.append("Peso total usado: ").append(resultado.pesoTotal).append("\n");
            sb.append("Beneficio total: ").append(resultado.beneficioTotal);

            mostrar(sb.toString());
        } catch (NumberFormatException ex) {
            mostrar("Ingrese una capacidad entera válida (toneladas).");
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    private void ejecutarRutaOptima() {
        try {
            Comunidad[] todas = comunidadesDelAlcance();
            List<Comunidad> candidatas = new ArrayList<>();
            for (Comunidad c : todas) {
                if (c.tieneUbicacion()) {
                    candidatas.add(c);
                }
            }
            if (candidatas.isEmpty()) {
                mostrar("No hay comunidades con ubicación en el alcance elegido.");
                return;
            }
            candidatas.sort(Comparator.comparingInt(Comunidad::getPrioridad).reversed());
            // Con "todos los centros" se sale del centro más cercano a la comunidad de mayor prioridad.
            CentroAcopio centro = centroDelAlcance(candidatas.get(0));
            List<Comunidad> elegidas = new ArrayList<>(
                    candidatas.subList(0, Math.min(candidatas.size(), MAX_NODOS_EXACTOS - 1)));

            // Se usa el id como nombre interno para evitar ambigüedades si hay nombres parecidos.
            String[] nombres = new String[elegidas.size() + 1];
            nombres[0] = "Centro";
            for (int i = 0; i < elegidas.size(); i++) {
                nombres[i + 1] = elegidas.get(i).getNombre();
            }
            double[][] distancias = matrizDistancias(centro, elegidas);

            ResultadoRuta resultado = Algoritmos.rutaOptima(nombres, distancias);

            StringBuilder sb = new StringBuilder("=== Ruta óptima de distribución (Backtracking) ===\n");
            sb.append("Centro de salida: ").append(centro.getNombre()).append("\n");
            sb.append("Comunidades consideradas (máx. ").append(MAX_NODOS_EXACTOS - 1)
                    .append(", las de mayor prioridad): ").append(elegidas.size()).append("\n");
            sb.append("Ruta encontrada: ").append(String.join(" -> ", resultado.mejorRuta)).append("\n");
            sb.append(String.format("Distancia total estimada por carretera: %.1f km%n", resultado.mejorDistancia));
            sb.append("Nodos explorados: ").append(resultado.nodosExplorados).append("\n");
            sb.append("Ramas podadas: ").append(resultado.nodosPodados);
            mostrar(sb.toString());
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    /** Distancias reales estimadas (Haversine x 1.35) entre el centro y cada comunidad. */
    private double[][] matrizDistancias(CentroAcopio centro, List<Comunidad> comunidades) {
        int n = comunidades.size() + 1;
        double[][] matriz = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    continue;
                }
                double lat1 = i == 0 ? centro.getLatitud() : comunidades.get(i - 1).getLatitud();
                double lon1 = i == 0 ? centro.getLongitud() : comunidades.get(i - 1).getLongitud();
                double lat2 = j == 0 ? centro.getLatitud() : comunidades.get(j - 1).getLatitud();
                double lon2 = j == 0 ? centro.getLongitud() : comunidades.get(j - 1).getLongitud();
                matriz[i][j] = GeoEcuador.distanciaCarreteraKm(lat1, lon1, lat2, lon2);
            }
        }
        return matriz;
    }

    private CentroAcopio centroDelAlcance(Comunidad referencia) throws PersistenciaException {
        Object elegido = comboAlcance.getSelectedItem();
        if (elegido instanceof CentroAcopio centro) {
            return centro;
        }
        return PlanificadorDistribucion.centroMasCercano(gestorCentros.listar(), referencia);
    }

    /** Comunidades que atiende el centro elegido (o todas), según el centro más cercano. */
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

    @Override
    public void refrescar() {
        Object anterior = comboAlcance.getSelectedItem();
        comboAlcance.removeAllItems();
        comboAlcance.addItem(TODOS);
        try {
            List<CentroAcopio> centros = gestorCentros.listar();
            centros.forEach(comboAlcance::addItem);
            if (anterior instanceof CentroAcopio previo) {
                centros.stream().filter(c -> c.getId() == previo.getId()).findFirst().ifPresent(comboAlcance::setSelectedItem);
            }
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    private void ejecutarAsignacionVehiculos() {
        try {
            Comunidad[] comunidades = comunidadesDelAlcance();
            List<Vehiculo> delAlcance = new ArrayList<>();
            for (Vehiculo v : gestorVehiculos.listarTodos()) {
                if (!(comboAlcance.getSelectedItem() instanceof CentroAcopio centro) || v.getCentroId() == centro.getId()) {
                    delAlcance.add(v.enToneladas());
                }
            }
            if (delAlcance.isEmpty() || comunidades.length == 0) {
                mostrar("Registre vehículos y comunidades en el alcance elegido.");
                return;
            }
            // Branch and Bound es exponencial: se limita a las comunidades de mayor prioridad.
            List<Comunidad> limitadas = new ArrayList<>(List.of(comunidades));
            limitadas.sort(Comparator.comparingInt(Comunidad::getPrioridad).reversed());
            Comunidad[] usadas = limitadas.subList(0, Math.min(limitadas.size(), MAX_NODOS_EXACTOS)).toArray(new Comunidad[0]);
            Vehiculo[] vehiculos = delAlcance.subList(0, Math.min(delAlcance.size(), MAX_NODOS_EXACTOS)).toArray(new Vehiculo[0]);

            ResultadoAsignacion resultado = Algoritmos.asignarVehiculosBranchAndBound(vehiculos, usadas);
            mostrar(construirResumenAsignacion(vehiculos, usadas, resultado)
                    + "\n(Demostración: no modifica la base de datos. El plan real se arma en la pestaña Planificación.)");
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    private String construirResumenAsignacion(Vehiculo[] vehiculos, Comunidad[] comunidades,
                                              ResultadoAsignacion resultado) {
        StringBuilder sb = new StringBuilder("=== Asignación de vehículos (Branch and Bound) ===\n");

        sb.append("Vehículos ");
        for (int i = 0; i < vehiculos.length; i++) {
            sb.append(vehiculos[i].getId()).append("=").append(vehiculos[i].getCapacidad()).append(" t");
            sb.append(i < vehiculos.length - 1 ? ", " : ". ");
        }

        sb.append("Comunidades: ");
        for (int i = 0; i < comunidades.length; i++) {
            sb.append(comunidades[i].getNombre()).append("=")
                    .append(comunidades[i].getRecursosRequeridos()).append(" t");
            sb.append(i < comunidades.length - 1 ? ", " : ". ");
        }

        if (resultado.total == 0) {
            sb.append("Ningún vehículo pudo asignarse: ninguna capacidad alcanza los recursos requeridos.");
        } else {
            sb.append("Asignar ");
            for (int i = 0; i < resultado.total; i++) {
                sb.append(resultado.vehiculoIds[i]).append("->").append(resultado.comunidadNombres[i]);
                sb.append(i < resultado.total - 1 ? " y " : ". ");
            }
            sb.append("Desperdicio=").append(String.format("%.1f", resultado.desperdicioTotal)).append(" t.");
        }

        sb.append("\nNodos explorados: ").append(resultado.nodosExplorados);
        sb.append(" | Ramas podadas: ").append(resultado.nodosPodados);

        return sb.toString();
    }

    private void mostrar(String texto) {
        areaResultados.setText(texto);
        areaResultados.setCaretPosition(0);
    }
}
