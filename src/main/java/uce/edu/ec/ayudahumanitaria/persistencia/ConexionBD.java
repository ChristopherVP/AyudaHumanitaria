package uce.edu.ec.ayudahumanitaria.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionBD {

    // ============================================================
    // MODO DE BASE DE DATOS: deja activa SOLO UNA de las dos líneas
    // ============================================================

    // OPCIÓN 1 - EN MEMORIA (desactivada): los datos se pierden al cerrar el programa
    // y en cada ejecución se cargan de nuevo los datos de ejemplo.
    // private static final String URL_POR_DEFECTO = "jdbc:h2:mem:ayudahumanitaria;DB_CLOSE_DELAY=-1";

    // OPCIÓN 2 - EN ARCHIVO (activa): los datos se conservan entre ejecuciones
    // (se guardan en la carpeta ./data/ayudahumanitaria.mv.db)
    private static final String URL_POR_DEFECTO = "jdbc:h2:file:./data/ayudahumanitaria";

    /** Permite cambiar la URL sin recompilar (las pruebas usan una base en memoria): -Dayudahumanitaria.db.url=... */
    private static final String URL = System.getProperty("ayudahumanitaria.db.url", URL_POR_DEFECTO);

    private static final String USUARIO = "sa";
    private static final String CLAVE = "";

    private static Connection conexion;

    private ConexionBD() {
    }

    public static synchronized Connection obtenerConexion() throws PersistenciaException {
        try {
            if (conexion == null || conexion.isClosed()) {
                Class.forName("org.h2.Driver");
                conexion = DriverManager.getConnection(URL, USUARIO, CLAVE);
            }
            return conexion;
        } catch (SQLException | ClassNotFoundException ex) {
            throw new PersistenciaException("No se pudo establecer conexión con la base de datos. "
                    + "Si el programa ya está abierto en otra ventana, ciérrelo e intente de nuevo.", ex);
        }
    }

    /**
     * Crea las tablas si no existen y migra las ya existentes (agrega columnas nuevas), de modo que
     * una base creada con una versión anterior sigue funcionando sin borrar datos.
     */
    public static void inicializarBD() throws PersistenciaException {
        try (Statement st = obtenerConexion().createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS CENTRO ("
                    + "ID INT AUTO_INCREMENT PRIMARY KEY, "
                    + "NOMBRE VARCHAR(100) NOT NULL UNIQUE, "
                    + "CIUDAD VARCHAR(80) NOT NULL, "
                    + "PROVINCIA VARCHAR(40) NOT NULL, "
                    + "LATITUD DOUBLE NOT NULL, "
                    + "LONGITUD DOUBLE NOT NULL, "
                    + "STOCK_TON DOUBLE DEFAULT 0 NOT NULL)");

            st.execute("CREATE TABLE IF NOT EXISTS COMUNIDAD ("
                    + "ID INT AUTO_INCREMENT PRIMARY KEY, "
                    + "NOMBRE VARCHAR(100) NOT NULL, "
                    + "HABITANTES INT NOT NULL, "
                    + "PRIORIDAD INT NOT NULL, "
                    + "RECURSOS_REQUERIDOS DOUBLE NOT NULL)");
            st.execute("ALTER TABLE CENTRO ADD COLUMN IF NOT EXISTS DIRECCION VARCHAR(150) DEFAULT '' NOT NULL");
            st.execute("ALTER TABLE COMUNIDAD ADD COLUMN IF NOT EXISTS DIRECCION VARCHAR(150) DEFAULT '' NOT NULL");
            // Registros anteriores a la dirección obligatoria: se les pone una referencia editable.
            st.execute("UPDATE CENTRO SET DIRECCION = 'Centro de acopio de ' || CIUDAD || ', ' || PROVINCIA"
                    + " || ' (referencia)' WHERE TRIM(DIRECCION) = ''");
            st.execute("ALTER TABLE COMUNIDAD ADD COLUMN IF NOT EXISTS PROVINCIA VARCHAR(40) DEFAULT '' NOT NULL");
            st.execute("ALTER TABLE COMUNIDAD ADD COLUMN IF NOT EXISTS LATITUD DOUBLE DEFAULT 0 NOT NULL");
            st.execute("ALTER TABLE COMUNIDAD ADD COLUMN IF NOT EXISTS LONGITUD DOUBLE DEFAULT 0 NOT NULL");
            st.execute("UPDATE COMUNIDAD SET DIRECCION = 'Cabecera de ' || NOMBRE || ', ' || PROVINCIA"
                    + " || ' (referencia)' WHERE TRIM(DIRECCION) = ''");

            st.execute("CREATE TABLE IF NOT EXISTS VEHICULO ("
                    + "ID VARCHAR(20) PRIMARY KEY, "
                    + "CAPACIDAD DOUBLE NOT NULL, "
                    + "ASIGNADO BOOLEAN DEFAULT FALSE, "
                    + "COMUNIDAD_ASIGNADA VARCHAR(100))");
            st.execute("ALTER TABLE VEHICULO ADD COLUMN IF NOT EXISTS CENTRO_ID INT");
            st.execute("ALTER TABLE VEHICULO ADD CONSTRAINT IF NOT EXISTS FK_VEHICULO_CENTRO "
                    + "FOREIGN KEY (CENTRO_ID) REFERENCES CENTRO(ID) ON DELETE SET NULL");

            st.execute("CREATE TABLE IF NOT EXISTS PLAN ("
                    + "ID INT AUTO_INCREMENT PRIMARY KEY, "
                    + "CREADO_EN TIMESTAMP NOT NULL, "
                    + "SALIDA TIMESTAMP NOT NULL, "
                    + "BITACORA CLOB, "
                    + "PENDIENTES CLOB)");

            st.execute("CREATE TABLE IF NOT EXISTS VIAJE ("
                    + "ID INT AUTO_INCREMENT PRIMARY KEY, "
                    + "PLAN_ID INT NOT NULL, "
                    + "CENTRO_ID INT, "
                    + "CENTRO_NOMBRE VARCHAR(100) NOT NULL, "
                    + "CENTRO_LAT DOUBLE NOT NULL, "
                    + "CENTRO_LON DOUBLE NOT NULL, "
                    + "VEHICULO_ID VARCHAR(20), "
                    + "VEHICULO_ETIQUETA VARCHAR(20) NOT NULL, "
                    + "CAPACIDAD_TON DOUBLE NOT NULL, "
                    + "CARGA_TON DOUBLE NOT NULL, "
                    + "DISTANCIA_KM DOUBLE NOT NULL, "
                    + "SALIDA TIMESTAMP NOT NULL, "
                    + "REGRESO TIMESTAMP NOT NULL, "
                    + "ESTADO VARCHAR(15) NOT NULL, "
                    + "CONSTRAINT FK_VIAJE_PLAN FOREIGN KEY (PLAN_ID) REFERENCES PLAN(ID) ON DELETE CASCADE, "
                    + "CONSTRAINT FK_VIAJE_CENTRO FOREIGN KEY (CENTRO_ID) REFERENCES CENTRO(ID) ON DELETE SET NULL, "
                    + "CONSTRAINT FK_VIAJE_VEHICULO FOREIGN KEY (VEHICULO_ID) REFERENCES VEHICULO(ID) ON DELETE SET NULL)");

            st.execute("CREATE TABLE IF NOT EXISTS PARADA ("
                    + "ID INT AUTO_INCREMENT PRIMARY KEY, "
                    + "VIAJE_ID INT NOT NULL, "
                    + "COMUNIDAD_ID INT, "
                    + "COMUNIDAD_NOMBRE VARCHAR(100) NOT NULL, "
                    + "PROVINCIA VARCHAR(40) NOT NULL, "
                    + "LATITUD DOUBLE NOT NULL, "
                    + "LONGITUD DOUBLE NOT NULL, "
                    + "ORDEN INT NOT NULL, "
                    + "TONELADAS DOUBLE NOT NULL, "
                    + "KM_TRAMO DOUBLE NOT NULL, "
                    + "KM_ACUMULADOS DOUBLE NOT NULL, "
                    + "LLEGADA TIMESTAMP NOT NULL, "
                    + "SALIDA TIMESTAMP NOT NULL, "
                    + "CONSTRAINT FK_PARADA_VIAJE FOREIGN KEY (VIAJE_ID) REFERENCES VIAJE(ID) ON DELETE CASCADE, "
                    + "CONSTRAINT FK_PARADA_COMUNIDAD FOREIGN KEY (COMUNIDAD_ID) REFERENCES COMUNIDAD(ID) ON DELETE SET NULL)");
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo inicializar la base de datos.", ex);
        }
    }

    public static void cerrarConexion() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
