package cl.vss.cotizador.model;

public class Producto {
    private int id;
    private String descripcionEs;
    private String descripcionEn;
    private String unidadMedida;
    private double valorPesos;

    // Getters y setters
    public int getId() { 
        return id; 
    }
    public void setId(int id) { 
        this.id = id; 
    }

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

    public double getValorPesos() { 
        return valorPesos; 
    }
    public void setValorPesos(double valorPesos) { 
        this.valorPesos = valorPesos; 
    }
}
