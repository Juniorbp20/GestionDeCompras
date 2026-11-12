package com.example.gestiondecompras.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.databinding.ActivityNuevaTarjetaBinding;
import com.example.gestiondecompras.models.Tarjeta;
import com.example.gestiondecompras.viewmodels.NuevaTarjetaViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class NuevaTarjetaActivity extends AppCompatActivity {

    public static final String EXTRA_TARJETA_ID = "extra_tarjeta_id";
    public static final String EXTRA_TARJETA_BANCO = "extra_tarjeta_banco";
    public static final String EXTRA_TARJETA_ALIAS = "extra_tarjeta_alias";
    public static final String EXTRA_TARJETA_LIMITE = "extra_tarjeta_limite";
    public static final String EXTRA_TARJETA_DIA_CORTE = "extra_tarjeta_dia_corte";
    public static final String EXTRA_TARJETA_DIA_PAGO = "extra_tarjeta_dia_pago";
    public static final String EXTRA_TARJETA_VENCIMIENTO = "extra_tarjeta_vencimiento";
    public static final String EXTRA_TARJETA_NOTAS = "extra_tarjeta_notas";
    public static final String EXTRA_TARJETA_DEUDA = "extra_tarjeta_deuda";

    private ActivityNuevaTarjetaBinding binding;
    private NuevaTarjetaViewModel viewModel;
    private long tarjetaIdEdicion = 0L;
    private double deudaOriginal = 0d;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNuevaTarjetaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(NuevaTarjetaViewModel.class);

        setupToolbar();
        setupSaveButton();
        cargarDatosEdicion(getIntent());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSaveButton() {
        binding.btnGuardar.setOnClickListener(v -> saveTarjeta());
        setupDateFormatting();
    }

    private void cargarDatosEdicion(Intent intent) {
        if (intent == null) {
            return;
        }
        tarjetaIdEdicion = intent.getLongExtra(EXTRA_TARJETA_ID, 0L);
        if (tarjetaIdEdicion <= 0L) {
            return;
        }

        binding.toolbar.setTitle(R.string.editar_tarjeta_title);
        binding.btnGuardar.setText(R.string.guardar_cambios);

        binding.etBanco.setText(intent.getStringExtra(EXTRA_TARJETA_BANCO));
        binding.etAlias.setText(intent.getStringExtra(EXTRA_TARJETA_ALIAS));
        binding.etLimite.setText(valorNumerico(intent, EXTRA_TARJETA_LIMITE));
        binding.etDiaCorte.setText(valorEntero(intent, EXTRA_TARJETA_DIA_CORTE));
        binding.etDiaPago.setText(valorEntero(intent, EXTRA_TARJETA_DIA_PAGO));
        binding.etFechaVencimiento.setText(intent.getStringExtra(EXTRA_TARJETA_VENCIMIENTO));
        binding.etNotas.setText(intent.getStringExtra(EXTRA_TARJETA_NOTAS));
        deudaOriginal = intent.getDoubleExtra(EXTRA_TARJETA_DEUDA, 0d);
    }

    private CharSequence valorNumerico(Intent intent, String extra) {
        if (!intent.hasExtra(extra)) {
            return "";
        }
        double valor = intent.getDoubleExtra(extra, 0d);
        if (valor == 0d) {
            return "";
        }
        return String.valueOf(valor);
    }

    private CharSequence valorEntero(Intent intent, String extra) {
        if (!intent.hasExtra(extra)) {
            return "";
        }
        int valor = intent.getIntExtra(extra, 0);
        if (valor == 0) {
            return "";
        }
        return String.valueOf(valor);
    }

    private void setupDateFormatting() {
        binding.etFechaVencimiento.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isUpdating;
            private int oldLength;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                oldLength = s.length();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isUpdating) {
                    return;
                }
                isUpdating = true;

                String str = s.toString().replaceAll("[^\\d]", ""); // Keep only digits
                String formatted = "";

                if (str.length() >= 2) {
                    formatted = str.substring(0, 2) + "/" + str.substring(2);
                    if (str.length() > 4) { // Limit to MM/YY (4 digits total)
                         formatted = formatted.substring(0, 5);
                    }
                } else {
                    formatted = str;
                }
                
                // Handle deletion of the slash naturally by checking if we reduced length
                // But generally, the above rebuilds the string securely. 
                // Only issue is cursor position.

                binding.etFechaVencimiento.setText(formatted);
                // Set cursor to end
                binding.etFechaVencimiento.setSelection(binding.etFechaVencimiento.getText().length());

                isUpdating = false;
            }
        });
    }

    private void saveTarjeta() {
        String banco = texto(binding.etBanco);
        String alias = texto(binding.etAlias);
        String limiteTexto = texto(binding.etLimite);
        String diaCorteTexto = texto(binding.etDiaCorte);
        String diaPagoTexto = texto(binding.etDiaPago);
        String fechaVencimiento = texto(binding.etFechaVencimiento);
        String notas = texto(binding.etNotas);

        if (TextUtils.isEmpty(banco) || TextUtils.isEmpty(limiteTexto) || TextUtils.isEmpty(diaCorteTexto) || TextUtils.isEmpty(fechaVencimiento)) {
            Toast.makeText(this, R.string.tarjeta_campos_obligatorios, Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Date Validation ---
        if (!isValidDate(fechaVencimiento)) {
             return; // Toast handled in isValidDate
        }

        double limite;
        int diaCorte;
        int diaPago;
        try {
            limite = Double.parseDouble(limiteTexto);
            diaCorte = Integer.parseInt(diaCorteTexto);
            diaPago = diaPagoTexto.isEmpty() ? 0 : Integer.parseInt(diaPagoTexto);
        } catch (NumberFormatException ex) {
            Toast.makeText(this, R.string.tarjeta_campos_obligatorios, Toast.LENGTH_SHORT).show();
            return;
        }

        if (diaCorte < 1 || diaCorte > 31) {
            binding.etDiaCorte.setError(getString(R.string.tarjeta_dia_invalido));
            Toast.makeText(this, R.string.tarjeta_dia_invalido, Toast.LENGTH_SHORT).show();
            return;
        }
        if (diaPago < 0 || diaPago > 31) {
            binding.etDiaPago.setError(getString(R.string.tarjeta_dia_invalido));
            Toast.makeText(this, R.string.tarjeta_dia_invalido, Toast.LENGTH_SHORT).show();
            return;
        }
        binding.etDiaCorte.setError(null);
        binding.etDiaPago.setError(null);

        Tarjeta tarjeta = new Tarjeta();
        tarjeta.setId(tarjetaIdEdicion);
        tarjeta.setBanco(banco);
        tarjeta.setAlias(alias);
        tarjeta.setLimiteCredito(limite);
        tarjeta.setDiaCorte(diaCorte);
        tarjeta.setDiaPago(diaPago);
        tarjeta.setFechaVencimiento(fechaVencimiento);
        tarjeta.setNotas(notas);
        tarjeta.setDeudaActual(deudaOriginal);

        viewModel.saveTarjeta(tarjeta);

        Toast.makeText(this, R.string.toast_tarjeta_guardada, Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private boolean isValidDate(String date) {
        if (date.length() != 5 || !date.contains("/")) {
            binding.etFechaVencimiento.setError("Formato inválido. Use MM/YY");
            return false;
        }

        String[] parts = date.split("/");
        if (parts.length != 2) {
             binding.etFechaVencimiento.setError("Formato inválido");
             return false;
        }

        int month, year;
        try {
            month = Integer.parseInt(parts[0]);
            year = Integer.parseInt(parts[1]) + 2000; // Assume 2000s
        } catch (NumberFormatException e) {
            binding.etFechaVencimiento.setError("Fecha inválida");
            return false;
        }

        if (month < 1 || month > 12) {
            binding.etFechaVencimiento.setError("Mes inválido (01-12)");
            return false;
        }

        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentYear = now.get(java.util.Calendar.YEAR);
        int currentMonth = now.get(java.util.Calendar.MONTH) + 1; // 0-indexed

        if (year < currentYear || (year == currentYear && month < currentMonth)) {
            binding.etFechaVencimiento.setError("La tarjeta está vencida");
            return false;
        }
        
        binding.etFechaVencimiento.setError(null);
        return true;
    }

    private String texto(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
