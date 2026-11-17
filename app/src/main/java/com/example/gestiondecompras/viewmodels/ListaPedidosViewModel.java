package com.example.gestiondecompras.viewmodels;

import com.example.gestiondecompras.utils.Dinero;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.Abono;
import com.example.gestiondecompras.models.Pedido;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListaPedidosViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final ExecutorService executorService;

    private final MutableLiveData<List<Pedido>> pedidos = new MutableLiveData<>();

    public ListaPedidosViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Pedido>> getPedidos() {
        return pedidos;
    }

    public void loadPedidos(String estado, String busqueda, Integer clienteId) {
        executorService.execute(() -> {
            String trimmedSearch = busqueda == null ? "" : busqueda.trim();
            String estadoQuery = mapEstado(estado);
            List<Pedido> resultado;

            if (isAtrasados(estado)) {
                resultado = db.pedidoDao().getPedidosAtrasados(System.currentTimeMillis());
                resultado = filtrar(resultado, trimmedSearch, clienteId);
            } else if ("POR_COBRAR".equals(estadoQuery)) {
                resultado = db.pedidoDao().getPedidosNoPagados();
                resultado = filtrar(resultado, trimmedSearch, clienteId);
            } else {
                resultado = db.pedidoDao().getPedidosFiltrados(estadoQuery, trimmedSearch, clienteId);
            }

            for (Pedido pedido : resultado) {
                pedido.totalAbonado = db.abonoDao().getTotalAbonado(pedido.getId());
            }
            pedidos.postValue(resultado);
        });
    }

    public void registrarAbono(Pedido pedido, double monto, String metodo, String notas,
                               MutableLiveData<Boolean> resultado) {
        executorService.execute(() -> db.runInTransaction(() -> {
            Abono abono = new Abono();
            abono.pedidoId = pedido.getId();
            abono.monto = Dinero.redondear(monto);
            abono.fechaEpoch = System.currentTimeMillis();
            abono.metodo = metodo;
            abono.notas = notas;
            db.abonoDao().insert(abono);

            double totalAbonado = db.abonoDao().getTotalAbonado(pedido.getId());
            pedido.totalAbonado = totalAbonado;
            if (pedido.estaPagadoCompleto()) {
                pedido.estado = Pedido.ESTADO_PAGADO;
            }
            db.pedidoDao().update(pedido);
            resultado.postValue(true);
        }));
    }

    public void eliminarAbono(Pedido pedido, Abono abono, MutableLiveData<Boolean> resultado) {
        executorService.execute(() -> db.runInTransaction(() -> {
            db.abonoDao().delete(abono);

            double totalAbonado = db.abonoDao().getTotalAbonado(pedido.getId());
            pedido.totalAbonado = totalAbonado;
            if (Pedido.ESTADO_PAGADO.equalsIgnoreCase(pedido.getEstado())
                    && !pedido.estaPagadoCompleto()) {
                pedido.estado = Pedido.ESTADO_PENDIENTE;
            }
            db.pedidoDao().update(pedido);
            resultado.postValue(true);
        }));
    }

    public LiveData<List<Abono>> getAbonos(long pedidoId) {
        MutableLiveData<List<Abono>> abonos = new MutableLiveData<>();
        executorService.execute(() -> {
            List<Abono> lista = db.abonoDao().getAbonosPorPedido(pedidoId);
            abonos.postValue(lista != null ? lista : new ArrayList<>());
        });
        return abonos;
    }

    public void deletePedido(Pedido pedido) {
        executorService.execute(() -> {
            db.pedidoDao().delete(pedido);
        });
    }

    public void updatePedido(Pedido pedido) {
        executorService.execute(() -> {
            db.pedidoDao().update(pedido);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }

    private boolean isAtrasados(String estado) {
        return estado != null && estado.equalsIgnoreCase("Atrasados");
    }

    private String mapEstado(String estado) {
        if (estado == null) return "";
        switch (estado.toLowerCase(Locale.getDefault())) {
            case "pendientes":
            case "pendiente":
                return Pedido.ESTADO_PENDIENTE;
            case "entregados":
            case "entregado":
                return Pedido.ESTADO_ENTREGADO;
            case "pagados":
            case "pagado":
                return Pedido.ESTADO_PAGADO;
            case "cancelados":
            case "cancelado":
                return Pedido.ESTADO_CANCELADO;
            case "por cobrar":
            case "por_cobrar":
                return "POR_COBRAR";
            default:
                return "";
        }
    }

    private List<Pedido> filtrar(List<Pedido> origen, String busqueda, Integer clienteId) {
        if ((busqueda == null || busqueda.isEmpty()) && clienteId == null) {
            return origen;
        }
        List<Pedido> filtrados = new ArrayList<>();
        String criterio = busqueda == null ? "" : busqueda.toLowerCase(Locale.getDefault());
        for (Pedido pedido : origen) {
            boolean coincideCliente = clienteId == null || pedido.getClienteId() == clienteId;
            boolean coincideBusqueda = criterio.isEmpty()
                    || pedido.getClienteNombre().toLowerCase(Locale.getDefault()).contains(criterio)
                    || pedido.getTienda().toLowerCase(Locale.getDefault()).contains(criterio);
            if (coincideCliente && coincideBusqueda) {
                filtrados.add(pedido);
            }
        }
        return filtrados;
    }
}
