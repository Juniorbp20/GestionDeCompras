package com.example.gestiondecompras.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.DashboardRow;
import com.example.gestiondecompras.models.Pedido;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final ExecutorService executorService;

    private final MutableLiveData<DashboardRow> dashboardData = new MutableLiveData<>();
    private final MutableLiveData<Integer> activeClientsCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> overdueOrdersCount = new MutableLiveData<>();
    private final MutableLiveData<Double> projectedEarnings = new MutableLiveData<>();
    private final MutableLiveData<java.util.List<Pedido>> upcomingOrders = new MutableLiveData<>();
    private final MutableLiveData<Double> monthExpenses = new MutableLiveData<>();
    private final MutableLiveData<Double> monthIncome = new MutableLiveData<>();
    private final MutableLiveData<Double> monthBalance = new MutableLiveData<>();

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        executorService = Executors.newSingleThreadExecutor();
        loadDashboardData();
    }

    public LiveData<DashboardRow> getDashboardData() {
        return dashboardData;
    }

    public LiveData<Integer> getActiveClientsCount() {
        return activeClientsCount;
    }

    public LiveData<Integer> getOverdueOrdersCount() {
        return overdueOrdersCount;
    }

    public void loadDashboardData() {
        executorService.execute(() -> {
            DashboardRow row = db.pedidoDao().getDashboard();
            dashboardData.postValue(row);

            int clients = db.clienteDao().getAllClientes().size();
            activeClientsCount.postValue(clients);

            int overdue = db.pedidoDao().getOverdueOrdersCount(System.currentTimeMillis());
            overdueOrdersCount.postValue(overdue);

            Double earnings = db.reporteDao().gananciaProyectada(null, null, "");
            projectedEarnings.postValue(earnings != null ? earnings : 0d);

            java.util.List<Pedido> proximos = db.pedidoDao().getProximosPedidos(System.currentTimeMillis(), 5);
            if (proximos != null) {
                for (Pedido pedido : proximos) {
                    pedido.totalAbonado = db.abonoDao().getTotalAbonado(pedido.getId());
                }
            }
            upcomingOrders.postValue(proximos != null ? proximos : Collections.emptyList());

            long[] mes = com.example.gestiondecompras.servicios.FinanzasServicio.rangoMes(System.currentTimeMillis(), 0);
            double gastos = db.movimientoDao().total(
                    com.example.gestiondecompras.models.finanzas.Movimiento.TIPO_GASTO, mes[0], mes[1]);
            double ingresos = db.abonoDao().getTotalRango(mes[0], mes[1]);
            monthExpenses.postValue(gastos);
            monthIncome.postValue(ingresos);
            monthBalance.postValue(ingresos - gastos);
        });
    }

    public LiveData<java.util.List<com.example.gestiondecompras.models.ClienteGanancia>> getClientEarnings() {
        MutableLiveData<java.util.List<com.example.gestiondecompras.models.ClienteGanancia>> data = new MutableLiveData<>();
        executorService.execute(() -> {
            data.postValue(db.pedidoDao().getGananciasPorCliente());
        });
        return data;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }

    public LiveData<Double> getProjectedEarnings() {
        return projectedEarnings;
    }

    public LiveData<java.util.List<Pedido>> getUpcomingOrders() {
        return upcomingOrders;
    }

    public LiveData<Double> getMonthExpenses() {
        return monthExpenses;
    }

    public LiveData<Double> getMonthIncome() {
        return monthIncome;
    }

    public LiveData<Double> getMonthBalance() {
        return monthBalance;
    }
}
