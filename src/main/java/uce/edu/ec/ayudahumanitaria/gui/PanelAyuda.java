package uce.edu.ec.ayudahumanitaria.gui;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.BorderLayout;

/** Pestaña de ayuda: explica el flujo del programa, las reglas, los algoritmos y cómo leer el mapa y los reportes. */
public class PanelAyuda extends JPanel {

    private static final String HTML = """
            <html><body style='font-family:SansSerif; font-size:13px; color:#1e2227; margin:14px;'>
            <a name='inicio'></a>
            <h1 style='color:#0f4076;'>Ayuda — Ayuda Humanitaria</h1>
            <p>Sistema para planificar la distribución de ayuda humanitaria en el Ecuador continental desde
            centros de acopio hacia comunidades, con fechas reales y rutas por vehículo.</p>
            <p><b>Contenido:</b>
            <a href='#flujo'>Flujo de trabajo</a> ·
            <a href='#centros'>Centros</a> ·
            <a href='#comunidades'>Comunidades</a> ·
            <a href='#vehiculos'>Vehículos</a> ·
            <a href='#plan'>Planificación</a> ·
            <a href='#algoritmos'>Algoritmos</a> ·
            <a href='#mapa'>Mapa</a> ·
            <a href='#reportes'>Reportes</a> ·
            <a href='#reglas'>Validaciones</a> ·
            <a href='#datos'>Datos y límites</a></p>

            <a name='flujo'></a><h2 style='color:#195a9e;'>1. Flujo de trabajo recomendado</h2>
            <ol>
              <li><b>Centros:</b> revise o registre los centros de acopio y su stock (toneladas).</li>
              <li><b>Comunidades:</b> registre comunidades con <b>dirección (obligatoria)</b>, habitantes, prioridad,
                  recursos requeridos y ubicación: al elegir el punto en el mapa, la <b>ciudad y la provincia se
                  completan solas</b>.</li>
              <li><b>Vehículos:</b> cada vehículo pertenece a un centro y tiene capacidad en kg.</li>
              <li><b>Planificación:</b> <b>primero elija qué vehículos envía cada centro y cuántos viajes hace cada uno</b>
                  (o pulse <i>Recomendar vehículos</i>), luego fecha y hora de salida y <i>Simular</i>. Sin vehículos
                  elegidos no se puede planificar. <i>Simulación rápida</i> hace todo con la sugerencia, sin guardar.
                  Si está conforme, pulse <i>Confirmar plan</i>.</li>
              <li><b>Reportes:</b> exporte el plan a Excel o PDF (con mapas) para sus registros.</li>
            </ol>

            <a name='centros'></a><h2 style='color:#195a9e;'>2. Centros de acopio</h2>
            <p>Son el origen de la distribución. Por defecto hay cinco: <b>Quito, Guayaquil, Cuenca</b> y dos
            puntos de la Amazonía: <b>Nueva Loja (norte)</b> y <b>Macas (sur)</b>, elegidos por ser capitales
            provinciales con acceso vial hacia las provincias amazónicas. Cada comunidad la atiende el centro
            <b>más cercano</b> (distancia estimada por carretera). Un centro no se puede eliminar si tiene vehículos
            o viajes vigentes, ni mover de lugar si tiene viajes planificados. Con <i>Reabastecer</i> se suman
            toneladas al stock.</p>

            <a name='comunidades'></a><h2 style='color:#195a9e;'>3. Comunidades</h2>
            <p><b>Beneficio = habitantes × prioridad.</b> La prioridad va de 1 a 10. La tabla muestra el centro asignado,
            la distancia, lo planificado y lo ya entregado. La ubicación debe estar dentro del Ecuador continental y
            ser coherente con la provincia elegida. Una comunidad no se elimina si tiene viajes vigentes.</p>

            <a name='vehiculos'></a><h2 style='color:#195a9e;'>4. Vehículos</h2>
            <p>Capacidad entre 500 y 40&nbsp;000 kg. Un vehículo solo hace viajes desde su centro. Su estado
            (<i>asignado</i> y comunidad) se actualiza solo según el reloj: mientras tenga un viaje planificado o en
            ruta aparece asignado.</p>

            <a name='plan'></a><h2 style='color:#195a9e;'>5. Planificación con fechas reales</h2>
            <p>Al simular, el programa calcula para cada viaje la hora de salida, de llegada a cada comunidad y de
            regreso. Supuestos (editables en el código, <code>ParametrosSimulacion</code>):</p>
            <ul>
              <li>Velocidad según el <b>estudio vial</b> (Costa 58, Sierra 48, Amazonía 42 km/h, menos si la provincia tiene
                  muchas vías en mal estado); máximo 8 h de conducción por día; descanso de 10 h entre jornadas.</li>
              <li>Descarga: 30 min por parada + 10 min por tonelada.</li>
              <li>2 h de preparación antes de que un vehículo salga a otro viaje.</li>
              <li>Máximo 7 paradas por viaje.</li>
            </ul>
            <p>Estados: <i>Planificado → En ruta → Completado</i>, según la hora actual; <i>Cancelado</i> si se cancela
            el plan (el stock no entregado vuelve al centro). La salida no puede estar en el pasado.</p>

            <a name='algoritmos'></a><h2 style='color:#195a9e;'>6. Algoritmos usados (los mismos del proyecto)</h2>
            <ol>
              <li><b>Greedy</b> — asigna cada comunidad a su centro más cercano y reparte el stock por prioridad.</li>
              <li><b>Divide y vencerás</b> — encuentra la comunidad con mayor beneficio de cada centro.</li>
              <li><b>Mochila 0/1 (programación dinámica)</b> — arma los grupos de comunidades que caben en un vehículo,
                  maximizando el beneficio.</li>
              <li><b>Branch and Bound</b> — asigna vehículos a grupos minimizando el desperdicio de capacidad.</li>
              <li><b>Backtracking con poda</b> — ordena las paradas de cada viaje para llegar antes a las comunidades de
                  mayor prioridad (beneficio por habitante) sin alargar de más el recorrido.</li>
              <li><b>Ordenamientos y búsquedas</b> — ordenan y buscan listas en toda la aplicación.</li>
            </ol>
            <p>Si un vehículo tiene 2 o más viajes, todos salen primero y repiten en rondas: el 2.º viaje sale cuando el
            vehículo regresó y se preparó, y atiende lo siguiente por prioridad (la columna <i>Viaje</i> muestra «2 de 3»).</p>
            <p>La pestaña <i>Algoritmos</i> muestra qué necesita cada comunidad, cuánto hay disponible, limita los campos a
            valores con sentido y permite probar cada uno por separado con los datos reales (no modifica la base).</p>

            <a name='mapa'></a><h2 style='color:#195a9e;'>7. Cómo leer el mapa</h2>
            <ul>
              <li><b>Círculo verde grande:</b> centro de acopio (anillo del color de su zona).</li>
              <li><b>Punto gris oscuro:</b> comunidad. <b>Punto gris claro:</b> ciudad de referencia.</li>
              <li><b>Línea de color:</b> ruta del viaje (un color por centro). <b>Círculo rojo numerado:</b> parada.</li>
              <li><b>Líneas de carretera:</b> color según el estado referencial de las vías de la provincia (rojo = 70 % o más
                  en mal estado, naranja = 50–69 %, verde = menos, gris = sin dato); grosor según importancia. El panel
                  derecho del mapa detalla corredores, provincias y fuentes. No es tránsito en tiempo real.</li>
            </ul>
            <p>Rueda del mouse = zoom · arrastrar = mover · doble clic = restablecer. El mapa es <b>esquemático</b>:
            las distancias son estimadas (línea recta × 1.35), no de un servicio de navegación.</p>

            <a name='reportes'></a><h2 style='color:#195a9e;'>8. Reportes</h2>
            <p><b>Excel:</b> hojas de resumen, centros, comunidades, plan, entregas y mapa. <b>PDF:</b> portada con
            logo, resumen, mapa general, tablas, itinerario de cada viaje con su mapa y pie con numeración.
            Se generan con los datos y el plan seleccionado en el momento de exportar.</p>

            <a name='reglas'></a><h2 style='color:#195a9e;'>9. Validaciones principales</h2>
            <ul>
              <li>Nombres únicos y sin caracteres no permitidos; textos con longitud máxima.</li>
              <li>Habitantes 1 – 2 000 000; prioridad 1 – 10; recursos 0.1 – 5 000 t.</li>
              <li>Coordenadas dentro del Ecuador continental y coherentes con la provincia.</li>
              <li>No se borra lo que está en uso (centros con vehículos o viajes; comunidades o vehículos con viajes vigentes).</li>
              <li>Al confirmar un plan se revalida stock, vehículos libres y necesidades dentro de una transacción.</li>
            </ul>

            <a name='datos'></a><h2 style='color:#195a9e;'>10. Datos y límites</h2>
            <p>Los datos se guardan en una base H2 en la carpeta <code>data/</code> junto a la aplicación y se conservan
            entre ejecuciones. Para usar base en memoria (se borra al cerrar) cambie la URL en
            <code>ConexionBD</code>. Las comunidades de ejemplo son referenciales; las distancias son aproximadas
            y los tiempos no consideran tráfico, clima ni estado de las vías.</p>
            <p><a href='#inicio'>Volver al inicio</a></p>
            </body></html>
            """;

    public PanelAyuda() {
        setLayout(new BorderLayout());
        setBackground(EstiloUI.FONDO_APP);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JEditorPane contenido = new JEditorPane();
        contenido.setEditorKit(new HTMLEditorKit());
        contenido.setContentType("text/html");
        contenido.setEditable(false);
        contenido.setText(HTML);
        contenido.setCaretPosition(0);
        contenido.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getDescription() != null
                    && e.getDescription().startsWith("#")) {
                contenido.scrollToReference(e.getDescription().substring(1));
            }
        });

        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(BorderFactory.createLineBorder(EstiloUI.BORDE_SUAVE, 1));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel tarjeta = EstiloUI.tarjeta("Cómo funciona el programa");
        tarjeta.add(scroll, BorderLayout.CENTER);
        add(tarjeta, BorderLayout.CENTER);
    }
}
