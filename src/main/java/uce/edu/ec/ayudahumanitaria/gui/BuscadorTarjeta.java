package uce.edu.ec.ayudahumanitaria.gui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.function.Consumer;

/** Tarjeta de búsqueda uniforme para Centros, Comunidades y Vehículos: un campo, «Buscar» y «Mostrar todos». */
final class BuscadorTarjeta {

    private BuscadorTarjeta() {
    }

    /**
     * @param titulo      título de la tarjeta
     * @param etiqueta    texto junto al campo (p. ej. «Nombre o ID:»)
     * @param alBuscar    recibe el texto escrito (ya sin espacios; nunca vacío)
     * @param mostrarTodo restablece la tabla completa
     * @param extra       componente opcional bajo la fila (resúmenes); puede ser {@code null}
     */
    static JPanel crear(String titulo, String etiqueta, Consumer<String> alBuscar, Runnable mostrarTodo,
                        JComponent extra) {
        JTextField campo = EstiloUI.campoTexto(18);
        JButton buscar = EstiloUI.botonNeutro("Buscar");
        JButton todos = EstiloUI.botonNeutro("Mostrar todos");

        Runnable ejecutar = () -> {
            String texto = campo.getText().trim();
            if (texto.isEmpty()) {
                mostrarTodo.run();
            } else {
                alBuscar.accept(texto);
            }
        };
        buscar.addActionListener(e -> ejecutar.run());
        campo.addActionListener(e -> ejecutar.run());
        todos.addActionListener(e -> {
            campo.setText("");
            mostrarTodo.run();
        });

        JPanel fila = EstiloUI.fila();
        fila.add(EstiloUI.etiqueta(etiqueta));
        fila.add(campo);
        fila.add(buscar);
        fila.add(todos);

        JPanel tarjeta = EstiloUI.tarjeta(titulo);
        JPanel contenido = new JPanel(new BorderLayout(0, 6));
        contenido.setOpaque(false);
        contenido.add(fila, BorderLayout.NORTH);
        if (extra != null) {
            contenido.add(extra, BorderLayout.CENTER);
        }
        tarjeta.add(contenido, BorderLayout.CENTER);
        return tarjeta;
    }
}
