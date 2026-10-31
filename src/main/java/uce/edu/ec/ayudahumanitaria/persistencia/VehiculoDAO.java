package uce.edu.ec.ayudahumanitaria.persistencia;

import uce.edu.ec.ayudahumanitaria.dominio.Vehiculo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class VehiculoDAO {

    public void insertar(Vehiculo v) throws PersistenciaException {
        String sql = "INSERT INTO VEHICULO (ID, CAPACIDAD, ASIGNADO, COMUNIDAD_ASIGNADA) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = ConexionBD.obtenerConexion().prepareStatement(sql)) {
            ps.setString(1, v.getId());
            ps.setDouble(2, v.getCapacidad());
            ps.setBoolean(3, v.isAsignado());
            ps.setString(4, v.getComunidadAsignada());
            ps.executeUpdate();
        } catch (SQLException ex) {
            if (ex.getSQLState() != null && ex.getSQLState().startsWith("23")) {
                throw new PersistenciaException(
                        "Ya existe un vehículo con el identificador " + v.getId() + ".", ex);
            }
            throw new PersistenciaException("No se pudo registrar el vehículo.", ex);
        }
    }

    public void actualizar(Vehiculo v) throws PersistenciaException {
        String sql = "UPDATE VEHICULO SET CAPACIDAD = ?, ASIGNADO = ?, COMUNIDAD_ASIGNADA = ? WHERE ID = ?";
        try (PreparedStatement ps = ConexionBD.obtenerConexion().prepareStatement(sql)) {
            ps.setDouble(1, v.getCapacidad());
            ps.setBoolean(2, v.isAsignado());
            ps.setString(3, v.getComunidadAsignada());
            ps.setString(4, v.getId());

            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas == 0) {
                throw new PersistenciaException("No existe un vehículo con ID " + v.getId() + ".");
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo actualizar el vehículo.", ex);
        }
    }

    public void eliminar(String id) throws PersistenciaException {
        String sql = "DELETE FROM VEHICULO WHERE ID = ?";
        try (PreparedStatement ps = ConexionBD.obtenerConexion().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo eliminar el vehículo.", ex);
        }
    }

    public List<Vehiculo> listarTodos() throws PersistenciaException {
        String sql = "SELECT * FROM VEHICULO";
        List<Vehiculo> resultado = new ArrayList<>();

        try (Statement st = ConexionBD.obtenerConexion().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo obtener el listado de vehículos.", ex);
        }
        return resultado;
    }

    private Vehiculo mapear(ResultSet rs) throws SQLException {
        Vehiculo v = new Vehiculo(rs.getString("ID"), rs.getDouble("CAPACIDAD"));
        v.setAsignado(rs.getBoolean("ASIGNADO"));
        v.setComunidadAsignada(rs.getString("COMUNIDAD_ASIGNADA"));
        return v;
    }
}
