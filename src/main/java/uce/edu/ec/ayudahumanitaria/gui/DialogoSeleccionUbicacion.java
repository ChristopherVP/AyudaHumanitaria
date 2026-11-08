package uce.edu.ec.ayudahumanitaria.gui;

import uce.edu.ec.ayudahumanitaria.dominio.CentroAcopio;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.geo.GeoEcuador;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.Optional;

/** Ventana modal con el mapa para elegir una ubicación con un clic. */
public final class DialogoSeleccionUbicacion {

    private DialogoSeleccionUbicacion() {
    }

    /**
     * Muestra el mapa y devuelve {latitud, longitud} si el usuario confirma un punto válido
     * (dentro del Ecuador continental), o vacío si cancela.
     */
    public static Optional<double[]> elegir(Component padre, List<CentroAcopio> centros, List<Comunidad> comunidades,
                                            double[] puntoInicial) {
        JDialog dialogo = new JDialog(SwingUtilities.getWindowAncestor(padre), "Elegir ubicación en el mapa",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        Logo.aplicarA(dialogo);

        MapaEcuadorPanel mapa = new MapaEcuadorPanel();
        mapa.setDatos(centros, comunidades, List.of());
        mapa.setCapas(MapaEcuadorPanel.Capas.porDefecto().conComunidades(true));
        double[][] elegido = new double[1][];
        JLabel estado = EstiloUI.textoSecundario("Haga clic en el mapa. Rueda: zoom · arrastrar: mover · doble clic: restablecer.");
        JButton aceptar = EstiloUI.botonPrimario("Usar este punto");
        JButton cancelar = EstiloUI.botonNeutro("Cancelar");
        aceptar.setEnabled(false);

        if (puntoInicial != null && GeoEcuador.estaEnEcuadorContinental(puntoInicial[0], puntoInicial[1])) {
            elegido[0] = puntoInicial;
            mapa.setPuntoElegido(puntoInicial[0], puntoInicial[1]);
            estado.setText("Punto actual: " + MapaEcuadorPanel.formatoCoordenadas(puntoInicial));
            aceptar.setEnabled(true);
        }

        mapa.activarSeleccion(latLon -> {
            if (!GeoEcuador.estaEnEcuadorContinental(latLon[0], latLon[1])) {
                elegido[0] = null;
                aceptar.setEnabled(false);
                estado.setText("Ese punto está fuera del Ecuador continental. Elija otro.");
                return;
            }
            elegido[0] = latLon;
            aceptar.setEnabled(true);
            estado.setText("Punto elegido: " + MapaEcuadorPanel.formatoCoordenadas(latLon)
                    + " · ciudad de referencia: " + GeoEcuador.ciudadMasCercana(latLon[0], latLon[1]).nombre());
        });

        aceptar.addActionListener(e -> dialogo.dispose());
        cancelar.addActionListener(e -> {
            elegido[0] = null;
            dialogo.dispose();
        });

        JPanel pie = EstiloUI.fila();
        pie.add(aceptar);
        pie.add(cancelar);
        pie.add(estado);

        dialogo.getContentPane().setLayout(new BorderLayout());
        dialogo.getContentPane().add(mapa, BorderLayout.CENTER);
        dialogo.getContentPane().add(pie, BorderLayout.SOUTH);
        dialogo.setPreferredSize(new Dimension(820, 700));
        dialogo.pack();
        dialogo.setLocationRelativeTo(padre);
        dialogo.setVisible(true);
        return Optional.ofNullable(elegido[0]);
    }
}
