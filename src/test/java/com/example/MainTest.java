package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

    @Test
    public void testMainClassExists() {
        // Verificar que la clase Main existe y puede ser instanciada
        assertDoesNotThrow(() -> {
            Main main = new Main();
            assertNotNull(main);
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