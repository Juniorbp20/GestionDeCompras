package com.example.gestiondecompras.utils;

/**
 * Operaciones con importes en pesos.
 *
 * Los importes se siguen guardando como {@code double} en la base de datos, pero todas las
 * escrituras se redondean a centavos y las comparaciones se hacen en centavos enteros, para
 * evitar errores de punto flotante (p. ej. 0.1 + 0.2 != 0.3) sin tener que migrar los datos.
 */
public final class Dinero {

    private Dinero() {
    }

    /** Convierte a centavos enteros, redondeando al centavo más cercano. */
    public static long aCentavos(double monto) {
        if (Double.isNaN(monto) || Double.isInfinite(monto)) {
            return 0L;
        }
        return Math.round(monto * 100.0);
    }

    /** Convierte centavos a pesos. */
    public static double desdeCentavos(long centavos) {
        return centavos / 100.0;
    }

    /** Redondea a dos decimales. Valores no numéricos se convierten en 0. */
    public static double redondear(double monto) {
        return desdeCentavos(aCentavos(monto));
    }

    /** Suma exacta en centavos. */
    public static double sumar(double a, double b) {
        return desdeCentavos(aCentavos(a) + aCentavos(b));
    }

    /** Resta exacta en centavos. */
    public static double restar(double a, double b) {
        return desdeCentavos(aCentavos(a) - aCentavos(b));
    }

    /** Compara dos importes a nivel de centavo: negativo, 0 o positivo. */
    public static int comparar(double a, double b) {
        return Long.compare(aCentavos(a), aCentavos(b));
    }

    /** {@code true} si el importe es 0 al redondear a centavos. */
    public static boolean esCero(double monto) {
        return aCentavos(monto) == 0L;
    }

    /** {@code true} si el importe es al menos un centavo. */
    public static boolean esPositivo(double monto) {
        return aCentavos(monto) > 0L;
    }

    /** {@code true} si {@code a} supera a {@code b} en al menos un centavo. */
    public static boolean mayorQue(double a, double b) {
        return comparar(a, b) > 0;
    }

    /** {@code true} si {@code a} es mayor o igual que {@code b} a nivel de centavo. */
    public static boolean mayorOIgual(double a, double b) {
        return comparar(a, b) >= 0;
    }

    /** Saldo pendiente (nunca negativo). */
    public static double saldo(double total, double abonado) {
        return desdeCentavos(Math.max(0L, aCentavos(total) - aCentavos(abonado)));
    }
}
