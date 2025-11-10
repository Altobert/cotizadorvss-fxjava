package cl.vss.cotizador.model;

import javafx.beans.property.*;

public class ItemCotizacionExcel {

    private final StringProperty codigo = new SimpleStringProperty();
    private final StringProperty descripcion = new SimpleStringProperty();
    private final IntegerProperty cantidad = new SimpleIntegerProperty();
    private final DoubleProperty precio = new SimpleDoubleProperty();
    private final StringProperty unidad = new SimpleStringProperty();
    private final StringProperty categoria = new SimpleStringProperty();

    // Campos adicionales
    private final DoubleProperty descuento = new SimpleDoubleProperty();
    private final DoubleProperty totalNeto = new SimpleDoubleProperty();
    private final StringProperty comentarios = new SimpleStringProperty();
    private final IntegerProperty disponibilidad = new SimpleIntegerProperty();
    private final DoubleProperty totalBruto = new SimpleDoubleProperty();

    public ItemCotizacionExcel(String codigo, String descripcion, int cantidad, double precio,
                               String unidad, String categoria,
                               double descuento, double totalNeto,
                               String comentarios, int disponibilidad, double totalBruto) {
        this.codigo.set(codigo);
        this.descripcion.set(descripcion);
        this.cantidad.set(cantidad);
        this.precio.set(precio);
        this.unidad.set(unidad);
        this.categoria.set(categoria);
        this.descuento.set(descuento);
        this.totalNeto.set(totalNeto);
        this.comentarios.set(comentarios);
        this.disponibilidad.set(disponibilidad);
        this.totalBruto.set(totalBruto);
    }

    public ItemCotizacionExcel(String codigo, String descripcion, int cantidad, double precio,
                               String unidad, String categoria) {
        this(codigo, descripcion, cantidad, precio, unidad, categoria, 0.0, 0.0, "", 0, 0.0);
    }

    // Getters
    public String getCodigo() { return codigo.get(); }
    public String getDescripcion() { return descripcion.get(); }
    public int getCantidad() { return cantidad.get(); }
    public double getPrecio() { return precio.get(); }
    public String getUnidad() { return unidad.get(); }
    public String getCategoria() { return categoria.get(); }
    public double getDescuento() { return descuento.get(); }
    public double getTotalNeto() { return totalNeto.get(); }
    public String getComentarios() { return comentarios.get(); }
    public int getDisponibilidad() { return disponibilidad.get(); }
    public double getTotalBruto() { return totalBruto.get(); }

    // Total calculado directamente
    public double getTotal() {
        return getCantidad() * getPrecio();
    }

    // Setters
    public void setCodigo(String value) { codigo.set(value); }
    public void setDescripcion(String value) { descripcion.set(value); }
    public void setCantidad(int value) { cantidad.set(value); }
    public void setPrecio(double value) { precio.set(value); }
    public void setUnidad(String value) { unidad.set(value); }
    public void setCategoria(String value) { categoria.set(value); }
    public void setDescuento(double value) { descuento.set(value); }
    public void setTotalNeto(double value) { totalNeto.set(value); }
    public void setComentarios(String value) { comentarios.set(value); }
    public void setDisponibilidad(int value) { disponibilidad.set(value); }
    public void setTotalBruto(double value) { totalBruto.set(value); }

    // Properties
    public StringProperty codigoProperty() { return codigo; }
    public StringProperty descripcionProperty() { return descripcion; }
    public IntegerProperty cantidadProperty() { return cantidad; }
    public DoubleProperty precioProperty() { return precio; }
    public StringProperty unidadProperty() { return unidad; }
    public StringProperty categoriaProperty() { return categoria; }
    public DoubleProperty descuentoProperty() { return descuento; }
    public DoubleProperty totalNetoProperty() { return totalNeto; }
    public StringProperty comentariosProperty() { return comentarios; }
    public IntegerProperty disponibilidadProperty() { return disponibilidad; }
    public DoubleProperty totalBrutoProperty() { return totalBruto; }
}
