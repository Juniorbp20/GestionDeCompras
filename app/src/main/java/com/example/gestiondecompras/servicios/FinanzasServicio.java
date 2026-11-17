package com.example.gestiondecompras.servicios;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Cuenta;
import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.models.finanzas.Presupuesto;
import com.example.gestiondecompras.models.finanzas.Recurrente;
import com.example.gestiondecompras.models.finanzas.TotalPorGrupo;
import com.example.gestiondecompras.utils.Dinero;
import com.example.gestiondecompras.utils.finanzas.CalculosFinancieros;
import com.example.gestiondecompras.utils.finanzas.RecurrenciaUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reglas de negocio de Finanzas. Métodos síncronos: llamarlos fuera del hilo principal.
 * Recibe la base de datos para poder probarse con una base en memoria.
 */
public class FinanzasServicio {

    // ===== Resultados =====

    /** Total de una categoría en un periodo, con su participación. */
    public static class TotalCategoria {
        public CategoriaFin categoria; // null = sin categoría
        public double total;
        public int cantidad;
        public double porcentaje;
    }

    public static class ResumenPeriodo {
        public double ingresos;
        public double gastos;
        public double balance;
        public double tasaAhorro;
        public int cantidad;
        public double ingresosAnterior;
        public double gastosAnterior;
        @Nullable public Double variacionIngresos;
        @Nullable public Double variacionGastos;
        public double promedioDiarioGasto;
        public List<TotalCategoria> gastosPorCategoria = new ArrayList<>();
        public List<TotalCategoria> ingresosPorCategoria = new ArrayList<>();
    }

    public static class SaldoCuenta {
        public Cuenta cuenta;
        public double saldo;
    }

    public static class EstadoPresupuesto {
        public CategoriaFin categoria;
        public double limite;
        public double gastado;
        public double porcentaje;
        public CalculosFinancieros.EstadoPresupuesto estado;
    }

    public static class PuntoMes {
        /** "AAAA-MM". */
        public String clave;
        public int anio;
        /** 0-11. */
        public int mes;
        public double ingresos;
        public double gastos;
    }

    /** Totales de una cuenta en un rango. */
    public static class TotalCuenta {
        public Cuenta cuenta; // null = sin cuenta
        public double ingresos;
        public double gastos;
        public double neto;
    }

    /** Reporte financiero completo de un rango de fechas. */
    public static class Reporte {
        public long desde;
        public long hasta;
        public double ingresos;
        public double gastos;
        public double balance;
        public double tasaAhorro;
        public int cantidad;
        public List<TotalCategoria> gastosPorCategoria = new ArrayList<>();
        public List<TotalCategoria> ingresosPorCategoria = new ArrayList<>();
        public List<TotalCuenta> porCuenta = new ArrayList<>();
        public List<PuntoMes> porMes = new ArrayList<>();
        public List<Movimiento> mayoresGastos = new ArrayList<>();
    }

    private final AppDatabase db;

    public FinanzasServicio(@NonNull AppDatabase db) {
        this.db = db;
    }

    // ===== Cuentas predeterminadas =====

    private static final String PREFS = "finanzas_prefs";
    private static final String CLAVE_CUENTAS_BASE = "cuentas_base_v1";

    /** Nombre y tipo de las cuentas con las que empieza Finanzas. */
    static final String[][] CUENTAS_BASE = {
            {"Efectivo", Cuenta.TIPO_EFECTIVO},
            {"Transferencia", Cuenta.TIPO_BANCO},
            {"Depósito", Cuenta.TIPO_BANCO}
    };

    /**
     * Crea las cuentas base que falten (comparando por nombre). Devuelve cuántas creó.
     * No toca las existentes, aunque estén archivadas.
     */
    public int crearCuentasBase() {
        return db.runInTransaction(() -> {
            int creadas = 0;
            for (String[] base : CUENTAS_BASE) {
                if (db.cuentaDao().buscarPorNombre(base[0]) == null) {
                    Cuenta c = new Cuenta();
                    c.nombre = base[0];
                    c.tipo = base[1];
                    c.saldoInicial = 0;
                    db.cuentaDao().insert(c);
                    creadas++;
                }
            }
            return creadas;
        });
    }

    /**
     * Crea las cuentas base una sola vez por instalación: si el usuario las borra después,
     * no vuelven a aparecer. Llamar fuera del hilo principal.
     */
    public static void asegurarCuentasBase(@NonNull Context context, @NonNull AppDatabase db) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(CLAVE_CUENTAS_BASE, false)) {
            return;
        }
        new FinanzasServicio(db).crearCuentasBase();
        prefs.edit().putBoolean(CLAVE_CUENTAS_BASE, true).apply();
    }

    // ===== Movimientos =====

    /** Crea o actualiza un movimiento (importe redondeado a centavos). Devuelve su id. */
    public long guardarMovimiento(@NonNull Movimiento m) {
        m.monto = Dinero.redondear(Math.abs(m.monto));
        if (m.id > 0) {
            db.movimientoDao().update(m);
            return m.id;
        }
        m.id = db.movimientoDao().insert(m);
        return m.id;
    }

    /**
     * Guarda un movimiento nuevo y crea su recurrente: este movimiento es la primera ocurrencia y
     * la siguiente se registrará sola en la fecha que corresponda según la frecuencia.
     */
    public long guardarConRecurrencia(@NonNull Movimiento m, @NonNull String frecuencia) {
        return db.runInTransaction(() -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(m.fechaEpoch);
            Recurrente r = new Recurrente();
            r.tipo = m.tipo;
            r.monto = Dinero.redondear(Math.abs(m.monto));
            r.descripcion = m.descripcion;
            r.notas = m.notas;
            r.categoriaId = m.categoriaId;
            r.cuentaId = m.cuentaId;
            r.frecuencia = frecuencia;
            r.diaAncla = c.get(Calendar.DAY_OF_MONTH);
            r.proximaFechaEpoch = RecurrenciaUtils.siguienteFecha(m.fechaEpoch, frecuencia, r.diaAncla);
            r.activo = true;
            r.id = db.recurrenteDao().insert(r);
            m.recurrenteId = r.id;
            return guardarMovimiento(m);
        });
    }

    public void eliminarMovimiento(@NonNull Movimiento m) {
        db.movimientoDao().delete(m);
    }

    // ===== Resumen y paneles =====

    public ResumenPeriodo resumen(long desde, long hasta, long desdeAnterior, long hastaAnterior) {
        ResumenPeriodo r = new ResumenPeriodo();
        r.ingresos = Dinero.redondear(db.movimientoDao().total(Movimiento.TIPO_INGRESO, desde, hasta));
        r.gastos = Dinero.redondear(db.movimientoDao().total(Movimiento.TIPO_GASTO, desde, hasta));
        r.balance = Dinero.restar(r.ingresos, r.gastos);
        r.tasaAhorro = CalculosFinancieros.tasaAhorro(r.ingresos, r.gastos);
        r.cantidad = db.movimientoDao().cantidad(desde, hasta);
        r.ingresosAnterior = Dinero.redondear(db.movimientoDao().total(Movimiento.TIPO_INGRESO, desdeAnterior, hastaAnterior));
        r.gastosAnterior = Dinero.redondear(db.movimientoDao().total(Movimiento.TIPO_GASTO, desdeAnterior, hastaAnterior));
        r.variacionIngresos = CalculosFinancieros.variacion(r.ingresos, r.ingresosAnterior);
        r.variacionGastos = CalculosFinancieros.variacion(r.gastos, r.gastosAnterior);
        int dias = (int) Math.max(1, Math.round((Math.min(hasta, System.currentTimeMillis()) - desde) / 86_400_000.0));
        r.promedioDiarioGasto = CalculosFinancieros.promedioDiario(r.gastos, dias);
        r.gastosPorCategoria = totalesPorCategoria(Movimiento.TIPO_GASTO, desde, hasta, r.gastos);
        r.ingresosPorCategoria = totalesPorCategoria(Movimiento.TIPO_INGRESO, desde, hasta, r.ingresos);
        return r;
    }

    public List<TotalCategoria> totalesPorCategoria(String tipo, long desde, long hasta, double totalTipo) {
        Map<Long, CategoriaFin> categorias = mapaCategorias();
        List<TotalCategoria> lista = new ArrayList<>();
        for (TotalPorGrupo g : db.movimientoDao().totalesPorCategoria(tipo, desde, hasta)) {
            TotalCategoria t = new TotalCategoria();
            t.categoria = g.grupoId != null ? categorias.get(g.grupoId) : null;
            t.total = Dinero.redondear(g.total);
            t.cantidad = g.cantidad;
            t.porcentaje = CalculosFinancieros.participacion(t.total, totalTipo);
            lista.add(t);
        }
        return lista;
    }

    public List<SaldoCuenta> saldosCuentas(boolean incluirArchivadas) {
        List<SaldoCuenta> lista = new ArrayList<>();
        List<Cuenta> cuentas = incluirArchivadas ? db.cuentaDao().getTodas() : db.cuentaDao().getActivas();
        for (Cuenta c : cuentas) {
            SaldoCuenta s = new SaldoCuenta();
            s.cuenta = c;
            s.saldo = Dinero.sumar(c.saldoInicial, db.cuentaDao().movimientoNeto(c.id));
            lista.add(s);
        }
        return lista;
    }

    /** Estado de los presupuestos en el mes que contiene {@code fechaDelMes}. */
    public List<EstadoPresupuesto> presupuestos(long fechaDelMes) {
        long[] mes = rangoMes(fechaDelMes, 0);
        Map<Long, CategoriaFin> categorias = mapaCategorias();
        List<EstadoPresupuesto> lista = new ArrayList<>();
        for (Presupuesto p : db.presupuestoDao().getTodos()) {
            CategoriaFin cat = categorias.get(p.categoriaId);
            if (cat == null || cat.archivada) {
                continue;
            }
            EstadoPresupuesto e = new EstadoPresupuesto();
            e.categoria = cat;
            e.limite = p.montoMensual;
            e.gastado = Dinero.redondear(db.movimientoDao().gastoDeCategoria(p.categoriaId, mes[0], mes[1]));
            e.porcentaje = CalculosFinancieros.porcentajePresupuesto(e.gastado, e.limite);
            e.estado = CalculosFinancieros.estadoPresupuesto(e.gastado, e.limite);
            lista.add(e);
        }
        lista.sort((a, b) -> Double.compare(b.porcentaje, a.porcentaje));
        return lista;
    }

    /** Ingresos y gastos de los últimos {@code meses} meses hasta el mes de {@code fechaFinal} incluido. */
    public List<PuntoMes> evolucionMensual(int meses, long fechaFinal) {
        long[] ultimo = rangoMes(fechaFinal, 0);
        long[] primero = rangoMes(fechaFinal, -(meses - 1));
        Map<String, PuntoMes> puntos = new HashMap<>();
        List<PuntoMes> orden = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(primero[0]);
        for (int i = 0; i < meses; i++) {
            PuntoMes p = new PuntoMes();
            p.anio = c.get(Calendar.YEAR);
            p.mes = c.get(Calendar.MONTH);
            p.clave = String.format(Locale.US, "%04d-%02d", p.anio, p.mes + 1);
            puntos.put(p.clave, p);
            orden.add(p);
            c.add(Calendar.MONTH, 1);
        }
        for (TotalPorGrupo g : db.movimientoDao().totalesPorMes(primero[0], ultimo[1])) {
            PuntoMes p = puntos.get(g.clave);
            if (p == null) continue;
            if (Movimiento.TIPO_INGRESO.equals(g.tipo)) {
                p.ingresos = Dinero.redondear(g.total);
            } else {
                p.gastos = Dinero.redondear(g.total);
            }
        }
        return orden;
    }

    // ===== Reportes =====

    public Reporte reporte(long desde, long hasta) {
        Reporte r = new Reporte();
        r.desde = desde;
        r.hasta = hasta;
        r.ingresos = Dinero.redondear(db.movimientoDao().total(Movimiento.TIPO_INGRESO, desde, hasta));
        r.gastos = Dinero.redondear(db.movimientoDao().total(Movimiento.TIPO_GASTO, desde, hasta));
        r.balance = Dinero.restar(r.ingresos, r.gastos);
        r.tasaAhorro = CalculosFinancieros.tasaAhorro(r.ingresos, r.gastos);
        r.cantidad = db.movimientoDao().cantidad(desde, hasta);
        r.gastosPorCategoria = totalesPorCategoria(Movimiento.TIPO_GASTO, desde, hasta, r.gastos);
        r.ingresosPorCategoria = totalesPorCategoria(Movimiento.TIPO_INGRESO, desde, hasta, r.ingresos);

        Map<Long, Cuenta> cuentas = mapaCuentas();
        Map<String, TotalCuenta> porCuenta = new java.util.LinkedHashMap<>();
        for (TotalPorGrupo g : db.movimientoDao().totalesPorCuenta(desde, hasta)) {
            String clave = String.valueOf(g.grupoId);
            TotalCuenta t = porCuenta.get(clave);
            if (t == null) {
                t = new TotalCuenta();
                t.cuenta = g.grupoId != null ? cuentas.get(g.grupoId) : null;
                porCuenta.put(clave, t);
            }
            if (Movimiento.TIPO_INGRESO.equals(g.tipo)) t.ingresos = Dinero.redondear(g.total);
            else t.gastos = Dinero.redondear(g.total);
            t.neto = Dinero.restar(t.ingresos, t.gastos);
        }
        r.porCuenta = new ArrayList<>(porCuenta.values());

        // Evolución mensual dentro del rango
        Calendar inicio = Calendar.getInstance();
        inicio.setTimeInMillis(desde);
        Calendar fin = Calendar.getInstance();
        fin.setTimeInMillis(hasta);
        int meses = (fin.get(Calendar.YEAR) - inicio.get(Calendar.YEAR)) * 12
                + fin.get(Calendar.MONTH) - inicio.get(Calendar.MONTH) + 1;
        if (meses >= 1 && meses <= 36) {
            r.porMes = evolucionMensual(meses, hasta);
        }
        r.mayoresGastos = db.movimientoDao().mayores(Movimiento.TIPO_GASTO, desde, hasta, 10);
        return r;
    }

    // ===== Recurrentes =====

    /**
     * Genera los movimientos de los recurrentes vencidos hasta {@code ahora} (incluidos los atrasados)
     * y avanza su próxima fecha. Es idempotente: no duplica un movimiento ya generado para la misma fecha.
     *
     * @return número de movimientos creados
     */
    public int generarRecurrentes(long ahora) {
        return db.runInTransaction(() -> {
            int creados = 0;
            for (Recurrente r : db.recurrenteDao().getVencidos(ahora)) {
                int guardia = 0;
                while (r.proximaFechaEpoch <= ahora && guardia++ < 400) {
                    if (db.movimientoDao().existeGenerado(r.id, r.proximaFechaEpoch) == 0) {
                        Movimiento m = new Movimiento();
                        m.tipo = r.tipo;
                        m.monto = Dinero.redondear(r.monto);
                        m.fechaEpoch = r.proximaFechaEpoch;
                        m.descripcion = r.descripcion;
                        m.notas = r.notas;
                        m.categoriaId = r.categoriaId;
                        m.cuentaId = r.cuentaId;
                        m.recurrenteId = r.id;
                        db.movimientoDao().insert(m);
                        creados++;
                    }
                    r.proximaFechaEpoch = RecurrenciaUtils.siguienteFecha(r.proximaFechaEpoch, r.frecuencia, r.diaAncla);
                }
                db.recurrenteDao().update(r);
            }
            return creados;
        });
    }

    // ===== Categorías =====

    /**
     * Elimina la categoría si no tiene movimientos; si los tiene, la archiva para conservar el historial.
     * @return true si se eliminó, false si se archivó
     */
    public boolean eliminarOArchivarCategoria(@NonNull CategoriaFin categoria) {
        return db.runInTransaction(() -> {
            if (db.categoriaFinDao().contarMovimientos(categoria.id) == 0) {
                db.categoriaFinDao().eliminar(categoria.id);
                return true;
            }
            categoria.archivada = true;
            db.categoriaFinDao().update(categoria);
            return false;
        });
    }

    /** Igual que con las categorías: elimina la cuenta si no tiene movimientos, si no la archiva. */
    public boolean eliminarOArchivarCuenta(@NonNull Cuenta cuenta) {
        return db.runInTransaction(() -> {
            if (db.cuentaDao().contarMovimientos(cuenta.id) == 0) {
                db.cuentaDao().eliminar(cuenta.id);
                return true;
            }
            cuenta.archivada = true;
            db.cuentaDao().update(cuenta);
            return false;
        });
    }

    public Map<Long, CategoriaFin> mapaCategorias() {
        Map<Long, CategoriaFin> mapa = new HashMap<>();
        for (CategoriaFin c : db.categoriaFinDao().getTodas()) {
            mapa.put(c.id, c);
        }
        return mapa;
    }

    public Map<Long, Cuenta> mapaCuentas() {
        Map<Long, Cuenta> mapa = new HashMap<>();
        for (Cuenta c : db.cuentaDao().getTodas()) {
            mapa.put(c.id, c);
        }
        return mapa;
    }

    // ===== Fechas =====

    /** [inicio, fin] del mes de {@code fecha} desplazado {@code desplazamiento} meses. */
    public static long[] rangoMes(long fecha, int desplazamiento) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(fecha);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MONTH, desplazamiento);
        long inicio = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        return new long[]{inicio, c.getTimeInMillis() - 1};
    }
}
