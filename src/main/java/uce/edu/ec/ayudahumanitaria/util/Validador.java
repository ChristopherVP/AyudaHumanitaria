package uce.edu.ec.ayudahumanitaria.util;

public class Validador {

    private Validador() {
    }

    public static void validarTexto(String valor, String campo) throws ValidationException {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ValidationException("El campo " + campo + " no puede estar vacío.");
        }
    }

    public static void validarPositivo(double valor, String campo) throws ValidationException {
        if (valor <= 0) {
            throw new ValidationException("El campo " + campo + " debe ser un valor positivo.");
        }
    }

    public static void validarRangoPrioridad(int prioridad) throws ValidationException {
        if (prioridad < 1 || prioridad > 10) {
            throw new ValidationException("La prioridad debe estar entre 1 y 10.");
        }
    }
}
