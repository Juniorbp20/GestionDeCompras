package com.example.gestiondecompras.utils.finanzas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

public class PeriodoTest {

    private long fecha(int anio, int mes, int dia) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(anio, mes - 1, dia, 12, 0, 0);
        return c.getTimeInMillis();
    }

    @Test
    public void mes_cubreDelPrimeroAlUltimoDia() {
        Periodo p = Periodo.de(Periodo.Tipo.MES, fecha(2026, 2, 14));
        assertTrue(p.contiene(fecha(2026, 2, 1)));
        assertTrue(p.contiene(fecha(2026, 2, 28)));
        assertFalse(p.contiene(fecha(2026, 3, 1)));
        assertEquals("Febrero 2026", p.etiqueta());
    }

    @Test
    public void trimestreYAnio() {
        Periodo t = Periodo.de(Periodo.Tipo.TRIMESTRE, fecha(2026, 11, 17));
        assertEquals("T4 2026", t.etiqueta());
        assertTrue(t.contiene(fecha(2026, 10, 1)));
        assertTrue(t.contiene(fecha(2026, 12, 31)));
        assertEquals("T3 2026", t.anterior().etiqueta());
        assertEquals("2026", Periodo.de(Periodo.Tipo.ANIO, fecha(2026, 6, 1)).etiqueta());
    }

    @Test
    public void mover_cruzaElAnio() {
        Periodo ene = Periodo.de(Periodo.Tipo.MES, fecha(2027, 1, 10));
        assertEquals("Diciembre 2026", ene.anterior().etiqueta());
        assertEquals("Marzo 2027", ene.mover(2).etiqueta());
        assertEquals("T1 2027", Periodo.de(Periodo.Tipo.TRIMESTRE, fecha(2026, 12, 1)).mover(1).etiqueta());
    }

    @Test
    public void diasTranscurridos() {
        Periodo nov = Periodo.de(Periodo.Tipo.MES, fecha(2026, 11, 1));
        assertEquals(17, nov.diasTranscurridos(fecha(2026, 11, 17)));
        assertEquals(30, nov.diasTranscurridos(fecha(2027, 1, 1)));
        assertEquals(1, nov.diasTranscurridos(fecha(2026, 10, 1)));
    }
}
