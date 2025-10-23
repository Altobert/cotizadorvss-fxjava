package com.example.service;

import com.example.model.Cotizacion;
import com.example.model.Cliente;
import com.example.model.ItemCotizacion;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para gestionar cotizaciones en memoria
 * Proporciona operaciones CRUD básicas
 */
public class CotizacionService {
    
    private static CotizacionService instance;
    private Map<String, Cotizacion> cotizaciones;
    private Map<String, Cliente> clientes;
    private int contadorCotizaciones;
    
    // Constructor privado para Singleton
    private CotizacionService() {
        this.cotizaciones = new ConcurrentHashMap<>();
        this.clientes = new ConcurrentHashMap<>();
        this.contadorCotizaciones = 1;
        inicializarDatosEjemplo();
    }
    
    // Obtener instancia singleton
    public static synchronized CotizacionService getInstance() {
        if (instance == null) {
            instance = new CotizacionService();
        }
        return instance;
    }
    
    // Inicializar con datos de ejemplo
    private void inicializarDatosEjemplo() {
        // Crear clientes de ejemplo
        Cliente cliente1 = new Cliente("Empresa ABC S.A.", "contacto@empresaabc.com", 
                                      "+56 9 1234 5678", "Av. Principal 123");
        cliente1.setCiudad("Santiago");
        cliente1.setPais("Chile");
        cliente1.setTipoDocumento("RUT");
        cliente1.setNumeroDocumento("12.345.678-9");
        
        Cliente cliente2 = new Cliente("Juan Pérez", "juan.perez@email.com", 
                                      "+56 9 8765 4321", "Calle Secundaria 456");
        cliente2.setCiudad("Valparaíso");
        cliente2.setPais("Chile");
        cliente2.setTipoDocumento("RUT");
        cliente2.setNumeroDocumento("98.765.432-1");
        
        clientes.put(cliente1.getId(), cliente1);
        clientes.put(cliente2.getId(), cliente2);
        
        // Crear cotización de ejemplo
        Cotizacion cotizacion1 = new Cotizacion(cliente1);
        cotizacion1.setNumeroCotizacion("COT-2024-001");
        cotizacion1.setFechaVencimiento(java.time.LocalDateTime.now().plusDays(30));
        cotizacion1.setEstado("PENDIENTE");
        cotizacion1.setNotas("Cotización para equipos de oficina");
        cotizacion1.setCondiciones("Pago a 30 días");
        
        // Agregar items
        ItemCotizacion item1 = new ItemCotizacion("LAP001", "Laptop HP ProBook 450", 850000, 2, "unidad", "Electrónicos");
        ItemCotizacion item2 = new ItemCotizacion("MON001", "Monitor Samsung 24\"", 180000, 3, "unidad", "Electrónicos");
        ItemCotizacion item3 = new ItemCotizacion("MOU001", "Mouse Inalámbrico Logitech", 25000, 5, "unidad", "Accesorios");
        
        cotizacion1.agregarItem(item1);
        cotizacion1.agregarItem(item2);
        cotizacion1.agregarItem(item3);
        
        cotizacion1.setDescuento(50000); // Descuento de $50,000
        cotizacion1.setImpuestos(0.19); // IVA 19%
        
        cotizaciones.put(cotizacion1.getId(), cotizacion1);
    }
    
    // Operaciones CRUD para Cotizaciones
    public Cotizacion crearCotizacion(Cotizacion cotizacion) {
        if (cotizacion.getNumeroCotizacion() == null || cotizacion.getNumeroCotizacion().isEmpty()) {
            cotizacion.setNumeroCotizacion("COT-" + java.time.LocalDate.now().getYear() + "-" + 
                                         String.format("%03d", contadorCotizaciones++));
        }
        cotizaciones.put(cotizacion.getId(), cotizacion);
        return cotizacion;
    }
    
    public Cotizacion obtenerCotizacion(String id) {
        return cotizaciones.get(id);
    }
    
    public List<Cotizacion> obtenerTodasLasCotizaciones() {
        return new ArrayList<>(cotizaciones.values());
    }
    
    public List<Cotizacion> buscarCotizacionesPorCliente(String clienteId) {
        return cotizaciones.values().stream()
            .filter(cotizacion -> cotizacion.getCliente() != null && 
                                 cotizacion.getCliente().getId().equals(clienteId))
            .collect(java.util.stream.Collectors.toList());
    }
    
    public List<Cotizacion> buscarCotizacionesPorEstado(String estado) {
        return cotizaciones.values().stream()
            .filter(cotizacion -> cotizacion.getEstado().equalsIgnoreCase(estado))
            .collect(java.util.stream.Collectors.toList());
    }
    
    public Cotizacion actualizarCotizacion(Cotizacion cotizacion) {
        if (cotizaciones.containsKey(cotizacion.getId())) {
            cotizaciones.put(cotizacion.getId(), cotizacion);
            return cotizacion;
        }
        return null;
    }
    
    public boolean eliminarCotizacion(String id) {
        return cotizaciones.remove(id) != null;
    }
    
    // Operaciones CRUD para Clientes
    public Cliente crearCliente(Cliente cliente) {
        clientes.put(cliente.getId(), cliente);
        return cliente;
    }
    
    public Cliente obtenerCliente(String id) {
        return clientes.get(id);
    }
    
    public List<Cliente> obtenerTodosLosClientes() {
        return new ArrayList<>(clientes.values());
    }
    
    public List<Cliente> buscarClientesPorNombre(String nombre) {
        return clientes.values().stream()
            .filter(cliente -> cliente.getNombre().toLowerCase().contains(nombre.toLowerCase()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    public Cliente actualizarCliente(Cliente cliente) {
        if (clientes.containsKey(cliente.getId())) {
            clientes.put(cliente.getId(), cliente);
            return cliente;
        }
        return null;
    }
    
    public boolean eliminarCliente(String id) {
        return clientes.remove(id) != null;
    }
    
    // Métodos de utilidad
    public List<String> obtenerEstadosDisponibles() {
        return Arrays.asList("BORRADOR", "PENDIENTE", "APROBADA", "RECHAZADA", "VENCIDA", "FACTURADA");
    }
    
    public List<String> obtenerCategoriasDisponibles() {
        return Arrays.asList("Electrónicos", "Accesorios", "Oficina", "Hogar", "Software", "Servicios");
    }
    
    public Map<String, Object> obtenerEstadisticas() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCotizaciones", cotizaciones.size());
        stats.put("totalClientes", clientes.size());
        stats.put("cotizacionesPendientes", buscarCotizacionesPorEstado("PENDIENTE").size());
        stats.put("cotizacionesAprobadas", buscarCotizacionesPorEstado("APROBADA").size());
        
        double totalVentas = cotizaciones.values().stream()
            .filter(cotizacion -> "APROBADA".equals(cotizacion.getEstado()))
            .mapToDouble(Cotizacion::getTotal)
            .sum();
        stats.put("totalVentas", totalVentas);
        
        return stats;
    }
}
