package com.example.gestiondecompras.utils;

/**
 * Utilidades para contactar a un cliente (llamada o WhatsApp).
 * Sin dependencias de Android para poder probarlas con JUnit.
 */
public final class ContactoUtils {

    private ContactoUtils() {
    }

    /** Deja solo los dígitos del teléfono (y el "+" inicial si lo hay) para marcar. */
    public static String numeroParaLlamar(String telefono) {
        if (telefono == null) {
            return "";
        }
        String limpio = telefono.trim();
        boolean internacional = limpio.startsWith("+");
        String digitos = limpio.replaceAll("[^0-9]", "");
        return internacional ? "+" + digitos : digitos;
    }

    /**
     * Clave para comparar teléfonos: los últimos 10 dígitos (ignora prefijo de país,
     * espacios, guiones y paréntesis). Devuelve "" si tiene menos de 7 dígitos.
     */
    public static String claveTelefono(String telefono) {
        if (telefono == null) {
            return "";
        }
        String digitos = telefono.replaceAll("[^0-9]", "");
        if (digitos.length() < 7) {
            return "";
        }
        return digitos.length() > 10 ? digitos.substring(digitos.length() - 10) : digitos;
    }

    /** {@code true} si ambos teléfonos son válidos y corresponden al mismo número. */
    public static boolean mismoTelefono(String a, String b) {
        String claveA = claveTelefono(a);
        return !claveA.isEmpty() && claveA.equals(claveTelefono(b));
    }

    /**
     * Número en formato internacional sin "+" para enlaces de WhatsApp (wa.me).
     * Los números dominicanos de 10 dígitos (809, 829, 849) se completan con el código de país 1.
     * Devuelve una cadena vacía si el número no es válido.
     */
    public static String numeroWhatsApp(String telefono) {
        if (telefono == null) {
            return "";
        }
        String digitos = telefono.replaceAll("[^0-9]", "");
        if (digitos.length() == 10
                && (digitos.startsWith("809") || digitos.startsWith("829") || digitos.startsWith("849"))) {
            return "1" + digitos;
        }
        return digitos.length() >= 8 ? digitos : "";
    }
}
