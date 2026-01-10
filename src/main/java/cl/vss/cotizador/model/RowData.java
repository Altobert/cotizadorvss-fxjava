package cl.vss.cotizador.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase para representar filas dinámicas en la tabla de cotizaciones
 * Permite manejar diferentes formatos de brokers con distintas columnas
 */
public class RowData {
    private final Map<String, StringProperty> data;
    
    public RowData() {
        this.data = new HashMap<>();
    }
    
    /**
     * Establece el valor de una celda
     * @param key Nombre de la columna
     * @param value Valor de la celda
     */
    public void set(String key, String value) {
        if (!data.containsKey(key)) {
            data.put(key, new SimpleStringProperty(value));
        } else {
            data.get(key).set(value);
        }
    }
    
    /**
     * Obtiene el valor de una celda
     * @param key Nombre de la columna
     * @return Valor de la celda o cadena vacía si no existe
     */
    public String get(String key) {
        StringProperty prop = data.get(key);
        return prop != null ? prop.get() : "";
    }
    
    /**
     * Obtiene la propiedad de una celda (para binding con JavaFX)
     * @param key Nombre de la columna
     * @return StringProperty de la celda
     */
    public StringProperty getProperty(String key) {
        if (!data.containsKey(key)) {
            data.put(key, new SimpleStringProperty(""));
        }
        return data.get(key);
    }
    
    /**
     * Verifica si existe una columna en los datos
     * @param key Nombre de la columna
     * @return true si existe, false si no
     */
    public boolean hasKey(String key) {
        return data.containsKey(key);
    }
    
    /**
     * Obtiene todas las claves (nombres de columnas)
     * @return Set de nombres de columnas
     */
    public java.util.Set<String> getKeys() {
        return data.keySet();
    }
    
    @Override
    public String toString() {
        return "RowData{" + data + "}";
    }
}
