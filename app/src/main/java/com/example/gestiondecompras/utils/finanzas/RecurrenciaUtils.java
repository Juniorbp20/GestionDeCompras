package com.example.gestiondecompras.utils.finanzas;

import java.util.Calendar;

/**
 * Cálculo de fechas de movimientos recurrentes. Sin dependencias de Android para probarse con JUnit.
 */
public final class RecurrenciaUtils {

    public static final String SEMANAL = "semanal";
    public static final String QUINCENAL = "quincenal";
    public static final String MENSUAL = "mensual";
    public static final String ANUAL = "anual";

    private RecurrenciaUtils() {
    }

    /**
     * Siguiente fecha a partir de {@code fecha}.
     * <ul>
     *   <li>semanal: +7 días</li>
     *   <li>quincenal: el día 15 y el último día de cada mes (como las quincenas de pago)</li>
     *   <li>mensual: mismo día del mes ({@code diaAncla}), ajustado al último día si el mes es más corto</li>
     *   <li>anual: mismo día y mes del año siguiente (29-feb → 28-feb en años no bisiestos)</li>
     * </ul>
     */
    public static long siguienteFecha(long fecha, String frecuencia, int diaAncla) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(fecha);
        int ancla = diaAncla >= 1 && diaAncla <= 31 ? diaAncla : c.get(Calendar.DAY_OF_MONTH);

        if (SEMANAL.equals(frecuencia)) {
            c.add(Calendar.DAY_OF_YEAR, 7);
        } else if (QUINCENAL.equals(frecuencia)) {
            int dia = c.get(Calendar.DAY_OF_MONTH);
            if (dia < 15) {
                c.set(Calendar.DAY_OF_MONTH, 15);
            } else if (dia < c.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            } else {
                c.set(Calendar.DAY_OF_MONTH, 1);
                c.add(Calendar.MONTH, 1);
                c.set(Calendar.DAY_OF_MONTH, 15);
            }
        } else if (ANUAL.equals(frecuencia)) {
            int mes = c.get(Calendar.MONTH);
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.add(Calendar.YEAR, 1);
            c.set(Calendar.MONTH, mes);
            c.set(Calendar.DAY_OF_MONTH, Math.min(ancla, c.getActualMaximum(Calendar.DAY_OF_MONTH)));
        } else {
            // Mensual (por defecto)
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.add(Calendar.MONTH, 1);
            c.set(Calendar.DAY_OF_MONTH, Math.min(ancla, c.getActualMaximum(Calendar.DAY_OF_MONTH)));
        }
        return c.getTimeInMillis();
    }

    /** Inicio del día (00:00:00.000) de la fecha indicada. */
    public static long inicioDelDia(long fecha) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(fecha);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
