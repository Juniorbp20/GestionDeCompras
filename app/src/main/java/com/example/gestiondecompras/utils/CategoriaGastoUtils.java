package com.example.gestiondecompras.utils;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.models.Gasto;

/**
 * Iconos y colores compartidos por las vistas de gastos.
 */
public final class CategoriaGastoUtils {

    private CategoriaGastoUtils() {
    }

    public static int icono(String categoria) {
        if (Gasto.CAT_COMIDA.equals(categoria)) {
            return R.drawable.ic_restaurant;
        } else if (Gasto.CAT_TRANSPORTE.equals(categoria)) {
            return R.drawable.ic_car;
        } else if (Gasto.CAT_HOGAR.equals(categoria)) {
            return R.drawable.ic_home;
        } else if (Gasto.CAT_SERVICIOS.equals(categoria)) {
            return R.drawable.ic_bolt;
        } else if (Gasto.CAT_SALUD.equals(categoria)) {
            return R.drawable.ic_medical;
        } else if (Gasto.CAT_ENTRETENIMIENTO.equals(categoria)) {
            return R.drawable.ic_movie;
        } else if (Gasto.CAT_COMPRAS.equals(categoria)) {
            return R.drawable.ic_cart;
        }
        return R.drawable.ic_wallet;
    }

    public static int color(String categoria) {
        if (Gasto.CAT_COMIDA.equals(categoria)) {
            return R.color.category_food;
        } else if (Gasto.CAT_TRANSPORTE.equals(categoria)) {
            return R.color.category_transport;
        } else if (Gasto.CAT_HOGAR.equals(categoria)) {
            return R.color.category_home;
        } else if (Gasto.CAT_SERVICIOS.equals(categoria)) {
            return R.color.category_services;
        } else if (Gasto.CAT_SALUD.equals(categoria)) {
            return R.color.category_health;
        } else if (Gasto.CAT_ENTRETENIMIENTO.equals(categoria)) {
            return R.color.category_entertainment;
        } else if (Gasto.CAT_COMPRAS.equals(categoria)) {
            return R.color.category_shopping;
        }
        return R.color.category_other;
    }
}
