package com.example.gestiondecompras;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.gestiondecompras.utils.RecordatorioUtils;
import com.example.gestiondecompras.workers.NotificationWorker;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.color.DynamicColors;

import java.util.concurrent.TimeUnit;

public class GestionComprasApp extends Application {

    private static final String TRABAJO_RECORDATORIOS = "recordatorios_diarios";

    @Override
    public void onCreate() {
        super.onCreate();

        // Restaurar el tema guardado (claro/oscuro/sistema) antes de crear cualquier Activity
        int theme = getSharedPreferences("settings", MODE_PRIVATE)
                .getInt("theme", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(theme);

        // DESACTIVAR COLORES DINÁMICOS (Evita el morado de Android 12+)
        DynamicColors.isDynamicColorAvailable();// No aplicamos DynamicColors.applyToActivitiesIfAvailable(this);
// Si ya estaba puesto por el asistente de Android Studio, esta es la causa.

        NotificationWorker.crearCanales(this);
        programarRecordatorios(this);

        // Inicializar el SDK de anuncios de Google
        new Thread(() -> MobileAds.initialize(this, initializationStatus -> {})).start();
    }

    /**
     * Programa (o actualiza) el trabajo periódico sin reiniciar su ciclo.
     * Se usa al arrancar la app para no perder la programación existente.
     */
    public static void programarRecordatorios(Context context) {
        programar(context, ExistingPeriodicWorkPolicy.UPDATE);
    }

    /**
     * Vuelve a programar desde cero. Se usa cuando el usuario cambia la hora,
     * la frecuencia o activa/desactiva los recordatorios en Ajustes.
     */
    public static void reprogramarRecordatorios(Context context) {
        programar(context, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE);
    }

    private static void programar(Context context, ExistingPeriodicWorkPolicy policy) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        WorkManager workManager = WorkManager.getInstance(context);

        if (!prefs.getBoolean("notif_enabled", true)) {
            workManager.cancelUniqueWork(TRABAJO_RECORDATORIOS);
            return;
        }

        int frecuencia = RecordatorioUtils.normalizarFrecuencia(
                prefs.getInt("notif_frequency_hours", RecordatorioUtils.FRECUENCIA_PREDETERMINADA));
        int hora = RecordatorioUtils.normalizarHoraResumen(prefs.getInt("reminder_hour", 9));
        int minuto = Math.max(0, Math.min(59, prefs.getInt("reminder_minute", 0)));

        long retraso = RecordatorioUtils.milisegundosHastaSiguienteFranja(
                System.currentTimeMillis(), hora, minuto, frecuencia);

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                NotificationWorker.class, frecuencia, TimeUnit.HOURS)
                .setInitialDelay(retraso, TimeUnit.MILLISECONDS)
                .build();

        workManager.enqueueUniquePeriodicWork(TRABAJO_RECORDATORIOS, policy, workRequest);
    }
}
