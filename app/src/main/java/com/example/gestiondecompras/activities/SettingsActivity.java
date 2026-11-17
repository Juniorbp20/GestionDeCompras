package com.example.gestiondecompras.activities;

import android.annotation.SuppressLint;
import android.Manifest;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.gestiondecompras.GestionComprasApp;
import com.example.gestiondecompras.R;
import com.example.gestiondecompras.databinding.ActivitySettingsBinding;
import com.example.gestiondecompras.utils.GoogleDriveService;
import com.example.gestiondecompras.utils.RecordatorioUtils;
import com.example.gestiondecompras.utils.UpdateChecker;
import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.workers.NotificationWorker;
import com.google.android.material.textfield.TextInputEditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.DriveScopes;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences sharedPreferences;
    private int selectedHour = 9;
    private int selectedMinute = 0;
    private GoogleSignInClient googleSignInClient;
    private GoogleDriveService googleDriveService;
    private Runnable pendingAction;
    private static final String DB_NAME = "GestionCompras.db";

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                updateNotificationPermissionStatus();
                if (!granted) {
                    Toast.makeText(this, R.string.notif_permiso_requerido, Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        abrirAjustesNotificaciones();
                    }
                }
            });

    private final ActivityResultLauncher<android.content.Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleSignInResult(result.getData());
                } else if (result.getData() != null) {
                    try {
                        GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                                .getResult(ApiException.class);
                        Toast.makeText(this, R.string.google_login_cancelado, Toast.LENGTH_SHORT).show();
                    } catch (ApiException e) {
                        Toast.makeText(this, getString(R.string.google_error, e.getStatusCode()), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, R.string.google_login_fallido, Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);

        setupGoogleSignIn();
        setupToolbar();
        loadSettings();
        setupListeners();
        updateNotificationPermissionStatus();
        updateGoogleLoginStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationPermissionStatus();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    @SuppressLint("SetTextI18n")
    private void loadSettings() {
        int theme = sharedPreferences.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (theme == AppCompatDelegate.MODE_NIGHT_YES) binding.rbDark.setChecked(true);
        else if (theme == AppCompatDelegate.MODE_NIGHT_NO) binding.rbLight.setChecked(true);
        else binding.rbSystem.setChecked(true);

        int reminderDays = sharedPreferences.getInt("reminder_time", 3);
        binding.etDiasEntrega.setText(String.valueOf(sharedPreferences.getInt("dias_entrega", 1)));
        binding.etDiasCorte.setText(String.valueOf(sharedPreferences.getInt("dias_corte", reminderDays)));
        binding.etDiasPago.setText(String.valueOf(sharedPreferences.getInt("dias_pago", 3)));

        binding.swNotificaciones.setChecked(sharedPreferences.getBoolean("notif_enabled", true));
        binding.llOpcionesNotificacion.setVisibility(
                binding.swNotificaciones.isChecked() ? View.VISIBLE : View.GONE);

        int frecuencia = sharedPreferences.getInt("notif_frequency_hours", 6);
        if (frecuencia == 12) {
            binding.chipFrecuencia12.setChecked(true);
        } else if (frecuencia == 24) {
            binding.chipFrecuencia24.setChecked(true);
        } else {
            binding.chipFrecuencia6.setChecked(true);
        }

        selectedHour = RecordatorioUtils.normalizarHoraResumen(sharedPreferences.getInt("reminder_hour", 9));
        selectedMinute = sharedPreferences.getInt("reminder_minute", 0);
        updateTimeText();

        boolean biometricEnabled = sharedPreferences.getBoolean("biometric_enabled", false);
        binding.swBiometric.setChecked(biometricEnabled);

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            binding.tvVersion.setText(getString(R.string.version_formato, versionName));
        } catch (Exception e) {
            binding.tvVersion.setText(R.string.version_desconocida);
        }
    }

    @SuppressLint("DefaultLocale")
    private void updateTimeText() {
        binding.tvReminderClockTime.setText(String.format("%02d:%02d", selectedHour, selectedMinute));
    }

    private void updateNotificationPermissionStatus() {
        boolean activo = NotificationWorker.puedeNotificar(this);
        binding.tvEstadoPermiso.setText(activo ? R.string.notif_estado_activo : R.string.notif_estado_inactivo);
        binding.tvEstadoPermiso.setTextColor(ContextCompat.getColor(this,
                activo ? R.color.success_color : R.color.error_color));
    }

    private void solicitarPermisoNotificaciones() {
        if (NotificationWorker.puedeNotificar(this)) {
            abrirAjustesNotificaciones();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            abrirAjustesNotificaciones();
        }
    }

    private void abrirAjustesNotificaciones() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void probarNotificacion() {
        if (!NotificationWorker.puedeNotificar(this)) {
            Toast.makeText(this, R.string.notif_permiso_requerido, Toast.LENGTH_SHORT).show();
            solicitarPermisoNotificaciones();
            return;
        }
        if (NotificationWorker.enviarNotificacionPrueba(this)) {
            Toast.makeText(this, R.string.notif_prueba_enviada, Toast.LENGTH_SHORT).show();
        }
    }

    private int leerDias(TextInputEditText campo, int predeterminado) {
        String texto = campo.getText() != null ? campo.getText().toString().trim() : "";
        if (texto.isEmpty()) {
            return predeterminado;
        }
        try {
            int valor = Integer.parseInt(texto);
            return valor >= 0 && valor <= 30 ? valor : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void setupListeners() {
        binding.tvReminderClockTime.setOnClickListener(v -> new android.app.TimePickerDialog(this, (view, hourOfDay, minute) -> {
            int horaPermitida = RecordatorioUtils.normalizarHoraResumen(hourOfDay);
            selectedHour = horaPermitida;
            selectedMinute = minute;
            updateTimeText();
            if (horaPermitida != hourOfDay) {
                Toast.makeText(this, getString(R.string.notif_hora_ajustada,
                        binding.tvReminderClockTime.getText()), Toast.LENGTH_LONG).show();
            }
        }, selectedHour, selectedMinute, true).show());

        binding.swNotificaciones.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.llOpcionesNotificacion.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        binding.tvEstadoPermiso.setOnClickListener(v -> solicitarPermisoNotificaciones());
        binding.btnProbarNotificacion.setOnClickListener(v -> probarNotificacion());

        binding.btnReset.setOnClickListener(v -> showResetConfirmation());
        binding.btnContact.setOnClickListener(v -> contactSupport());
        binding.btnCheckUpdate.setOnClickListener(v -> checkUpdatesManually());
        binding.btnSaveSettings.setOnClickListener(v -> saveSettings());

        binding.btnDriveBackup.setOnClickListener(v -> {
            pendingAction = this::performDriveBackup;
            signInAndDo(pendingAction);
        });
        binding.btnDriveRestore.setOnClickListener(v -> {
            pendingAction = this::performDriveRestore;
            signInAndDo(pendingAction);
        });

        binding.btnGoogleLogout.setOnClickListener(v -> {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                logoutGoogle();
            } else {
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
            }
        });
    }

    private void signInAndDo(Runnable action) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && account.getGrantedScopes().contains(new Scope(DriveScopes.DRIVE_FILE))) {
            googleDriveService = new GoogleDriveService(this, account.getEmail());
            action.run();
        } else {
            googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
        }
    }

    private void handleSignInResult(android.content.Intent data) {
        Task<GoogleSignInAccount> completedTask = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            googleDriveService = new GoogleDriveService(this, account.getEmail());
            updateGoogleLoginStatus();
            if (pendingAction != null) {
                pendingAction.run();
                pendingAction = null;
            }
        } catch (ApiException e) {
            Toast.makeText(this, getString(R.string.google_error, e.getStatusCode()), Toast.LENGTH_SHORT).show();
        }
    }

    private void logoutGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            googleDriveService = null;
            updateGoogleLoginStatus();
            Toast.makeText(SettingsActivity.this, R.string.google_sesion_cerrada, Toast.LENGTH_SHORT).show();
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateGoogleLoginStatus() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            binding.btnGoogleLogout.setText(R.string.google_cerrar_sesion);
            binding.btnGoogleLogout.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
        } else {
            binding.btnGoogleLogout.setText(R.string.google_iniciar_sesion);
            binding.btnGoogleLogout.setTextColor(getResources().getColor(android.R.color.holo_blue_dark, getTheme()));
        }
    }

    private void ensureDbFileExists() {
        AppDatabase db = AppDatabase.getInstance(this);
        db.getOpenHelper().getWritableDatabase();
    }

    private void performDriveBackup() {
        try {
            ensureDbFileExists();
            AppDatabase.getInstance(this).getOpenHelper().getWritableDatabase().execSQL("PRAGMA checkpoint(FULL)");
            
            File dbFile = getDatabasePath(DB_NAME);
            
            if (!dbFile.exists()) {
                Toast.makeText(this, R.string.respaldo_sin_archivo, Toast.LENGTH_SHORT).show();
                return;
            }
            
            Toast.makeText(this, R.string.respaldo_subiendo, Toast.LENGTH_SHORT).show();
            googleDriveService.uploadBackup(dbFile, new GoogleDriveService.DriveCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(() -> Toast.makeText(SettingsActivity.this, R.string.respaldo_subido, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> Toast.makeText(SettingsActivity.this, getString(R.string.error_detalle, e.getMessage()), Toast.LENGTH_LONG).show());
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.respaldo_error_preparar, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void performDriveRestore() {
        AppDatabase.getInstance(this).close();
        
        File dbFile = getDatabasePath(DB_NAME);
        File walFile = new File(dbFile.getPath() + "-wal");
        File shmFile = new File(dbFile.getPath() + "-shm");
        
        if (walFile.exists()) walFile.delete();
        if (shmFile.exists()) shmFile.delete();

        Toast.makeText(this, R.string.respaldo_descargando, Toast.LENGTH_SHORT).show();
        googleDriveService.downloadBackup(dbFile, new GoogleDriveService.DriveCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, R.string.respaldo_restaurado, Toast.LENGTH_LONG).show();
                    restartApp();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(SettingsActivity.this, getString(R.string.error_detalle, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void saveSettings() {
        int theme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (binding.rbDark.isChecked()) theme = AppCompatDelegate.MODE_NIGHT_YES;
        else if (binding.rbLight.isChecked()) theme = AppCompatDelegate.MODE_NIGHT_NO;
        
        AppCompatDelegate.setDefaultNightMode(theme);

        boolean notificacionesActivas = binding.swNotificaciones.isChecked();
        int diasEntrega = leerDias(binding.etDiasEntrega, 1);
        int diasCorte = leerDias(binding.etDiasCorte, 3);
        int diasPago = leerDias(binding.etDiasPago, 3);
        if (diasEntrega < 0 || diasCorte < 0 || diasPago < 0) {
            if (notificacionesActivas) {
                Toast.makeText(this, R.string.notif_dias_invalidos, Toast.LENGTH_SHORT).show();
                return;
            }
            // Con las notificaciones apagadas no se bloquea el guardado: se conservan los valores previos.
            if (diasEntrega < 0) diasEntrega = sharedPreferences.getInt("dias_entrega", 1);
            if (diasCorte < 0) diasCorte = sharedPreferences.getInt("dias_corte", 3);
            if (diasPago < 0) diasPago = sharedPreferences.getInt("dias_pago", 3);
        }

        int frecuencia = 6;
        if (binding.chipFrecuencia12.isChecked()) {
            frecuencia = 12;
        } else if (binding.chipFrecuencia24.isChecked()) {
            frecuencia = 24;
        }

        // Solo se reinicia el ciclo del trabajo si cambia algo que afecta a su programación.
        boolean cambiaProgramacion =
                sharedPreferences.getBoolean("notif_enabled", true) != notificacionesActivas
                        || sharedPreferences.getInt("notif_frequency_hours",
                                RecordatorioUtils.FRECUENCIA_PREDETERMINADA) != frecuencia
                        || sharedPreferences.getInt("reminder_hour", 9) != selectedHour
                        || sharedPreferences.getInt("reminder_minute", 0) != selectedMinute;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("theme", theme);
        editor.putBoolean("biometric_enabled", binding.swBiometric.isChecked());
        editor.putBoolean("notif_enabled", notificacionesActivas);
        editor.putInt("dias_entrega", diasEntrega);
        editor.putInt("dias_corte", diasCorte);
        editor.putInt("dias_pago", diasPago);
        editor.putInt("notif_frequency_hours", frecuencia);
        editor.putInt("reminder_time", diasCorte);
        editor.putInt("reminder_hour", selectedHour);
        editor.putInt("reminder_minute", selectedMinute);
        editor.apply();

        if (cambiaProgramacion) {
            GestionComprasApp.reprogramarRecordatorios(this);
        }

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void restartApp() {
        android.content.Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        finishAffinity();
    }

    private void showResetConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.reset_confirm_titulo)
            .setMessage(R.string.reset_confirm_pregunta)
            .setPositiveButton(R.string.reset_borrar_todo, (dialog, which) -> {
                AppDatabase.getInstance(this).close();
                File dbFile = getDatabasePath(DB_NAME);
                new File(dbFile.getPath() + "-wal").delete();
                new File(dbFile.getPath() + "-shm").delete();
                if(dbFile.delete()) {
                    Toast.makeText(this, R.string.reset_datos_borrados, Toast.LENGTH_SHORT).show();
                    finishAffinity();
                }
            })
            .setNegativeButton(R.string.accion_cancelar, null)
            .show();
    }

    private void contactSupport() {
         android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SENDTO);
         intent.setData(android.net.Uri.parse("mailto:")); 
         intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"sssuport2@gmail.com"});
         intent.setPackage("com.google.android.gm");
         if (intent.resolveActivity(getPackageManager()) != null) {
             startActivity(intent);
         }
    }

    private void checkUpdatesManually() {
        com.example.gestiondecompras.utils.UpdateHelper.checkAndPrompt(this, true);
    }
}
