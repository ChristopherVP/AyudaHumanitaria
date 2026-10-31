package uce.edu.ec.ayudahumanitaria.dominio;

import uce.edu.ec.ayudahumanitaria.persistencia.ComunidadDAO;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;
import uce.edu.ec.ayudahumanitaria.util.Validador;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GestorComunidades {

    private final ComunidadDAO dao = new ComunidadDAO();

    public void registrar(String nombre, int habitantes, int prioridad, double recursos)
            throws ValidationException, PersistenciaException {

        Validador.validarTexto(nombre, "nombre");
        Validador.validarPositivo(habitantes, "habitantes");
        Validador.validarRangoPrioridad(prioridad);
        Validador.validarPositivo(recursos, "recursos requeridos");

        dao.insertar(new Comunidad(nombre, habitantes, prioridad, recursos));
    }

    public void modificar(Comunidad c) throws ValidationException, PersistenciaException {
        Validador.validarTexto(c.getNombre(), "nombre");
        Validador.validarPositivo(c.getHabitantes(), "habitantes");
        Validador.validarRangoPrioridad(c.getPrioridad());
        Validador.validarPositivo(c.getRecursosRequeridos(), "recursos requeridos");

        dao.actualizar(c);
    }

    public void eliminar(int id) throws PersistenciaException {
        dao.eliminar(id);
    }

    public Comunidad[] listarTodas() throws PersistenciaException {
        List<Comunidad> lista = dao.listarTodas();
        Comunidad[] comunidades = lista.toArray(new Comunidad[0]);
        Algoritmos.quickSort(comunidades, Comparator.comparingInt(Comunidad::getPrioridad).reversed());
        return comunidades;
    }

    public Comunidad[] buscar(String textoBusqueda) throws PersistenciaException {
        String texto = textoBusqueda == null ? "" : textoBusqueda.toUpperCase();
        Comunidad[] todas = listarTodas();
        Comunidad[] temporal = new Comunidad[todas.length];
        int contador = 0;

        for (Comunidad c : todas) {
            if (c.getNombre().toUpperCase().contains(texto)) {
                temporal[contador++] = c;
            }
        }
        return Arrays.copyOf(temporal, contador);
    }

    public Comunidad buscarPorId(int id) throws PersistenciaException {
        Comunidad[] todas = dao.listarTodas().toArray(new Comunidad[0]);
        Comparator<Comunidad> porId = Comparator.comparingInt(Comunidad::getId);
        Algoritmos.quickSort(todas, porId);

        Comunidad clave = new Comunidad(id, "", 0, 1, 0);
        int indice = Algoritmos.busquedaBinariaRecursiva(todas, clave, porId);
        return indice == -1 ? null : todas[indice];
    }
}
