package com.example.gestiondecompras.activities;

import com.example.gestiondecompras.utils.Dinero;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.databinding.ActivityNuevoGastoBinding;
import com.example.gestiondecompras.models.Gasto;
import com.example.gestiondecompras.viewmodels.NuevoGastoViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NuevoGastoActivity extends AppCompatActivity {

    public static final String EXTRA_GASTO_ID = "gasto_id";

    private ActivityNuevoGastoBinding binding;
    private NuevoGastoViewModel viewModel;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private long fechaSeleccionada;
    private Gasto gastoEditar = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNuevoGastoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(NuevoGastoViewModel.class);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupCategorias();
        setupFecha();
        setupGuardar();

        long id = getIntent().getLongExtra(EXTRA_GASTO_ID, -1);
        if (id != -1) {
            getSupportActionBar().setTitle(R.string.editar_gasto_title);
            cargarGasto(id);
        } else {
            fechaSeleccionada = System.currentTimeMillis();
            binding.etFecha.setText(sdf.format(new Date(fechaSeleccionada)));
        }
    }

    private void setupCategorias() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.item_dropdown, Gasto.CATEGORIAS);
        binding.spinnerCategoria.setAdapter(adapter);
    }

    private void setupFecha() {
        binding.etFecha.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(fechaSeleccionada);
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar seleccionada = Calendar.getInstance();
                seleccionada.set(year, month, dayOfMonth, 0, 0, 0);
                seleccionada.set(Calendar.MILLISECOND, 0);
                fechaSeleccionada = seleccionada.getTimeInMillis();
                binding.etFecha.setText(sdf.format(new Date(fechaSeleccionada)));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void cargarGasto(long id) {
        MutableLiveData<Gasto> resultado = new MutableLiveData<>();
        resultado.observe(this, gasto -> {
            if (gasto == null) {
                return;
            }
            gastoEditar = gasto;
            fechaSeleccionada = gasto.getFechaEpoch();
            binding.etFecha.setText(sdf.format(new Date(fechaSeleccionada)));
            binding.etMonto.setText(String.valueOf(gasto.getMonto()));
            binding.etDescripcion.setText(gasto.getDescripcion());
            binding.etNotas.setText(gasto.getNotas());
            for (int i = 0; i < Gasto.CATEGORIAS.length; i++) {
                if (Gasto.CATEGORIAS[i].equals(gasto.getCategoria())) {
                    binding.spinnerCategoria.setText(Gasto.CATEGORIAS[i], false);
                    break;
                }
            }
        });
        viewModel.getGasto(id, resultado);
    }

    private void setupGuardar() {
        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void guardar() {
        String categoria = binding.spinnerCategoria.getText() != null
                ? binding.spinnerCategoria.getText().toString() : "";
        String descripcion = binding.etDescripcion.getText() != null
                ? binding.etDescripcion.getText().toString().trim() : "";
        String montoTexto = binding.etMonto.getText() != null
                ? binding.etMonto.getText().toString().trim() : "";
        String notas = binding.etNotas.getText() != null
                ? binding.etNotas.getText().toString().trim() : "";

        if (categoria.isEmpty()) {
            Toast.makeText(this, R.string.gastos_categoria_obligatoria, Toast.LENGTH_SHORT).show();
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(montoTexto);
            if (monto <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            Toast.makeText(this, R.string.gastos_monto_invalido, Toast.LENGTH_SHORT).show();
            return;
        }

        if (descripcion.isEmpty()) {
            Toast.makeText(this, R.string.gastos_descripcion_obligatoria, Toast.LENGTH_SHORT).show();
            return;
        }

        if (gastoEditar == null) {
            Gasto gasto = new Gasto();
            gasto.setCategoria(categoria);
            gasto.setDescripcion(descripcion);
            gasto.setMonto(Dinero.redondear(monto));
            gasto.setFechaEpoch(fechaSeleccionada);
            gasto.setNotas(notas);

            MutableLiveData<Boolean> resultado = new MutableLiveData<>();
            resultado.observe(this, ok -> {
                Toast.makeText(this, R.string.gastos_guardado, Toast.LENGTH_SHORT).show();
                finish();
            });
            viewModel.insertGasto(gasto, resultado);
        } else {
            gastoEditar.setCategoria(categoria);
            gastoEditar.setDescripcion(descripcion);
            gastoEditar.setMonto(Dinero.redondear(monto));
            gastoEditar.setFechaEpoch(fechaSeleccionada);
            gastoEditar.setNotas(notas);

            MutableLiveData<Boolean> resultado = new MutableLiveData<>();
            resultado.observe(this, ok -> {
                Toast.makeText(this, R.string.gastos_editado, Toast.LENGTH_SHORT).show();
                finish();
            });
            viewModel.updateGasto(gastoEditar, resultado);
        }
    }

    private Intent intentResultado() {
        return new Intent(this, ListaGastosActivity.class);
    }
}
