package com.example.gestiondecompras.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Cuenta;
import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.models.finanzas.Recurrente;
import com.example.gestiondecompras.servicios.FinanzasServicio;
import com.example.gestiondecompras.utils.finanzas.Periodo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Estado compartido por las pestañas de Finanzas (Resumen, Movimientos y Reportes). */
public class FinanzasViewModel extends AndroidViewModel {

    /** Datos del panel Resumen. */
    public static class Panel {
        public Periodo periodo;
        public FinanzasServicio.ResumenPeriodo resumen;
        public List<FinanzasServicio.PuntoMes> evolucion = Collections.emptyList();
        public List<FinanzasServicio.EstadoPresupuesto> presupuestos = Collections.emptyList();
        public List<FinanzasServicio.SaldoCuenta> saldos = Collections.emptyList();
        public List<Recurrente> proximos = Collections.emptyList();
        public List<Movimiento> recientes = Collections.emptyList();
    }

    /** Filtros de la pestaña Movimientos. Campos null = sin filtro. */
    public static class Filtro {
        public String tipo = "";
        public Long categoriaId;
        public Long cuentaId;
        public Long desde;
        public Long hasta;
        public Double montoMin;
        public Double montoMax;
        public String texto = "";

        public boolean tieneFiltrosAvanzados() {
            return categoriaId != null || cuentaId != null || desde != null || hasta != null
                    || montoMin != null || montoMax != null;
        }

        public Filtro copia() {
            Filtro f = new Filtro();
            f.tipo = tipo; f.categoriaId = categoriaId; f.cuentaId = cuentaId; f.desde = desde;
            f.hasta = hasta; f.montoMin = montoMin; f.montoMax = montoMax; f.texto = texto;
            return f;
        }
    }

    private final AppDatabase db;
    private final FinanzasServicio servicio;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Periodo periodo = Periodo.de(Periodo.Tipo.MES, System.currentTimeMillis());
    private Filtro filtro = new Filtro();

    private final MutableLiveData<Panel> panel = new MutableLiveData<>();
    private final MutableLiveData<List<Movimiento>> movimientos = new MutableLiveData<>();
    private final MutableLiveData<Map<Long, CategoriaFin>> categorias = new MutableLiveData<>();
    private final MutableLiveData<Map<Long, Cuenta>> cuentas = new MutableLiveData<>();

    public FinanzasViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        servicio = new FinanzasServicio(db);
    }

    public LiveData<Panel> getPanel() { return panel; }
    public LiveData<List<Movimiento>> getMovimientos() { return movimientos; }
    public LiveData<Map<Long, CategoriaFin>> getCategorias() { return categorias; }
    public LiveData<Map<Long, Cuenta>> getCuentas() { return cuentas; }
    public Periodo getPeriodo() { return periodo; }
    public Filtro getFiltro() { return filtro.copia(); }
    public FinanzasServicio getServicio() { return servicio; }

    public void setPeriodo(Periodo nuevo) {
        periodo = nuevo;
        cargarPanel();
    }

    public void setFiltro(Filtro nuevo) {
        filtro = nuevo.copia();
        cargarMovimientos();
    }

    /** Recarga todo (al volver de un formulario, tras borrar, etc.). */
    public void recargar() {
        executor.execute(() -> {
            // Los recurrentes vencidos se registran antes de mostrar los datos
            servicio.generarRecurrentes(System.currentTimeMillis());
            categorias.postValue(servicio.mapaCategorias());
            cuentas.postValue(servicio.mapaCuentas());
        });
        cargarPanel();
        cargarMovimientos();
    }

    public void cargarPanel() {
        final Periodo p = periodo;
        executor.execute(() -> {
            Panel datos = new Panel();
            datos.periodo = p;
            Periodo ant = p.anterior();
            datos.resumen = servicio.resumen(p.inicio, p.fin, ant.inicio, ant.fin);
            datos.evolucion = servicio.evolucionMensual(6, Math.min(p.fin, System.currentTimeMillis()));
            datos.presupuestos = servicio.presupuestos(Math.min(p.fin, System.currentTimeMillis()));
            datos.saldos = servicio.saldosCuentas(false);
            long ahora = System.currentTimeMillis();
            List<Recurrente> proximos = new ArrayList<>();
            for (Recurrente r : db.recurrenteDao().getTodos()) {
                if (r.activo && r.proximaFechaEpoch <= ahora + 7L * 86_400_000L) proximos.add(r);
            }
            datos.proximos = proximos;
            datos.recientes = db.movimientoDao().recientes(5);
            panel.postValue(datos);
        });
    }

    public void cargarMovimientos() {
        final Filtro f = filtro.copia();
        executor.execute(() -> movimientos.postValue(db.movimientoDao().buscar(
                f.tipo == null ? "" : f.tipo, f.categoriaId, f.cuentaId, f.desde, f.hasta,
                f.montoMin, f.montoMax, f.texto == null ? "" : f.texto.trim())));
    }

    public void eliminar(Movimiento m) {
        // El ejecutor es de un solo hilo: el panel se recalcula después del borrado
        executor.execute(() -> servicio.eliminarMovimiento(m));
        cargarPanel();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
