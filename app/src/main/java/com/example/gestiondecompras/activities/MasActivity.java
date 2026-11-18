package com.example.gestiondecompras.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.gestiondecompras.R;
import com.example.gestiondecompras.databinding.ActivityMasBinding;
import com.example.gestiondecompras.utils.NavegacionInferior;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

/**
 * Pestaña "Más": perfil de Google y accesos agrupados a las secciones secundarias
 * (Tarjetas, Reportes, Calendario, Ajustes y Ayuda).
 */
public class MasActivity extends AppCompatActivity {

    private ActivityMasBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavegacionInferior.configurar(this, binding.bottomNav, R.id.tab_mas,
                () -> binding.scrollMas.smoothScrollTo(0, 0));

        binding.filaTarjetas.setOnClickListener(v -> abrir(TarjetasActivity.class));
        binding.filaTiendas.setOnClickListener(v -> abrir(TiendasActivity.class));
        binding.filaReportes.setOnClickListener(v -> abrir(ReportesActivity.class));
        binding.filaCalendario.setOnClickListener(v -> abrir(CalendarioActivity.class));
        binding.filaAjustes.setOnClickListener(v -> abrir(SettingsActivity.class));
        binding.filaAyuda.setOnClickListener(v -> abrir(AyudaActivity.class));
        binding.cardPerfil.setOnClickListener(v -> abrir(SettingsActivity.class));

        mostrarVersion();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mostrarPerfil();
    }

    private void abrir(Class<?> pantalla) {
        startActivity(new Intent(this, pantalla));
    }

    private void mostrarPerfil() {
        GoogleSignInAccount cuenta = GoogleSignIn.getLastSignedInAccount(this);
        if (cuenta != null) {
            binding.tvPerfilNombre.setText(cuenta.getDisplayName() != null
                    ? cuenta.getDisplayName() : getString(R.string.app_name));
            binding.tvPerfilEmail.setText(cuenta.getEmail());
            if (cuenta.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(cuenta.getPhotoUrl())
                        .placeholder(R.drawable.ic_shoppong)
                        .circleCrop()
                        .into(binding.ivPerfil);
            }
        } else {
            binding.tvPerfilNombre.setText(R.string.mas_sin_cuenta);
            binding.tvPerfilEmail.setText(R.string.mas_vincular);
            binding.ivPerfil.setImageResource(R.drawable.ic_shoppong);
        }
    }

    private void mostrarVersion() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            binding.tvVersionMas.setText(getString(R.string.app_name) + " · v" + version);
        } catch (PackageManager.NameNotFoundException e) {
            binding.tvVersionMas.setText(R.string.app_name);
        }
    }
}
