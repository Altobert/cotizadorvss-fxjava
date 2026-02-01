package cl.vss.cotizador;

/**
 * Launcher class para ejecutar la aplicación JavaFX desde un JAR ejecutable.
 * Esta clase evita problemas con JavaFX cuando se empaqueta con maven-shade-plugin.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
