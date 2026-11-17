package com.example.gestiondecompras.viewmodels;

import com.example.gestiondecompras.servicios.TarjetasServicio;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.Tarjeta;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListaTarjetasViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final ExecutorService executorService;

    private final MutableLiveData<List<Tarjeta>> tarjetas = new MutableLiveData<>();
    private final MutableLiveData<String> mensajes = new MutableLiveData<>();

    public ListaTarjetasViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Tarjeta>> getTarjetas() {
        return tarjetas;
    }

    public LiveData<String> getMensajes() {
        return mensajes;
    }

    public void loadTarjetas() {
        executorService.execute(() -> tarjetas.postValue(db.tarjetaDao().getAllTarjetas()));
    }

    public void deleteTarjeta(Tarjeta tarjeta) {
        executorService.execute(() -> {
            try {
                TarjetasServicio.ResultadoEliminacion r = new TarjetasServicio(db).eliminar(tarjeta);
                if (r == TarjetasServicio.ResultadoEliminacion.EN_USO) {
                    mensajes.postValue("tarjeta_en_uso");
                } else {
                    mensajes.postValue("tarjeta_eliminada");
                    loadTarjetas();
                }
            } catch (Exception e) {
                mensajes.postValue("tarjeta_error");
            }
        });
    }

    /** Registra un pago a la tarjeta (reduce su deuda) y recarga la lista. */
    public void registrarPago(Tarjeta tarjeta, double monto) {
        executorService.execute(() -> {
            TarjetasServicio.ResultadoPago r = new TarjetasServicio(db).registrarPago(tarjeta.id, monto);
            if (r == TarjetasServicio.ResultadoPago.REGISTRADO) {
                mensajes.postValue("tarjeta_pagada");
                loadTarjetas();
            } else if (r == TarjetasServicio.ResultadoPago.EXCEDE_DEUDA) {
                mensajes.postValue("pago_excede_deuda");
            } else {
                mensajes.postValue("pago_invalido");
            }
        });
    }

    public void updateTarjeta(Tarjeta tarjeta) {
        executorService.execute(() -> {
            db.tarjetaDao().update(tarjeta);
            loadTarjetas();
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
