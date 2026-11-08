package uce.edu.ec.ayudahumanitaria.geo;

import java.util.List;

/**
 * Catálogo geográfico (datos fijos) del Ecuador continental: provincias, ciudades de referencia y la
 * red vial principal que se dibuja como fondo del mapa esquemático.
 * <p>
 * Las coordenadas son aproximadas (precisión de calle no requerida): sirven para validar ubicaciones,
 * calcular distancias estimadas y dibujar el mapa. Galápagos queda fuera porque no tiene conexión terrestre.
 */
public final class CatalogoEcuador {

    private CatalogoEcuador() {
    }

    /** Ciudad de referencia (nodo gris del mapa). */
    public record Ciudad(String nombre, double latitud, double longitud) {
    }

    /** Tramo de carretera entre dos ciudades del catálogo (por nombre). */
    public record Carretera(String desde, String hasta) {
    }

    /** Provincia con su centro aproximado y un radio máximo (km) para validar coherencia de ubicaciones. */
    public record Provincia(String nombre, String region, double latitud, double longitud, double radioKm) {
    }

    // ------------------------------------------------------------------ provincias

    public static final List<Provincia> PROVINCIAS = List.of(
            new Provincia("Esmeraldas", "Costa", 0.80, -79.30, 125),
            new Provincia("Manabí", "Costa", -0.95, -80.20, 150),
            new Provincia("Santo Domingo de los Tsáchilas", "Costa", -0.30, -79.20, 55),
            new Provincia("Los Ríos", "Costa", -1.50, -79.50, 95),
            new Provincia("Guayas", "Costa", -2.20, -79.90, 140),
            new Provincia("Santa Elena", "Costa", -2.20, -80.70, 75),
            new Provincia("El Oro", "Costa", -3.40, -79.80, 95),
            new Provincia("Carchi", "Sierra", 0.75, -77.95, 65),
            new Provincia("Imbabura", "Sierra", 0.35, -78.20, 65),
            new Provincia("Pichincha", "Sierra", -0.20, -78.60, 95),
            new Provincia("Cotopaxi", "Sierra", -0.80, -78.80, 75),
            new Provincia("Tungurahua", "Sierra", -1.25, -78.70, 55),
            new Provincia("Bolívar", "Sierra", -1.70, -79.00, 60),
            new Provincia("Chimborazo", "Sierra", -2.00, -78.75, 80),
            new Provincia("Cañar", "Sierra", -2.60, -78.90, 65),
            new Provincia("Azuay", "Sierra", -3.00, -79.00, 100),
            new Provincia("Loja", "Sierra", -4.00, -79.60, 130),
            new Provincia("Sucumbíos", "Amazonía", 0.10, -76.70, 125),
            new Provincia("Orellana", "Amazonía", -0.60, -76.90, 145),
            new Provincia("Napo", "Amazonía", -0.90, -77.80, 85),
            new Provincia("Pastaza", "Amazonía", -1.70, -77.60, 150),
            new Provincia("Morona Santiago", "Amazonía", -2.50, -78.00, 155),
            new Provincia("Zamora Chinchipe", "Amazonía", -4.00, -78.80, 115)
    );

    // ------------------------------------------------------------------ ciudades

    public static final List<Ciudad> CIUDADES = List.of(
            // Costa
            new Ciudad("Esmeraldas", 0.9592, -79.6539),
            new Ciudad("Atacames", 0.8695, -79.8399),
            new Ciudad("Muisne", 0.6000, -80.0333),
            new Ciudad("Borbón", 1.0951, -78.9964),
            new Ciudad("San Lorenzo", 1.2886, -78.8359),
            new Ciudad("Quinindé", 0.3236, -79.4733),
            new Ciudad("Santo Domingo", -0.2520, -79.1720),
            new Ciudad("El Carmen", -0.2667, -79.4333),
            new Ciudad("Pedernales", 0.0717, -80.0522),
            new Ciudad("Jama", -0.2000, -80.2667),
            new Ciudad("Chone", -0.6833, -80.0950),
            new Ciudad("Calceta", -0.8451, -80.1647),
            new Ciudad("Bahía de Caráquez", -0.6000, -80.4167),
            new Ciudad("Manta", -0.9677, -80.7089),
            new Ciudad("Portoviejo", -1.0546, -80.4545),
            new Ciudad("Rocafuerte", -0.9167, -80.4500),
            new Ciudad("Jipijapa", -1.3486, -80.5786),
            new Ciudad("Quevedo", -1.0225, -79.4604),
            new Ciudad("Ventanas", -1.4500, -79.4667),
            new Ciudad("Vinces", -1.5556, -79.7500),
            new Ciudad("Balzar", -1.3667, -79.9000),
            new Ciudad("Babahoyo", -1.8022, -79.5342),
            new Ciudad("Daule", -1.8620, -79.9780),
            new Ciudad("Guayaquil", -2.1894, -79.8891),
            new Ciudad("Milagro", -2.1347, -79.5878),
            new Ciudad("El Triunfo", -2.2917, -79.3833),
            new Ciudad("La Troncal", -2.4228, -79.3378),
            new Ciudad("Naranjal", -2.6711, -79.6161),
            new Ciudad("Playas", -2.6310, -80.3890),
            new Ciudad("Santa Elena", -2.2267, -80.8590),
            new Ciudad("La Libertad", -2.2333, -80.9000),
            new Ciudad("Salinas", -2.2167, -80.9500),
            new Ciudad("Machala", -3.2581, -79.9554),
            new Ciudad("Pasaje", -3.3256, -79.8044),
            new Ciudad("Santa Rosa", -3.4486, -79.9603),
            new Ciudad("Huaquillas", -3.4767, -80.2306),
            new Ciudad("Zaruma", -3.6906, -79.6117),
            // Sierra
            new Ciudad("Tulcán", 0.8119, -77.7173),
            new Ciudad("San Gabriel", 0.5976, -77.8240),
            new Ciudad("Mira", 0.5522, -78.0775),
            new Ciudad("Ibarra", 0.3517, -78.1223),
            new Ciudad("Otavalo", 0.2342, -78.2610),
            new Ciudad("Cayambe", 0.0417, -78.1458),
            new Ciudad("Quito", -0.1807, -78.4678),
            new Ciudad("Machachi", -0.5086, -78.5667),
            new Ciudad("Latacunga", -0.9314, -78.6155),
            new Ciudad("Pujilí", -0.9500, -78.6833),
            new Ciudad("La Maná", -0.9417, -79.2333),
            new Ciudad("Salcedo", -1.0467, -78.5900),
            new Ciudad("Ambato", -1.2417, -78.6197),
            new Ciudad("Pelileo", -1.3300, -78.5500),
            new Ciudad("Baños", -1.3964, -78.4247),
            new Ciudad("Riobamba", -1.6636, -78.6546),
            new Ciudad("Guaranda", -1.5905, -79.0014),
            new Ciudad("Guamote", -1.9333, -78.7167),
            new Ciudad("Alausí", -2.2000, -78.8472),
            new Ciudad("Cañar", -2.5588, -78.9386),
            new Ciudad("Azogues", -2.7397, -78.8486),
            new Ciudad("Cuenca", -2.9001, -79.0059),
            new Ciudad("Gualaceo", -2.8917, -78.7761),
            new Ciudad("Girón", -3.1581, -79.1486),
            new Ciudad("Saraguro", -3.6167, -79.2333),
            new Ciudad("Loja", -3.9931, -79.2042),
            new Ciudad("Catamayo", -3.9833, -79.3500),
            new Ciudad("Cariamanga", -4.3333, -79.5500),
            new Ciudad("Macará", -4.3833, -79.9500),
            // Amazonía
            new Ciudad("Nueva Loja", 0.0847, -76.8950),
            new Ciudad("Lumbaqui", 0.0500, -77.3300),
            new Ciudad("Shushufindi", -0.1833, -76.6500),
            new Ciudad("Joya de los Sachas", -0.3000, -76.8500),
            new Ciudad("Coca", -0.4667, -76.9833),
            new Ciudad("Loreto", -0.7167, -77.3000),
            new Ciudad("Baeza", -0.4642, -77.8869),
            new Ciudad("Tena", -0.9933, -77.8133),
            new Ciudad("Puyo", -1.4860, -78.0030),
            new Ciudad("Macas", -2.3087, -78.1167),
            new Ciudad("Sucúa", -2.4667, -78.1667),
            new Ciudad("Méndez", -2.7333, -78.3167),
            new Ciudad("Limón", -2.9667, -78.4167),
            new Ciudad("Gualaquiza", -3.4000, -78.5667),
            new Ciudad("El Pangui", -3.6167, -78.5667),
            new Ciudad("Yantzaza", -3.8333, -78.7500),
            new Ciudad("Zamora", -4.0692, -78.9567)
    );

    // ------------------------------------------------------------------ red vial (fondo del mapa)

    public static final List<Carretera> CARRETERAS = List.of(
            // Panamericana (E35), de norte a sur
            new Carretera("Tulcán", "San Gabriel"),
            new Carretera("San Gabriel", "Mira"),
            new Carretera("Mira", "Ibarra"),
            new Carretera("Ibarra", "Otavalo"),
            new Carretera("Otavalo", "Cayambe"),
            new Carretera("Cayambe", "Quito"),
            new Carretera("Quito", "Machachi"),
            new Carretera("Machachi", "Latacunga"),
            new Carretera("Latacunga", "Salcedo"),
            new Carretera("Salcedo", "Ambato"),
            new Carretera("Ambato", "Riobamba"),
            new Carretera("Riobamba", "Guamote"),
            new Carretera("Guamote", "Alausí"),
            new Carretera("Alausí", "Cañar"),
            new Carretera("Cañar", "Azogues"),
            new Carretera("Azogues", "Cuenca"),
            new Carretera("Cuenca", "Girón"),
            new Carretera("Girón", "Saraguro"),
            new Carretera("Saraguro", "Loja"),
            new Carretera("Loja", "Catamayo"),
            new Carretera("Catamayo", "Cariamanga"),
            new Carretera("Cariamanga", "Macará"),
            // Norte y costa norte
            new Carretera("Ibarra", "San Lorenzo"),
            new Carretera("San Lorenzo", "Borbón"),
            new Carretera("Borbón", "Esmeraldas"),
            new Carretera("Esmeraldas", "Atacames"),
            new Carretera("Atacames", "Muisne"),
            new Carretera("Esmeraldas", "Quinindé"),
            new Carretera("Quinindé", "Santo Domingo"),
            new Carretera("Quito", "Santo Domingo"),
            new Carretera("Santo Domingo", "El Carmen"),
            new Carretera("Santo Domingo", "Quevedo"),
            new Carretera("Santo Domingo", "Pedernales"),
            new Carretera("Pedernales", "Jama"),
            new Carretera("Jama", "Bahía de Caráquez"),
            new Carretera("El Carmen", "Chone"),
            new Carretera("Chone", "Bahía de Caráquez"),
            new Carretera("Chone", "Calceta"),
            new Carretera("Calceta", "Portoviejo"),
            new Carretera("Portoviejo", "Rocafuerte"),
            new Carretera("Portoviejo", "Manta"),
            new Carretera("Portoviejo", "Jipijapa"),
            new Carretera("Jipijapa", "Santa Elena"),
            // Litoral central y sur
            new Carretera("Quevedo", "Ventanas"),
            new Carretera("Ventanas", "Babahoyo"),
            new Carretera("Quevedo", "Vinces"),
            new Carretera("Vinces", "Babahoyo"),
            new Carretera("Vinces", "Balzar"),
            new Carretera("Balzar", "Daule"),
            new Carretera("Daule", "Guayaquil"),
            new Carretera("Babahoyo", "Guayaquil"),
            new Carretera("Guayaquil", "Milagro"),
            new Carretera("Milagro", "El Triunfo"),
            new Carretera("El Triunfo", "La Troncal"),
            new Carretera("La Troncal", "Cañar"),
            new Carretera("El Triunfo", "Naranjal"),
            new Carretera("Naranjal", "Pasaje"),
            new Carretera("Pasaje", "Machala"),
            new Carretera("Pasaje", "Girón"),
            new Carretera("Machala", "Santa Rosa"),
            new Carretera("Santa Rosa", "Huaquillas"),
            new Carretera("Machala", "Zaruma"),
            new Carretera("Guayaquil", "Playas"),
            new Carretera("Guayaquil", "Santa Elena"),
            new Carretera("Santa Elena", "La Libertad"),
            new Carretera("La Libertad", "Salinas"),
            // Cordillera central
            new Carretera("Latacunga", "Pujilí"),
            new Carretera("Pujilí", "La Maná"),
            new Carretera("La Maná", "Quevedo"),
            new Carretera("Ambato", "Pelileo"),
            new Carretera("Pelileo", "Baños"),
            new Carretera("Ambato", "Guaranda"),
            new Carretera("Guaranda", "Babahoyo"),
            new Carretera("Guaranda", "Riobamba"),
            new Carretera("Cuenca", "Gualaceo"),
            // Oriente
            new Carretera("Quito", "Baeza"),
            new Carretera("Baeza", "Tena"),
            new Carretera("Baeza", "Lumbaqui"),
            new Carretera("Lumbaqui", "Nueva Loja"),
            new Carretera("Nueva Loja", "Shushufindi"),
            new Carretera("Nueva Loja", "Joya de los Sachas"),
            new Carretera("Shushufindi", "Joya de los Sachas"),
            new Carretera("Joya de los Sachas", "Coca"),
            new Carretera("Coca", "Loreto"),
            new Carretera("Loreto", "Tena"),
            new Carretera("Salcedo", "Tena"),
            new Carretera("Baños", "Puyo"),
            new Carretera("Tena", "Puyo"),
            new Carretera("Puyo", "Macas"),
            new Carretera("Riobamba", "Macas"),
            new Carretera("Macas", "Sucúa"),
            new Carretera("Sucúa", "Méndez"),
            new Carretera("Méndez", "Limón"),
            new Carretera("Gualaceo", "Limón"),
            new Carretera("Limón", "Gualaquiza"),
            new Carretera("Gualaquiza", "El Pangui"),
            new Carretera("El Pangui", "Yantzaza"),
            new Carretera("Yantzaza", "Zamora"),
            new Carretera("Zamora", "Loja")
    );

    // ------------------------------------------------------------------ búsquedas

    /** Devuelve la ciudad del catálogo con ese nombre, o {@code null} si no existe. */
    public static Ciudad ciudad(String nombre) {
        for (Ciudad c : CIUDADES) {
            if (c.nombre().equalsIgnoreCase(nombre)) {
                return c;
            }
        }
        return null;
    }

    /** Devuelve la provincia con ese nombre (sin distinguir mayúsculas), o {@code null} si no existe. */
    public static Provincia provincia(String nombre) {
        if (nombre == null) {
            return null;
        }
        for (Provincia p : PROVINCIAS) {
            if (p.nombre().equalsIgnoreCase(nombre.trim())) {
                return p;
            }
        }
        return null;
    }

    public static String[] nombresProvincias() {
        String[] nombres = new String[PROVINCIAS.size()];
        for (int i = 0; i < nombres.length; i++) {
            nombres[i] = PROVINCIAS.get(i).nombre();
        }
        return nombres;
    }
}
