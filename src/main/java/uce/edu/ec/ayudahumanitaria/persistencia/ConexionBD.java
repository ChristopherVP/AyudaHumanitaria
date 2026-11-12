package uce.edu.ec.ayudahumanitaria.persistencia;

import uce.edu.ec.ayudahumanitaria.DB.ConfiguracionDB;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionBD {

    /**
     * Origen de datos: PostgreSQL si la URL empieza por jdbc:postgresql: (se define en db.properties);
     * si no hay URL, H2 en archivo (./data/ayudahumanitaria). Las pruebas usan H2 en memoria.
     */
    private static final ConfiguracionDB CONFIG = ConfiguracionDB.cargar();

    private static Connection conexion;

    private ConexionBD() {
    }

    public static synchronized Connection obtenerConexion() throws PersistenciaException {
        try {
            if (conexion == null || conexion.isClosed() || !conexion.isValid(2)) {
                CONFIG.validar();
                Class.forName(CONFIG.esPostgres() ? "org.postgresql.Driver" : "org.h2.Driver");
                conexion = DriverManager.getConnection(CONFIG.url(), CONFIG.usuario(), CONFIG.clave());
                System.out.println("[BD] Conectado a " + CONFIG.url() + " (base: " + conexion.getCatalog() + ")");
            }
            return conexion;
        } catch (IllegalStateException ex) {
            throw new PersistenciaException(ex.getMessage(), ex);
        } catch (SQLException | ClassNotFoundException ex) {
            throw new PersistenciaException(CONFIG.esPostgres()
                    ? "No se pudo conectar a PostgreSQL (" + CONFIG.url() + "). Verifique que el servidor esté "
                    + "encendido y que DB_URL, DB_USER y DB_PASSWORD de db.properties sean correctos."
                    : "No se pudo establecer conexión con la base de datos. "
                    + "Si el programa ya está abierto en otra ventana, ciérrelo e intente de nuevo.", ex);
        }
    }

    /**
     * Crea las tablas si no existen y migra las ya existentes (agrega columnas nuevas), de modo que
     * una base creada con una versión anterior sigue funcionando sin borrar datos.
     */
    public static void inicializarBD() throws PersistenciaException {
        if (CONFIG.esPostgres()) {
            crearEsquemaPostgres();
            return;
        }
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

    /** Sentencias idempotentes: se pueden ejecutar en cada arranque sin perder datos. */
    private static final String[] ESQUEMA_POSTGRES = {
        """
        CREATE TABLE IF NOT EXISTS centro (
            id SERIAL PRIMARY KEY,
            nombre VARCHAR(100) NOT NULL UNIQUE,
            ciudad VARCHAR(80) NOT NULL,
            provincia VARCHAR(40) NOT NULL,
            latitud DOUBLE PRECISION NOT NULL,
            longitud DOUBLE PRECISION NOT NULL,
            stock_ton DOUBLE PRECISION NOT NULL DEFAULT 0,
            direccion VARCHAR(150) NOT NULL DEFAULT '')""",
        """
        CREATE TABLE IF NOT EXISTS comunidad (
            id SERIAL PRIMARY KEY,
            nombre VARCHAR(100) NOT NULL,
            provincia VARCHAR(40) NOT NULL DEFAULT '',
            habitantes INT NOT NULL,
            prioridad INT NOT NULL,
            recursos_requeridos DOUBLE PRECISION NOT NULL,
            latitud DOUBLE PRECISION NOT NULL DEFAULT 0,
            longitud DOUBLE PRECISION NOT NULL DEFAULT 0,
            direccion VARCHAR(150) NOT NULL DEFAULT '')""",
        """
        CREATE TABLE IF NOT EXISTS vehiculo (
            id VARCHAR(20) PRIMARY KEY,
            capacidad DOUBLE PRECISION NOT NULL,
            asignado BOOLEAN DEFAULT FALSE,
            comunidad_asignada VARCHAR(100),
            centro_id INT REFERENCES centro(id) ON DELETE SET NULL)""",
        """
        CREATE TABLE IF NOT EXISTS plan (
            id SERIAL PRIMARY KEY,
            creado_en TIMESTAMP NOT NULL,
            salida TIMESTAMP NOT NULL,
            bitacora TEXT,
            pendientes TEXT)""",
        """
        CREATE TABLE IF NOT EXISTS viaje (
            id SERIAL PRIMARY KEY,
            plan_id INT NOT NULL REFERENCES plan(id) ON DELETE CASCADE,
            centro_id INT REFERENCES centro(id) ON DELETE SET NULL,
            centro_nombre VARCHAR(100) NOT NULL,
            centro_lat DOUBLE PRECISION NOT NULL,
            centro_lon DOUBLE PRECISION NOT NULL,
            vehiculo_id VARCHAR(20) REFERENCES vehiculo(id) ON DELETE SET NULL,
            vehiculo_etiqueta VARCHAR(20) NOT NULL,
            capacidad_ton DOUBLE PRECISION NOT NULL,
            carga_ton DOUBLE PRECISION NOT NULL,
            distancia_km DOUBLE PRECISION NOT NULL,
            salida TIMESTAMP NOT NULL,
            regreso TIMESTAMP NOT NULL,
            estado VARCHAR(15) NOT NULL)""",
        """
        CREATE TABLE IF NOT EXISTS parada (
            id SERIAL PRIMARY KEY,
            viaje_id INT NOT NULL REFERENCES viaje(id) ON DELETE CASCADE,
            comunidad_id INT REFERENCES comunidad(id) ON DELETE SET NULL,
            comunidad_nombre VARCHAR(100) NOT NULL,
            provincia VARCHAR(40) NOT NULL,
            latitud DOUBLE PRECISION NOT NULL,
            longitud DOUBLE PRECISION NOT NULL,
            orden INT NOT NULL,
            toneladas DOUBLE PRECISION NOT NULL,
            km_tramo DOUBLE PRECISION NOT NULL,
            km_acumulados DOUBLE PRECISION NOT NULL,
            llegada TIMESTAMP NOT NULL,
            salida TIMESTAMP NOT NULL)""",
        // Si las tablas ya existían (creadas a mano con una versión anterior), agrega lo que falte.
        "ALTER TABLE centro ADD COLUMN IF NOT EXISTS direccion VARCHAR(150) NOT NULL DEFAULT ''",
        "ALTER TABLE comunidad ADD COLUMN IF NOT EXISTS direccion VARCHAR(150) NOT NULL DEFAULT ''",
        "ALTER TABLE comunidad ADD COLUMN IF NOT EXISTS provincia VARCHAR(40) NOT NULL DEFAULT ''",
        "ALTER TABLE comunidad ADD COLUMN IF NOT EXISTS latitud DOUBLE PRECISION NOT NULL DEFAULT 0",
        "ALTER TABLE comunidad ADD COLUMN IF NOT EXISTS longitud DOUBLE PRECISION NOT NULL DEFAULT 0",
        "ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS centro_id INT REFERENCES centro(id) ON DELETE SET NULL"
    };

    /** PostgreSQL: crea las tablas que falten al arrancar (igual que hace el modo H2), sin script manual. */
    private static void crearEsquemaPostgres() throws PersistenciaException {
        try (Statement st = obtenerConexion().createStatement()) {
            for (String sentencia : ESQUEMA_POSTGRES) {
                st.execute(sentencia);
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudieron crear/verificar las tablas en PostgreSQL ("
                    + CONFIG.url() + "). Detalle: " + ex.getMessage(), ex);
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