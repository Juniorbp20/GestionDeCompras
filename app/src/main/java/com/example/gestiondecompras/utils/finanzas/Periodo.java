package com.example.gestiondecompras.utils.finanzas;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Periodo de consulta de Finanzas: mes, trimestre o año, que se puede desplazar hacia atrás o adelante.
 * Inmutable y sin dependencias de Android para probarse con JUnit.
 */
public final class Periodo {

    public enum Tipo { MES, TRIMESTRE, ANIO }

    private static final Locale ES = Locale.forLanguageTag("es-DO");

    public final Tipo tipo;
    /** Inicio del periodo (00:00 del primer día). */
    public final long inicio;
    /** Fin del periodo (último milisegundo del último día). */
    public final long fin;

    private Periodo(Tipo tipo, long inicio, long fin) {
        this.tipo = tipo;
        this.inicio = inicio;
        this.fin = fin;
    }

    /** Periodo del tipo indicado que contiene la fecha. */
    public static Periodo de(Tipo tipo, long fecha) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(fecha);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        int meses;
        if (tipo == Tipo.TRIMESTRE) {
            c.set(Calendar.MONTH, (c.get(Calendar.MONTH) / 3) * 3);
            meses = 3;
        } else if (tipo == Tipo.ANIO) {
            c.set(Calendar.MONTH, Calendar.JANUARY);
            meses = 12;
        } else {
            meses = 1;
        }
        long inicio = c.getTimeInMillis();
        c.add(Calendar.MONTH, meses);
        return new Periodo(tipo, inicio, c.getTimeInMillis() - 1);
    }

    /** Periodo del mismo tipo desplazado {@code pasos} unidades (negativo = hacia atrás). */
    public Periodo mover(int pasos) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(inicio);
        int meses = tipo == Tipo.TRIMESTRE ? 3 : tipo == Tipo.ANIO ? 12 : 1;
        c.add(Calendar.MONTH, meses * pasos);
        return de(tipo, c.getTimeInMillis());
    }

    public Periodo anterior() {
        return mover(-1);
    }

    /** Mismo inicio con otro tipo (p. ej. al cambiar de Mes a Año). */
    public Periodo conTipo(Tipo nuevo) {
        return de(nuevo, inicio);
    }

    /** {@code true} si el periodo contiene la fecha. */
    public boolean contiene(long fecha) {
        return fecha >= inicio && fecha <= fin;
    }

    /** Días transcurridos del periodo hasta {@code ahora} (todo el periodo si ya terminó). */
    public int diasTranscurridos(long ahora) {
        long hasta = Math.min(fin, Math.max(inicio, ahora));
        return (int) Math.max(1, Math.ceil((hasta - inicio + 1) / 86_400_000.0));
    }

    /** Texto para mostrar: "Noviembre 2026", "T4 2026" o "2026". */
    public String etiqueta() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(inicio);
        int anio = c.get(Calendar.YEAR);
        if (tipo == Tipo.ANIO) {
            return String.valueOf(anio);
        }
        if (tipo == Tipo.TRIMESTRE) {
            return "T" + (c.get(Calendar.MONTH) / 3 + 1) + " " + anio;
        }
        String mes = new SimpleDateFormat("MMMM yyyy", ES).format(new Date(inicio));
        return mes.substring(0, 1).toUpperCase(ES) + mes.substring(1);
    }
}
