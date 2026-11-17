package com.example.gestiondecompras.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.ClienteWithMetrics;
import com.example.gestiondecompras.models.Gasto;
import com.example.gestiondecompras.models.Pedido;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Búsqueda global en clientes, pedidos y gastos. */
public class BusquedaViewModel extends AndroidViewModel {

    private static final int LIMITE = 20;

    /** Resultados de una búsqueda; {@code texto} permite descartar respuestas antiguas. */
    public static final class Resultados {
        public final String texto;
        public final List<ClienteWithMetrics> clientes;
        public final List<Pedido> pedidos;
        public final List<Gasto> gastos;

        Resultados(String texto, List<ClienteWithMetrics> clientes, List<Pedido> pedidos, List<Gasto> gastos) {
            this.texto = texto;
            this.clientes = clientes;
            this.pedidos = pedidos;
            this.gastos = gastos;
        }

        public boolean vacio() {
            return clientes.isEmpty() && pedidos.isEmpty() && gastos.isEmpty();
        }
    }

    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Resultados> resultados = new MutableLiveData<>();
    private volatile String ultimoTexto = "";

    public BusquedaViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
    }

    public LiveData<Resultados> getResultados() {
        return resultados;
    }

    public void buscar(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        ultimoTexto = limpio;
        if (limpio.length() < 2) {
            resultados.setValue(new Resultados(limpio, Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList()));
            return;
        }
        executor.execute(() -> {
            List<ClienteWithMetrics> clientes = db.clienteDao().buscarClientes(limpio, LIMITE);
            List<Pedido> pedidos = db.pedidoDao().buscarPedidos(limpio, LIMITE);
            for (Pedido pedido : pedidos) {
                pedido.totalAbonado = db.abonoDao().getTotalAbonado(pedido.getId());
            }
            List<Gasto> gastos = db.gastoDao().buscarGastos(limpio, LIMITE);
            // Si el usuario siguió escribiendo, se descartan resultados de un texto anterior
            if (limpio.equals(ultimoTexto)) {
                resultados.postValue(new Resultados(limpio, clientes, pedidos, gastos));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
