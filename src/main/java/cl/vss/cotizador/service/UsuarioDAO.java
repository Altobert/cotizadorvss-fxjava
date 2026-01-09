package cl.vss.cotizador.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import cl.vss.cotizador.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;

public class UsuarioDAO {

    private static final Logger logger = LogManager.getLogger(UsuarioDAO.class);
    private Connection conn;

    public UsuarioDAO(Connection conn) {
        this.conn = conn;
    }

    // ============================================================
    // VALIDAR LOGIN
    // ============================================================
    public Usuario validarLogin(String correo, String password) {
        logger.info("Intento de login para correo: {}", correo);
        String sql = "SELECT * FROM usuario WHERE correo = ? AND activo = TRUE";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, correo.trim());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String hash = rs.getString("password_hash");

                if (BCrypt.checkpw(password, hash)) {
                    Usuario u = new Usuario();
                    u.setId(rs.getLong("id"));
                    u.setNombre(rs.getString("nombre"));
                    u.setCorreo(rs.getString("correo"));
                    u.setRol(rs.getString("rol"));
                    u.setActivo(rs.getBoolean("activo"));
                    u.setPasswordHash(hash);

                    logger.info("Login exitoso para usuario: {} ({})", u.getNombre(), correo);
                    return u;
                } else {
                    logger.warn("Contraseña incorrecta para correo: {}", correo);
                }
            } else {
                logger.warn("No se encontró usuario con correo {} y activo=TRUE", correo);
            }

        } catch (SQLException ex) {
            logger.error("Error durante validación de login para correo: {}", correo, ex);
        }

        return null;
    }

    // ============================================================
    // BUSCAR POR CORREO
    // ============================================================
    public Usuario buscarPorCorreo(String correo) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE correo = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, correo.trim());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Usuario u = new Usuario();
                u.setId(rs.getLong("id"));
                u.setNombre(rs.getString("nombre"));
                u.setCorreo(rs.getString("correo"));
                u.setRol(rs.getString("rol"));
                u.setActivo(rs.getBoolean("activo"));
                u.setPasswordHash(rs.getString("password_hash"));
                return u;
            }
        }

        return null;
    }

    // ============================================================
    // INSERTAR USUARIO (HASH YA GENERADO)
    // ============================================================
    public void insertarUsuario(String nombre, String correo, String rol, String passwordHash) throws SQLException {
        logger.info("Insertando nuevo usuario: nombre={}, correo={}, rol={}", nombre, correo, rol);

        String sql = "INSERT INTO usuario (nombre, correo, rol, activo, fecha_creacion, password_hash) " +
                     "VALUES (?, ?, ?, TRUE, NOW(), ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, correo.trim());
            ps.setString(3, rol);
            ps.setString(4, passwordHash);
            ps.executeUpdate();

            logger.info("Usuario insertado exitosamente: {}", correo);

        } catch (SQLException ex) {
            logger.error("Error al insertar usuario: {}", correo, ex);
            throw ex;
        }
    }

    // ============================================================
    // CREAR USUARIO (HASH INTERNO)
    // ============================================================
    public void crearUsuario(String nombre, String correo, String rol, boolean activo, String password) {
        logger.info("Creando nuevo usuario: nombre={}, correo={}, rol={}, activo={}", nombre, correo, rol, activo);

        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO usuario (nombre, correo, rol, activo, fecha_creacion, password_hash) " +
                     "VALUES (?, ?, ?, ?, NOW(), ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, correo.trim());
            ps.setString(3, rol);
            ps.setBoolean(4, activo);
            ps.setString(5, hash);
            ps.executeUpdate();

            logger.info("Usuario creado exitosamente: {}", correo);

        } catch (SQLException ex) {
            logger.error("Error al crear usuario: {}", correo, ex);
        }
    }

    // ============================================================
    // LISTAR USUARIOS (CRUD)
    // ============================================================
    public List<Usuario> listarUsuarios() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuario ORDER BY id";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Usuario u = new Usuario();
                u.setId(rs.getLong("id"));
                u.setNombre(rs.getString("nombre"));
                u.setCorreo(rs.getString("correo"));
                u.setRol(rs.getString("rol"));
                u.setActivo(rs.getBoolean("activo"));
                u.setPasswordHash(rs.getString("password_hash"));
                lista.add(u);
            }
        }

        return lista;
    }

    // ============================================================
    // ELIMINAR USUARIO
    // ============================================================
    public void eliminarUsuario(long id) throws SQLException {
        String sql = "DELETE FROM usuario WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // ============================================================
    // ACTUALIZAR USUARIO
    // ============================================================
    public void actualizarUsuario(Usuario u) throws SQLException {
        String sql = "UPDATE usuario SET nombre = ?, correo = ?, rol = ?, activo = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getCorreo().trim());
            ps.setString(3, u.getRol());
            ps.setBoolean(4, u.isActivo());
            ps.setLong(5, u.getId());
            ps.executeUpdate();
        }
    }
}
