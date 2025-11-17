package cl.vss.cotizador.model;

import java.time.LocalDate;

public class ParametroComercial {
    private int id;
    private double tipoCambioUsado;
    private double porcentajeUtilidad;
    private LocalDate fechaVigencia;

    // Getters y setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getTipoCambioUsado() { return tipoCambioUsado; }
    public void setTipoCambioUsado(double tipoCambioUsado) { this.tipoCambioUsado = tipoCambioUsado; }

    public double getPorcentajeUtilidad() { return porcentajeUtilidad; }
    public void setPorcentajeUtilidad(double porcentajeUtilidad) { this.porcentajeUtilidad = porcentajeUtilidad; }

    public LocalDate getFechaVigencia() { return fechaVigencia; }
    public void setFechaVigencia(LocalDate fechaVigencia) { this.fechaVigencia = fechaVigencia; }
}
