package com.example.gestiondecompras.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DineroTest {

    @Test
    public void redondear_aDosDecimales() {
        assertEquals(10.13, Dinero.redondear(10.125), 0.0);
        assertEquals(0.3, Dinero.redondear(0.1 + 0.2), 0.0);
        assertEquals(0.0, Dinero.redondear(Double.NaN), 0.0);
        assertEquals(-5.5, Dinero.redondear(-5.499999), 0.0);
    }

    @Test
    public void sumarYRestar_sinErroresDePuntoFlotante() {
        assertEquals(0.3, Dinero.sumar(0.1, 0.2), 0.0);
        assertEquals(0.1, Dinero.restar(0.3, 0.2), 0.0);
        assertEquals(1000.01, Dinero.sumar(999.99, 0.02), 0.0);
    }

    @Test
    public void comparaciones_aNivelDeCentavo() {
        assertTrue(Dinero.esCero(0.004));
        assertFalse(Dinero.esCero(0.006));
        assertTrue(Dinero.esPositivo(0.01));
        assertFalse(Dinero.esPositivo(0.0));
        assertFalse(Dinero.mayorQue(100.0, 0.1 * 1000));
        assertTrue(Dinero.mayorOIgual(0.1 + 0.2, 0.3));
        assertTrue(Dinero.mayorQue(100.01, 100.0));
        assertEquals(0, Dinero.comparar(0.1 + 0.2, 0.3));
    }

    @Test
    public void saldo_nuncaNegativoYExacto() {
        assertEquals(0.0, Dinero.saldo(100.0, 100.000001), 0.0);
        assertEquals(0.0, Dinero.saldo(100.0, 150.0), 0.0);
        assertEquals(33.34, Dinero.saldo(100.0, 66.66), 0.0);
    }
}
