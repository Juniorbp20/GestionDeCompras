package com.example.gestiondecompras.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.Gasto;
import com.example.gestiondecompras.models.GastoCategoria;
import com.example.gestiondecompras.models.ResumenFinanciero;
import com.example.gestiondecompras.utils.FinanzasUtils;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListaGastosViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final ExecutorService executorService;

    private final MutableLiveData<List<Gasto>> gastos = new MutableLiveData<>();
    private final MutableLiveData<ResumenFinanciero> resumen = new MutableLiveData<>();

    private Long ultimoDesde;
    private Long ultimoHasta;
    private String ultimaBusqueda = "";
    private String ultimaCategoria = "";

    public ListaGastosViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Gasto>> getGastos() {
        return gastos;
    }

    public LiveData<ResumenFinanciero> getResumen() {
        return resumen;
    }

    /**
     * Carga la lista filtrada y, en paralelo, el resumen del periodo seleccionado.
     * El resumen solo depende de las fechas, no de la búsqueda ni de la categoría.
     */
    public void loadGastos(Long desde, Long hasta, String busqueda, String categoria) {
        ultimoDesde = desde;
        ultimoHasta = hasta;
        ultimaBusqueda = busqueda != null ? busqueda : "";
        ultimaCategoria = categoria != null ? categoria : "";

        executorService.execute(() -> {
            List<Gasto> lista = db.gastoDao().getGastosFiltrados(
                    desde, hasta, ultimaBusqueda, ultimaCategoria);
            if (lista == null) {
                lista = Collections.emptyList();
            }
            gastos.postValue(lista);

            long desdeQuery = desde != null ? desde : 0L;
            long hastaQuery = hasta != null ? hasta : Long.MAX_VALUE;

            ResumenFinanciero datos = new ResumenFinanciero();
            datos.gastos = db.gastoDao().getTotalRango(desdeQuery, hastaQuery);
            datos.ingresos = db.abonoDao().getTotalRango(desdeQuery, hastaQuery);
            datos.balance = datos.ingresos - datos.gastos;
            datos.cantidadGastos = db.gastoDao().getCantidadRango(desdeQuery, hastaQuery);

            List<GastoCategoria> categorias = db.gastoDao()
                    .getTotalesPorCategoria(desdeQuery, hastaQuery);
            datos.categorias = categorias != null ? categorias : Collections.emptyList();

            if (desde != null && hasta != null) {
                int dias = FinanzasUtils.diasDelPeriodo(desde, hasta);
                datos.mostrarPromedio = dias > 0;
                datos.promedioDiario = dias > 0 ? datos.gastos / dias : 0;

                long duracion = Math.max(1L, hasta - desde + 1L);
                long anteriorHasta = desde - 1L;
                long anteriorDesde = anteriorHasta - duracion + 1L;
                if (anteriorDesde >= 0) {
                    datos.gastosPeriodoAnterior = db.gastoDao()
                            .getTotalRango(anteriorDesde, anteriorHasta);
                    datos.hayPeriodoAnterior = datos.gastosPeriodoAnterior > 0;
                    if (datos.hayPeriodoAnterior) {
                        datos.variacionPorcentual = FinanzasUtils.variacionPorcentual(
                                datos.gastos, datos.gastosPeriodoAnterior);
                    }
                }
            }

            resumen.postValue(datos);
        });
    }

    public void deleteGasto(Gasto gasto) {
        executorService.execute(() -> {
            db.gastoDao().delete(gasto);
            loadGastos(ultimoDesde, ultimoHasta, ultimaBusqueda, ultimaCategoria);
        });
    }

    public static long[] inicioYFinDelMes() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long inicio = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.MILLISECOND, -1);
        long fin = cal.getTimeInMillis();
        return new long[]{inicio, fin};
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
