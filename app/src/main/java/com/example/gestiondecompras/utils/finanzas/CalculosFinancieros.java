package com.example.gestiondecompras.utils.finanzas;

import com.example.gestiondecompras.utils.Dinero;

/** Cálculos del panel financiero. Sin dependencias de Android para probarse con JUnit. */
public final class CalculosFinancieros {

    /** Estado de un presupuesto según el porcentaje consumido. */
    public enum EstadoPresupuesto { NORMAL, ALERTA, EXCEDIDO }

    /** Porcentaje a partir del cual se avisa de un presupuesto. */
    public static final double UMBRAL_ALERTA = 80.0;

    private CalculosFinancieros() {
    }

    /** Porcentaje de ingresos que se ahorró (puede ser negativo). 0 si no hubo ingresos. */
    public static double tasaAhorro(double ingresos, double gastos) {
        if (!Dinero.esPositivo(ingresos)) {
            return 0;
        }
        return redondear1((ingresos - gastos) / ingresos * 100.0);
    }

    /** Variación porcentual de {@code actual} respecto a {@code anterior}; null si no hay base. */
    public static Double variacion(double actual, double anterior) {
        if (!Dinero.esPositivo(Math.abs(anterior))) {
            return null;
        }
        return redondear1((actual - anterior) / Math.abs(anterior) * 100.0);
    }

    /** Porcentaje consumido de un presupuesto (sin tope). */
    public static double porcentajePresupuesto(double gastado, double limite) {
        if (!Dinero.esPositivo(limite)) {
            return 0;
        }
        return redondear1(gastado / limite * 100.0);
    }

    public static EstadoPresupuesto estadoPresupuesto(double gastado, double limite) {
        double pct = porcentajePresupuesto(gastado, limite);
        if (pct > 100.0) {
            return EstadoPresupuesto.EXCEDIDO;
        }
        return pct >= UMBRAL_ALERTA ? EstadoPresupuesto.ALERTA : EstadoPresupuesto.NORMAL;
    }

    /** Promedio diario de un total en un número de días (mínimo 1). */
    public static double promedioDiario(double total, int dias) {
        return Dinero.redondear(total / Math.max(1, dias));
    }

    /** Participación porcentual de {@code parte} en {@code total}. */
    public static double participacion(double parte, double total) {
        if (!Dinero.esPositivo(total)) {
            return 0;
        }
        return redondear1(parte / total * 100.0);
    }

    private static double redondear1(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
