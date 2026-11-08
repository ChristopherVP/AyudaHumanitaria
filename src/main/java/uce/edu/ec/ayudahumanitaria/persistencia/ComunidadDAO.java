package uce.edu.ec.ayudahumanitaria.persistencia;

import uce.edu.ec.ayudahumanitaria.dominio.Comunidad;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ComunidadDAO {

    public void insertar(Comunidad c) throws PersistenciaException {
        String sql = "INSERT INTO COMUNIDAD (NOMBRE, PROVINCIA, HABITANTES, PRIORIDAD, RECURSOS_REQUERIDOS, "
                + "LATITUD, LONGITUD, DIRECCION) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = ConexionBD.obtenerConexion()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getProvincia());
            ps.setInt(3, c.getHabitantes());
            ps.setInt(4, c.getPrioridad());
            ps.setDouble(5, c.getRecursosRequeridos());
            ps.setDouble(6, c.getLatitud());
            ps.setDouble(7, c.getLongitud());
            ps.setString(8, c.getDireccion());
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    c.setId(llaves.getInt(1));
                }
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo registrar la comunidad.", ex);
        }
    }

    public void actualizar(Comunidad c) throws PersistenciaException {
        String sql = "UPDATE COMUNIDAD SET NOMBRE = ?, PROVINCIA = ?, HABITANTES = ?, PRIORIDAD = ?, "
                + "RECURSOS_REQUERIDOS = ?, LATITUD = ?, LONGITUD = ?, DIRECCION = ? WHERE ID = ?";
        try (PreparedStatement ps = ConexionBD.obtenerConexion().prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getProvincia());
            ps.setInt(3, c.getHabitantes());
            ps.setInt(4, c.getPrioridad());
            ps.setDouble(5, c.getRecursosRequeridos());
            ps.setDouble(6, c.getLatitud());
            ps.setDouble(7, c.getLongitud());
            ps.setString(8, c.getDireccion());
            ps.setInt(9, c.getId());

            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas == 0) {
                throw new PersistenciaException("No existe una comunidad con ID " + c.getId() + ".");
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo actualizar la comunidad.", ex);
        }
    }

    public void eliminar(int id) throws PersistenciaException {
        String sql = "DELETE FROM COMUNIDAD WHERE ID = ?";
        try (PreparedStatement ps = ConexionBD.obtenerConexion().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo eliminar la comunidad.", ex);
        }
    }

    public List<Comunidad> listarTodas() throws PersistenciaException {
        String sql = "SELECT * FROM COMUNIDAD";
        List<Comunidad> resultado = new ArrayList<>();

        try (Statement st = ConexionBD.obtenerConexion().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo obtener el listado de comunidades.", ex);
        }
        return resultado;
    }

    private Comunidad mapear(ResultSet rs) throws SQLException {
        return new Comunidad(
                rs.getInt("ID"),
                rs.getString("NOMBRE"),
                rs.getString("PROVINCIA"),
                rs.getInt("HABITANTES"),
                rs.getInt("PRIORIDAD"),
                rs.getDouble("RECURSOS_REQUERIDOS"),
                rs.getDouble("LATITUD"),
                rs.getDouble("LONGITUD"),
                rs.getString("DIRECCION")
        );
    }
}
