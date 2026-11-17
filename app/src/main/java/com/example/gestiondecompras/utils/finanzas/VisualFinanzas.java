package com.example.gestiondecompras.utils.finanzas;

import com.example.gestiondecompras.R;

/** Paleta de colores e iconos disponibles para categorías y cuentas de Finanzas. */
public final class VisualFinanzas {

    private VisualFinanzas() {
    }

    /** Claves de color guardadas en la base de datos, en el orden en que se ofrecen. */
    public static final String[] COLORES = {
            "naranja", "azul", "marron", "dorado", "rojo", "morado",
            "verde_azulado", "gris", "verde", "indigo", "rosa", "cian"
    };

    /** Claves de icono guardadas en la base de datos, en el orden en que se ofrecen. */
    public static final String[] ICONOS = {
            "comida", "transporte", "hogar", "servicios", "salud", "ocio", "compras", "billetera",
            "trabajo", "regalo", "educacion", "ahorro", "banco", "pagos", "personas", "tendencia"
    };

    public static int color(String clave) {
        if (clave == null) return R.color.category_other;
        switch (clave) {
            case "naranja": return R.color.category_food;
            case "azul": return R.color.category_transport;
            case "marron": return R.color.category_home;
            case "dorado": return R.color.category_services;
            case "rojo": return R.color.category_health;
            case "morado": return R.color.category_entertainment;
            case "verde_azulado": return R.color.category_shopping;
            case "verde": return R.color.category_green;
            case "indigo": return R.color.category_indigo;
            case "rosa": return R.color.category_pink;
            case "cian": return R.color.category_cyan;
            default: return R.color.category_other;
        }
    }

    public static int icono(String clave) {
        if (clave == null) return R.drawable.ic_wallet;
        switch (clave) {
            case "comida": return R.drawable.ic_restaurant;
            case "transporte": return R.drawable.ic_car;
            case "hogar": return R.drawable.ic_home;
            case "servicios": return R.drawable.ic_bolt;
            case "salud": return R.drawable.ic_medical;
            case "ocio": return R.drawable.ic_movie;
            case "compras": return R.drawable.ic_cart;
            case "trabajo": return R.drawable.ic_work;
            case "regalo": return R.drawable.ic_gift;
            case "educacion": return R.drawable.ic_school;
            case "ahorro": return R.drawable.ic_savings;
            case "banco": return R.drawable.ic_account_balance;
            case "pagos": return R.drawable.ic_payments;
            case "personas": return R.drawable.ic_people;
            case "tendencia": return R.drawable.ic_trending_up;
            default: return R.drawable.ic_wallet;
        }
    }

    /** Icono según el tipo de cuenta. */
    public static int iconoCuenta(String tipo) {
        if ("banco".equals(tipo)) return R.drawable.ic_account_balance;
        if ("tarjeta".equals(tipo)) return R.drawable.ic_credit_card;
        if ("efectivo".equals(tipo)) return R.drawable.ic_payments;
        return R.drawable.ic_wallet;
    }
}
