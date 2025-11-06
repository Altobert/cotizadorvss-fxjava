package cl.vss.cotizador;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

    @Test
    public void testMainClassExists() {
        // Verificar que la clase Main existe sin instanciarla (evita problemas con JavaFX)
        assertDoesNotThrow(() -> {
            Class<?> mainClass = Main.class;
            assertNotNull(mainClass);
            assertTrue(mainClass.getName().equals("cl.vss.cotizador.Main"));
        });
    }

    @Test
    public void testMainMethod() {
        // Verificar que el método main existe
        assertDoesNotThrow(() -> {
            Main.class.getMethod("main", String[].class);
        });
    }
}