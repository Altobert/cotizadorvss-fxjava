package cl.vss.cotizador.demo;

import cl.vss.cotizador.model.Cotizacion;
import cl.vss.cotizador.model.ResultadoProcesamiento;
import cl.vss.cotizador.service.ProcesadorLoteCotizaciones;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Demo para procesar todas las cotizaciones de un directorio
 * Muestra el uso de detectores, metadatos y manejo de errores
 */
public class DemoProcesadorLote {
    
    public static void main(String[] args) {
        // Configurar logging
        configurarLogging();
        
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║     PROCESADOR DE LOTE DE COTIZACIONES - DEMOSTRACIÓN         ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
        
        // Directorio de cotizaciones
        String directorio = "/Users/albertosanmartin/Documents/VSS-COTIZACIONES";
        
        System.out.println("📂 Directorio: " + directorio);
        System.out.println("⏳ Iniciando procesamiento...\n");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
        
        // Crear procesador
        ProcesadorLoteCotizaciones procesador = new ProcesadorLoteCotizaciones();
        
        // Procesar directorio
        ResultadoProcesamiento resultado = procesador.procesarDirectorio(directorio);
        
        // Generar y mostrar reporte
        String reporte = procesador.generarReporte(resultado);
        System.out.println(reporte);
        
        // Mostrar ejemplos de cotizaciones extraídas
        if (resultado.getCantidadCotizaciones() > 0) {
            System.out.println("📋 EJEMPLOS DE COTIZACIONES EXTRAÍDAS\n");
            System.out.println("───────────────────────────────────────────────────────────────");
            
            int ejemplos = Math.min(10, resultado.getCantidadCotizaciones());
            for (int i = 0; i < ejemplos; i++) {
                Cotizacion cot = resultado.getCotizacionesExitosas().get(i);
                
                System.out.println(String.format("\n📄 Cotización #%d:", i + 1));
                System.out.println("  Archivo:     " + cot.getArchivoOrigen());
                System.out.println("  Hoja:        " + cot.getHojaOrigen());
                System.out.println("  Fila:        " + cot.getFilaOrigen());
                System.out.println("  Detector:    " + cot.getTipoDetector());
                System.out.println("  Cliente:     " + 
                    (cot.getCliente() != null ? cot.getCliente().getNombre() : "N/A"));
                System.out.println("  Items:       " + cot.getItems().size());
                
                if (!cot.getItems().isEmpty()) {
                    System.out.println("  Primer item: " + 
                        cot.getItems().get(0).getDescripcion());
                }
            }
            
            System.out.println("\n");
        }
        
        // Resumen final
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("✅ Procesamiento completado");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
    }
    
    private static void configurarLogging() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        
        // Configurar handler de consola
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);
        handler.setFormatter(new SimpleFormatter());
        
        // Remover handlers existentes
        for (java.util.logging.Handler h : rootLogger.getHandlers()) {
            rootLogger.removeHandler(h);
        }
        
        rootLogger.addHandler(handler);
    }
}
