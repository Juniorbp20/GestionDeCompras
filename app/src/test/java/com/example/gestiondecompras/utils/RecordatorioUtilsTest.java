package com.example.gestiondecompras.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

public class RecordatorioUtilsTest {

    private Calendar fecha(int anio, int mes, int dia, int hora, int minuto) {
        Calendar calendario = Calendar.getInstance();
        calendario.set(anio, mes - 1, dia, hora, minuto, 0);
        calendario.set(Calendar.MILLISECOND, 0);
        return calendario;
    }

    @Test
    public void esHoraSilencio_respetaFranjaPermitida() {
        assertTrue(RecordatorioUtils.esHoraSilencio(7));
        assertFalse(RecordatorioUtils.esHoraSilencio(8));
        assertFalse(RecordatorioUtils.esHoraSilencio(21));
        assertTrue(RecordatorioUtils.esHoraSilencio(22));
        assertTrue(RecordatorioUtils.esHoraSilencio(2));
    }

    @Test
    public void normalizarFrecuencia_soloAceptaValoresSoportados() {
        assertEquals(6, RecordatorioUtils.normalizarFrecuencia(6));
        assertEquals(12, RecordatorioUtils.normalizarFrecuencia(12));
        assertEquals(24, RecordatorioUtils.normalizarFrecuencia(24));
        assertEquals(6, RecordatorioUtils.normalizarFrecuencia(5));
        assertEquals(6, RecordatorioUtils.normalizarFrecuencia(0));
    }

    @Test
    public void milisegundosHastaSiguienteFranja_usaLaSiguienteFranjaDelDia() {
        Calendar ahora = fecha(2026, 1, 10, 10, 15);
        long retraso = RecordatorioUtils.milisegundosHastaSiguienteFranja(
                ahora.getTimeInMillis(), 9, 0, 6);

        Calendar resultado = Calendar.getInstance();
        resultado.setTimeInMillis(ahora.getTimeInMillis() + retraso);
        assertEquals(15, resultado.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, resultado.get(Calendar.MINUTE));
        assertEquals(10, resultado.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void milisegundosHastaSiguienteFranja_antesDeLaHoraUsaHoy() {
        Calendar ahora = fecha(2026, 1, 10, 7, 30);
        long retraso = RecordatorioUtils.milisegundosHastaSiguienteFranja(
                ahora.getTimeInMillis(), 9, 0, 6);

        Calendar resultado = Calendar.getInstance();
        resultado.setTimeInMillis(ahora.getTimeInMillis() + retraso);
        assertEquals(9, resultado.get(Calendar.HOUR_OF_DAY));
        assertEquals(10, resultado.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void milisegundosHastaSiguienteFranja_cruzaLaMedianoche() {
        Calendar ahora = fecha(2026, 1, 10, 22, 10);
        long retraso = RecordatorioUtils.milisegundosHastaSiguienteFranja(
                ahora.getTimeInMillis(), 9, 0, 6);

        Calendar resultado = Calendar.getInstance();
        resultado.setTimeInMillis(ahora.getTimeInMillis() + retraso);
        assertEquals(3, resultado.get(Calendar.HOUR_OF_DAY));
        assertEquals(11, resultado.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void proximaFechaDelMes_usaElMismoMesSiAunNoPaso() {
        Calendar hoy = fecha(2026, 1, 10, 12, 0);
        Calendar proxima = RecordatorioUtils.proximaFechaDelMes(15, hoy);

        assertEquals(Calendar.JANUARY, proxima.get(Calendar.MONTH));
        assertEquals(15, proxima.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void proximaFechaDelMes_pasaAlSiguienteMes() {
        Calendar hoy = fecha(2026, 1, 31, 12, 0);
        Calendar proxima = RecordatorioUtils.proximaFechaDelMes(15, hoy);

        assertEquals(Calendar.FEBRUARY, proxima.get(Calendar.MONTH));
        assertEquals(15, proxima.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void proximaFechaDelMes_ajustaDiasQueNoExistenEnElMes() {
        Calendar hoy = fecha(2026, 2, 10, 12, 0);
        Calendar proxima = RecordatorioUtils.proximaFechaDelMes(31, hoy);

        assertEquals(Calendar.FEBRUARY, proxima.get(Calendar.MONTH));
        assertEquals(28, proxima.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void diasHastaProximoDia_calculaElDiaActualYElSiguiente() {
        assertEquals(0, RecordatorioUtils.diasHastaProximoDia(20, fecha(2026, 1, 20, 8, 0)));
        assertEquals(1, RecordatorioUtils.diasHastaProximoDia(1, fecha(2026, 1, 31, 8, 0)));
        assertEquals(1, RecordatorioUtils.diasHastaProximoDia(31, fecha(2026, 2, 27, 8, 0)));
    }

    @Test
    public void claveDedupe_incluyeTipoIdYFechaDelEvento() {
        Calendar evento = fecha(2026, 3, 5, 0, 0);
        assertEquals("ent_42_a_20260305",
                RecordatorioUtils.claveDedupe("ent", 42, "a", evento));
        assertEquals("20260305", RecordatorioUtils.claveDia(evento));
    }
}
