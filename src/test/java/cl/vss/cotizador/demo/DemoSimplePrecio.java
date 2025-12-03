package cl.vss.cotizador.demo;

import cl.vss.cotizador.service.CotizacionService;

public class DemoSimplePrecio {
    
    public static void main(String[] args) {
        System.out.println("=== DEMO MÉTODOS CONSULTA PRECIO ===");
        
        CotizacionService service = new CotizacionService();
        
        // Ejemplos de descripciones para probar
        String[] descripcionesEjemplo = {
            "CHOCOLATE",
            "AJINOMOTO", 
            "ALIÑO COMPLETO",
            "PAN RALLADO",
            "ATUN EN AGUA",
            "producto_inexistente"
        };
        
        System.out.println("\n🔍 Probando consultarPrecioPorDescripcion():");
        System.out.println("==============================================");
        for (String descripcion : descripcionesEjemplo) {
            double precio = service.consultarPrecioPorDescripcion(descripcion);
            System.out.printf("%-20s | Precio: $%.2f%n", descripcion, precio);
        }
        
        System.out.println("\n🎯 Probando consultarPrecioExactoPorDescripcion():");
        System.out.println("==================================================");
        String[] descripcionesExactas = {
            "AJINOMOTO",
            "ALL SPICE POWDER 1 KG",
            "PAN RALLADO 250 GR",
            "ATUN EN AGUA 170 GR"
        };
        
        for (String descripcion : descripcionesExactas) {
            double precio = service.consultarPrecioExactoPorDescripcion(descripcion);
            System.out.printf("%-25s | Precio: $%.2f%n", descripcion, precio);
        }
        
        System.out.println("\n=== FIN DEMO ===");
    }
}