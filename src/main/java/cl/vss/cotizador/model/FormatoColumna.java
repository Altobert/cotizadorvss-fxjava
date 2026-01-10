package cl.vss.cotizador.model;

/**
 * Modelo que representa una columna de un formato de broker
 */
public class FormatoColumna {
    private Integer columnaId;
    private Integer formatoId;
    private String campoEstandar;
    private String nombreColumnaOriginal;
    private Integer indiceColumna;
    private String letraColumna;
    private String tipoDato;
    private Boolean requerido;
    private String descripcion;
    private String colorFondo;
    private String colorTexto;
    private Boolean esNegrita;
    private Boolean esCursiva;
    private Boolean tieneBorde;
    
    public FormatoColumna() {
    }
    
    // Getters y Setters
    public Integer getColumnaId() {
        return columnaId;
    }
    
    public void setColumnaId(Integer columnaId) {
        this.columnaId = columnaId;
    }
    
    public Integer getFormatoId() {
        return formatoId;
    }
    
    public void setFormatoId(Integer formatoId) {
        this.formatoId = formatoId;
    }
    
    public String getCampoEstandar() {
        return campoEstandar;
    }
    
    public void setCampoEstandar(String campoEstandar) {
        this.campoEstandar = campoEstandar;
    }
    
    public String getNombreColumnaOriginal() {
        return nombreColumnaOriginal;
    }
    
    public void setNombreColumnaOriginal(String nombreColumnaOriginal) {
        this.nombreColumnaOriginal = nombreColumnaOriginal;
    }
    
    public Integer getIndiceColumna() {
        return indiceColumna;
    }
    
    public void setIndiceColumna(Integer indiceColumna) {
        this.indiceColumna = indiceColumna;
    }
    
    public String getLetraColumna() {
        return letraColumna;
    }
    
    public void setLetraColumna(String letraColumna) {
        this.letraColumna = letraColumna;
    }
    
    public String getTipoDato() {
        return tipoDato;
    }
    
    public void setTipoDato(String tipoDato) {
        this.tipoDato = tipoDato;
    }
    
    public Boolean getRequerido() {
        return requerido;
    }
    
    public void setRequerido(Boolean requerido) {
        this.requerido = requerido;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getColorFondo() {
        return colorFondo;
    }
    
    public void setColorFondo(String colorFondo) {
        this.colorFondo = colorFondo;
    }
    
    public String getColorTexto() {
        return colorTexto;
    }
    
    public void setColorTexto(String colorTexto) {
        this.colorTexto = colorTexto;
    }
    
    public Boolean getEsNegrita() {
        return esNegrita;
    }
    
    public void setEsNegrita(Boolean esNegrita) {
        this.esNegrita = esNegrita;
    }
    
    public Boolean getEsCursiva() {
        return esCursiva;
    }
    
    public void setEsCursiva(Boolean esCursiva) {
        this.esCursiva = esCursiva;
    }
    
    public Boolean getTieneBorde() {
        return tieneBorde;
    }
    
    public void setTieneBorde(Boolean tieneBorde) {
        this.tieneBorde = tieneBorde;
    }
    
    @Override
    public String toString() {
        return "FormatoColumna{" +
                "columnaId=" + columnaId +
                ", campoEstandar='" + campoEstandar + '\'' +
                ", nombreColumnaOriginal='" + nombreColumnaOriginal + '\'' +
                ", indiceColumna=" + indiceColumna +
                ", letraColumna='" + letraColumna + '\'' +
                '}';
    }
}
