package com.example.gestiondecompras.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.servicios.FinanzasServicio;

import java.util.concurrent.TimeUnit;

/** Registra cada día los movimientos recurrentes que ya vencieron. */
public class RecurrentesWorker extends Worker {

    private static final String TRABAJO = "finanzas_recurrentes";

    public RecurrentesWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void programar(Context context) {
        PeriodicWorkRequest solicitud = new PeriodicWorkRequest.Builder(RecurrentesWorker.class, 12, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, solicitud);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            new FinanzasServicio(AppDatabase.getInstance(getApplicationContext()))
                    .generarRecurrentes(System.currentTimeMillis());
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
