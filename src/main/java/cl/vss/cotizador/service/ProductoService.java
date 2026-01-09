package cl.vss.cotizador.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cl.vss.cotizador.model.Producto;
import cl.vss.cotizador.model.Familia;
import cl.vss.cotizador.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductoService {
    private static final Logger logger = LogManager.getLogger(ProductoService.class);

    // CREATE
    public void agregarProducto(Producto p) throws Exception {
        logger.info("Agregando producto: {} | Familia ID: {} | Precio: ${}", 
                    p.getDescripcionEs(), p.getFamiliaId(), p.getValorPesos());
        
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
            logger.info("Producto agregado exitosamente: {}", p.getDescripcionEs());
        } catch (Exception e) {
            logger.error("Error al agregar producto: {}", p.getDescripcionEs(), e);
            throw e;
        }
    }

    // READ productos
    public List<Producto> listarProductos() {
        logger.debug("Consultando lista de productos");
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
            logger.info("Se recuperaron {} productos", lista.size());
        } catch (Exception e) {
            logger.error("Error al listar productos", e);
            e.printStackTrace();
        }
        return lista;
    }

    // READ familias (como objetos)
    public List<Familia> listarFamilias() {
        logger.debug("Consultando lista de familias");
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
            logger.info("Se recuperaron {} familias", lista.size());
        } catch (Exception e) {
            logger.error("Error al listar familias", e);
            e.printStackTrace();
        }
        return lista;
    }

    // ✅ Listar familias como String (para ComboBox)
    public List<String> listarFamiliasNombres() {
        List<String> familias = new ArrayList<>();
        String sql = "SELECT nombre FROM familia_producto ORDER BY nombre";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                familias.add(rs.getString("nombre"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return familias;
    }

    // ✅ Listar productos por familia
    public List<Producto> listarPorFamilia(String familiaNombre) {
        logger.debug("Consultando productos por familia: {}", familiaNombre);
        List<Producto> lista = new ArrayList<>();
        String sql = """
            SELECT p.id, p.descripcion_es, p.descripcion_en, p.unidad_medida, 
                   p.valor_pesos, p.familia_id, p.fecha_actualizacion
            FROM producto p
            JOIN familia_producto f ON p.familia_id = f.id
            WHERE f.nombre = ?
            ORDER BY p.descripcion_es
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, familiaNombre);
            ResultSet rs = ps.executeQuery();

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
            logger.info("Se recuperaron {} productos para familia: {}", lista.size(), familiaNombre);

        } catch (Exception e) {
            logger.error("Error al listar productos por familia: {}", familiaNombre, e);
            e.printStackTrace();
        }

        return lista;
    }

    // ✅ Convertir nombre de familia → id de familia
    public int obtenerIdFamiliaPorNombre(String nombreFamilia) {
        String sql = "SELECT id FROM familia_producto WHERE nombre = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombreFamilia);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1; // si no existe
    }

    // ✅ NUEVO: Mapa optimizado nombre → id (para filtros rápidos)
    public Map<String, Integer> obtenerMapaFamilias() {
        Map<String, Integer> mapa = new HashMap<>();
        String sql = "SELECT id, nombre FROM familia_producto";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                mapa.put(rs.getString("nombre"), rs.getInt("id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mapa;
    }

    // UPDATE
    public void actualizarProducto(Producto p) throws Exception {
        logger.info("Actualizando producto ID {}: {} | Familia ID: {} | Precio: ${}", 
                    p.getId(), p.getDescripcionEs(), p.getFamiliaId(), p.getValorPesos());
        
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
            logger.info("Producto ID {} actualizado exitosamente", p.getId());
        } catch (Exception e) {
            logger.error("Error al actualizar producto ID {}: {}", p.getId(), p.getDescripcionEs(), e);
            throw e;
        }
    }

    // DELETE
    public void eliminarProducto(int id) throws Exception {
        logger.warn("Eliminando producto ID: {}", id);
        
        String sql = "DELETE FROM producto WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Producto ID {} eliminado exitosamente", id);
            } else {
                logger.warn("No se encontró producto con ID {} para eliminar", id);
            }
        } catch (Exception e) {
            logger.error("Error al eliminar producto ID: {}", id, e);
            throw e;
        }
    }
}
