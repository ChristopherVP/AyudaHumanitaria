package uce.edu.ec.ayudahumanitaria.ejecutable;

import uce.edu.ec.ayudahumanitaria.dominio.DatosEjemplo;
import uce.edu.ec.ayudahumanitaria.dominio.GestorComunidades;
import uce.edu.ec.ayudahumanitaria.dominio.GestorVehiculos;
import uce.edu.ec.ayudahumanitaria.gui.SplashScreen;
import uce.edu.ec.ayudahumanitaria.gui.VentanaPrincipal;
import uce.edu.ec.ayudahumanitaria.persistencia.ConexionBD;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;

public class Main {

    public static void main(String[] args) {
        GestorComunidades gestorComunidades = new GestorComunidades();
        GestorVehiculos gestorVehiculos = new GestorVehiculos();

        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = new SplashScreen();
            splash.setVisible(true);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    splash.actualizar(20, "Inicializando base de datos...");
                    ConexionBD.inicializarBD();
                    Thread.sleep(600);

                    splash.actualizar(60, "Cargando datos...");
                    DatosEjemplo.cargarSiVacio(gestorComunidades, gestorVehiculos);
                    Thread.sleep(600);

                    splash.actualizar(90, "Iniciando interfaz...");
                    Thread.sleep(800);

                    splash.actualizar(100, "Listo");
                    Thread.sleep(300);
                    return null;
                }

                @Override
                protected void done() {
                    splash.dispose();
                    try {
                        get();
                    } catch (ExecutionException ex) {
                        JOptionPane.showMessageDialog(null, ex.getCause().getMessage(),
                                "Error crítico", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }

                    Runtime.getRuntime().addShutdownHook(new Thread(ConexionBD::cerrarConexion));
                    new VentanaPrincipal(gestorComunidades, gestorVehiculos).setVisible(true);
                }
            }.execute();
        });
    }
}