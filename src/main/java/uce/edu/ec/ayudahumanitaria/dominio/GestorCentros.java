package uce.edu.ec.ayudahumanitaria.dominio;

import uce.edu.ec.ayudahumanitaria.persistencia.CentroDAO;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.persistencia.PlanDAO;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;
import uce.edu.ec.ayudahumanitaria.util.Validador;

import java.util.List;

/** Reglas de negocio de los centros de acopio. */
public class GestorCentros {

    private final CentroDAO dao = new CentroDAO();
    private final PlanDAO planDao = new PlanDAO();

    public CentroAcopio registrar(String nombre, String ciudad, String provincia, double latitud, double longitud,
                                  double stock) throws ValidationException, PersistenciaException {
        CentroAcopio nuevo = validar(0, nombre, ciudad, provincia, latitud, longitud, stock);
        verificarNombreUnico(nuevo.getNombre(), 0);
        return dao.insertar(nuevo);
    }

    public void modificar(CentroAcopio original, String nombre, String ciudad, String provincia, double latitud,
                          double longitud, double stock) throws ValidationException, PersistenciaException {
        CentroAcopio nuevo = validar(original.getId(), nombre, ciudad, provincia, latitud, longitud, stock);
        verificarNombreUnico(nuevo.getNombre(), original.getId());
        boolean cambioUbicacion = Math.abs(original.getLatitud() - latitud) > 1e-9
                || Math.abs(original.getLongitud() - longitud) > 1e-9;
        if (cambioUbicacion && planDao.contarViajesVigentesDeCentro(original.getId()) > 0) {
            throw new ValidationException("El centro tiene viajes planificados o en ruta: "
                    + "no se puede cambiar su ubicación hasta que terminen.");
        }
        dao.actualizar(nuevo);
    }

    /** Un centro solo se elimina si no tiene vehículos ni viajes vigentes. */
    public void eliminar(int id) throws ValidationException, PersistenciaException {
        if (dao.contarVehiculos(id) > 0) {
            throw new ValidationException("El centro tiene vehículos asignados. "
                    + "Reasígnelos o elimínelos antes de borrar el centro.");
        }
        if (planDao.contarViajesVigentesDeCentro(id) > 0) {
            throw new ValidationException("El centro tiene viajes planificados o en ruta.");
        }
        dao.eliminar(id);
    }

    /** Suma toneladas al stock del centro (reabastecimiento). */
    public void reabastecer(CentroAcopio centro, double toneladas) throws ValidationException, PersistenciaException {
        Validador.validarRango(toneladas, 0.1, Validador.MAX_STOCK_TON, "toneladas a ingresar");
        if (centro.getStockToneladas() + toneladas > Validador.MAX_STOCK_TON) {
            throw new ValidationException("El stock del centro no puede superar " + (long) Validador.MAX_STOCK_TON
                    + " toneladas.");
        }
        dao.ajustarStock(centro.getId(), toneladas);
    }

    public List<CentroAcopio> listar() throws PersistenciaException {
        return dao.listarTodos();
    }

    public CentroAcopio buscarPorId(int id) throws PersistenciaException {
        return dao.buscarPorId(id);
    }

    private CentroAcopio validar(int id, String nombre, String ciudad, String provincia, double latitud,
                                 double longitud, double stock) throws ValidationException {
        String nombreLimpio = Validador.validarNombre(nombre, "nombre");
        String ciudadLimpia = Validador.validarNombre(ciudad, "ciudad");
        String provinciaValida = Validador.validarUbicacion(provincia, latitud, longitud);
        Validador.validarRango(stock, 0, Validador.MAX_STOCK_TON, "stock (toneladas)");
        return new CentroAcopio(id, nombreLimpio, ciudadLimpia, provinciaValida, latitud, longitud, stock);
    }

    private void verificarNombreUnico(String nombre, int idPropio) throws ValidationException, PersistenciaException {
        for (CentroAcopio c : dao.listarTodos()) {
            if (c.getId() != idPropio && c.getNombre().equalsIgnoreCase(nombre)) {
                throw new ValidationException("Ya existe un centro llamado " + nombre + ".");
            }
        }
    }
}
