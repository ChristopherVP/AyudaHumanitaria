package uce.edu.ec.ayudahumanitaria.dominio;

import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.persistencia.VehiculoDAO;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;
import uce.edu.ec.ayudahumanitaria.util.Validador;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GestorVehiculos {

    private final VehiculoDAO dao = new VehiculoDAO();

    public void registrar(String id, double capacidad) throws ValidationException, PersistenciaException {
        Validador.validarTexto(id, "identificador");
        Validador.validarPositivo(capacidad, "capacidad");

        dao.insertar(new Vehiculo(id, capacidad));
    }

    public void modificar(Vehiculo v) throws ValidationException, PersistenciaException {
        Validador.validarPositivo(v.getCapacidad(), "capacidad");
        dao.actualizar(v);
    }

    public void eliminar(String id) throws PersistenciaException {
        dao.eliminar(id);
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
