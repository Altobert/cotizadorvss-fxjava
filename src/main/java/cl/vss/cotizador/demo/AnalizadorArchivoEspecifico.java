package cl.vss.cotizador.demo;

import cl.vss.cotizador.service.CotizacionService;
import java.io.File;

/**
 * Analizador específico para el archivo QTN_GOF_233.xlsx
 */
public class AnalizadorArchivoEspecifico {
    
    public static void main(String[] args) {
        CotizacionService service = new CotizacionService();
        
        String rutaArchivo = "/Users/albertosanmartin/proyectos/vss/cotizadorvss-fxjava/BD_VSS/Planillas/QTN_GOF_233.xlsx";
        File archivo = new File(rutaArchivo);
        
        if (archivo.exists()) {
            System.out.println("🎯 ANALIZANDO ARCHIVO ESPECÍFICO: QTN_GOF_233.xlsx");
            System.out.println("📁 Ruta: " + rutaArchivo);
            System.out.println("📊 Tamaño: " + archivo.length() + " bytes");
            System.out.println("=====================================");
            
            // Ejecutar análisis completo
            service.analizarEstructuraExcel(archivo);
            
            System.out.println("=====================================");
            System.out.println("✅ ANÁLISIS COMPLETADO");
            System.out.println("Revise la salida anterior para ver la estructura detallada");
            
        } else {
            System.out.println("❌ ERROR: No se encontró el archivo en la ruta especificada");
            System.out.println("📁 Ruta verificada: " + rutaArchivo);
            System.out.println("🔍 Asegúrese de que el archivo existe");
        }
    }
}