package com.example.gestiondecompras;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.gestiondecompras.models.Pedido;

import org.junit.Test;

public class PedidoModelTest {

    @Test
    public void estados_sonConstantesEsperadas() {
        assertEquals("pendiente", Pedido.ESTADO_PENDIENTE);
        assertEquals("entregado", Pedido.ESTADO_ENTREGADO);
        assertEquals("pagado", Pedido.ESTADO_PAGADO);
        assertEquals("cancelado", Pedido.ESTADO_CANCELADO);
        assertEquals("en camino", Pedido.ESTADO_EN_CAMINO);
    }

    @Test
    public void pedidoNuevo_estadoPendientePorDefecto() {
        Pedido p = new Pedido();
        assertEquals(Pedido.ESTADO_PENDIENTE, p.estado);
        assertEquals(0L, p.fechaRegistroEpoch.longValue());
    }

    @Test
    public void getters_manejanNullEnNombres() {
        Pedido p = new Pedido();
        assertEquals("", p.getClienteNombre());
        assertEquals("", p.getTienda());
        assertNull(p.getTiendaId());
        assertNull(p.getTarjetaId());
    }

    @Test
    public void saldo_descuentaAbonosYNoEsNegativo() {
        Pedido p = new Pedido();
        p.totalGeneral = 1000.0;

        p.setTotalAbonado(300.0);
        assertEquals(700.0, p.getSaldo(), 0.001);

        p.setTotalAbonado(1200.0);
        assertEquals(0.0, p.getSaldo(), 0.001);
    }

    @Test
    public void pedidoCompleto_roundtripDatos() {
        Pedido p = new Pedido();
        p.setClienteId(7);
        p.setClienteNombre("Ana");
        p.setTiendaId(3L);
        p.setTarjetaId(1L);
        p.montoCompra = 500.0;
        p.ganancia = 100.0;
        p.totalGeneral = 600.0;
        p.estado = Pedido.ESTADO_PAGADO;

        assertEquals(7, p.getClienteId());
        assertEquals("Ana", p.getClienteNombre());
        assertEquals(Long.valueOf(3), p.getTiendaId());
        assertEquals(Long.valueOf(1), p.getTarjetaId());
        assertEquals(500.0, p.montoCompra, 0.001);
        assertEquals(100.0, p.ganancia, 0.001);
        assertEquals(600.0, p.totalGeneral, 0.001);
        assertEquals(Pedido.ESTADO_PAGADO, p.estado);
        assertEquals(p.getTiendaId(), p.getTiendaId());
    }
}