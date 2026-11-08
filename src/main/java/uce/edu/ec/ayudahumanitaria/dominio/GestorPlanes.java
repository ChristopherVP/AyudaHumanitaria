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

    /** Calcula (sin guardar) el plan para la hora de salida indicada con los datos actuales. */
    public PlanDistribucion simular(LocalDateTime salida) throws ValidationException, PersistenciaException {
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
                vehiculos, libreDesde, salida, ahora, parametros);
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
