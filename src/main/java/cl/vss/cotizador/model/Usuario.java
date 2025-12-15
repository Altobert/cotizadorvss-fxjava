package cl.vss.cotizador.model;

public class Usuario {
    private Long id;
    private String nombre;
    private String correo;
    private String rol;
    private boolean activo;
    private String passwordHash;

    // Constructor vacío
    public Usuario() {}

    // Constructor con parámetros
    public Usuario(Long id, String nombre, String correo, String rol, boolean activo, String passwordHash) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
        this.activo = activo;
        this.passwordHash = passwordHash;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
