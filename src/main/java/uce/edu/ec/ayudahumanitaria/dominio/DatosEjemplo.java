package uce.edu.ec.ayudahumanitaria.dominio;

import uce.edu.ec.ayudahumanitaria.persistencia.PersistenciaException;
import uce.edu.ec.ayudahumanitaria.util.ValidationException;

public class DatosEjemplo {

    private DatosEjemplo() {
    }

    public static void cargarSiVacio(GestorComunidades gestorComunidades, GestorVehiculos gestorVehiculos)
            throws ValidationException, PersistenciaException {

        if (gestorComunidades.listarTodas().length == 0) {
            gestorComunidades.registrar("Comunidad A", 5000, 9, 4);
            gestorComunidades.registrar("Comunidad B", 8000, 6, 6);
            gestorComunidades.registrar("Comunidad C", 6500, 6, 5);
            gestorComunidades.registrar("Comunidad D", 9000, 5, 7);
        }

        if (gestorVehiculos.listarTodos().length == 0) {
            gestorVehiculos.registrar("V1", 1000);
            gestorVehiculos.registrar("V2", 1500);
        }
    }
}
