package cl.vss.cotizador.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Modelo genérico para cotizaciones
 * Permite manejar diferentes tipos de cotizaciones de manera flexible
 */
public class Cotizacion {
    
    private String id;
    private String numeroCotizacion;
    private Cliente cliente;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaVencimiento;
    private List<ItemCotizacion> items;
    private double subtotal;
    private double descuento;
    private double impuestos;
    private double total;
    private String estado;
    private String notas;
    private String condiciones;
    
    // Metadatos de origen (Punto 2)
    private String archivoOrigen;
    private String hojaOrigen;
    private int filaOrigen;
    private String tipoDetector; // Indica qué detector procesó este archivo
    
    // Constructor por defecto
    public Cotizacion() {
        this.id = UUID.randomUUID().toString();
        this.items = new ArrayList<>();
        this.fechaCreacion = LocalDateTime.now();
        this.estado = "BORRADOR";
        this.descuento = 0.0;
        this.impuestos = 0.0;
    }
    
    // Constructor con cliente
    public Cotizacion(Cliente cliente) {
        this();
        this.cliente = cliente;
    }
    
    // Métodos para agregar items
    public void agregarItem(ItemCotizacion item) {
        this.items.add(item);
        calcularTotales();
    }
    
    public void removerItem(ItemCotizacion item) {
        this.items.remove(item);
        calcularTotales();
    }
    
    // Calcular totales automáticamente
    public void calcularTotales() {
        this.subtotal = items.stream()
            .mapToDouble(ItemCotizacion::getSubtotal)
            .sum();
        
        this.total = subtotal - descuento + impuestos;
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNumeroCotizacion() { return numeroCotizacion; }
    public void setNumeroCotizacion(String numeroCotizacion) { this.numeroCotizacion = numeroCotizacion; }
    
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    public LocalDateTime getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDateTime fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    
    public List<ItemCotizacion> getItems() { return items; }
    public void setItems(List<ItemCotizacion> items) { 
        this.items = items; 
        calcularTotales();
    }
    
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    
    public double getDescuento() { return descuento; }
    public void setDescuento(double descuento) { 
        this.descuento = descuento; 
        calcularTotales();
    }
    
    public double getImpuestos() { return impuestos; }
    public void setImpuestos(double impuestos) { 
        this.impuestos = impuestos; 
        calcularTotales();
    }
    
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    
    public String getCondiciones() { return condiciones; }
    public void setCondiciones(String condiciones) { this.condiciones = condiciones; }
    
    // Getters y Setters para metadatos de origen
    public String getArchivoOrigen() { return archivoOrigen; }
    public void setArchivoOrigen(String archivoOrigen) { this.archivoOrigen = archivoOrigen; }
    
    public String getHojaOrigen() { return hojaOrigen; }
    public void setHojaOrigen(String hojaOrigen) { this.hojaOrigen = hojaOrigen; }
    
    public int getFilaOrigen() { return filaOrigen; }
    public void setFilaOrigen(int filaOrigen) { this.filaOrigen = filaOrigen; }
    
    public String getTipoDetector() { return tipoDetector; }
    public void setTipoDetector(String tipoDetector) { this.tipoDetector = tipoDetector; }
    
    @Override
    public String toString() {
        return "Cotizacion{" +
                "numeroCotizacion='" + numeroCotizacion + '\'' +
                ", cliente=" + (cliente != null ? cliente.getNombre() : "Sin cliente") +
                ", total=" + total +
                ", estado='" + estado + '\'' +
                '}';
    }
}
