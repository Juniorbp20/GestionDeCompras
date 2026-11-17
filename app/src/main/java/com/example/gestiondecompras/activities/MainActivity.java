package com.example.gestiondecompras.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.gestiondecompras.R;
import com.example.gestiondecompras.adapters.PedidosAdapter;
import com.example.gestiondecompras.databinding.ActivityMainBinding;
import com.example.gestiondecompras.utils.NavegacionInferior;
import com.example.gestiondecompras.utils.UpdateChecker;
import com.google.android.gms.ads.AdRequest;
import com.example.gestiondecompras.viewmodels.DashboardViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DashboardViewModel viewModel;
    private PedidosAdapter proximosAdapter;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, R.string.notif_permiso_concedido, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.notif_permiso_rechazado, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        updateGreeting();
        setupUpcomingList();
        setupClickListeners();
        setupNavegacion();
        observeViewModel();
        
        checkBiometricSecurity();
        checkFirstRun();
        loadBannerAd();
        askNotificationPermission();
        checkForUpdates();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void loadBannerAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        binding.adView.loadAd(adRequest);
    }

    private void checkFirstRun() {
        android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("is_first_run", true);
        if (isFirstRun) {
            startActivity(new Intent(this, AyudaActivity.class));
            prefs.edit().putBoolean("is_first_run", false).apply();
        }
    }

    private void checkBiometricSecurity() {
        android.content.SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean biometricEnabled = prefs.getBoolean("biometric_enabled", false);

        if (biometricEnabled) {
            Executor executor = ContextCompat.getMainExecutor(this);
            androidx.biometric.BiometricPrompt biometricPrompt = new androidx.biometric.BiometricPrompt(MainActivity.this,
                    executor, new androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    if (errorCode == androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED || 
                        errorCode == androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        finishAffinity();
                    } else {
                         Toast.makeText(MainActivity.this, getString(R.string.error_autenticacion, errString), Toast.LENGTH_LONG).show();
                         finishAffinity();
                    }
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull androidx.biometric.BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                }
            });

            androidx.biometric.BiometricPrompt.PromptInfo promptInfo = new androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.biometric_title))
                    .setSubtitle(getString(R.string.biometric_subtitle))
                    .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG | 
                                              androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();

            biometricPrompt.authenticate(promptInfo);
        }
    }

    /** Barra inferior (Inicio activo) y acceso directo a Ajustes en la barra superior. */
    private void setupNavegacion() {
        NavegacionInferior.configurar(this, binding.bottomNav, R.id.tab_inicio,
                () -> binding.scrollInicio.smoothScrollTo(0, 0));

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.accion_ajustes) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.accion_buscar) {
                startActivity(new Intent(this, BusquedaActivity.class));
                return true;
            }
            return false;
        });
    }

    private void updateGreeting() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int greetingRes;
        if (hour < 12) {
            greetingRes = R.string.dashboard_greeting_morning;
        } else if (hour < 18) {
            greetingRes = R.string.dashboard_greeting_afternoon;
        } else {
            greetingRes = R.string.dashboard_greeting_evening;
        }
        binding.tvGreeting.setText(getString(greetingRes));
    }

    private void setupUpcomingList() {
        proximosAdapter = new PedidosAdapter(null);
        binding.rvProximosPedidos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProximosPedidos.setAdapter(proximosAdapter);
    }

    private void setupClickListeners() {
        binding.chipNuevoPedido.setOnClickListener(v ->
                startActivity(new Intent(this, NuevoPedidoActivity.class)));

        binding.btnVacioAccion.setOnClickListener(v ->
                startActivity(new Intent(this, NuevoPedidoActivity.class)));

        binding.chipNuevoGasto.setOnClickListener(v ->
                startActivity(new Intent(this, NuevoGastoActivity.class)));

        binding.chipNuevoCliente.setOnClickListener(v ->
                startActivity(new Intent(this, NuevoClienteActivity.class)));

        binding.chipCalendario.setOnClickListener(v ->
                startActivity(new Intent(this, CalendarioActivity.class)));

        binding.cardTotalPendiente.setOnClickListener(v -> {
            Intent intent = new Intent(this, ListaPedidosActivity.class);
            intent.putExtra("filtro_estado", "por_cobrar");
            startActivity(intent);
        });

        binding.cardTotalPagado.setOnClickListener(v -> {
            Intent intent = new Intent(this, ListaPedidosActivity.class);
            intent.putExtra("filtro_estado", "pagado");
            startActivity(intent);
        });

        binding.cardPedidosAtrasados.setOnClickListener(v -> {
            Intent intent = new Intent(this, ListaPedidosActivity.class);
            intent.putExtra("filtro_atrasados", true);
            startActivity(intent);
        });

        binding.cardGananciaEsperada.setOnClickListener(v -> showClientEarningsDialog());

        binding.cardGastosMes.setOnClickListener(v ->
                startActivity(new Intent(this, ListaGastosActivity.class)));
    }

    @SuppressLint("DefaultLocale")
    private void observeViewModel() {
        viewModel.getDashboardData().observe(this, dashboardRow -> {
            if (dashboardRow != null) {
                binding.tvTotalPendiente.setText(String.format("RD$ %,.2f", dashboardRow.totalPendiente));
                binding.tvTotalPendienteCard.setText(String.format("RD$ %,.2f", dashboardRow.totalPendiente));
                binding.tvTotalPagado.setText(String.format("RD$ %,.2f", dashboardRow.totalPagado));
            }
        });

        viewModel.getOverdueOrdersCount().observe(this, count -> {
            if (count != null) {
                binding.tvPedidosAtrasados.setText(String.valueOf(count));
            }
        });

        viewModel.getProjectedEarnings().observe(this, earnings -> {
            if (earnings != null) {
                binding.tvGananciaEsperadaCard.setText(String.format("RD$ %,.2f", earnings));
            }
        });

        viewModel.getUpcomingOrders().observe(this, pedidos -> {
            if (pedidos != null && !pedidos.isEmpty()) {
                binding.emptyState.setVisibility(View.GONE);
                binding.rvProximosPedidos.setVisibility(View.VISIBLE);
                proximosAdapter.actualizarLista(pedidos);
            } else {
                binding.rvProximosPedidos.setVisibility(View.GONE);
                binding.emptyState.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getMonthExpenses().observe(this, total -> {
            if (total != null) {
                binding.tvGastosMes.setText(String.format("RD$ %,.2f", total));
            }
        });
    }

    private void checkForUpdates() {
        com.example.gestiondecompras.utils.UpdateHelper.checkAndPrompt(this, false);
    }

    private void showClientEarningsDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        androidx.recyclerview.widget.RecyclerView rv = new androidx.recyclerview.widget.RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        com.example.gestiondecompras.adapters.ClienteGananciaAdapter adapter = new com.example.gestiondecompras.adapters.ClienteGananciaAdapter();
        rv.setAdapter(adapter);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        rv.setPadding(pad, pad, pad, pad);
        
        builder.setTitle(R.string.ganancias_por_cliente);
        builder.setView(rv);
        builder.setPositiveButton(R.string.accion_cerrar, null);
        
        viewModel.getClientEarnings().observe(this, adapter::actualizarLista);
        
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadDashboardData();
        if (binding.adView != null) {
            binding.adView.resume();
        }
    }

    @Override
    protected void onPause() {
        if (binding.adView != null) {
            binding.adView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (binding.adView != null) {
            binding.adView.destroy();
        }
        super.onDestroy();
    }
}
