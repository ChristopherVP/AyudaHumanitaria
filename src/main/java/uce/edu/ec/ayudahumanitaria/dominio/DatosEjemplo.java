package uce.edu.ec.ayudahumanitaria.dominio;

import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;

import java.util.List;

/**
 * Datos de ejemplo para la primera ejecución (referenciales: ajústalos con los datos reales).
 * Centros: Quito, Guayaquil y Cuenca, más dos centros en la Amazonía (Nueva Loja al norte y Macas al sur)
 * para cubrir el oriente, que queda lejos de los centros de la Sierra y la Costa.
 */
public class DatosEjemplo {

    private DatosEjemplo() {
    }

    public static void cargarSiVacio(GestorCentros gestorCentros, GestorComunidades gestorComunidades,
                                     GestorVehiculos gestorVehiculos)
            throws ValidationException, PersistenciaException {

        if (gestorCentros.listar().isEmpty()) {
            gestorCentros.registrar("Centro Quito", "Quito", "Pichincha", -0.1807, -78.4678, 150);
            gestorCentros.registrar("Centro Guayaquil", "Guayaquil", "Guayas", -2.1894, -79.8891, 180);
            gestorCentros.registrar("Centro Cuenca", "Cuenca", "Azuay", -2.9001, -79.0059, 120);
            gestorCentros.registrar("Centro Amazonía Norte (Nueva Loja)", "Nueva Loja", "Sucumbíos", 0.0847, -76.8950, 90);
            gestorCentros.registrar("Centro Amazonía Sur (Macas)", "Macas", "Morona Santiago", -2.3087, -78.1167, 90);
        }

        if (gestorVehiculos.listarTodos().length == 0) {
            List<CentroAcopio> centros = gestorCentros.listar();
            registrarFlota(gestorVehiculos, centroPorCiudad(centros, "Quito"), "UIO", 12000, 8000, 5000);
            registrarFlota(gestorVehiculos, centroPorCiudad(centros, "Guayaquil"), "GYE", 15000, 12000, 8000, 5000);
            registrarFlota(gestorVehiculos, centroPorCiudad(centros, "Cuenca"), "CUE", 10000, 6000);
            registrarFlota(gestorVehiculos, centroPorCiudad(centros, "Nueva Loja"), "NLJ", 8000, 5000);
            registrarFlota(gestorVehiculos, centroPorCiudad(centros, "Macas"), "MCS", 8000, 5000);
        }

        if (gestorComunidades.listarTodas().length == 0) {
            // nombre, provincia, habitantes, prioridad (1-10), toneladas requeridas, latitud, longitud
            Object[][] datos = {
                    {"Borbón", "Esmeraldas", 9000, 8, 9.0, 1.0951, -78.9964},
                    {"Muisne", "Esmeraldas", 7500, 9, 8.0, 0.6000, -80.0333},
                    {"Pedernales", "Manabí", 12000, 9, 11.0, 0.0717, -80.0522},
                    {"Jama", "Manabí", 6000, 8, 6.0, -0.2000, -80.2667},
                    {"Jipijapa", "Manabí", 15000, 6, 12.0, -1.3486, -80.5786},
                    {"Manglaralto", "Santa Elena", 5000, 5, 4.0, -1.8500, -80.7500},
                    {"Playas", "Guayas", 9000, 4, 6.0, -2.6310, -80.3890},
                    {"Naranjal", "Guayas", 11000, 5, 8.0, -2.6711, -79.6161},
                    {"Vinces", "Los Ríos", 10000, 5, 7.0, -1.5556, -79.7500},
                    {"Zaruma", "El Oro", 6500, 4, 5.0, -3.6906, -79.6117},
                    {"Macará", "Loja", 8000, 6, 6.0, -4.3833, -79.9500},
                    {"Saraguro", "Loja", 7000, 5, 5.0, -3.6167, -79.2333},
                    {"Nabón", "Azuay", 4500, 6, 4.0, -3.3333, -79.0667},
                    {"Sígsig", "Azuay", 6000, 5, 4.5, -3.0500, -78.7833},
                    {"El Tambo", "Cañar", 4000, 5, 3.5, -2.4167, -78.9333},
                    {"Alausí", "Chimborazo", 7000, 7, 6.0, -2.2000, -78.8472},
                    {"Colta", "Chimborazo", 9000, 8, 8.0, -1.7167, -78.7500},
                    {"Chillanes", "Bolívar", 5000, 6, 4.0, -1.9667, -79.0500},
                    {"Sigchos", "Cotopaxi", 6500, 7, 5.5, -0.7000, -78.8833},
                    {"Cayambe", "Pichincha", 14000, 5, 9.0, 0.0417, -78.1458},
                    {"Pedro Vicente Maldonado", "Pichincha", 5500, 5, 4.0, 0.0833, -79.0500},
                    {"Intag (Apuela)", "Imbabura", 3500, 6, 3.0, 0.3500, -78.5000},
                    {"El Ángel", "Carchi", 4000, 5, 3.0, 0.6167, -77.9333},
                    {"Tarapoa", "Sucumbíos", 3500, 8, 3.5, 0.0167, -76.3333},
                    {"Shushufindi", "Sucumbíos", 16000, 6, 11.0, -0.1833, -76.6500},
                    {"Joya de los Sachas", "Orellana", 9000, 6, 6.0, -0.3000, -76.8500},
                    {"Loreto", "Orellana", 6000, 7, 5.0, -0.7167, -77.3000},
                    {"Archidona", "Napo", 7000, 6, 5.0, -0.9167, -77.8000},
                    {"Sarayaku", "Pastaza", 1200, 10, 2.5, -1.7333, -77.4833},
                    {"Taisha", "Morona Santiago", 4000, 9, 4.5, -2.3833, -77.5000},
                    {"Gualaquiza", "Morona Santiago", 6500, 6, 5.0, -3.4000, -78.5667},
                    {"Yantzaza", "Zamora Chinchipe", 7500, 5, 5.5, -3.8333, -78.7500}
            };
            for (Object[] d : datos) {
                gestorComunidades.registrar((String) d[0], (String) d[1], (Integer) d[2], (Integer) d[3],
                        (Double) d[4], (Double) d[5], (Double) d[6]);
            }
        }
    }

    private static CentroAcopio centroPorCiudad(List<CentroAcopio> centros, String ciudad) {
        for (CentroAcopio c : centros) {
            if (c.getCiudad().equalsIgnoreCase(ciudad)) {
                return c;
            }
        }
        return null;
    }

    private static void registrarFlota(GestorVehiculos gestor, CentroAcopio centro, String prefijo,
                                       double... capacidadesKg) throws ValidationException, PersistenciaException {
        if (centro == null) {
            return;
        }
        for (int i = 0; i < capacidadesKg.length; i++) {
            gestor.registrar(String.format("%s-%03d", prefijo, i + 1), capacidadesKg[i], centro.getId());
        }
    }
}
