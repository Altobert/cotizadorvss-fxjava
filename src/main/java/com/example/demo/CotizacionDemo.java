package com.example.demo;

import com.example.service.CotizacionService;

import java.io.File;

public class CotizacionDemo {

    public static void main(String[] args) {
        CotizacionService cotizacionService = new CotizacionService();

        // Ruta al archivo Excel que quieres actualizar
        File archivo = new File("FERNANDINA.xlsx");

        // Ejecuta la actualización de precios
        boolean exito = cotizacionService.actualizarArchivoConPrecios(archivo);

        if (exito) {
            System.out.println("✅ Archivo actualizado con nuevos precios.");
        } else {
            System.out.println("❌ Error al actualizar el archivo.");
        }
    }
}
