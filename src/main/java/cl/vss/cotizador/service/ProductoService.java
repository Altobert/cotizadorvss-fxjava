package cl.vss.cotizador.service;

import cl.vss.cotizador.model.Producto;
import cl.vss.cotizador.model.Familia;
import cl.vss.cotizador.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProductoService {

    // CREATE
    public void agregarProducto(Producto p) throws Exception {
        String sql = "INSERT INTO producto (descripcion_es, descripcion_en, unidad_medida, valor_pesos, familia_id, fecha_actualizacion) " +
                     "VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getDescripcionEs());
            ps.setString(2, p.getDescripcionEn());
            ps.setString(3, p.getUnidadMedida());
            ps.setDouble(4, p.getValorPesos());
            ps.setInt(5, p.getFamiliaId());
            ps.executeUpdate();
        }
    }

    // READ productos
    public List<Producto> listarProductos() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT id, descripcion_es, descripcion_en, unidad_medida, valor_pesos, familia_id, fecha_actualizacion " +
                     "FROM producto ORDER BY id DESC";
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
                p.setFamiliaId(rs.getInt("familia_id"));
                if (rs.getTimestamp("fecha_actualizacion") != null) {
                    p.setFechaActualizacion(rs.getTimestamp("fecha_actualizacion").toLocalDateTime());
                }
                lista.add(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    // READ familias
    public List<Familia> listarFamilias() {
        List<Familia> lista = new ArrayList<>();
        String sql = "SELECT id, nombre FROM familia_producto ORDER BY nombre";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Familia f = new Familia();
                f.setId(rs.getInt("id"));
                f.setNombre(rs.getString("nombre"));
                lista.add(f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    // UPDATE
    public void actualizarProducto(Producto p) throws Exception {
        String sql = "UPDATE producto SET descripcion_es=?, descripcion_en=?, unidad_medida=?, valor_pesos=?, familia_id=?, fecha_actualizacion=NOW() " +
                     "WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getDescripcionEs());
            ps.setString(2, p.getDescripcionEn());
            ps.setString(3, p.getUnidadMedida());
            ps.setDouble(4, p.getValorPesos());
            ps.setInt(5, p.getFamiliaId());
            ps.setInt(6, p.getId());
            ps.executeUpdate();
        }
    }

    // DELETE
    public void eliminarProducto(int id) throws Exception {
        String sql = "DELETE FROM producto WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
