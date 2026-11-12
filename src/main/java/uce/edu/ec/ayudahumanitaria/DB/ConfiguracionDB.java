package uce.edu.ec.ayudahumanitaria.DB;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Datos de conexión a la base. Nada de claves en el código. Orden de búsqueda: propiedades del sistema
 * (pruebas), el archivo local {@code db.properties} (junto al pom.xml, ignorado por Git) y por último las
 * variables de entorno ({@code DB_URL}, {@code DB_USER}, {@code DB_PASSWORD}). Sin {@code DB_URL} se usa H2 en archivo.
 */
public record ConfiguracionDB(String url, String usuario, String clave) {

    private static final String ARCHIVO_LOCAL = "db.properties";
    public static final String URL_H2_POR_DEFECTO = "jdbc:h2:file:./data/ayudahumanitaria";

    private static final Properties ARCHIVO = cargarArchivo();

    public static ConfiguracionDB cargar() {
        String url = leer("ayudahumanitaria.db.url", "DB_URL", URL_H2_POR_DEFECTO);
        boolean postgres = url.startsWith("jdbc:postgresql:");
        String usuario = leer("ayudahumanitaria.db.user", "DB_USER", postgres ? "" : "sa");
        String clave = leer("ayudahumanitaria.db.password", "DB_PASSWORD", "");
        return new ConfiguracionDB(url, usuario, clave);
    }

    public boolean esPostgres() {
        return url.startsWith("jdbc:postgresql:");
    }

    /** Falla con un mensaje claro si falta algún dato de PostgreSQL (nunca muestra la clave). */
    public void validar() {
        if (esPostgres() && (usuario.isBlank() || clave.isBlank())) {
            throw new IllegalStateException("Faltan DB_USER o DB_PASSWORD de PostgreSQL. Defínalos en el archivo "
                    + "db.properties (junto al pom.xml) o como variables de entorno.");
        }
    }

    private static String leer(String propiedad, String variable, String porDefecto) {
        String valor = System.getProperty(propiedad);
        if (valor == null || valor.isBlank()) {
            valor = ARCHIVO.getProperty(variable);
        }
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(variable);
        }
        return valor == null || valor.isBlank() ? porDefecto : valor.trim();
    }

    /** Lee db.properties (claves DB_URL, DB_USER, DB_PASSWORD) si existe; si no, queda vacío. */
    private static Properties cargarArchivo() {
        Properties propiedades = new Properties();
        Path ruta = Path.of(ARCHIVO_LOCAL);
        if (Files.isReadable(ruta)) {
            try (InputStream in = Files.newInputStream(ruta)) {
                propiedades.load(in);
            } catch (IOException ex) {
                System.err.println("No se pudo leer " + ARCHIVO_LOCAL + ": " + ex.getMessage());
            }
        }
        return propiedades;
    }

    /** Evita que la clave aparezca en logs o excepciones. */
    @Override
    public String toString() {
        return "ConfiguracionDB[url=" + url + ", usuario=" + usuario + "]";
    }
}