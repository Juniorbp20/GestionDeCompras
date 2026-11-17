package com.example.gestiondecompras.utils.finanzas;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;

public class RecurrenciaUtilsTest {

    private long fecha(int anio, int mes, int dia) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(anio, mes - 1, dia, 9, 0, 0);
        return c.getTimeInMillis();
    }

    private String ymd(long epoch) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(epoch);
        return String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void semanal_sumaSieteDias() {
        assertEquals("2026-12-02", ymd(RecurrenciaUtils.siguienteFecha(fecha(2026, 11, 25), "semanal", 0)));
    }

    @Test
    public void mensual_ajustaAlFinDeMesYVuelveAlDiaAncla() {
        long ene31 = fecha(2026, 1, 31);
        long feb = RecurrenciaUtils.siguienteFecha(ene31, "mensual", 31);
        assertEquals("2026-02-28", ymd(feb));
        long mar = RecurrenciaUtils.siguienteFecha(feb, "mensual", 31);
        assertEquals("2026-03-31", ymd(mar));
        assertEquals("2027-01-05", ymd(RecurrenciaUtils.siguienteFecha(fecha(2026, 12, 5), "mensual", 5)));
    }

    @Test
    public void quincenal_alternaDia15YFinDeMes() {
        assertEquals("2026-02-15", ymd(RecurrenciaUtils.siguienteFecha(fecha(2026, 2, 1), "quincenal", 0)));
        assertEquals("2026-02-28", ymd(RecurrenciaUtils.siguienteFecha(fecha(2026, 2, 15), "quincenal", 0)));
        assertEquals("2026-03-15", ymd(RecurrenciaUtils.siguienteFecha(fecha(2026, 2, 28), "quincenal", 0)));
    }

    @Test
    public void anual_respetaBisiestos() {
        assertEquals("2029-02-28", ymd(RecurrenciaUtils.siguienteFecha(fecha(2028, 2, 29), "anual", 29)));
        assertEquals("2030-02-28", ymd(RecurrenciaUtils.siguienteFecha(fecha(2029, 2, 28), "anual", 29)));
        assertEquals("2032-02-29", ymd(RecurrenciaUtils.siguienteFecha(fecha(2031, 2, 28), "anual", 29)));
    }
}
