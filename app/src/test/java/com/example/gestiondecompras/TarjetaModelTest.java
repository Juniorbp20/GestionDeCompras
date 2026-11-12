package com.example.gestiondecompras;

import static org.junit.Assert.assertEquals;

import com.example.gestiondecompras.models.Tarjeta;

import org.junit.Test;

public class TarjetaModelTest {

    @Test
    public void getDisponible_restaDeudaDelLimite() {
        Tarjeta t = new Tarjeta();
        t.limite = 1000.0;
        t.deudaActual = 300.0;
        assertEquals(700.0, t.getDisponible(), 0.001);
    }

    @Test
    public void getDisponible_nuncaNegativo() {
        Tarjeta t = new Tarjeta();
        t.limite = 500.0;
        t.deudaActual = 800.0;
        assertEquals(0.0, t.getDisponible(), 0.001);
    }

    @Test
    public void toString_prefiereAlias() {
        Tarjeta t = new Tarjeta();
        t.banco = "Popular";
        t.alias = "Visa Platino";
        assertEquals("Visa Platino", t.toString());
        t.alias = "";
        assertEquals("Popular", t.toString());
        t.alias = null;
        t.banco = null;
        assertEquals("", t.toString());
    }

    @Test
    public void getDiaVencimiento_devuelveCadenaMMYY() {
        Tarjeta t = new Tarjeta();
        t.fechaVencimiento = "08-27";
        assertEquals("08-27", t.getDiaVencimiento());
    }

    @Test
    public void getters_roundtrip() {
        Tarjeta t = new Tarjeta();
        t.setBanco("BHD");
        t.setAlias("Mastercard Oro");
        t.setLimite(50000.0);
        t.setDeudaActual(12000.0);
        t.setDiaCorte(15);
        t.setDiaPago(5);
        t.setFechaVencimiento("12-27");
        t.setNotas("Notas");

        assertEquals("BHD", t.getBanco());
        assertEquals("Mastercard Oro", t.getAlias());
        assertEquals(50000.0, t.getLimite(), 0.001);
        assertEquals(50000.0, t.getLimiteCredito(), 0.001);
        assertEquals(12000.0, t.getDeudaActual(), 0.001);
        assertEquals(15, t.getDiaCorte());
        assertEquals(5, t.getDiaPago());
        assertEquals("12-27", t.getFechaVencimiento());
        assertEquals("Notas", t.getNotas());
    }

    @Test
    public void diaPago_porDefectoEsCero() {
        assertEquals(0, new Tarjeta().getDiaPago());
    }
}