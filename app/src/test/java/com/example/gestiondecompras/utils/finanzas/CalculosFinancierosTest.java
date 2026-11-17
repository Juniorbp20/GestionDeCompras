package com.example.gestiondecompras.utils.finanzas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CalculosFinancierosTest {

    @Test
    public void tasaAhorro() {
        assertEquals(25.0, CalculosFinancieros.tasaAhorro(1000, 750), 0.0);
        assertEquals(-50.0, CalculosFinancieros.tasaAhorro(1000, 1500), 0.0);
        assertEquals(0.0, CalculosFinancieros.tasaAhorro(0, 300), 0.0);
    }

    @Test
    public void variacion() {
        assertEquals(10.0, CalculosFinancieros.variacion(110, 100), 0.0);
        assertEquals(-33.3, CalculosFinancieros.variacion(200, 300), 0.0);
        assertNull(CalculosFinancieros.variacion(50, 0));
    }

    @Test
    public void presupuesto() {
        assertEquals(50.0, CalculosFinancieros.porcentajePresupuesto(500, 1000), 0.0);
        assertEquals(CalculosFinancieros.EstadoPresupuesto.NORMAL, CalculosFinancieros.estadoPresupuesto(799, 1000));
        assertEquals(CalculosFinancieros.EstadoPresupuesto.ALERTA, CalculosFinancieros.estadoPresupuesto(800, 1000));
        assertEquals(CalculosFinancieros.EstadoPresupuesto.ALERTA, CalculosFinancieros.estadoPresupuesto(1000, 1000));
        assertEquals(CalculosFinancieros.EstadoPresupuesto.EXCEDIDO, CalculosFinancieros.estadoPresupuesto(1000.5, 1000));
    }

    @Test
    public void promedioYParticipacion() {
        assertEquals(33.33, CalculosFinancieros.promedioDiario(100, 3), 0.0);
        assertEquals(100.0, CalculosFinancieros.promedioDiario(100, 0), 0.0);
        assertEquals(12.5, CalculosFinancieros.participacion(125, 1000), 0.0);
        assertEquals(0.0, CalculosFinancieros.participacion(10, 0), 0.0);
    }
}
