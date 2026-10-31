package uce.edu.ec.ayudahumanitaria.util;

public class ValidationException extends Exception {

    public ValidationException(String mensaje) {
        super(mensaje);
    }

    public ValidationException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
