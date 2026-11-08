package uce.edu.ec.ayudahumanitaria.util;

import uce.edu.ec.ayudahumanitaria.dominio.Parada;
import uce.edu.ec.ayudahumanitaria.dominio.Viaje;
import uce.edu.ec.ayudahumanitaria.geo.CatalogoEcuador.Ciudad;
import uce.edu.ec.ayudahumanitaria.geo.GeoEcuador;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Textos de fechas, duraciones e itinerarios compartidos por la interfaz y los reportes. */
public final class FormatoPlan {

    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FormatoPlan() {
    }

    public static String fechaHora(LocalDateTime fecha) {
        return fecha == null ? "—" : FECHA_HORA.format(fecha);
    }

    public static String duracion(Duration d) {
        long minutos = d.toMinutes();
        long dias = minutos / (24 * 60);
        long horas = (minutos % (24 * 60)) / 60;
        long resto = minutos % 60;
        String texto = horas + " h " + resto + " min";
        return dias > 0 ? dias + " d " + texto : texto;
    }

    public static String numero(double valor, int decimales) {
        return String.format(Locale.US, "%." + decimales + "f", valor);
    }

    /** Ciudades del catálogo por las que pasa aproximadamente el tramo (solo orientativo). */
    public static List<String> ciudadesDelTramo(double lat1, double lon1, double lat2, double lon2) {
        List<String> nombres = new ArrayList<>();
        for (Ciudad c : GeoEcuador.ciudadesEnElTramo(lat1, lon1, lat2, lon2, 14)) {
            nombres.add(c.nombre());
        }
        return nombres;
    }

    /** Itinerario completo de un viaje: salida, cada parada con su hora y el regreso al centro. */
    public static String itinerario(Viaje v) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Viaje %s · %s · vehículo %s%n", v.id() == 0 ? "(simulado)" : String.valueOf(v.id()),
                v.centro().getNombre(), v.vehiculoId()));
        sb.append(String.format("Carga: %s t de %s t (%s %%) · Distancia: %s km · Duración: %s%n",
                numero(v.cargaToneladas(), 1), numero(v.capacidadToneladas(), 1), numero(v.ocupacion() * 100, 0),
                numero(v.distanciaKm(), 0), duracion(v.duracion())));
        sb.append("Salida de ").append(v.centro().getCiudad()).append(": ").append(fechaHora(v.salida())).append("\n");
        double latAnterior = v.centro().getLatitud();
        double lonAnterior = v.centro().getLongitud();
        for (Parada p : v.paradas()) {
            List<String> pasa = ciudadesDelTramo(latAnterior, lonAnterior, p.latitud(), p.longitud());
            sb.append(String.format("  %d. %s (%s) · %s km del tramo (%s km acum.)%n", p.orden(), p.comunidadNombre(),
                    p.provincia(), numero(p.kmTramo(), 0), numero(p.kmAcumulados(), 0)));
            if (!pasa.isEmpty()) {
                sb.append("     pasa cerca de: ").append(String.join(", ", pasa)).append("\n");
            }
            sb.append(String.format("     llega %s · entrega %s t · sale %s%n", fechaHora(p.llegada()),
                    numero(p.toneladas(), 1), fechaHora(p.salida())));
            latAnterior = p.latitud();
            lonAnterior = p.longitud();
        }
        sb.append("Regreso a ").append(v.centro().getCiudad()).append(": ").append(fechaHora(v.regreso())).append("\n");
        return sb.toString();
    }

    /** Comunidades de un viaje separadas por flechas. */
    public static String ruta(Viaje v) {
        return v.paradas().stream().map(Parada::comunidadNombre).collect(Collectors.joining(" → "));
    }
}
