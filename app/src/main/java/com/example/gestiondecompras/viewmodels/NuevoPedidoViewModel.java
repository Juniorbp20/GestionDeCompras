package com.example.gestiondecompras.viewmodels;

import com.example.gestiondecompras.utils.Dinero;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.Abono;
import com.example.gestiondecompras.models.Cliente;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.models.Tarjeta;
import com.example.gestiondecompras.models.Tienda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NuevoPedidoViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final ExecutorService executorService;
    private static final List<String> DEFAULT_TIENDAS = Arrays.asList(
            "Temu",
            "Shein",
            "Amazon",
            "Ebay",
            "AliExpress",
            "Alibaba"
    );

    private final MutableLiveData<List<Cliente>> clientes = new MutableLiveData<>();
    private final MutableLiveData<List<Tienda>> tiendas = new MutableLiveData<>();
    private final MutableLiveData<List<Tarjeta>> tarjetas = new MutableLiveData<>();

    public NuevoPedidoViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        executorService = Executors.newSingleThreadExecutor();
        loadInitialData();
    }

    public LiveData<List<Cliente>> getClientes() {
        return clientes;
    }

    public LiveData<List<Tienda>> getTiendas() {
        return tiendas;
    }

    public LiveData<List<Tarjeta>> getTarjetas() {
        return tarjetas;
    }

    public void loadInitialData() {
        executorService.execute(() -> {
            clientes.postValue(db.clienteDao().getAllClientes());

            List<Tienda> tiendasActuales = new ArrayList<>(db.tiendaDao().getAllTiendas());
            if (tiendasActuales.isEmpty()) {
                for (String nombre : DEFAULT_TIENDAS) {
                    Tienda tienda = new Tienda();
                    tienda.setNombre(nombre);
                    db.tiendaDao().insert(tienda);
                }
                tiendasActuales = db.tiendaDao().getAllTiendas();
            }
            tiendas.postValue(tiendasActuales);

            tarjetas.postValue(db.tarjetaDao().getAllTarjetas());
        });
    }

    public void savePedido(Pedido pedido, Tarjeta tarjeta, double abonoInicial, String metodoAbono) {
        executorService.execute(() -> db.runInTransaction(() -> {
            if (pedido.getId() > 0) {
                // Obtener el pedido anterior para comparar el monto
                Pedido oldPedido = db.pedidoDao().getPedidoById(pedido.getId());
                if (oldPedido != null) {
                    double diff = Dinero.restar(pedido.getMontoCompra(), oldPedido.getMontoCompra());

                    // Solo actualizar la deuda si hay una diferencia en el monto numérico
                    if (!Dinero.esCero(diff) && tarjeta != null && pedido.tarjetaId == tarjeta.id) {
                        tarjeta.setDeudaActual(Dinero.sumar(tarjeta.getDeudaActual(), diff));
                        db.tarjetaDao().update(tarjeta);
                    }
                }
                db.pedidoDao().update(pedido);
            } else {
                long pedidoId = db.pedidoDao().insert(pedido);
                pedido.id = pedidoId;

                if (tarjeta != null) {
                    tarjeta.setDeudaActual(Dinero.sumar(tarjeta.getDeudaActual(), pedido.getMontoCompra()));
                    db.tarjetaDao().update(tarjeta);
                }

                if (Dinero.esPositivo(abonoInicial)) {
                    Abono abono = new Abono();
                    abono.pedidoId = pedidoId;
                    abono.monto = Dinero.redondear(abonoInicial);
                    abono.fechaEpoch = System.currentTimeMillis();
                    abono.metodo = metodoAbono != null ? metodoAbono : Abono.METODO_EFECTIVO;
                    db.abonoDao().insert(abono);
                }
            }
        }));
    }

    public LiveData<Pedido> getPedido(long id) {
        MutableLiveData<Pedido> pedidoLiveData = new MutableLiveData<>();
        executorService.execute(() -> {
            Pedido p = db.pedidoDao().getPedidoById(id);
            if (p != null) {
                p.totalAbonado = db.abonoDao().getTotalAbonado(id);
            }
            pedidoLiveData.postValue(p);
        });
        return pedidoLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
