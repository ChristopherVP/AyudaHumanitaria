package uce.edu.ec.ayudahumanitaria.dominio;

import uce.edu.ec.ayudahumanitaria.persistencia.ComunidadDAO;
import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.persistencia.PlanDAO;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;
import uce.edu.ec.ayudahumanitaria.util.Validador;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GestorComunidades {

    private final ComunidadDAO dao = new ComunidadDAO();
    private final PlanDAO planDao = new PlanDAO();

    public Comunidad registrar(String nombre, String provincia, int habitantes, int prioridad, double recursos,
                               double latitud, double longitud) throws ValidationException, PersistenciaException {
        Comunidad nueva = validar(0, nombre, provincia, habitantes, prioridad, recursos, latitud, longitud);
        verificarNombreUnico(nueva.getNombre(), 0);
        dao.insertar(nueva);
        return nueva;
    }

    public void modificar(Comunidad c) throws ValidationException, PersistenciaException {
        Comunidad validada = validar(c.getId(), c.getNombre(), c.getProvincia(), c.getHabitantes(),
                c.getPrioridad(), c.getRecursosRequeridos(), c.getLatitud(), c.getLongitud());
        verificarNombreUnico(validada.getNombre(), c.getId());

        double planificado = planDao.toneladasPlanificadas().getOrDefault(c.getId(), 0.0);
        if (validada.getRecursosRequeridos() + 0.06 < planificado) {
            throw new ValidationException("La comunidad ya tiene " + String.format("%.2f", planificado)
                    + " t comprometidas en planes de distribución; los recursos requeridos no pueden ser menores.");
        }
        dao.actualizar(validada);
    }

    /** Una comunidad con viajes planificados o en ruta no se puede eliminar. */
    public void eliminar(int id) throws ValidationException, PersistenciaException {
        if (planDao.contarViajesVigentesDeComunidad(id) > 0) {
            throw new ValidationException("La comunidad tiene entregas planificadas o en ruta. "
                    + "Cancele el plan o espere a que termine.");
        }
        dao.eliminar(id);
    }

    private Comunidad validar(int id, String nombre, String provincia, int habitantes, int prioridad,
                              double recursos, double latitud, double longitud) throws ValidationException {
        String nombreLimpio = Validador.validarNombre(nombre, "nombre");
        Validador.validarRango(habitantes, 1, Validador.MAX_HABITANTES, "habitantes");
        Validador.validarRangoPrioridad(prioridad);
        Validador.validarRango(recursos, Validador.MIN_RECURSOS_TON, Validador.MAX_RECURSOS_TON,
                "recursos requeridos (toneladas)");
        String provinciaValida = Validador.validarUbicacion(provincia, latitud, longitud);
        return new Comunidad(id, nombreLimpio, provinciaValida, habitantes, prioridad, recursos, latitud, longitud);
    }

    private void verificarNombreUnico(String nombre, int idPropio) throws ValidationException, PersistenciaException {
        for (Comunidad existente : dao.listarTodas()) {
            if (existente.getId() != idPropio && existente.getNombre().equalsIgnoreCase(nombre)) {
                throw new ValidationException("Ya existe una comunidad llamada " + nombre + ".");
            }
        }
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
