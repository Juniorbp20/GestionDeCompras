package com.example.gestiondecompras.workers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.utils.GoogleDriveService;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Copia de seguridad automática diaria en Google Drive.
 * Solo se ejecuta si el usuario la activó en Ajustes y tiene una cuenta de Google vinculada
 * con permiso de Drive.
 */
public class RespaldoWorker extends Worker {

    public static final String PREF_ACTIVO = "respaldo_auto";
    public static final String PREF_ULTIMO = "respaldo_ultimo_epoch";
    private static final String TRABAJO = "respaldo_automatico";
    private static final String DB_NAME = "GestionCompras.db";

    public RespaldoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /** Programa (o cancela) la copia diaria según la preferencia del usuario. */
    public static void programar(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        WorkManager wm = WorkManager.getInstance(context);
        if (!prefs.getBoolean(PREF_ACTIVO, false)) {
            wm.cancelUniqueWork(TRABAJO);
            return;
        }
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();
        PeriodicWorkRequest solicitud = new PeriodicWorkRequest.Builder(RespaldoWorker.class, 24, TimeUnit.HOURS)
                .setConstraints(restricciones)
                .build();
        wm.enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.UPDATE, solicitud);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_ACTIVO, false)) {
            return Result.success();
        }

        GoogleSignInAccount cuenta = GoogleSignIn.getLastSignedInAccount(context);
        if (cuenta == null || cuenta.getEmail() == null
                || !cuenta.getGrantedScopes().contains(new Scope(DriveScopes.DRIVE_FILE))) {
            // Sin cuenta vinculada no hay nada que hacer; no se reintenta
            return Result.success();
        }

        File copia = new File(context.getCacheDir(), "respaldo_automatico.db");
        try {
            copiarBaseDeDatos(context, copia);
            new GoogleDriveService(context, cuenta.getEmail()).uploadBackupSync(copia);
            prefs.edit().putLong(PREF_ULTIMO, System.currentTimeMillis()).apply();
            return Result.success();
        } catch (IOException e) {
            return getRunAttemptCount() < 3 ? Result.retry() : Result.failure();
        } catch (Exception e) {
            return Result.failure();
        } finally {
            //noinspection ResultOfMethodCallIgnored
            copia.delete();
        }
    }

    /**
     * Copia el archivo de la base de datos dentro de una transacción para que ninguna
     * escritura lo modifique a mitad de la copia.
     */
    private static void copiarBaseDeDatos(Context context, File destino) throws IOException {
        AppDatabase db = AppDatabase.getInstance(context);
        File origen = context.getDatabasePath(DB_NAME);
        IOException[] error = new IOException[1];
        db.runInTransaction(() -> {
            try (InputStream in = new FileInputStream(origen);
                 OutputStream out = new FileOutputStream(destino)) {
                byte[] buffer = new byte[16 * 1024];
                int leidos;
                while ((leidos = in.read(buffer)) != -1) {
                    out.write(buffer, 0, leidos);
                }
            } catch (IOException e) {
                error[0] = e;
            }
        });
        if (error[0] != null) {
            throw error[0];
        }
    }
}
