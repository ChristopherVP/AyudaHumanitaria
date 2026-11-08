package uce.edu.ec.ayudahumanitaria.dominio;

import uce.edu.ec.ayudahumanitaria.persistencia.CentroDAO;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.persistencia.PlanDAO;
import uce.edu.ec.ayudahumanitaria.persistencia.VehiculoDAO;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;
import uce.edu.ec.ayudahumanitaria.util.Validador;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GestorVehiculos {

    private final VehiculoDAO dao = new VehiculoDAO();
    private final CentroDAO centroDao = new CentroDAO();
    private final PlanDAO planDao = new PlanDAO();

    /** @param capacidadKg capacidad de carga en kilogramos */
    public void registrar(String id, double capacidadKg, int centroId)
            throws ValidationException, PersistenciaException {
        String idValido = Validador.validarIdVehiculo(id);
        validarCapacidadYCentro(capacidadKg, centroId);
        dao.insertar(new Vehiculo(idValido, capacidadKg, centroId));
    }

    public void modificar(Vehiculo v) throws ValidationException, PersistenciaException {
        validarCapacidadYCentro(v.getCapacidad(), v.getCentroId());
        if (planDao.contarViajesVigentesDeVehiculo(v.getId()) > 0) {
            throw new ValidationException("El vehículo tiene viajes planificados o en ruta: "
                    + "no se puede modificar hasta que terminen.");
        }
        dao.actualizar(v);
    }

    public void eliminar(String id) throws ValidationException, PersistenciaException {
        if (planDao.contarViajesVigentesDeVehiculo(id) > 0) {
            throw new ValidationException("El vehículo tiene viajes planificados o en ruta.");
        }
        dao.eliminar(id);
    }

    private void validarCapacidadYCentro(double capacidadKg, int centroId)
            throws ValidationException, PersistenciaException {
        Validador.validarRango(capacidadKg, Validador.MIN_CAPACIDAD_KG, Validador.MAX_CAPACIDAD_KG,
                "capacidad (kg)");
        if (centroId <= 0 || centroDao.buscarPorId(centroId) == null) {
            throw new ValidationException("Seleccione el centro de acopio al que pertenece el vehículo.");
        }
    }

    public Vehiculo[] listarTodos() throws PersistenciaException {
        List<Vehiculo> lista = dao.listarTodos();
        Vehiculo[] vehiculos = lista.toArray(new Vehiculo[0]);
        Algoritmos.quickSort(vehiculos, Comparator.comparingDouble(Vehiculo::getCapacidad).reversed());
        return vehiculos;
    }

    public Vehiculo[] buscar(String textoBusqueda) throws PersistenciaException {
        String texto = textoBusqueda == null ? "" : textoBusqueda.toUpperCase();
        Vehiculo[] todos = listarTodos();
        Vehiculo[] temporal = new Vehiculo[todos.length];
        int contador = 0;

        for (Vehiculo v : todos) {
            if (v.getId().toUpperCase().contains(texto)) {
                temporal[contador++] = v;
            }
        }
        return Arrays.copyOf(temporal, contador);
    }

    public Vehiculo buscarPorId(String id) throws PersistenciaException {
        Vehiculo[] todos = listarTodos();
        Comparator<Vehiculo> porId = Comparator.comparing(Vehiculo::getId);
        Algoritmos.quickSort(todos, porId);

        Vehiculo clave = new Vehiculo(id, 0);
        int indice = Algoritmos.busquedaBinariaRecursiva(todos, clave, porId);
        return indice == -1 ? null : todos[indice];
    }
}
