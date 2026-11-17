package com.example.gestiondecompras.activities;

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

        binding.tvFechaSeleccionada.setText("Hoy: " + df.format(new Date()));
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
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_detalle_pedido, null);
        
        android.widget.TextView tvProducto = view.findViewById(R.id.tv_detalle_producto);
        android.widget.TextView tvTienda = view.findViewById(R.id.tv_detalle_tienda);
        android.widget.TextView tvFechaCompra = view.findViewById(R.id.tv_detalle_fecha_compra);
        android.widget.TextView tvFechaEntrega = view.findViewById(R.id.tv_detalle_fecha_entrega);
        android.widget.TextView tvCosto = view.findViewById(R.id.tv_detalle_costo);
        android.widget.TextView tvVenta = view.findViewById(R.id.tv_detalle_venta);
        android.widget.TextView tvGanancia = view.findViewById(R.id.tv_detalle_ganancia);
        android.widget.TextView tvAbonado = view.findViewById(R.id.tv_detalle_abonado);
        android.widget.TextView tvSaldo = view.findViewById(R.id.tv_detalle_saldo);
        android.widget.TextView tvEstado = view.findViewById(R.id.tv_detalle_estado);
        com.google.android.material.button.MaterialButton btnEnviar = view.findViewById(R.id.btn_enviar_resumen);
        view.findViewById(R.id.btn_registrar_abono).setVisibility(android.view.View.GONE);
        view.findViewById(R.id.btn_ver_abonos).setVisibility(android.view.View.GONE);
        
        tvProducto.setText(pedido.getClienteNombre());
        tvTienda.setText(pedido.getTienda());
        
        tvFechaCompra.setText(pedido.getFechaRegistro() != null ? df.format(pedido.getFechaRegistro()) : "N/A");
        tvFechaEntrega.setText(pedido.getFechaEntrega() != null ? df.format(pedido.getFechaEntrega()) : "N/A");
        
        tvCosto.setText(String.format(Locale.getDefault(), "RD$ %,.2f", pedido.getMontoCompra()));
        tvVenta.setText(String.format(Locale.getDefault(), "RD$ %,.2f", pedido.getTotalGeneral()));
        tvGanancia.setText(String.format(Locale.getDefault(), "RD$ %,.2f", pedido.getGanancia()));
        tvAbonado.setText(String.format(Locale.getDefault(), "RD$ %,.2f", pedido.getTotalAbonado()));
        tvSaldo.setText(String.format(Locale.getDefault(), "RD$ %,.2f", pedido.getSaldo()));
        
        tvEstado.setText(pedido.getEstado());
        
        int colorRes;
        if (com.example.gestiondecompras.models.Pedido.ESTADO_PAGADO.equalsIgnoreCase(pedido.getEstado())) {
            colorRes = R.color.status_paid;
        } else if (com.example.gestiondecompras.models.Pedido.ESTADO_ENTREGADO.equalsIgnoreCase(pedido.getEstado())) {
            colorRes = R.color.status_delivered;
        } else if (com.example.gestiondecompras.models.Pedido.ESTADO_CANCELADO.equalsIgnoreCase(pedido.getEstado())) {
            colorRes = R.color.status_cancelled;
        } else {
            colorRes = R.color.status_pending;
        }
        
        tvEstado.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            androidx.core.content.ContextCompat.getColor(this, colorRes)
        ));
        tvEstado.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
        
        btnEnviar.setOnClickListener(v -> enviarResumenCliente(pedido));

        builder.setView(view)
               .setPositiveButton("Cerrar", null)
               .show();
    }

    private void enviarResumenCliente(com.example.gestiondecompras.models.Pedido pedido) {
        String fechaRegistro = pedido.getFechaRegistro() != null ? df.format(pedido.getFechaRegistro()) : "N/A";
        String fechaEntrega = pedido.getFechaEntrega() != null ? df.format(pedido.getFechaEntrega()) : "N/A";

        String mensaje = "*Resumen del Pedido*\n\n"
                + getString(R.string.pedido_detalle_cliente) + ": " + pedido.getClienteNombre() + "\n"
                + getString(R.string.pedido_detalle_tienda) + ": " + pedido.getTienda() + "\n"
                + getString(R.string.pedido_detalle_fecha_compra) + ": " + fechaRegistro + "\n"
                + getString(R.string.pedido_detalle_fecha_entrega) + ": " + fechaEntrega + "\n"
                + getString(R.string.pedido_detalle_costo) + ": RD$ " + String.format(Locale.getDefault(), "%,.2f", pedido.getMontoCompra()) + "\n"
                + getString(R.string.pedido_detalle_venta) + ": RD$ " + String.format(Locale.getDefault(), "%,.2f", pedido.getTotalGeneral()) + "\n"
                + getString(R.string.pedido_detalle_ganancia) + ": RD$ " + String.format(Locale.getDefault(), "%,.2f", pedido.getGanancia()) + "\n"
                + "Estado: " + pedido.getEstado();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.pedido_share_subject, pedido.getClienteNombre()));
        intent.putExtra(Intent.EXTRA_TEXT, mensaje);
        startActivity(Intent.createChooser(intent, getString(R.string.pedido_enviar_resumen)));
    }

    @Override
    public void onPedidoLongClick(com.example.gestiondecompras.models.Pedido pedido) {
        // En el calendario solo se consulta: el botón ⋮ y la pulsación larga abren el detalle
        onPedidoClick(pedido);
    }
}

