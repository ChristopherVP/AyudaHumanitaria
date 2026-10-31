package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoAsignacion;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoMochila;
import uce.edu.ec.ayudahumanitaria.dominio.Algoritmos.ResultadoRuta;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorVehiculos;
import uce.edu.ec.ayudahumanitaria.dominio.Vehiculo;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

public class PanelAlgoritmos extends JPanel {

    private final GestorComunidades gestorComunidades;
    private final GestorVehiculos gestorVehiculos;
    private final PanelVehiculos panelVehiculos;

    private final JTextField campoSuministros = EstiloUI.campoTexto(8);
    private final JTextField campoCapacidadMochila = EstiloUI.campoTexto(8);

    private final JTextArea areaResultados = new JTextArea();

    public PanelAlgoritmos(GestorComunidades gestorComunidades, GestorVehiculos gestorVehiculos,
                            PanelVehiculos panelVehiculos) {
        this.gestorComunidades = gestorComunidades;
        this.gestorVehiculos = gestorVehiculos;
        this.panelVehiculos = panelVehiculos;

        setLayout(new BorderLayout(12, 12));
        setBackground(EstiloUI.FONDO_APP);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel panelSecciones = new JPanel(new GridLayout(0, 1, 0, 8));
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
            Comunidad[] comunidades = gestorComunidades.listarTodas();
            if (comunidades.length == 0) {
                mostrar("No hay comunidades registradas.");
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
            double suministros = Double.parseDouble(campoSuministros.getText());
            Comunidad[] comunidades = gestorComunidades.listarTodas();

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
            int capacidad = Integer.parseInt(campoCapacidadMochila.getText());
            Comunidad[] comunidades = gestorComunidades.listarTodas();

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
            mostrar("Ingrese una capacidad numérica válida.");
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    private void ejecutarRutaOptima() {
        try {
            Comunidad[] comunidades = gestorComunidades.listarTodas();

            if (comunidades.length < 2) {
                mostrar("Se necesitan al menos 2 comunidades para calcular una ruta.");
                return;
            }
            if (comunidades.length > 8) {
                mostrar("Por eficiencia, esta demo admite hasta 8 comunidades.");
                return;
            }

            String[] nombres = new String[comunidades.length + 1];
            nombres[0] = "Centro";
            for (int i = 0; i < comunidades.length; i++) {
                nombres[i + 1] = comunidades[i].getNombre();
            }
            double[][] distancias = generarMatrizDistancias(nombres);

            ResultadoRuta resultado = Algoritmos.rutaOptima(nombres, distancias);

            StringBuilder sb = new StringBuilder("=== Ruta óptima de distribución (Backtracking) ===\n");
            sb.append("Ruta encontrada: ").append(String.join(" -> ", resultado.mejorRuta)).append("\n");
            sb.append("Distancia total: ").append(resultado.mejorDistancia).append("\n");
            sb.append("Nodos explorados: ").append(resultado.nodosExplorados).append("\n");
            sb.append("Ramas podadas: ").append(resultado.nodosPodados);

            mostrar(sb.toString());
        } catch (PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    private double[][] generarMatrizDistancias(String[] nombres) {
        int n = nombres.length;
        double[][] matriz = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matriz[i][j] = (i == j) ? 0.0 : 10 + Math.abs(i - j) * 8;
            }
        }
        return matriz;
    }

    private void ejecutarAsignacionVehiculos() {
        try {
            Vehiculo[] vehiculos = gestorVehiculos.listarTodos();
            Comunidad[] comunidades = gestorComunidades.listarTodas();

            if (vehiculos.length == 0 || comunidades.length == 0) {
                mostrar("Registre vehículos y comunidades antes de asignar.");
                return;
            }

            ResultadoAsignacion resultado = Algoritmos.asignarVehiculosBranchAndBound(vehiculos, comunidades);

            for (Vehiculo v : vehiculos) {
                v.setAsignado(false);
                v.setComunidadAsignada(null);
            }
            for (int i = 0; i < resultado.total; i++) {
                for (Vehiculo v : vehiculos) {
                    if (v.getId().equals(resultado.vehiculoIds[i])) {
                        v.setAsignado(true);
                        v.setComunidadAsignada(resultado.comunidadNombres[i]);
                    }
                }
            }
            for (Vehiculo v : vehiculos) {
                gestorVehiculos.modificar(v);
            }
            if (panelVehiculos != null) {
                panelVehiculos.cargarTabla();
            }

            mostrar(construirResumenAsignacion(vehiculos, comunidades, resultado));
        } catch (ValidationException | PersistenciaException ex) {
            mostrar(ex.getMessage());
        }
    }

    private String construirResumenAsignacion(Vehiculo[] vehiculos, Comunidad[] comunidades,
                                                ResultadoAsignacion resultado) {
        StringBuilder sb = new StringBuilder("=== Asignación de vehículos (Branch and Bound) ===\n");

        sb.append("Vehículos ");
        for (int i = 0; i < vehiculos.length; i++) {
            sb.append(vehiculos[i].getId()).append("=").append((long) vehiculos[i].getCapacidad()).append("kg");
            sb.append(i < vehiculos.length - 1 ? ", " : ". ");
        }

        sb.append("Comunidades: ");
        for (int i = 0; i < comunidades.length; i++) {
            sb.append(comunidades[i].getNombre()).append("=")
                    .append((long) comunidades[i].getRecursosRequeridos()).append("kg");
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
            sb.append("Desperdicio=").append((long) resultado.desperdicioTotal).append("kg.");
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
