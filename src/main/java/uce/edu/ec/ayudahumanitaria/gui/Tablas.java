package uce.edu.ec.ayudahumanitaria.gui;

import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

/** Utilidades para dar anchos fijos a las columnas (con barra horizontal si no caben). */
final class Tablas {

    private Tablas() {
    }

    static void anchos(JTable tabla, int... anchos) {
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableColumnModel columnas = tabla.getColumnModel();
        for (int i = 0; i < anchos.length && i < columnas.getColumnCount(); i++) {
            columnas.getColumn(i).setPreferredWidth(anchos[i]);
        }
    }
}
