package uce.edu.ec.ayudahumanitaria.reportes;

import uce.edu.ec.ayudahumanitaria.dominio.CentroAcopio;
import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;
import uce.edu.ec.ayudahumanitaria.dominio.PlanDistribucion;
import uce.edu.ec.ayudahumanitaria.dominio.Vehiculo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Datos operativos que comparten los reportes Excel y PDF: centros, comunidades, flota, el plan elegido,
 * avance por comunidad y las imágenes de los mapas ya renderizadas (PNG).
 *
 * @param plan        plan a reportar; puede ser {@code null} si aún no hay ninguno
 * @param mapaGeneral PNG del mapa completo con todas las rutas del plan
 * @param mapasViaje  PNG del mapa de cada viaje, en el mismo orden que {@code plan.viajes()} (puede estar vacío)
 */
public record DatosOperativos(
        LocalDateTime generadoEn,
        List<CentroAcopio> centros,
        List<Comunidad> comunidades,
        List<Vehiculo> vehiculos,
        PlanDistribucion plan,
        Map<Integer, Double> planificadoPorComunidad,
        Map<Integer, Double> entregadoPorComunidad,
        byte[] mapaGeneral,
        List<byte[]> mapasViaje) {

    public DatosOperativos {
        centros = List.copyOf(centros);
        comunidades = List.copyOf(comunidades);
        vehiculos = List.copyOf(vehiculos);
        planificadoPorComunidad = Map.copyOf(planificadoPorComunidad);
        entregadoPorComunidad = Map.copyOf(entregadoPorComunidad);
        mapasViaje = List.copyOf(mapasViaje);
    }
}
