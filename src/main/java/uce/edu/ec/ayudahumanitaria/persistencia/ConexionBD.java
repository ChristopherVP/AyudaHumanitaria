package uce.edu.ec.ayudahumanitaria.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionBD {

    private static final String URL = "jdbc:h2:mem:ayudahumanitaria;DB_CLOSE_DELAY=-1";
    private static final String USUARIO = "sa";
    private static final String CLAVE = "";

    private static Connection conexion;

    private ConexionBD() {
    }

    public static Connection obtenerConexion() throws PersistenciaException {
        try {
            if (conexion == null || conexion.isClosed()) {
                Class.forName("org.h2.Driver");
                conexion = DriverManager.getConnection(URL, USUARIO, CLAVE);
            }
            return conexion;
        } catch (SQLException | ClassNotFoundException ex) {
            throw new PersistenciaException("No se pudo establecer conexión con la base de datos.", ex);
        }
    }

    public static void inicializarBD() throws PersistenciaException {
        try (Statement st = obtenerConexion().createStatement()) {
            st.execute(
                    "CREATE TABLE IF NOT EXISTS COMUNIDAD ("
                            + "ID INT AUTO_INCREMENT PRIMARY KEY, "
                            + "NOMBRE VARCHAR(100) NOT NULL, "
                            + "HABITANTES INT NOT NULL, "
                            + "PRIORIDAD INT NOT NULL, "
                            + "RECURSOS_REQUERIDOS DOUBLE NOT NULL)"
            );

            st.execute(
                    "CREATE TABLE IF NOT EXISTS VEHICULO ("
                            + "ID VARCHAR(20) PRIMARY KEY, "
                            + "CAPACIDAD DOUBLE NOT NULL, "
                            + "ASIGNADO BOOLEAN DEFAULT FALSE, "
                            + "COMUNIDAD_ASIGNADA VARCHAR(100))"
            );
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
