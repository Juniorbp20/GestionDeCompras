package com.example.gestiondecompras.utils;

import java.util.List;
import java.util.Locale;

/**
 * Generación de CSV compatible con Excel y Google Sheets (RFC 4180).
 * Sin dependencias de Android para poder probarla con JUnit.
 */
public final class CsvUtils {

    /** Marca UTF-8 para que Excel muestre bien tildes y eñes. */
    public static final String BOM = "﻿";
    private static final String SALTO = "\r\n";

    private CsvUtils() {
    }

    /** Escapa un campo: lo entrecomilla si contiene coma, comillas o saltos de línea. */
    public static String escapar(String campo) {
        if (campo == null) {
            return "";
        }
        boolean requiereComillas = campo.contains(",") || campo.contains("\"")
                || campo.contains("\n") || campo.contains("\r");
        // Evita que Excel interprete el texto como fórmula (inyección CSV)
        if (!campo.isEmpty() && "=+-@".indexOf(campo.charAt(0)) >= 0 && !esNumero(campo)) {
            campo = "'" + campo;
        }
        if (requiereComillas) {
            return "\"" + campo.replace("\"", "\"\"") + "\"";
        }
        return campo;
    }

    /** Importe con punto decimal y sin separador de miles, legible como número por las hojas de cálculo. */
    public static String importe(double monto) {
        return String.format(Locale.US, "%.2f", Dinero.redondear(monto));
    }

    /** Une los campos de una fila escapándolos. */
    public static String fila(String... campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapar(campos[i]));
        }
        return sb.toString();
    }

    /** Documento completo: BOM, encabezado y filas. */
    public static String documento(String[] encabezado, List<String[]> filas) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append(fila(encabezado)).append(SALTO);
        for (String[] f : filas) {
            sb.append(fila(f)).append(SALTO);
        }
        return sb.toString();
    }

    private static boolean esNumero(String texto) {
        try {
            Double.parseDouble(texto);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
