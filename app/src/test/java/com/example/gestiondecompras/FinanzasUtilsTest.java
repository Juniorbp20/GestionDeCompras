package com.example.gestiondecompras;

import static org.junit.Assert.assertEquals;

import com.example.gestiondecompras.utils.FinanzasUtils;

import org.junit.Test;

public class FinanzasUtilsTest {

    private static final long DIA = 24L * 60L * 60L * 1000L;

    @Test
    public void diasDelPeriodo_incluyeAmbosExtremos() {
        assertEquals(1, FinanzasUtils.diasDelPeriodo(0, 0));
        assertEquals(2, FinanzasUtils.diasDelPeriodo(0, DIA));
        assertEquals(31, FinanzasUtils.diasDelPeriodo(0, 30 * DIA));
        assertEquals(0, FinanzasUtils.diasDelPeriodo(DIA, 0));
    }

    @Test
    public void variacionPorcentual_calculaAumentoYReduccion() {
        assertEquals(25.0, FinanzasUtils.variacionPorcentual(125.0, 100.0), 0.001);
        assertEquals(-20.0, FinanzasUtils.variacionPorcentual(80.0, 100.0), 0.001);
        assertEquals(0.0, FinanzasUtils.variacionPorcentual(100.0, 100.0), 0.001);
    }

    @Test
    public void variacionPorcentual_sinPeriodoAnteriorEsCero() {
        assertEquals(0.0, FinanzasUtils.variacionPorcentual(500.0, 0.0), 0.001);
        assertEquals(0.0, FinanzasUtils.variacionPorcentual(500.0, -10.0), 0.001);
    }
}
