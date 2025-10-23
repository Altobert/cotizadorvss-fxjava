package com.example.model;

/**
 * Modelo para clientes en el sistema de cotizaciones
 */
public class Cliente {
    
    private String id;
    private String nombre;
    private String email;
    private String telefono;
    private String direccion;
    private String ciudad;
    private String codigoPostal;
    private String pais;
    private String tipoDocumento;
    private String numeroDocumento;
    private String notas;
    
    // Constructor por defecto
    public Cliente() {
        this.id = java.util.UUID.randomUUID().toString();
    }
    
    // Constructor básico
    public Cliente(String nombre, String email) {
        this();
        this.nombre = nombre;
        this.email = email;
    }
    
    // Constructor completo
    public Cliente(String nombre, String email, String telefono, String direccion) {
        this(nombre, email);
        this.telefono = telefono;
        this.direccion = direccion;
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    
    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }
    
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
    
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    
    // Método para obtener dirección completa
    public String getDireccionCompleta() {
        StringBuilder direccionCompleta = new StringBuilder();
        
        if (direccion != null && !direccion.isEmpty()) {
            direccionCompleta.append(direccion);
        }
        
        if (ciudad != null && !ciudad.isEmpty()) {
            if (direccionCompleta.length() > 0) direccionCompleta.append(", ");
            direccionCompleta.append(ciudad);
        }
        
        if (codigoPostal != null && !codigoPostal.isEmpty()) {
            if (direccionCompleta.length() > 0) direccionCompleta.append(" ");
            direccionCompleta.append(codigoPostal);
        }
        
        if (pais != null && !pais.isEmpty()) {
            if (direccionCompleta.length() > 0) direccionCompleta.append(", ");
            direccionCompleta.append(pais);
        }
        
        return direccionCompleta.toString();
    }
    
    @Override
    public String toString() {
        return "Cliente{" +
                "nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", telefono='" + telefono + '\'' +
                '}';
    }
}
