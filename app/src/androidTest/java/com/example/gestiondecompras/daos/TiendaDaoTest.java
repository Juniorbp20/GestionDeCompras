package com.example.gestiondecompras.daos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.Cliente;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.models.Tienda;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class TiendaDaoTest {

    private AppDatabase db;
    private TiendaDao dao;

    @Before
    public void setUp() {
        db = TestDb.crear();
        dao = db.tiendaDao();
    }

    @After
    public void tearDown() {
        TestDb.cerrar(db);
    }

    @Test
    public void insertYGetAll_tiendasGuardadas() {
        long id = dao.insert(crearTienda("Temu"));
        dao.insert(crearTienda("Shein"));
        List<Tienda> tiendas = dao.getAllTiendas();
        assertEquals(2, tiendas.size());
        // Ordenadas alfabéticamente
        assertEquals("Shein", tiendas.get(0).getNombre());
        assertEquals(id, tiendas.get(1).getId());
        assertEquals("Temu", tiendas.get(1).getNombre());
    }

    @Test
    public void getAll_ordenSinDistinguirMayusculas() {
        dao.insert(crearTienda("temu"));
        dao.insert(crearTienda("Amazon"));
        dao.insert(crearTienda("ebay"));
        List<Tienda> tiendas = dao.getAllTiendas();
        assertEquals("Amazon", tiendas.get(0).getNombre());
        assertEquals("ebay", tiendas.get(1).getNombre());
        assertEquals("temu", tiendas.get(2).getNombre());
    }

    @Test
    public void buscarPorNombre_ignoraMayusculasYEspacios() {
        long id = dao.insert(crearTienda("Amazon"));
        Tienda encontrada = dao.buscarPorNombre("  aMaZoN ");
        assertNotNull(encontrada);
        assertEquals(id, encontrada.getId());
        assertNull(dao.buscarPorNombre("Temu"));
    }

    private long nuevoPedido(long tiendaId, String nombreTienda) {
        Cliente cliente = new Cliente();
        cliente.nombre = "Ana";
        cliente.apellido = "Lopez";
        cliente.email = "ana@test.com";
        cliente.telefono = "8090000000";
        long clienteId = db.clienteDao().insert(cliente);
        Pedido p = new Pedido();
        p.clienteId = clienteId;
        p.clienteNombre = "Ana";
        p.tiendaId = tiendaId;
        p.setTienda(nombreTienda);
        p.montoCompra = 100;
        p.totalGeneral = 120;
        p.fechaRegistroEpoch = System.currentTimeMillis();
        p.estado = Pedido.ESTADO_PENDIENTE;
        return db.pedidoDao().insert(p);
    }

    @Test
    public void conteo_cuentaPedidosPorTienda() {
        long amazon = dao.insert(crearTienda("Amazon"));
        long temu = dao.insert(crearTienda("Temu"));
        nuevoPedido(amazon, "Amazon");
        nuevoPedido(amazon, "Amazon");
        assertEquals(2, dao.contarPedidos(amazon));
        assertEquals(0, dao.contarPedidos(temu));
        List<TiendaDao.TiendaConConteo> lista = dao.getTiendasConConteo();
        assertEquals("Amazon", lista.get(0).nombre);
        assertEquals(2, lista.get(0).pedidos);
        assertEquals(0, lista.get(1).pedidos);
    }

    @Test
    public void renombrar_actualizaTiendaYPedidos() {
        long id = dao.insert(crearTienda("Amazn"));
        long pedidoId = nuevoPedido(id, "Amazn");
        db.runInTransaction(() -> {
            dao.actualizarNombre(id, "Amazon");
            dao.renombrarEnPedidos(id, "Amazon");
        });
        assertEquals("Amazon", dao.getAllTiendas().get(0).getNombre());
        assertEquals("Amazon", db.pedidoDao().getPedidoById(pedidoId).getTienda());
    }

    @Test
    public void eliminarPorId_borraTiendaSinPedidos() {
        long id = dao.insert(crearTienda("Shein"));
        assertEquals(1, dao.eliminarPorId(id));
        assertTrue(dao.getAllTiendas().isEmpty());
    }

    @Test
    public void update_modificaNombre() {
        Tienda t = crearTienda("Temu");
        long id = dao.insert(t);
        t.setId(id);
        t.setNombre("Temu Oficial");
        assertEquals(1, dao.update(t));
        assertEquals("Temu Oficial", dao.getAllTiendas().get(0).getNombre());
    }

    @Test
    public void delete_eliminaTienda() {
        Tienda t = crearTienda("Amazon");
        long id = dao.insert(t);
        t.setId(id);
        assertEquals(1, dao.delete(t));
        assertTrue(dao.getAllTiendas().isEmpty());
    }

    @Test
    public void toString_devuelveNombre() {
        Tienda t = crearTienda("AliExpress");
        assertEquals("AliExpress", t.toString());
        t.nombre = null;
        assertEquals("", t.toString());
    }

    private Tienda crearTienda(String nombre) {
        Tienda t = new Tienda();
        t.setNombre(nombre);
        return t;
    }
}