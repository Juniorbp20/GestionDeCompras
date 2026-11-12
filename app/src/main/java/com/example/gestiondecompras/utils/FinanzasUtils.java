package com.example.gestiondecompras.utils;

/**
 * Cálculos usados por el resumen de Finanzas.
 */
public final class FinanzasUtils {

    private static final long MILLIS_POR_DIA = 24L * 60L * 60L * 1000L;

    private FinanzasUtils() {
    }

    public static int diasDelPeriodo(long desde, long hasta) {
        if (hasta < desde) {
            return 0;
        }
        return (int) ((hasta - desde) / MILLIS_POR_DIA) + 1;
    }

    /**
     * @return variación porcentual del gasto actual contra el anterior.
     *         Positivo significa que se gastó más.
     */
    public static double variacionPorcentual(double actual, double anterior) {
        if (anterior <= 0) {
            return 0;
        }
        return ((actual - anterior) / anterior) * 100.0;
    }
}
