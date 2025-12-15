package cl.vss.cotizador.util;

import org.mindrot.jbcrypt.BCrypt;

public class TestHash {
    public static void main(String[] args) {
        // Genera un hash para la clave "1234"
        String hash = BCrypt.hashpw("1234", BCrypt.gensalt());
        System.out.println("Hash generado para 1234: " + hash);
    }
}
