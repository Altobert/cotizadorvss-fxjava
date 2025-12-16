package cl.vss.cotizador.service;

import java.time.LocalDateTime;

/**
 * POJO para representar un registro de auditoría.
 */
public class AuditoriaRegistro {
    private String parametro;
    private String valorAnterior;
    private String valorNuevo;
    private String usuario;
    private String correoUsuario;
    private LocalDateTime fecha;

    public AuditoriaRegistro(String parametro, String valorAnterior, String valorNuevo,
                             String usuario, String correoUsuario, LocalDateTime fecha) {
        this.parametro = parametro;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.usuario = usuario;
        this.correoUsuario = correoUsuario;
        this.fecha = fecha;
    }

    // Getters
    public String getParametro() { return parametro; }
    public String getValorAnterior() { return valorAnterior; }
    public String getValorNuevo() { return valorNuevo; }
    public String getUsuario() { return usuario; }
    public String getCorreoUsuario() { return correoUsuario; }
    public LocalDateTime getFecha() { return fecha; }
}


