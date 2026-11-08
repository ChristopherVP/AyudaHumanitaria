package uce.edu.ec.ayudahumanitaria.geo;

import uce.edu.ec.ayudahumanitaria.geo.CatalogoEcuador.Provincia;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Estudio vial resumido del Ecuador continental: corredores principales, estado de la red estatal por provincia
 * y velocidades medias estimadas. Sirve para dibujar el mapa según el estado de las vías y para que el
 * cronograma de los viajes no suponga la misma velocidad en la Costa, la Sierra y la Amazonía.
 * <p>
 * <b>Importante:</b> es un estudio <i>referencial</i> hecho con cifras publicadas (ver {@link #FUENTES}); no
 * es un reporte en tiempo real. Antes de despachar un viaje conviene revisar los cierres vigentes del
 * Ministerio de Infraestructura y Transporte y del ECU 911.
 */
public final class EstudioVial {

    /** Fuentes de las cifras usadas (consultadas en noviembre de 2026). */
    public static final List<String> FUENTES = List.of(
            "Primicias (13/04/2026): estado de la red vial estatal, datos al cierre de 2025 (Ministerio de Infraestructura y Transporte).",
            "Primicias (25/03/2022): 10.264 km de red vial estatal y cierres por deslizamientos en la Amazonía, Sierra sur y Costa.",
            "Primicias (23/02/2025): cierres por lluvias y deslaves en Macas–Riobamba, Cuenca–Girón–Pasaje y la Ruta del Spondylus.");

    /** Tipo de vía según su importancia. */
    public enum Tipo {
        TRONCAL("Troncal"), PRINCIPAL("Principal"), SECUNDARIA("Secundaria / lastre");

        private final String etiqueta;

        Tipo(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String etiqueta() {
            return etiqueta;
        }
    }

    /** Corredor vial: un conjunto de tramos (pares de ciudades consecutivas del catálogo) con sus observaciones. */
    public record Corredor(String codigo, String nombre, String region, Tipo tipo, String observacion,
                           List<String[]> tramos) {
        public String titulo() {
            return codigo.isEmpty() ? nombre : nombre + " (" + codigo + ")";
        }
    }

    /** Porcentaje de vías estatales en mal estado por provincia (promedio de los últimos seis años, MIT). */
    private static final Map<String, Double> PORCENTAJE_MAL_ESTADO = Map.ofEntries(
            Map.entry("Zamora Chinchipe", 0.95), Map.entry("Sucumbíos", 0.76), Map.entry("Napo", 0.75),
            Map.entry("Morona Santiago", 0.74), Map.entry("Imbabura", 0.74), Map.entry("Carchi", 0.69),
            Map.entry("Orellana", 0.68), Map.entry("Los Ríos", 0.68), Map.entry("Azuay", 0.66));

    /** Velocidad media de referencia (km/h) por región, mezclando troncales, principales y secundarias. */
    private static final Map<String, Double> VELOCIDAD_REGION = Map.of("Costa", 58.0, "Sierra", 48.0, "Amazonía", 42.0);

    /** Cuánto baja la velocidad cuando el 100 % de las vías de la provincia está en mal estado. */
    private static final double PENALIZACION_MAXIMA = 0.30;
    private static final double VELOCIDAD_MINIMA = 25.0;
    /** Velocidad "nacional" de referencia: la que usan los parámetros de simulación por defecto. */
    public static final double VELOCIDAD_REFERENCIA = 50.0;

    private static final List<Corredor> CORREDORES = List.of(
            corredor("E35", "Panamericana", "Sierra", Tipo.TRONCAL,
                    "Eje norte–sur de la Sierra. Cruza Carchi (69 %), Imbabura (74 %) y Azuay (66 %), provincias con muchas "
                            + "vías estatales en mal estado; en páramo y curvas la velocidad real baja.",
                    "Tulcán>San Gabriel>Mira>Ibarra>Otavalo>Cayambe>Quito>Machachi>Latacunga>Salcedo>Ambato>Riobamba"
                            + ">Guamote>Alausí>Cañar>Azogues>Cuenca>Girón>Saraguro>Loja>Catamayo>Cariamanga>Macará"),
            corredor("E15", "Ruta del Spondylus y costa norte", "Costa", Tipo.TRONCAL,
                    "Corredor costero. Deslizamientos en la entrada a Manglaralto con tránsito controlado (feb. 2025); "
                            + "en invierno hay tramos de Manabí con deterioro.",
                    "Esmeraldas>Atacames>Muisne", "Pedernales>Jama>Bahía de Caráquez", "Portoviejo>Manta",
                    "Portoviejo>Jipijapa>Santa Elena>La Libertad>Salinas", "Santo Domingo>Pedernales"),
            corredor("E45", "Troncal Amazónica", "Amazonía", Tipo.TRONCAL,
                    "Cruza Sucumbíos (76 %), Orellana (68 %), Napo (75 %), Morona Santiago (74 %) y Zamora Chinchipe (95 %): "
                            + "las provincias con más vías estatales en mal estado. Derrumbes frecuentes con lluvias.",
                    "Nueva Loja>Joya de los Sachas>Coca>Loreto>Tena>Puyo>Macas>Sucúa>Méndez>Limón>Gualaquiza"
                            + ">El Pangui>Yantzaza>Zamora"),
            corredor("", "Quito – Baeza – Nueva Loja y Baeza – Tena", "Amazonía", Tipo.PRINCIPAL,
                    "Acceso principal al oriente norte. El sector Piedra Fina (Napo) estuvo cerrado por deslizamientos en 2022.",
                    "Quito>Baeza>Lumbaqui>Nueva Loja", "Baeza>Tena", "Salcedo>Tena", "Ambato>Pelileo>Baños>Puyo",
                    "Riobamba>Macas", "Zamora>Loja", "Gualaceo>Limón"),
            corredor("", "Cruces Sierra – Costa", "Sierra", Tipo.PRINCIPAL,
                    "Cuenca–Molleturo–Naranjal y Cuenca–Girón–Pasaje han tenido cierres por deslizamientos en época lluviosa "
                            + "(2022 y 2025); Cotopaxi y Bolívar también tienen tramos de montaña.",
                    "Cuenca>Girón>Pasaje", "Quito>Santo Domingo", "Latacunga>Pujilí>La Maná>Quevedo",
                    "Ambato>Guaranda>Babahoyo", "Guaranda>Riobamba", "Cañar>La Troncal"),
            corredor("", "Red troncal de la Costa", "Costa", Tipo.PRINCIPAL,
                    "Vías planas y más rápidas. Los Ríos tiene 68 % de sus vías estatales en mal estado; en invierno hay "
                            + "inundaciones locales.",
                    "Santo Domingo>Quevedo>Ventanas>Babahoyo>Guayaquil", "Guayaquil>Milagro>El Triunfo>Naranjal>Pasaje>Machala"
                            + ">Santa Rosa>Huaquillas", "Santo Domingo>Quinindé>Esmeraldas", "Guayaquil>Santa Elena",
                    "Guayaquil>Daule>Balzar>Vinces>Quevedo", "Esmeraldas>Borbón>San Lorenzo>Ibarra",
                    "El Triunfo>La Troncal", "Machala>Zaruma"));

    private EstudioVial() {
    }

    private static Corredor corredor(String codigo, String nombre, String region, Tipo tipo, String observacion,
                                     String... cadenas) {
        List<String[]> tramos = new ArrayList<>();
        for (String cadena : cadenas) {
            String[] ciudades = cadena.split(">");
            for (int i = 0; i + 1 < ciudades.length; i++) {
                tramos.add(new String[]{ciudades[i], ciudades[i + 1]});
            }
        }
        return new Corredor(codigo, nombre, region, tipo, observacion, List.copyOf(tramos));
    }

    public static List<Corredor> corredores() {
        return CORREDORES;
    }

    /** Corredor al que pertenece el tramo entre dos ciudades (sin importar el sentido), si existe. */
    public static Optional<Corredor> corredorDelTramo(String desde, String hasta) {
        for (Corredor c : CORREDORES) {
            for (String[] t : c.tramos()) {
                if ((t[0].equals(desde) && t[1].equals(hasta)) || (t[0].equals(hasta) && t[1].equals(desde))) {
                    return Optional.of(c);
                }
            }
        }
        return Optional.empty();
    }

    /** Tipo de vía del tramo; los que no pertenecen a ningún corredor se consideran secundarios. */
    public static Tipo tipoDelTramo(String desde, String hasta) {
        return corredorDelTramo(desde, hasta).map(Corredor::tipo).orElse(Tipo.SECUNDARIA);
    }

    /** Fracción (0–1) de vías estatales en mal estado de la provincia; {@code NaN} si no hay dato publicado. */
    public static double porcentajeMalEstado(String provincia) {
        return PORCENTAJE_MAL_ESTADO.getOrDefault(provincia, Double.NaN);
    }

    /** Provincias con dato publicado, de la peor a la mejor. */
    public static List<Map.Entry<String, Double>> provinciasPorEstado() {
        List<Map.Entry<String, Double>> lista = new ArrayList<>(PORCENTAJE_MAL_ESTADO.entrySet());
        lista.sort((a, b) -> {
            int porValor = Double.compare(b.getValue(), a.getValue());
            return porValor != 0 ? porValor : a.getKey().compareTo(b.getKey());
        });
        return lista;
    }

    /** Estado del tramo entre dos ciudades según las provincias que une (promedio de las que tienen dato). */
    public static double malEstadoDelTramo(String ciudadA, String ciudadB) {
        double a = porcentajeMalEstado(provinciaDeCiudad(ciudadA));
        double b = porcentajeMalEstado(provinciaDeCiudad(ciudadB));
        if (Double.isNaN(a)) {
            return b;
        }
        return Double.isNaN(b) ? a : (a + b) / 2;
    }

    private static String provinciaDeCiudad(String ciudad) {
        CatalogoEcuador.Ciudad c = CatalogoEcuador.ciudad(ciudad);
        return c == null ? "" : GeoEcuador.provinciaDe(c.latitud(), c.longitud()).nombre();
    }

    /** Velocidad media (km/h) de referencia de una región. */
    public static double velocidadRegionKmH(String region) {
        return VELOCIDAD_REGION.getOrDefault(region, VELOCIDAD_REFERENCIA);
    }

    /** Velocidad media estimada en un punto: la de su región, reducida si su provincia tiene vías en mal estado. */
    public static double velocidadEnPuntoKmH(double latitud, double longitud) {
        Provincia provincia = GeoEcuador.provinciaDe(latitud, longitud);
        double base = velocidadRegionKmH(provincia.region());
        double malo = porcentajeMalEstado(provincia.nombre());
        double factor = Double.isNaN(malo) ? 1.0 : 1.0 - PENALIZACION_MAXIMA * malo;
        return Math.max(VELOCIDAD_MINIMA, base * factor);
    }

    /** Velocidad media estimada de un tramo: promedio de la velocidad en sus dos extremos. */
    public static double velocidadKmH(double lat1, double lon1, double lat2, double lon2) {
        return (velocidadEnPuntoKmH(lat1, lon1) + velocidadEnPuntoKmH(lat2, lon2)) / 2.0;
    }

    /** Tiempo de manejo estimado (horas) para recorrer {@code km} entre dos puntos. */
    public static double horasDeManejo(double km, double lat1, double lon1, double lat2, double lon2) {
        return km / velocidadKmH(lat1, lon1, lat2, lon2);
    }

    /** Descripción del estado de las vías de la provincia para mostrar en pantalla. */
    public static String descripcionEstado(String provincia) {
        double malo = porcentajeMalEstado(provincia);
        if (Double.isNaN(malo)) {
            return "sin dato publicado";
        }
        return String.format(Locale.US, "%.0f %% de vías estatales en mal estado", malo * 100);
    }
}
