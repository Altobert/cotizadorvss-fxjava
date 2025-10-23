package com.example.model;

/**
 * Modelo para items/productos en una cotización
 */
public class ItemCotizacion {
    
    private String id;
    private String codigo;
    private String descripcion;
    private double precioUnitario;
    private int cantidad;
    private double descuentoPorcentaje;
    private double descuentoMonto;
    private String unidad;
    private String categoria;
    private String notas;
    
    // Constructor por defecto
    public ItemCotizacion() {
        this.id = java.util.UUID.randomUUID().toString();
        this.cantidad = 1;
        this.descuentoPorcentaje = 0.0;
        this.descuentoMonto = 0.0;
        this.unidad = "unidad";
    }
    
    // Constructor básico
    public ItemCotizacion(String descripcion, double precioUnitario, int cantidad) {
        this();
        this.descripcion = descripcion;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
    }
    
    // Constructor completo
    public ItemCotizacion(String codigo, String descripcion, double precioUnitario, 
                         int cantidad, String unidad, String categoria) {
        this(descripcion, precioUnitario, cantidad);
        this.codigo = codigo;
        this.unidad = unidad;
        this.categoria = categoria;
    }
    
    // Calcular subtotal del item
    public double getSubtotal() {
        double subtotal = precioUnitario * cantidad;
        
        // Aplicar descuento por porcentaje
        if (descuentoPorcentaje > 0) {
            subtotal = subtotal * (1 - descuentoPorcentaje / 100);
        }
        
        // Aplicar descuento por monto
        if (descuentoMonto > 0) {
            subtotal = subtotal - descuentoMonto;
        }
        
        return Math.max(0, subtotal); // No permitir valores negativos
    }
    
    // Calcular descuento total aplicado
    public double getDescuentoTotal() {
        double subtotalSinDescuento = precioUnitario * cantidad;
        return subtotalSinDescuento - getSubtotal();
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    
    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
    
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    
    public double getDescuentoPorcentaje() { return descuentoPorcentaje; }
    public void setDescuentoPorcentaje(double descuentoPorcentaje) { 
        this.descuentoPorcentaje = descuentoPorcentaje; 
    }
    
    public double getDescuentoMonto() { return descuentoMonto; }
    public void setDescuentoMonto(double descuentoMonto) { 
        this.descuentoMonto = descuentoMonto; 
    }
    
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    
    @Override
    public String toString() {
        return "ItemCotizacion{" +
                "codigo='" + codigo + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", cantidad=" + cantidad +
                ", precioUnitario=" + precioUnitario +
                ", subtotal=" + getSubtotal() +
                '}';
    }
}
