package cl.vss.cotizador.service;

import cl.vss.cotizador.model.Producto;
import cl.vss.cotizador.util.DBConnection;
import java.sql.*;
import java.util.*;

public class ProductoService {

    public List<Producto> listarProductos() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT id, descripcion_es, descripcion_en, unidad_medida, valor_pesos FROM producto";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Producto p = new Producto();
                p.setId(rs.getInt("id"));
                p.setDescripcionEs(rs.getString("descripcion_es"));
                p.setDescripcionEn(rs.getString("descripcion_en"));
                p.setUnidadMedida(rs.getString("unidad_medida"));
                p.setValorPesos(rs.getDouble("valor_pesos"));
                lista.add(p);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public void guardarProducto(Producto p) {
        String sql = "INSERT INTO producto (descripcion_es, descripcion_en, unidad_medida, valor_pesos, fecha_actualizacion) VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getDescripcionEs());
            ps.setString(2, p.getDescripcionEn());
            ps.setString(3, p.getUnidadMedida());
            ps.setDouble(4, p.getValorPesos());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void actualizarProducto(Producto p) {
        String sql = "UPDATE producto SET descripcion_es=?, descripcion_en=?, unidad_medida=?, valor_pesos=?, fecha_actualizacion=NOW() WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getDescripcionEs());
            ps.setString(2, p.getDescripcionEn());
            ps.setString(3, p.getUnidadMedida());
            ps.setDouble(4, p.getValorPesos());
            ps.setInt(5, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void eliminarProducto(int id) {
        String sql = "DELETE FROM producto WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
