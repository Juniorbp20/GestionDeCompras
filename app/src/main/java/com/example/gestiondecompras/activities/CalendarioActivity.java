package com.example.gestiondecompras.activities;

import com.example.gestiondecompras.utils.DetallePedido;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.adapters.PedidosAdapter;
import com.example.gestiondecompras.databinding.ActivityCalendarioBinding;
import com.example.gestiondecompras.viewmodels.CalendarioViewModel;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarioActivity extends AppCompatActivity implements PedidosAdapter.OnPedidoClickListener {

    private ActivityCalendarioBinding binding;
    private CalendarioViewModel viewModel;
    private PedidosAdapter adapter;
    private final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalendarioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CalendarioViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupCalendar();
        setupRecyclerView();
        setupSearch();
        observeViewModel();

        binding.tvFechaSeleccionada.setText(getString(R.string.calendario_hoy, df.format(new Date())));
        viewModel.loadPedidos(new Date().getTime(), "");
    }

    private void setupSearch() {
        binding.etBuscarCliente.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { reloadDay(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void reloadDay() {
        String busqueda = binding.etBuscarCliente.getText() != null
                ? binding.etBuscarCliente.getText().toString().trim() : "";
        viewModel.loadPedidos(binding.calendarView.getDate(), busqueda);
    }

    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((v, y, m, d) -> {
            Calendar c = Calendar.getInstance();
            c.set(y, m, d, 0, 0, 0);
            Date sel = c.getTime();
            binding.tvFechaSeleccionada.setText(df.format(sel));
            viewModel.loadPedidos(sel.getTime(), binding.etBuscarCliente.getText() != null
                    ? binding.etBuscarCliente.getText().toString().trim() : "");
        });
    }

    private void setupRecyclerView() {
        binding.rvPedidosDelDia.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PedidosAdapter(this);
        binding.rvPedidosDelDia.setAdapter(adapter);
    }

    @SuppressLint("SetTextI18n")
    private void observeViewModel() {
        viewModel.getPedidos().observe(this, pedidos -> {
            if (pedidos != null) {
                adapter.actualizarLista(pedidos);
                binding.tvCantidadPedidos.setText("(" + pedidos.size() + " pedidos)");
            } else {
                adapter.actualizarLista(Collections.emptyList());
                binding.tvCantidadPedidos.setText("(0 pedidos)");
            }
        });
    }

    @Override
    public void onPedidoClick(com.example.gestiondecompras.models.Pedido pedido) {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_detalle_pedido, null);
        // En el calendario el detalle es de consulta: sin registrar ni ver abonos
        view.findViewById(R.id.btn_registrar_abono).setVisibility(android.view.View.GONE);
        view.findViewById(R.id.btn_ver_abonos).setVisibility(android.view.View.GONE);
        DetallePedido.rellenar(view, pedido);
        view.findViewById(R.id.btn_enviar_resumen).setOnClickListener(v ->
                DetallePedido.compartirResumen(this, pedido));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(R.string.accion_cerrar, null)
                .show();
    }

    @Override
    public void onPedidoLongClick(com.example.gestiondecompras.models.Pedido pedido) {
        // En el calendario solo se consulta: el botón ⋮ y la pulsación larga abren el detalle
        onPedidoClick(pedido);
    }
}

