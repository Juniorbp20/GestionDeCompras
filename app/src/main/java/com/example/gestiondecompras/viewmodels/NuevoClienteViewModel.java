package com.example.gestiondecompras.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.Cliente;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NuevoClienteViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final ExecutorService executorService;
    private final MutableLiveData<Cliente> clienteEdicion = new MutableLiveData<>();

    public NuevoClienteViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<Cliente> getClienteEdicion() {
        return clienteEdicion;
    }

    /** Carga el cliente que se va a editar. */
    public void cargarCliente(long id) {
        executorService.execute(() -> clienteEdicion.postValue(db.clienteDao().getClienteById(id)));
    }

    /** Inserta un cliente nuevo o actualiza uno existente (id &gt; 0) y devuelve su id. */
    public void saveCliente(Cliente cliente, MutableLiveData<Long> idGuardado) {
        executorService.execute(() -> {
            if (cliente.id > 0) {
                db.clienteDao().update(cliente);
            } else {
                cliente.id = db.clienteDao().insert(cliente);
            }
            idGuardado.postValue(cliente.id);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
