package cl.vss.cotizador.util;

import org.mindrot.jbcrypt.BCrypt;

public class GenerarHash {
    public static void main(String[] args) {

        String passwordPlano = "Admin123*"; // ← aquí pones la clave que quieras
        String hash = BCrypt.hashpw(passwordPlano, BCrypt.gensalt());

        System.out.println("Hash generado:");
        System.out.println(hash);
    }
}
