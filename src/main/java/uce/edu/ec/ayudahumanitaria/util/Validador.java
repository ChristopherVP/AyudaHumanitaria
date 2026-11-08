package uce.edu.ec.ayudahumanitaria.util;

import uce.edu.ec.ayudahumanitaria.geo.CatalogoEcuador;
import uce.edu.ec.ayudahumanitaria.geo.GeoEcuador;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/** Validaciones de entrada en los límites del sistema (formularios y gestores). */
public class Validador {

    /** Letras (con tildes), números, espacios y signos habituales en nombres de lugares. */
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[\\p{L}\\p{N}][\\p{L}\\p{N} .,'()/#&-]*$");
    /** Placa o código de vehículo: 2 a 15 caracteres entre letras, números y guion. */
    private static final Pattern ID_VEHICULO = Pattern.compile("^[A-Z0-9][A-Z0-9-]{1,14}$");

    public static final int MAX_LONGITUD_NOMBRE = 100;
    public static final int MAX_HABITANTES = 2_000_000;
    public static final double MIN_RECURSOS_TON = 0.1;
    public static final double MAX_RECURSOS_TON = 5_000;
    public static final double MIN_CAPACIDAD_KG = 500;
    public static final double MAX_CAPACIDAD_KG = 40_000;
    public static final double MAX_STOCK_TON = 1_000_000;

    private Validador() {
    }

    public static void validarTexto(String valor, String campo) throws ValidationException {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ValidationException("El campo " + campo + " no puede estar vacío.");
        }
    }

    public static void validarPositivo(double valor, String campo) throws ValidationException {
        if (Double.isNaN(valor) || Double.isInfinite(valor) || valor <= 0) {
            throw new ValidationException("El campo " + campo + " debe ser un valor positivo.");
        }
    }

    public static void validarRangoPrioridad(int prioridad) throws ValidationException {
        if (prioridad < 1 || prioridad > 10) {
            throw new ValidationException("La prioridad debe estar entre 1 y 10.");
        }
    }

    public static void validarRango(double valor, double minimo, double maximo, String campo)
            throws ValidationException {
        if (Double.isNaN(valor) || Double.isInfinite(valor) || valor < minimo || valor > maximo) {
            throw new ValidationException("El campo " + campo + " debe estar entre " + formatear(minimo)
                    + " y " + formatear(maximo) + ".");
        }
    }

    /** Nombre de comunidad, centro o ciudad: no vacío, longitud máxima y solo caracteres normales. */
    public static String validarNombre(String valor, String campo) throws ValidationException {
        validarTexto(valor, campo);
        String limpio = valor.trim().replaceAll("\\s+", " ");
        if (limpio.length() > MAX_LONGITUD_NOMBRE) {
            throw new ValidationException("El campo " + campo + " no puede superar " + MAX_LONGITUD_NOMBRE
                    + " caracteres.");
        }
        if (!NOMBRE_VALIDO.matcher(limpio).matches()) {
            throw new ValidationException("El campo " + campo
                    + " solo admite letras, números, espacios y los signos . , ' ( ) / # & -");
        }
        return limpio;
    }

    /** Código/placa del vehículo en mayúsculas. Devuelve el valor normalizado. */
    public static String validarIdVehiculo(String valor) throws ValidationException {
        validarTexto(valor, "identificador");
        String normalizado = valor.trim().toUpperCase();
        if (!ID_VEHICULO.matcher(normalizado).matches()) {
            throw new ValidationException("El identificador debe tener de 2 a 15 caracteres: letras, números "
                    + "y guion (ejemplo: UIO-001).");
        }
        return normalizado;
    }

    /** Provincia del catálogo, coordenadas dentro del Ecuador continental y coherentes con la provincia. */
    public static String validarUbicacion(String provincia, double latitud, double longitud)
            throws ValidationException {
        CatalogoEcuador.Provincia p = CatalogoEcuador.provincia(provincia);
        if (p == null) {
            throw new ValidationException("Seleccione una provincia válida del Ecuador continental.");
        }
        if (Double.isNaN(latitud) || Double.isNaN(longitud)
                || !GeoEcuador.estaEnEcuadorContinental(latitud, longitud)) {
            throw new ValidationException("Las coordenadas deben estar dentro del Ecuador continental "
                    + "(latitud entre " + GeoEcuador.LAT_MIN + " y " + GeoEcuador.LAT_MAX
                    + ", longitud entre " + GeoEcuador.LON_MIN + " y " + GeoEcuador.LON_MAX + ").");
        }
        if (!GeoEcuador.esCoherenteConProvincia(p.nombre(), latitud, longitud)) {
            throw new ValidationException("Las coordenadas no corresponden a la provincia " + p.nombre()
                    + ". Revise la provincia o ubique el punto en el mapa.");
        }
        return p.nombre();
    }

    /** La salida no puede estar en el pasado (1 minuto de tolerancia) ni a más de un año. */
    public static void validarFechaSalida(LocalDateTime salida, LocalDateTime ahora) throws ValidationException {
        if (salida == null) {
            throw new ValidationException("Indique la fecha y hora de salida.");
        }
        if (salida.isBefore(ahora.minusMinutes(1))) {
            throw new ValidationException("La fecha y hora de salida no puede estar en el pasado.");
        }
        if (salida.isAfter(ahora.plusYears(1))) {
            throw new ValidationException("La fecha de salida no puede superar un año desde hoy.");
        }
    }

    private static String formatear(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
