package cl.vss.cotizador.model;

import java.time.LocalDateTime;

public class Producto {
    private int id;
    private String descripcionEs;
    private String descripcionEn;
    private String unidadMedida;
    private double valorPesos;
    private int familiaId;                // nuevo campo
    private LocalDateTime fechaActualizacion; // trazabilidad

    // 👉 Constructores
    public Producto() {
    }

    public Producto(int id, String descripcionEs, String descripcionEn, String unidadMedida, double valorPesos) {
        this.id = id;
        this.descripcionEs = descripcionEs;
        this.descripcionEn = descripcionEn;
        this.unidadMedida = unidadMedida;
        this.valorPesos = valorPesos;
    }

    // 👉 Getters y setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDescripcionEs() { return descripcionEs; }
    public void setDescripcionEs(String descripcionEs) { this.descripcionEs = descripcionEs; }

    public String getDescripcionEn() { return descripcionEn; }
    public void setDescripcionEn(String descripcionEn) { this.descripcionEn = descripcionEn; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public double getValorPesos() { return valorPesos; }
    public void setValorPesos(double valorPesos) { this.valorPesos = valorPesos; }

    public int getFamiliaId() { return familiaId; }
    public void setFamiliaId(int familiaId) { this.familiaId = familiaId; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    // 👉 toString()
    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
                ", descripcionEs='" + descripcionEs + '\'' +
                ", descripcionEn='" + descripcionEn + '\'' +
                ", unidadMedida='" + unidadMedida + '\'' +
                ", valorPesos=" + valorPesos +
                ", familiaId=" + familiaId +
                ", fechaActualizacion=" + fechaActualizacion +
                '}';
    }
}
