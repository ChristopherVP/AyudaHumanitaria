package uce.edu.ec.ayudahumanitaria.gui;

import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

/** Utilidades para repartir el ancho de las columnas de una tabla. */
final class Tablas {

    private Tablas() {
    }

    static void anchos(JTable tabla, int... anchos) {
        // Las columnas se reparten todo el ancho disponible, pero nunca bajan del 70 % de su ancho ideal
        // (si no caben, aparece la barra horizontal).
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumnModel columnas = tabla.getColumnModel();
        for (int i = 0; i < anchos.length && i < columnas.getColumnCount(); i++) {
            columnas.getColumn(i).setPreferredWidth(anchos[i]);
            columnas.getColumn(i).setMinWidth((int) (anchos[i] * 0.7));
        }
    }
}
