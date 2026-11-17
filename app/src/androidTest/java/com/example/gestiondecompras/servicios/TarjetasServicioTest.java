package com.example.gestiondecompras.servicios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.gestiondecompras.daos.TestDb;
import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.Cliente;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.models.Tarjeta;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Eliminación y pagos de tarjetas. */
@RunWith(AndroidJUnit4.class)
public class TarjetasServicioTest {

    private AppDatabase db;
    private TarjetasServicio servicio;
    private long clienteId;

    @Before
    public void setUp() {
        db = TestDb.crear();
        servicio = new TarjetasServicio(db);
        Cliente cliente = new Cliente();
        cliente.nombre = "Ana";
        clienteId = db.clienteDao().insert(cliente);
    }

    @After
    public void tearDown() {
        TestDb.cerrar(db);
    }

    private Tarjeta crearTarjeta(double deuda) {
        Tarjeta t = new Tarjeta();
        t.banco = "Popular";
        t.limite = 50000.0;
        t.deudaActual = deuda;
        t.diaCorte = 15;
        t.id = db.tarjetaDao().insert(t);
        return t;
    }

    private long crearPedido(long tarjetaId, String estado) {
        Pedido p = new Pedido();
        p.clienteId = clienteId;
        p.tarjetaId = tarjetaId;
        p.estado = estado;
        p.totalGeneral = 100.0;
        return db.pedidoDao().insert(p);
    }

    @Test
    public void eliminar_conPedidosPendientes_seBloquea() {
        Tarjeta t = crearTarjeta(0);
        crearPedido(t.id, Pedido.ESTADO_PENDIENTE);

        assertEquals(TarjetasServicio.ResultadoEliminacion.EN_USO, servicio.eliminar(t));
        assertEquals(1, db.tarjetaDao().getAllTarjetas().size());
    }

    @Test
    public void eliminar_conPedidosPagados_desvinculaYElimina() {
        Tarjeta t = crearTarjeta(0);
        long pedidoId = crearPedido(t.id, Pedido.ESTADO_PAGADO);

        assertEquals(TarjetasServicio.ResultadoEliminacion.ELIMINADA, servicio.eliminar(t));
        assertTrue(db.tarjetaDao().getAllTarjetas().isEmpty());
        assertNull(db.pedidoDao().getPedidoById(pedidoId).tarjetaId);
    }

    @Test
    public void registrarPago_reduceLaDeudaYRechazaExcesos() {
        Tarjeta t = crearTarjeta(1000.0);

        assertEquals(TarjetasServicio.ResultadoPago.REGISTRADO, servicio.registrarPago(t.id, 250.10));
        assertEquals(749.90, db.tarjetaDao().getTarjetaById(t.id).getDeudaActual(), 0.0);

        assertEquals(TarjetasServicio.ResultadoPago.EXCEDE_DEUDA, servicio.registrarPago(t.id, 800.0));
        assertEquals(TarjetasServicio.ResultadoPago.MONTO_INVALIDO, servicio.registrarPago(t.id, 0.0));
        assertEquals(749.90, db.tarjetaDao().getTarjetaById(t.id).getDeudaActual(), 0.0);
    }
}
