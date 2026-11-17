package com.example.gestiondecompras.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.ClienteWithMetrics;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Cuenta;
import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.servicios.FinanzasServicio;
import com.example.gestiondecompras.models.Pedido;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Búsqueda global en clientes, pedidos y movimientos de Finanzas. */
public class BusquedaViewModel extends AndroidViewModel {

    private static final int LIMITE = 20;

    /** Resultados de una búsqueda; {@code texto} permite descartar respuestas antiguas. */
    public static final class Resultados {
        public final String texto;
        public final List<ClienteWithMetrics> clientes;
        public final List<Pedido> pedidos;
        public final List<Movimiento> movimientos;
        public java.util.Map<Long, CategoriaFin> categorias = java.util.Collections.emptyMap();
        public java.util.Map<Long, Cuenta> cuentas = java.util.Collections.emptyMap();

        Resultados(String texto, List<ClienteWithMetrics> clientes, List<Pedido> pedidos, List<Movimiento> movimientos) {
            this.texto = texto;
            this.clientes = clientes;
            this.pedidos = pedidos;
            this.movimientos = movimientos;
        }

        public boolean vacio() {
            return clientes.isEmpty() && pedidos.isEmpty() && movimientos.isEmpty();
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
            List<Movimiento> movimientos = db.movimientoDao().buscarTexto(limpio, LIMITE);
            // Si el usuario siguió escribiendo, se descartan resultados de un texto anterior
            if (limpio.equals(ultimoTexto)) {
                Resultados r = new Resultados(limpio, clientes, pedidos, movimientos);
                FinanzasServicio servicio = new FinanzasServicio(db);
                r.categorias = servicio.mapaCategorias();
                r.cuentas = servicio.mapaCuentas();
                resultados.postValue(r);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
