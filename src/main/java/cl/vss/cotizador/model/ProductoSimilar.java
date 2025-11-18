package cl.vss.cotizador.model;

/**
 * Clase que representa un producto similar encontrado en la búsqueda
 */
public class ProductoSimilar {
    private String descripcionEs;
    private String descripcionEn;
    private String unidadMedida;
    private double precioVentaNeto;

    public ProductoSimilar() {
    }

    public ProductoSimilar(String descripcionEs, String descripcionEn, String unidadMedida, double precioVentaNeto) {
        this.descripcionEs = descripcionEs;
        this.descripcionEn = descripcionEn;
        this.unidadMedida = unidadMedida;
        this.precioVentaNeto = precioVentaNeto;
    }

    // Getters y Setters
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

    @Override
    public String toString() {
        return "ProductoSimilar{" +
                "descripcionEs='" + descripcionEs + '\'' +
                ", descripcionEn='" + descripcionEn + '\'' +
                ", unidadMedida='" + unidadMedida + '\'' +
                ", precioVentaNeto=" + precioVentaNeto +
                '}';
    }
}