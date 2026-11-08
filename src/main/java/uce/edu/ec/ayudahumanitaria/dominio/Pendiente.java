package uce.edu.ec.ayudahumanitaria.dominio;

/**
 * Necesidad que el plan no pudo cubrir (total o parcialmente) y el motivo.
 *
 * @param centroNombre nombre del centro responsable (o "—" si no hay ninguno)
 * @param toneladas    toneladas sin cubrir
 */
public record Pendiente(int comunidadId, String comunidadNombre, String centroNombre, double toneladas, String motivo) {
}
