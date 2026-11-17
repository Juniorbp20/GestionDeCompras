package com.example.gestiondecompras.servicios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.gestiondecompras.daos.TestDb;
import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.Abono;
import com.example.gestiondecompras.models.Cliente;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.models.Tarjeta;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/** Flujos principales de pedidos: crear, editar, abonar y eliminar. */
@RunWith(AndroidJUnit4.class)
public class PedidosServicioTest {

    private AppDatabase db;
    private PedidosServicio servicio;
    private long clienteId;
    private long tarjetaA;
    private long tarjetaB;

    @Before
    public void setUp() {
        db = TestDb.crear();
        servicio = new PedidosServicio(db);

        Cliente cliente = new Cliente();
        cliente.nombre = "Ana";
        clienteId = db.clienteDao().insert(cliente);
        tarjetaA = db.tarjetaDao().insert(tarjeta("Popular", 1000.0));
        tarjetaB = db.tarjetaDao().insert(tarjeta("BHD", 0.0));
    }

    @After
    public void tearDown() {
        TestDb.cerrar(db);
    }

    private Tarjeta tarjeta(String banco, double deuda) {
        Tarjeta t = new Tarjeta();
        t.banco = banco;
        t.limite = 50000.0;
        t.deudaActual = deuda;
        t.diaCorte = 15;
        return t;
    }

    private Pedido pedido(long tarjetaId, double compra, double ganancia) {
        Pedido p = new Pedido();
        p.clienteId = clienteId;
        p.setClienteNombre("Ana");
        p.tarjetaId = tarjetaId;
        p.montoCompra = compra;
        p.ganancia = ganancia;
        p.totalGeneral = compra + ganancia;
        p.estado = Pedido.ESTADO_PENDIENTE;
        return p;
    }

    private double deuda(long tarjetaId) {
        return db.tarjetaDao().getTarjetaById(tarjetaId).getDeudaActual();
    }

    @Test
    public void crearPedido_sumaLaCompraALaDeudaYGuardaElAbonoInicial() {
        long id = servicio.guardarPedido(pedido(tarjetaA, 500.0, 100.0), 200.0, Abono.METODO_EFECTIVO);

        assertEquals(1500.0, deuda(tarjetaA), 0.0);
        assertEquals(200.0, db.abonoDao().getTotalAbonado(id), 0.0);
    }

    @Test
    public void editarMonto_ajustaSoloLaDiferencia() {
        Pedido p = pedido(tarjetaA, 500.0, 100.0);
        servicio.guardarPedido(p, 0, null);

        p.montoCompra = 650.0;
        servicio.guardarPedido(p, 0, null);

        assertEquals(1650.0, deuda(tarjetaA), 0.0);
    }

    @Test
    public void cambiarDeTarjeta_mueveLaCompraALaNuevaTarjeta() {
        Pedido p = pedido(tarjetaA, 500.0, 100.0);
        servicio.guardarPedido(p, 0, null);

        p.tarjetaId = tarjetaB;
        servicio.guardarPedido(p, 0, null);

        assertEquals(1000.0, deuda(tarjetaA), 0.0);
        assertEquals(500.0, deuda(tarjetaB), 0.0);
    }

    @Test
    public void eliminarPedido_descuentaLaCompraYBorraSusAbonos() {
        Pedido p = pedido(tarjetaA, 500.0, 100.0);
        long id = servicio.guardarPedido(p, 100.0, null);

        servicio.eliminarPedido(p);

        assertEquals(1000.0, deuda(tarjetaA), 0.0);
        assertNull(db.pedidoDao().getPedidoById(id));
        assertEquals(0.0, db.abonoDao().getTotalAbonado(id), 0.0);
    }

    @Test
    public void registrarAbonos_marcaPagadoAlSaldarConImportesDecimales() {
        Pedido p = pedido(tarjetaA, 0.1, 0.2); // total 0.30000000000000004 en double
        long id = servicio.guardarPedido(p, 0, null);

        servicio.registrarAbono(p, 0.1, Abono.METODO_EFECTIVO, "");
        assertEquals(Pedido.ESTADO_PENDIENTE, db.pedidoDao().getPedidoById(id).estado);

        servicio.registrarAbono(p, 0.2, Abono.METODO_EFECTIVO, "");
        assertEquals(Pedido.ESTADO_PAGADO, db.pedidoDao().getPedidoById(id).estado);
    }

    @Test
    public void eliminarAbono_devuelveElPedidoAPendiente() {
        Pedido p = pedido(tarjetaA, 500.0, 100.0);
        long id = servicio.guardarPedido(p, 0, null);
        servicio.registrarAbono(p, 600.0, Abono.METODO_EFECTIVO, "");
        assertEquals(Pedido.ESTADO_PAGADO, db.pedidoDao().getPedidoById(id).estado);

        List<Abono> abonos = db.abonoDao().getAbonosPorPedido(id);
        servicio.eliminarAbono(p, abonos.get(0));

        assertEquals(Pedido.ESTADO_PENDIENTE, db.pedidoDao().getPedidoById(id).estado);
    }
}
