package com.example.gestiondecompras.daos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.Cliente;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.models.Tarjeta;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class TarjetaDaoTest {

    private AppDatabase db;
    private TarjetaDao dao;
    private long clienteId;

    @Before
    public void setUp() {
        db = TestDb.crear();
        dao = db.tarjetaDao();

        Cliente cliente = new Cliente();
        cliente.nombre = "Ana";
        cliente.apellido = "Lopez";
        cliente.email = "ana@test.com";
        cliente.telefono = "8090000000";
        clienteId = db.clienteDao().insert(cliente);
    }

    @After
    public void tearDown() {
        TestDb.cerrar(db);
    }

    private Tarjeta nuevaTarjeta(String banco, String alias, double limite, double deuda) {
        Tarjeta t = new Tarjeta();
        t.banco = banco;
        t.alias = alias;
        t.limite = limite;
        t.deudaActual = deuda;
        t.diaCorte = 15;
        t.fechaVencimiento = "08-27";
        return t;
    }

    @Test
    public void insertYGetAll_tarjetasGuardadas() {
        long id = dao.insert(nuevaTarjeta("Popular", "Visa", 50000.0, 1000.0));
        dao.insert(nuevaTarjeta("BHD", "Mastercard", 30000.0, 2000.0));

        List<Tarjeta> tarjetas = dao.getAllTarjetas();
        assertEquals(2, tarjetas.size());
        assertEquals(id, tarjetas.get(0).getId());
        assertEquals("Popular", tarjetas.get(0).getBanco());
        assertEquals(49000.0, tarjetas.get(0).getDisponible(), 0.001);
        assertEquals(15, tarjetas.get(0).getDiaCorte());
        assertEquals(0, tarjetas.get(0).getDiaPago());
    }

    @Test
    public void diaPago_seGuardaYActualiza() {
        Tarjeta t = nuevaTarjeta("Popular", "Visa", 50000.0, 1000.0);
        t.setDiaPago(5);
        long id = dao.insert(t);

        Tarjeta guardada = dao.getAllTarjetas().get(0);
        assertEquals(5, guardada.getDiaPago());

        t.setId(id);
        t.setDiaPago(20);
        assertEquals(1, dao.update(t));
        assertEquals(20, dao.getAllTarjetas().get(0).getDiaPago());
    }

    @Test
    public void update_modificaDeudaYLimite() {
        Tarjeta t = nuevaTarjeta("Popular", "Visa", 50000.0, 1000.0);
        long id = dao.insert(t);
        t.setId(id);
        t.setDeudaActual(5000.0);
        assertEquals(1, dao.update(t));

        Tarjeta actualizada = dao.getAllTarjetas().get(0);
        assertEquals(5000.0, actualizada.getDeudaActual(), 0.001);
        assertEquals(45000.0, actualizada.getDisponible(), 0.001);
    }

    @Test
    public void delete_eliminaTarjeta() {
        Tarjeta t = nuevaTarjeta("Popular", "Visa", 50000.0, 1000.0);
        long id = dao.insert(t);
        t.setId(id);
        assertEquals(1, dao.delete(t));
        assertTrue(dao.getAllTarjetas().isEmpty());
    }

    @Test
    public void countPedidosPorTarjeta_relacionaPedidos() {
        long id = dao.insert(nuevaTarjeta("Popular", "Visa", 50000.0, 1000.0));

        Pedido p = new Pedido();
        p.clienteId = clienteId;
        p.tarjetaId = id;
        p.estado = Pedido.ESTADO_PENDIENTE;
        p.totalGeneral = 200.0;
        db.pedidoDao().insert(p);

        Pedido p2 = new Pedido();
        p2.clienteId = clienteId;
        p2.tarjetaId = id;
        p2.estado = Pedido.ESTADO_PAGADO;
        p2.totalGeneral = 300.0;
        db.pedidoDao().insert(p2);

        assertEquals(1, db.pedidoDao().countPedidosPorTarjeta(id));
        assertEquals(0, db.pedidoDao().countPedidosPorTarjeta(999));
    }
}