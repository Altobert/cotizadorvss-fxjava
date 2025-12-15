package cl.vss.cotizador.service;

import java.sql.*;
import cl.vss.cotizador.model.Usuario;
import org.mindrot.jbcrypt.BCrypt; // librería para encriptar y validar contraseñas

public class UsuarioDAO {
    private Connection conn;

    // Constructor recibe la conexión a la BD
    public UsuarioDAO(Connection conn) {
        this.conn = conn;
    }

    // Validar login: correo + contraseña
    public Usuario validarLogin(String correo, String password) {
        String sql = "SELECT * FROM usuario WHERE correo = ? AND activo = TRUE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, correo);
            ResultSet rs = ps.executeQuery();

            System.out.println("Intento login: correo=" + correo + ", password=" + password);

            if (rs.next()) {
                String hash = rs.getString("password_hash");
                System.out.println("Hash en BD: " + hash);

                boolean ok = BCrypt.checkpw(password, hash);
                System.out.println("Resultado checkpw: " + ok);

                if (ok) {
                    Usuario u = new Usuario();
                    u.setId(rs.getLong("id"));
                    u.setNombre(rs.getString("nombre"));
                    u.setCorreo(rs.getString("correo"));
                    u.setRol(rs.getString("rol"));
                    u.setActivo(rs.getBoolean("activo"));
                    u.setPasswordHash(hash);
                    return u;
                }
            } else {
                System.out.println("No se encontró usuario con correo " + correo + " y activo=TRUE");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Buscar usuario por correo (para saber si existe)
    public Usuario buscarPorCorreo(String correo) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE correo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, correo);
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

    // Insertar usuario con hash ya generado (usado por LoginController)
    public void insertarUsuario(String nombre, String correo, String rol, String passwordHash) throws SQLException {
        String sql = "INSERT INTO usuario (nombre, correo, rol, activo, fecha_creacion, password_hash) VALUES (?, ?, ?, TRUE, NOW(), ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, correo);
            ps.setString(3, rol);
            ps.setString(4, passwordHash);
            ps.executeUpdate();
        }
    }

    // Crear nuevo usuario generando hash internamente (solo admin)
    public void crearUsuario(String nombre, String correo, String rol, boolean activo, String password) {
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO usuario (nombre, correo, rol, activo, fecha_creacion, password_hash) VALUES (?, ?, ?, ?, NOW(), ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, correo);
            ps.setString(3, rol);
            ps.setBoolean(4, activo);
            ps.setString(5, hash);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
