package com.example.gestiondecompras.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.Tarjeta;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NuevaTarjetaViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final ExecutorService executorService;

    public NuevaTarjetaViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    /** Guarda la tarjeta y devuelve su id por {@code idGuardado} (para seleccionarla al volver). */
    public void saveTarjeta(Tarjeta tarjeta, androidx.lifecycle.MutableLiveData<Long> idGuardado) {
        executorService.execute(() -> {
            long id;
            if (tarjeta.getId() > 0) {
                db.tarjetaDao().update(tarjeta);
                id = tarjeta.getId();
            } else {
                id = db.tarjetaDao().insert(tarjeta);
            }
            idGuardado.postValue(id);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
