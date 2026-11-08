package uce.edu.ec.ayudahumanitaria.dominio;

import uce.edu.ec.ayudahumanitaria.persistencia.CentroDAO;
import uce.edu.ec.ayudahumanitaria.persistencia.ComunidadDAO;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.persistencia.PlanDAO;
import uce.edu.ec.ayudahumanitaria.persistencia.VehiculoDAO;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;
import uce.edu.ec.ayudahumanitaria.util.Validador;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquesta la simulación y el registro de planes de distribución: lee los datos reales de la base,
 * ejecuta el {@link PlanificadorDistribucion}, y al confirmar guarda el plan y descuenta el stock.
 */
public class GestorPlanes {

    private final PlanDAO planDao = new PlanDAO();
    private final CentroDAO centroDao = new CentroDAO();
    private final ComunidadDAO comunidadDao = new ComunidadDAO();
    private final VehiculoDAO vehiculoDao = new VehiculoDAO();
    private final ParametrosSimulacion parametros;

    public GestorPlanes() {
        this(ParametrosSimulacion.porDefecto());
    }

    public GestorPlanes(ParametrosSimulacion parametros) {
        this.parametros = parametros;
    }

    public ParametrosSimulacion getParametros() {
        return parametros;
    }

    /** Calcula (sin guardar) el plan con toda la flota; útil para pruebas y reportes rápidos. */
    public PlanDistribucion simular(LocalDateTime salida) throws ValidationException, PersistenciaException {
        return calcular(salida, null);
    }

    /**
     * Calcula (sin guardar) el plan para la hora de salida indicada usando solo los vehículos y viajes elegidos.
     *
     * @throws ValidationException si no se eligió ningún vehículo o alguno no existe
     */
    public PlanDistribucion simular(LocalDateTime salida, SeleccionEnvio seleccion)
            throws ValidationException, PersistenciaException {
        if (seleccion == null || seleccion.estaVacia()) {
            throw new ValidationException("Elija al menos un vehículo (con 1 o más viajes) para planificar.");
        }
        Map<String, Vehiculo> existentes = new HashMap<>();
        for (Vehiculo v : vehiculoDao.listarTodos()) {
            existentes.put(v.getId(), v);
        }
        for (String id : seleccion.viajesPorVehiculo().keySet()) {
            Vehiculo v = existentes.get(id);
            if (v == null) {
                throw new ValidationException("El vehículo " + id + " ya no existe.");
            }
            if (v.getCentroId() == 0) {
                throw new ValidationException("El vehículo " + id + " no pertenece a ningún centro.");
            }
        }
        return calcular(salida, seleccion);
    }

    /** Simulación rápida: propone vehículos con {@link RecomendadorEnvio} y calcula el plan que saldría en 30 min. */
    public ResultadoRapido simulacionRapida() throws ValidationException, PersistenciaException {
        RecomendadorEnvio.Recomendacion recomendacion = recomendar();
        if (recomendacion.seleccion().estaVacia()) {
            throw new ValidationException("No hay necesidades pendientes con stock y vehículos para simular.");
        }
        LocalDateTime salida = LocalDateTime.now().plusMinutes(30).withSecond(0).withNano(0);
        return new ResultadoRapido(recomendacion, simular(salida, recomendacion.seleccion()));
    }

    public record ResultadoRapido(RecomendadorEnvio.Recomendacion recomendacion, PlanDistribucion plan) {
    }

    /** Situación actual por centro: necesidades de sus comunidades, stock y flota disponible. */
    public List<RecomendadorEnvio.ResumenCentro> resumenPorCentro() throws PersistenciaException {
        sincronizar();
        return RecomendadorEnvio.resumenPorCentro(centroDao.listarTodos(),
                comunidadDao.listarTodas().toArray(new Comunidad[0]), planDao.toneladasPlanificadas(),
                vehiculoDao.listarTodos().toArray(new Vehiculo[0]));
    }

    public RecomendadorEnvio.Recomendacion recomendar() throws PersistenciaException {
        return RecomendadorEnvio.recomendar(resumenPorCentro());
    }

    private PlanDistribucion calcular(LocalDateTime salida, SeleccionEnvio seleccion)
            throws ValidationException, PersistenciaException {
        LocalDateTime ahora = LocalDateTime.now();
        sincronizar();
        Validador.validarFechaSalida(salida, ahora);

        List<CentroAcopio> centros = centroDao.listarTodos();
        if (centros.isEmpty()) {
            throw new ValidationException("Registre al menos un centro de acopio.");
        }
        Comunidad[] comunidades = comunidadDao.listarTodas().toArray(new Comunidad[0]);
        if (comunidades.length == 0) {
            throw new ValidationException("Registre al menos una comunidad.");
        }
        Vehiculo[] vehiculos = vehiculoDao.listarTodos().toArray(new Vehiculo[0]);

        Map<String, LocalDateTime> libreDesde = new HashMap<>();
        long segundosPreparacion = Math.round(parametros.preparacionHoras() * 3600);
        for (Map.Entry<String, LocalDateTime> e : planDao.regresoDeViajesVigentes().entrySet()) {
            libreDesde.put(e.getKey(), e.getValue().plusSeconds(segundosPreparacion));
        }

        return PlanificadorDistribucion.planificar(centros, comunidades, planDao.toneladasPlanificadas(),
                vehiculos, libreDesde, salida, ahora, parametros, seleccion);
    }

    /** Guarda el plan simulado. Valida de nuevo stock, vehículos y necesidades dentro de una transacción. */
    public PlanDistribucion confirmar(PlanDistribucion plan) throws ValidationException, PersistenciaException {
        if (plan == null || plan.estaVacio()) {
            throw new ValidationException("No hay viajes que confirmar. Genere primero una simulación.");
        }
        if (plan.id() != 0) {
            throw new ValidationException("Este plan ya fue confirmado.");
        }
        if (plan.salidaProgramada().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new ValidationException("La hora de salida ya pasó. Vuelva a simular con una hora futura.");
        }
        PlanDistribucion guardado = planDao.guardar(plan);
        sincronizar();
        return guardado;
    }

    public void cancelar(int planId) throws PersistenciaException {
        planDao.cancelar(planId, LocalDateTime.now());
        sincronizar();
    }

    /** Actualiza estados de viajes (planificado/en ruta/completado) y el uso de vehículos según la hora real. */
    public void sincronizar() throws PersistenciaException {
        planDao.sincronizarEstados(LocalDateTime.now());
    }

    public List<PlanDistribucion> listarPlanes() throws PersistenciaException {
        sincronizar();
        return planDao.listarPlanes();
    }

    /** Toneladas comprometidas por comunidad en planes no cancelados. */
    public Map<Integer, Double> planificadoPorComunidad() throws PersistenciaException {
        return planDao.toneladasPlanificadas();
    }

    /** Toneladas ya entregadas por comunidad (viajes completados). */
    public Map<Integer, Double> entregadoPorComunidad() throws PersistenciaException {
        return planDao.toneladasEntregadas();
    }
}
