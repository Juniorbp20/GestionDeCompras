package com.example.gestiondecompras.servicios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.gestiondecompras.daos.TestDb;
import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Cuenta;
import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.models.finanzas.Presupuesto;
import com.example.gestiondecompras.models.finanzas.Recurrente;
import com.example.gestiondecompras.utils.finanzas.CalculosFinancieros;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class FinanzasServicioTest {

    private AppDatabase db;
    private FinanzasServicio servicio;
    private long comida;
    private long sueldo;
    private long efectivo;
    private long banco;

    @Before
    public void setUp() {
        db = TestDb.crear();
        servicio = new FinanzasServicio(db);
        comida = categoria("Comida", CategoriaFin.TIPO_GASTO);
        sueldo = categoria("Sueldo", CategoriaFin.TIPO_INGRESO);
        efectivo = cuenta("Efectivo", 100.0);
        banco = cuenta("Banco", 0.0);
    }

    @After
    public void tearDown() {
        TestDb.cerrar(db);
    }

    @Test
    public void crearCuentasBase_creaSoloLasQueFaltan() {
        // setUp ya creó "Efectivo": solo faltan Transferencia y Depósito
        assertEquals(2, servicio.crearCuentasBase());
        assertEquals(Cuenta.TIPO_BANCO, db.cuentaDao().buscarPorNombre("transferencia").tipo);
        assertEquals(Cuenta.TIPO_BANCO, db.cuentaDao().buscarPorNombre(" Depósito ").tipo);
        // Una segunda llamada no duplica nada
        assertEquals(0, servicio.crearCuentasBase());
        assertEquals(4, db.cuentaDao().getTodas().size());
    }

    private long categoria(String nombre, String tipo) {
        CategoriaFin c = new CategoriaFin();
        c.nombre = nombre;
        c.tipo = tipo;
        c.colorKey = "azul";
        c.iconoKey = "billetera";
        return db.categoriaFinDao().insert(c);
    }

    private long cuenta(String nombre, double saldoInicial) {
        Cuenta c = new Cuenta();
        c.nombre = nombre;
        c.tipo = Cuenta.TIPO_EFECTIVO;
        c.saldoInicial = saldoInicial;
        return db.cuentaDao().insert(c);
    }

    private long fecha(int anio, int mes, int dia) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(anio, mes - 1, dia, 10, 0, 0);
        return c.getTimeInMillis();
    }

    private void movimiento(String tipo, double monto, long categoria, long cuenta, long fecha) {
        Movimiento m = new Movimiento();
        m.tipo = tipo;
        m.monto = monto;
        m.categoriaId = categoria;
        m.cuentaId = cuenta;
        m.fechaEpoch = fecha;
        m.descripcion = "x";
        servicio.guardarMovimiento(m);
    }

    @Test
    public void resumen_calculaTotalesBalanceAhorroYCategorias() {
        movimiento(Movimiento.TIPO_INGRESO, 1000, sueldo, banco, fecha(2026, 11, 1));
        movimiento(Movimiento.TIPO_GASTO, 250, comida, efectivo, fecha(2026, 11, 5));
        movimiento(Movimiento.TIPO_GASTO, 50.10, comida, efectivo, fecha(2026, 11, 20));
        movimiento(Movimiento.TIPO_GASTO, 999, comida, efectivo, fecha(2026, 10, 20)); // mes anterior

        long[] nov = FinanzasServicio.rangoMes(fecha(2026, 11, 15), 0);
        long[] oct = FinanzasServicio.rangoMes(fecha(2026, 11, 15), -1);
        FinanzasServicio.ResumenPeriodo r = servicio.resumen(nov[0], nov[1], oct[0], oct[1]);

        assertEquals(1000.0, r.ingresos, 0.0);
        assertEquals(300.10, r.gastos, 0.0);
        assertEquals(699.90, r.balance, 0.0);
        assertEquals(70.0, r.tasaAhorro, 0.0);
        assertEquals(999.0, r.gastosAnterior, 0.0);
        assertEquals(1, r.gastosPorCategoria.size());
        assertEquals("Comida", r.gastosPorCategoria.get(0).categoria.nombre);
        assertEquals(100.0, r.gastosPorCategoria.get(0).porcentaje, 0.0);
        assertNull(r.variacionIngresos);
    }

    @Test
    public void saldos_sumanSaldoInicialIngresosYRestanGastos() {
        movimiento(Movimiento.TIPO_INGRESO, 500, sueldo, efectivo, fecha(2026, 11, 1));
        movimiento(Movimiento.TIPO_GASTO, 120, comida, efectivo, fecha(2026, 11, 2));

        List<FinanzasServicio.SaldoCuenta> saldos = servicio.saldosCuentas(false);
        double saldoEfectivo = 0;
        for (FinanzasServicio.SaldoCuenta s : saldos) {
            if (s.cuenta.id == efectivo) saldoEfectivo = s.saldo;
        }
        assertEquals(480.0, saldoEfectivo, 0.0);
    }

    @Test
    public void presupuestos_indicanAlertaYExceso() {
        Presupuesto p = new Presupuesto();
        p.categoriaId = comida;
        p.montoMensual = 1000;
        db.presupuestoDao().guardar(p);
        movimiento(Movimiento.TIPO_GASTO, 850, comida, efectivo, fecha(2026, 11, 3));

        List<FinanzasServicio.EstadoPresupuesto> estados = servicio.presupuestos(fecha(2026, 11, 10));
        assertEquals(1, estados.size());
        assertEquals(CalculosFinancieros.EstadoPresupuesto.ALERTA, estados.get(0).estado);

        movimiento(Movimiento.TIPO_GASTO, 200, comida, efectivo, fecha(2026, 11, 4));
        estados = servicio.presupuestos(fecha(2026, 11, 10));
        assertEquals(CalculosFinancieros.EstadoPresupuesto.EXCEDIDO, estados.get(0).estado);
    }

    @Test
    public void recurrentes_generanLosVencidosSinDuplicar() {
        Recurrente r = new Recurrente();
        r.tipo = Movimiento.TIPO_GASTO;
        r.monto = 300;
        r.descripcion = "Alquiler";
        r.categoriaId = comida;
        r.cuentaId = banco;
        r.frecuencia = Recurrente.FREC_MENSUAL;
        r.diaAncla = 5;
        r.proximaFechaEpoch = fecha(2026, 9, 5);
        r.activo = true;
        r.id = db.recurrenteDao().insert(r);

        long ahora = fecha(2026, 11, 10);
        assertEquals(3, servicio.generarRecurrentes(ahora)); // sep, oct y nov
        assertEquals(0, servicio.generarRecurrentes(ahora)); // idempotente

        Recurrente actualizado = db.recurrenteDao().getById(r.id);
        assertEquals(fecha(2026, 12, 5), actualizado.proximaFechaEpoch);
    }

    @Test
    public void categoriaConMovimientos_seArchivaEnLugarDeBorrarse() {
        movimiento(Movimiento.TIPO_GASTO, 10, comida, efectivo, fecha(2026, 11, 1));
        CategoriaFin cat = db.categoriaFinDao().getById(comida);
        assertFalse(servicio.eliminarOArchivarCategoria(cat));
        assertTrue(db.categoriaFinDao().getById(comida).archivada);

        long vacia = categoria("Vacía", CategoriaFin.TIPO_GASTO);
        assertTrue(servicio.eliminarOArchivarCategoria(db.categoriaFinDao().getById(vacia)));
        assertNull(db.categoriaFinDao().getById(vacia));
    }

    @Test
    public void busqueda_combinaFiltros() {
        movimiento(Movimiento.TIPO_GASTO, 50, comida, efectivo, fecha(2026, 11, 1));
        movimiento(Movimiento.TIPO_GASTO, 500, comida, banco, fecha(2026, 11, 2));
        movimiento(Movimiento.TIPO_INGRESO, 900, sueldo, banco, fecha(2026, 11, 3));

        assertEquals(2, db.movimientoDao().buscar("gasto", null, null, null, null, null, null, "").size());
        assertEquals(1, db.movimientoDao().buscar("", null, banco, null, null, 100.0, 600.0, "").size());
        assertEquals(1, db.movimientoDao().buscar("", null, null, null, null, null, null, "Sueldo").size());
    }
}
