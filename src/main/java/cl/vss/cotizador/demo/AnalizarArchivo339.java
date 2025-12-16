package cl.vss.cotizador.demo;

import cl.vss.cotizador.service.CotizacionService;

/**
 * 🔍 ANALIZADOR SIMPLE PARA ARCHIVO ESPECÍFICO
 */
public class AnalizarArchivo339 {
    
    public static void main(String[] args) {
        System.out.println("🔍 ANALIZANDO ARCHIVO 339-FR250126.xlsx");
        System.out.println("========================================");
        
        CotizacionService service = new CotizacionService();
        service.analizarArchivoEspecifico("/Users/albertosanmartin/Documents/VSS-COTIZACIONES/339-FR250126.xlsx");
        
        System.out.println("\n✅ Análisis completado. Revisa el output para crear el mapeo.");
    }
}