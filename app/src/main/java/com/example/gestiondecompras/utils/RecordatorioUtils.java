package com.example.gestiondecompras.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Locale;

/**
 * Reglas de fechas y horarios usadas por los recordatorios.
 * Se mantienen sin dependencias de Android para poder probarlas con JUnit.
 */
public final class RecordatorioUtils {

    public static final int HORA_INICIO_PERMITIDA = 8;
    public static final int HORA_FIN_PERMITIDA = 22;
    public static final int FRECUENCIA_PREDETERMINADA = 6;

    private RecordatorioUtils() {
    }

    public static boolean esHoraSilencio(int horaDelDia) {
        return horaDelDia < HORA_INICIO_PERMITIDA || horaDelDia >= HORA_FIN_PERMITIDA;
    }

    public static int normalizarHoraResumen(int hora) {
        return Math.max(HORA_INICIO_PERMITIDA, Math.min(HORA_FIN_PERMITIDA - 1, hora));
    }

    public static int normalizarFrecuencia(int frecuenciaHoras) {
        if (frecuenciaHoras == 12 || frecuenciaHoras == 24) {
            return frecuenciaHoras;
        }
        return FRECUENCIA_PREDETERMINADA;
    }

    /**
     * Calcula el tiempo hasta la siguiente franja de trabajo. Las franjas se anclan
     * a la hora configurada y se repiten cada {@code frecuenciaHoras}.
     */
    public static long milisegundosHastaSiguienteFranja(long ahoraMillis, int hora,
                                                        int minuto, int frecuenciaHoras) {
        Calendar ahora = Calendar.getInstance();
        ahora.setTimeInMillis(ahoraMillis);

        Calendar base = (Calendar) ahora.clone();
        base.set(Calendar.HOUR_OF_DAY, normalizarHoraResumen(hora));
        base.set(Calendar.MINUTE, Math.max(0, Math.min(59, minuto)));
        base.set(Calendar.SECOND, 0);
        base.set(Calendar.MILLISECOND, 0);

        int frecuencia = normalizarFrecuencia(frecuenciaHoras);
        int iteraciones = 48 / frecuencia + 1;
        for (int i = 0; i < iteraciones; i++) {
            Calendar candidato = (Calendar) base.clone();
            candidato.add(Calendar.HOUR_OF_DAY, i * frecuencia);
            if (!candidato.before(ahora)) {
                return candidato.getTimeInMillis() - ahoraMillis;
            }
        }

        Calendar respaldo = (Calendar) base.clone();
        respaldo.add(Calendar.DAY_OF_YEAR, 2);
        return respaldo.getTimeInMillis() - ahoraMillis;
    }

    /**
     * Devuelve la próxima fecha en la que ocurre el día del mes indicado.
     * Si el mes no tiene ese día (por ejemplo 31 en febrero), se ajusta al último día.
     */
    public static Calendar proximaFechaDelMes(int dia, Calendar hoy) {
        int diaValido = Math.max(1, Math.min(31, dia));

        Calendar inicioHoy = (Calendar) hoy.clone();
        inicioHoy.set(Calendar.HOUR_OF_DAY, 0);
        inicioHoy.set(Calendar.MINUTE, 0);
        inicioHoy.set(Calendar.SECOND, 0);
        inicioHoy.set(Calendar.MILLISECOND, 0);

        Calendar candidato = (Calendar) inicioHoy.clone();
        candidato.set(Calendar.DAY_OF_MONTH,
                Math.min(diaValido, candidato.getActualMaximum(Calendar.DAY_OF_MONTH)));
        if (candidato.before(inicioHoy)) {
            candidato.add(Calendar.MONTH, 1);
            candidato.set(Calendar.DAY_OF_MONTH,
                    Math.min(diaValido, candidato.getActualMaximum(Calendar.DAY_OF_MONTH)));
        }
        return candidato;
    }

    public static int diasHastaProximoDia(int dia, Calendar hoy) {
        return diasEntre(hoy, proximaFechaDelMes(dia, hoy));
    }

    public static int diasEntre(Calendar desde, Calendar hasta) {
        ZoneId zona = ZoneId.systemDefault();
        LocalDate fechaDesde = Instant.ofEpochMilli(desde.getTimeInMillis()).atZone(zona).toLocalDate();
        LocalDate fechaHasta = Instant.ofEpochMilli(hasta.getTimeInMillis()).atZone(zona).toLocalDate();
        return (int) ChronoUnit.DAYS.between(fechaDesde, fechaHasta);
    }

    public static String claveDia(Calendar fecha) {
        return String.format(Locale.US, "%04d%02d%02d",
                fecha.get(Calendar.YEAR),
                fecha.get(Calendar.MONTH) + 1,
                fecha.get(Calendar.DAY_OF_MONTH));
    }

    public static String claveDedupe(String tipo, long id, String cuando, Calendar fechaEvento) {
        return tipo + "_" + id + "_" + cuando + "_" + claveDia(fechaEvento);
    }
}
