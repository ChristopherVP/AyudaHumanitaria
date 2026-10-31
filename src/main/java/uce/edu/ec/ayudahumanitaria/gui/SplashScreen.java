package uce.edu.ec.ayudahumanitaria.gui;

import javax.swing.*;
import java.awt.*;

public class SplashScreen extends JWindow {

    private final JProgressBar barra;
    private final JLabel estado;

    public SplashScreen() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(EstiloUI.AZUL_PRIMARIO_OSCURO);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EstiloUI.AZUL_PRIMARIO, 2),
                BorderFactory.createEmptyBorder(35, 40, 30, 40)));

        JLabel titulo = new JLabel("Ayuda Humanitaria");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 30));
        titulo.setForeground(Color.WHITE);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitulo = new JLabel("Sistema Inteligente de Distribución de Ayuda Humanitaria");
        subtitulo.setFont(EstiloUI.FUENTE_BASE);
        subtitulo.setForeground(EstiloUI.AZUL_PRIMARIO_CLARO);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        barra = new JProgressBar(0, 100);
        barra.setForeground(EstiloUI.VERDE_ACCION);
        barra.setBackground(EstiloUI.AZUL_PRIMARIO);
        barra.setBorderPainted(false);
        barra.setPreferredSize(new Dimension(400, 10));
        barra.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));

        estado = new JLabel("Iniciando...");
        estado.setFont(EstiloUI.FUENTE_BASE);
        estado.setForeground(EstiloUI.AZUL_PRIMARIO_CLARO);
        estado.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titulo);
        panel.add(Box.createVerticalStrut(8));
        panel.add(subtitulo);
        panel.add(Box.createVerticalStrut(40));
        panel.add(barra);
        panel.add(Box.createVerticalStrut(12));
        panel.add(estado);

        setContentPane(panel);
        setSize(560, 260);
        setLocationRelativeTo(null);
    }

    public void actualizar(int valor, String texto) {
        SwingUtilities.invokeLater(() -> {
            barra.setValue(valor);
            estado.setText(texto);
        });
    }
}