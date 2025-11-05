package com.example.model;

/**
 * Representa un ítem dentro de una cotización
 */
public class ItemCotizacion {

    private String codigo;
    private String descripcion;
    private double precioUnitario;
    private int cantidad;
    private String unidad;
    private String categoria;

    // Constructor vacío
    public ItemCotizacion() {
    }

    // Constructor completo
    public ItemCotizacion(String codigo, String descripcion, double precioUnitario, int cantidad, String unidad, String categoria) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.categoria = categoria;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public double getSubtotal() {
        return precioUnitario * cantidad;
    }

    public double getDescuentoTotal() {
        // Simulación: puedes ajustar según tu lógica real
        return 0.0;
    }

    @Override
    public String toString() {
        return codigo + " - " + descripcion + " (" + cantidad + " " + unidad + ")";
    }
}
