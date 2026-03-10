package cl.vss.cotizador.model;

/**
 * Clase que representa un producto similar encontrado en la búsqueda
 */
public class ProductoSimilar {

    // 🔵 NUEVO: ID REAL DEL PRODUCTO (NO ROMPE NADA)
    private int idProducto;

    private String descripcionEs;
    private String descripcionEn;
    private String unidadMedida;
    private double precioVentaNeto;
    private double precioVentaNetoDolares;
    private double valorPesos;

    public ProductoSimilar() {
        this.precioVentaNetoDolares = 0.0;
    }

    // 🔵 Constructor original (SE MANTIENE para no romper el service)
    public ProductoSimilar(String descripcionEs, String descripcionEn, String unidadMedida,
                            double precioVentaNeto, double precioVentaNetoDolares, double valorPesos) {
        this.descripcionEs = descripcionEs;
        this.descripcionEn = descripcionEn;
        this.unidadMedida = unidadMedida;
        this.precioVentaNeto = precioVentaNeto;
        this.precioVentaNetoDolares = precioVentaNetoDolares;
        this.valorPesos = valorPesos;
    }

    // 🔵 Getter/Setter del ID (nuevo)
    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    // Getters y Setters existentes
    public String getDescripcionEs() {
        return descripcionEs;
    }

    public void setDescripcionEs(String descripcionEs) {
        this.descripcionEs = descripcionEs;
    }

    public String getDescripcionEn() {
        return descripcionEn;
    }

    public void setDescripcionEn(String descripcionEn) {
        this.descripcionEn = descripcionEn;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public double getPrecioVentaNeto() {
        return precioVentaNeto;
    }

    public void setPrecioVentaNeto(double precioVentaNeto) {
        this.precioVentaNeto = precioVentaNeto;
    }

    public double getPrecioVentaNetoDolares() {
        return precioVentaNetoDolares;
    }

    public void setPrecioVentaNetoDolares(double precioVentaNetoDolares) {
        this.precioVentaNetoDolares = precioVentaNetoDolares;
    }

    public double getValorPesos() {
        return valorPesos;
    }

    public void setValorPesos(double valorPesos) {
        this.valorPesos = valorPesos;
    }

    @Override
    public String toString() {
        return "ProductoSimilar{" +
                "idProducto=" + idProducto +
                ", descripcionEs='" + descripcionEs + '\'' +
                ", descripcionEn='" + descripcionEn + '\'' +
                ", unidadMedida='" + unidadMedida + '\'' +
                ", precioVentaNeto=" + precioVentaNeto +
                ", precioVentaNetoDolares=" + precioVentaNetoDolares +
                ", valorPesos=" + valorPesos +
                '}';
    }
}
