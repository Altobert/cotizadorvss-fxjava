package cl.vss.cotizador.demo;

import cl.vss.cotizador.service.CotizacionService;
import java.util.Map;

/**
 * 🎯 EJEMPLO: Cómo agregar formatos de Excel personalizados
 * ======================================================
 * 
 * Este ejemplo muestra cómo configurar nuevos formatos de Excel
 * sin modificar el código principal del CotizacionService.
 * 
 * 💡 VENTAJAS:
 * ✅ No requiere modificar código Java existente
 * ✅ Fácil configuración de nuevos clientes
 * ✅ Detección automática de formatos
 * ✅ Sistema extensible y mantenible
 * 
 * 🔧 USO:
 * 1. Ejecuta este ejemplo para registrar formatos personalizados
 * 2. Usa "Analizar Estructura Excel" en la aplicación para diagnosticar archivos
 * 3. Usa "Cargar Excel" - el sistema detectará y aplicará el formato automáticamente
 */
public class EjemploFormatosPersonalizados {
    
    public static void main(String[] args) {
        System.out.println("🚀 CONFIGURADOR DE FORMATOS EXCEL PERSONALIZADOS");
        System.out.println("================================================");
        
        CotizacionService service = new CotizacionService();
        
        // 📊 EJEMPLO 1: Cliente Naviero Internacional
        configurarFormatoNaviero(service);
        
        // 🏭 EJEMPLO 2: Proveedor Industrial  
        configurarFormatoIndustrial(service);
        
        // 🛒 EJEMPLO 3: Formato de Compras Corporativas
        configurarFormatoComprasCorporativas(service);
        
        // ⚕️ EJEMPLO 4: Proveedor Médico
        configurarFormatoMedico(service);
        
        System.out.println("\n📋 FORMATOS REGISTRADOS:");
        service.mostrarInformacionFormatos();
        
        System.out.println("\n🎓 INSTRUCCIONES DE USO:");
        System.out.println("1. 📂 Abre tu archivo Excel en la aplicación");
        System.out.println("2. 🔍 Usa 'Analizar Estructura' para ver qué formato se detecta");
        System.out.println("3. 📥 Usa 'Cargar Excel' - el formato se aplicará automáticamente");
        System.out.println("4. 📊 Los datos aparecerán correctamente mapeados en la tabla");
        
        System.out.println("\n✨ ¡Sistema de configuración listo para usar!");
    }
    
    /**
     * 🚢 FORMATO NAVIERO INTERNACIONAL
     * Para cotizaciones marítimas y provision orders
     */
    private static void configurarFormatoNaviero(CotizacionService service) {
        System.out.println("\n🚢 Configurando formato NAVIERO_INTERNACIONAL...");
        
        service.agregarFormatoPersonalizado(
            "NAVIERO_INTERNACIONAL",
            new String[]{"SHIP", "VESSEL", "MARITIME", "CRUISE", "CARGO", "PROVISION"}, // patrones de archivo
            new String[]{"SHIP TO", "VESSEL NAME", "DELIVERY", "CANTD", "PRICES"}, // patrones de columna
            Map.of(
                "codigo", new String[]{"ITEM", "PART", "SKU", "CODE"},
                "descripcion", new String[]{"PRODUCT", "ITEM DESCRIPTION", "DESCRIPTION", "NAME"},
                "cantidad", new String[]{"CANTD", "QTY", "QUANTITY", "UNITS"},
                "precio", new String[]{"PRICES", "UNIT PRICE", "PRICE", "COST"},
                "unidad", new String[]{"UNIT", "U.M.", "UOM", "MEASURE"},
                "totalbruto", new String[]{"USD TOTAL", "TOTAL", "AMOUNT", "SUBTOTAL"},
                "comentarios", new String[]{"REMARKS", "COMMENTS", "NOTES", "OBSERVATIONS"}
            )
        );
        
        System.out.println("   ✅ Formato naviero configurado");
    }
    
    /**
     * 🏭 FORMATO PROVEEDOR INDUSTRIAL
     * Para pedidos industriales y órdenes de compra
     */
    private static void configurarFormatoIndustrial(CotizacionService service) {
        System.out.println("\n🏭 Configurando formato PROVEEDOR_INDUSTRIAL...");
        
        service.agregarFormatoPersonalizado(
            "PROVEEDOR_INDUSTRIAL",
            new String[]{"INDUSTRIAL", "MANUFACTURING", "FACTORY", "PLANT"}, 
            new String[]{"PART NUMBER", "MACHINE", "EQUIPMENT", "SPARE"},
            Map.of(
                "codigo", new String[]{"PART NUMBER", "PART#", "ITEM CODE", "SKU"},
                "descripcion", new String[]{"PRODUCT DESCRIPTION", "ITEM NAME", "COMPONENT"},
                "cantidad", new String[]{"ORDER QTY", "REQUIRED", "NEEDED", "QUANTITY"},
                "precio", new String[]{"UNIT PRICE", "LIST PRICE", "WHOLESALE", "COST"},
                "unidad", new String[]{"UNIT", "EA", "SET", "KIT"},
                "categoria", new String[]{"CATEGORY", "TYPE", "CLASS", "GROUP"},
                "comentarios", new String[]{"SPECIFICATIONS", "NOTES", "DETAILS"}
            )
        );
        
        System.out.println("   ✅ Formato industrial configurado");
    }
    
    /**
     * 🛒 FORMATO COMPRAS CORPORATIVAS
     * Para departamentos de compras y adquisiciones
     */
    private static void configurarFormatoComprasCorporativas(CotizacionService service) {
        System.out.println("\n🛒 Configurando formato COMPRAS_CORPORATIVAS...");
        
        service.agregarFormatoPersonalizado(
            "COMPRAS_CORPORATIVAS",
            new String[]{"PURCHASE", "PROCUREMENT", "CORPORATE", "ACQUISITION"},
            new String[]{"PO NUMBER", "VENDOR", "BUDGET", "APPROVAL"},
            Map.of(
                "codigo", new String[]{"PO NUMBER", "REQ#", "ORDER ID", "REFERENCE"},
                "descripcion", new String[]{"ITEM DESCRIPTION", "SERVICE", "PRODUCT NAME"},
                "cantidad", new String[]{"QUANTITY", "QTY ORDERED", "UNITS"},
                "precio", new String[]{"UNIT COST", "PRICE EACH", "RATE"},
                "unidad", new String[]{"UOM", "UNIT", "EACH"},
                "totalbruto", new String[]{"LINE TOTAL", "EXTENDED COST", "AMOUNT"},
                "categoria", new String[]{"BUDGET CODE", "CATEGORY", "DEPT"},
                "comentarios", new String[]{"NOTES", "DELIVERY INSTRUCTIONS", "REMARKS"}
            )
        );
        
        System.out.println("   ✅ Formato corporativo configurado");
    }
    
    /**
     * ⚕️ FORMATO PROVEEDOR MÉDICO
     * Para suministros médicos y equipamiento hospitalario
     */
    private static void configurarFormatoMedico(CotizacionService service) {
        System.out.println("\n⚕️ Configurando formato PROVEEDOR_MEDICO...");
        
        service.agregarFormatoPersonalizado(
            "PROVEEDOR_MEDICO",
            new String[]{"MEDICAL", "HOSPITAL", "CLINIC", "PHARMACEUTICAL", "HEALTH"},
            new String[]{"NDC", "LOT", "EXPIRY", "STERILE", "MEDICAL DEVICE"},
            Map.of(
                "codigo", new String[]{"NDC", "ITEM#", "MEDICAL CODE", "DEVICE ID"},
                "descripcion", new String[]{"MEDICAL PRODUCT", "DEVICE NAME", "PHARMACEUTICAL"},
                "cantidad", new String[]{"QUANTITY", "BOXES", "UNITS", "DOSES"},
                "precio", new String[]{"UNIT PRICE", "COST PER UNIT", "WHOLESALE"},
                "unidad", new String[]{"UNIT", "BOX", "BOTTLE", "PACK"},
                "categoria", new String[]{"MEDICAL CATEGORY", "DRUG CLASS", "DEVICE TYPE"},
                "comentarios", new String[]{"LOT NUMBER", "EXPIRY DATE", "SPECIAL HANDLING", "NOTES"}
            )
        );
        
        System.out.println("   ✅ Formato médico configurado");
    }
    
    /**
     * 💡 PLANTILLA PARA CREAR NUEVOS FORMATOS
     */
    public static void plantillaNuevoFormato() {
        System.out.println("\n💡 PLANTILLA PARA CREAR FORMATO PERSONALIZADO:");
        System.out.println("----------------------------------------------");
        System.out.println("service.agregarFormatoPersonalizado(");
        System.out.println("    \"MI_FORMATO_PERSONALIZADO\",");
        System.out.println("    new String[]{\"PATRON1\", \"PATRON2\"},  // patrones en nombre archivo");
        System.out.println("    new String[]{\"COLUMNA1\", \"COLUMNA2\"}, // patrones en columnas");
        System.out.println("    Map.of(");
        System.out.println("        \"codigo\", new String[]{\"MI_CODIGO\", \"ITEM_ID\"},");
        System.out.println("        \"descripcion\", new String[]{\"MI_DESCRIPCION\", \"PRODUCT_NAME\"},");
        System.out.println("        \"cantidad\", new String[]{\"MI_CANTIDAD\", \"QTY\"},");
        System.out.println("        \"precio\", new String[]{\"MI_PRECIO\", \"UNIT_PRICE\"}");
        System.out.println("    )");
        System.out.println(");");
    }
}