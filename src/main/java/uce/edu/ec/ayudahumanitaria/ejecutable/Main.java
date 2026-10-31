package uce.edu.ec.ayudahumanitaria.ejecutable;

import uce.edu.ec.ayudahumanitaria.dominio.DatosEjemplo;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorVehiculos;
import uce.edu.ec.ayudahumanitaria.gui.VentanaPrincipal;
import uce.edu.ec.ayudahumanitaria.persistencia.ConexionBD;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        GestorComunidades gestorComunidades = new GestorComunidades();
        GestorVehiculos gestorVehiculos = new GestorVehiculos();

        try {
            ConexionBD.inicializarBD();
            DatosEjemplo.cargarSiVacio(gestorComunidades, gestorVehiculos);
        } catch (ValidationException | PersistenciaException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error crítico", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(ConexionBD::cerrarConexion));

        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal(gestorComunidades, gestorVehiculos);
            ventana.setVisible(true);
        });
    }
}
