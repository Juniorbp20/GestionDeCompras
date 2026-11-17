package com.example.gestiondecompras.activities;

import com.example.gestiondecompras.utils.Dinero;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.utils.BorradoConDeshacer;
import com.example.gestiondecompras.utils.HojaAcciones;
import com.example.gestiondecompras.utils.NavegacionInferior;
import com.example.gestiondecompras.adapters.AbonosAdapter;
import com.example.gestiondecompras.adapters.PedidosAdapter;
import com.example.gestiondecompras.databinding.ActivityListaPedidosBinding;
import com.example.gestiondecompras.models.Abono;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.viewmodels.ListaPedidosViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class ListaPedidosActivity extends AppCompatActivity implements PedidosAdapter.OnPedidoClickListener {

    private ActivityListaPedidosBinding binding;
    private ListaPedidosViewModel viewModel;
    private PedidosAdapter adapter;
    private final BorradoConDeshacer borrado = new BorradoConDeshacer();
    private Integer filtroClienteId = null;
    private String filtroClienteNombre = null;
    private final String[] estados = {"Todos", "Por Cobrar", "Pendientes", "Entregados", "Pagados", "Atrasados"};
    private boolean filtroAtrasados = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListaPedidosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ListaPedidosViewModel.class);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (getIntent() != null) {
            if (getIntent().hasExtra("cliente_id")) {
                // Clientes envía el id como long; se acepta también int por compatibilidad
                long idCliente = getIntent().getLongExtra("cliente_id", -1L);
                if (idCliente <= 0) {
                    idCliente = getIntent().getIntExtra("cliente_id", -1);
                }
                filtroClienteId = idCliente > 0 ? (int) idCliente : null;
            }
            if (getIntent().hasExtra("cliente_nombre")) {
                filtroClienteNombre = getIntent().getStringExtra("cliente_nombre");
            }
            if (getIntent().hasExtra("filtro_atrasados")) {
                filtroAtrasados = getIntent().getBooleanExtra("filtro_atrasados", false);
            }
        }

        configurarNavegacion();
        setupRecyclerView();
        setupFilters();
        setupFab();
        observeViewModel();

        actualizarTitulo();
        // cargarPedidos(); // Moved to after filter setup to avoid double load or wrong filter
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarPedidos();
    }

    /**
     * Como pestaña principal muestra la barra inferior y oculta la flecha atrás.
     * Abierta desde un cliente (pedidos de ese cliente) es una vista de detalle:
     * sin barra inferior y con flecha para volver a Clientes.
     */
    private void configurarNavegacion() {
        if (filtroClienteId != null) {
            binding.bottomNav.setVisibility(View.GONE);
            return;
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        binding.toolbar.setNavigationIcon(null);
        NavegacionInferior.configurar(this, binding.bottomNav, R.id.tab_pedidos,
                () -> binding.rvPedidos.smoothScrollToPosition(0));
    }

    private void setupRecyclerView() {
        binding.rvPedidos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PedidosAdapter(this);
        binding.rvPedidos.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                Pedido pedido = adapter.getPedidoAt(position);
                if (direction == ItemTouchHelper.LEFT && pedido != null) {
                    eliminarConDeshacer(pedido);
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvPedidos);
    }

    private void setupFilters() {
        binding.spinnerFiltroEstado.setAdapter(
                new ArrayAdapter<>(this, R.layout.item_dropdown, estados)
        );

        int selection = 0; // Default: Todos
        if (filtroAtrasados) {
            selection = 5; // Atrasados
        } else if (getIntent() != null && getIntent().hasExtra("filtro_estado")) {
            String extra = getIntent().getStringExtra("filtro_estado");
            if (extra != null) {
                switch (extra.toLowerCase()) {
                    case "por_cobrar": selection = 1; break;
                    case "pendiente": selection = 2; break;
                    case "entregado": selection = 3; break;
                    case "pagado": selection = 4; break;
                    // default 0
                }
            }
        } else {
            selection = 0;
        }

        binding.spinnerFiltroEstado.setText(estados[selection], false);

        binding.spinnerFiltroEstado.setOnItemClickListener((parent, view, position, id) -> cargarPedidos());

        binding.etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { cargarPedidos(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFab() {
        binding.fabNuevoPedido.setOnClickListener(v -> {
            startActivity(new Intent(this, NuevoPedidoActivity.class));
        });
        binding.btnVacioAccion.setOnClickListener(v ->
                startActivity(new Intent(this, NuevoPedidoActivity.class)));
    }

    private void observeViewModel() {
        viewModel.getPedidos().observe(this, pedidos -> {
            if (pedidos != null) {
                adapter.actualizarLista(pedidos);
                binding.emptyState.setVisibility(pedidos.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void cargarPedidos() {
        String filtroEstado = binding.spinnerFiltroEstado.getText() != null
                ? binding.spinnerFiltroEstado.getText().toString() : "";
        String busqueda = binding.etBuscar.getText().toString();
        viewModel.loadPedidos(filtroEstado, busqueda, filtroClienteId);
    }

    @Override
    public void onPedidoClick(Pedido pedido) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_detalle_pedido, null);
        
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
        com.google.android.material.button.MaterialButton btnRegistrarAbono = view.findViewById(R.id.btn_registrar_abono);
        com.google.android.material.button.MaterialButton btnVerAbonos = view.findViewById(R.id.btn_ver_abonos);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        
        tvProducto.setText(pedido.getClienteNombre());
        tvTienda.setText(pedido.getTienda());
        
        tvFechaCompra.setText(pedido.getFechaRegistro() != null ? sdf.format(pedido.getFechaRegistro()) : "N/A");
        tvFechaEntrega.setText(pedido.getFechaEntrega() != null ? sdf.format(pedido.getFechaEntrega()) : "N/A");
        
        tvCosto.setText(String.format("RD$ %,.2f", pedido.getMontoCompra()));
        tvVenta.setText(String.format("RD$ %,.2f", pedido.getTotalGeneral()));
        tvGanancia.setText(String.format("RD$ %,.2f", pedido.getGanancia()));
        tvAbonado.setText(moneda(pedido.getTotalAbonado()));
        tvSaldo.setText(moneda(pedido.getSaldo()));

        aplicarEstado(tvEstado, pedido.getEstado());

        btnEnviar.setOnClickListener(v -> enviarResumenCliente(pedido, sdf));

        AlertDialog dialog = builder.setView(view)
               .setPositiveButton("Cerrar", null)
               .setNeutralButton("Editar", (d, which) -> {
                   d.dismiss();
                   Intent i = new Intent(this, NuevoPedidoActivity.class);
                   i.putExtra("pedido_id", pedido.getId());
                   startActivity(i);
               })
               .create();

        btnRegistrarAbono.setOnClickListener(v ->
                mostrarDialogoAbono(pedido, tvAbonado, tvSaldo, tvEstado));
        btnVerAbonos.setOnClickListener(v ->
                mostrarHistorialAbonos(pedido, tvAbonado, tvSaldo, tvEstado));

        dialog.show();
    }

    private void mostrarDialogoAbono(Pedido pedido, android.widget.TextView tvAbonado,
                                     android.widget.TextView tvSaldo, android.widget.TextView tvEstado) {
        if (Pedido.ESTADO_CANCELADO.equalsIgnoreCase(pedido.getEstado())) {
            Snackbar.make(binding.rvPedidos, R.string.abono_pedido_cancelado, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!Dinero.esPositivo(pedido.getSaldo())) {
            Snackbar.make(binding.rvPedidos, R.string.abono_pedido_pagado, Snackbar.LENGTH_SHORT).show();
            return;
        }

        View view = getLayoutInflater().inflate(R.layout.dialog_registrar_abono, null);
        android.widget.TextView tvSaldoActual = view.findViewById(R.id.tv_saldo_abono);
        android.widget.EditText etMonto = view.findViewById(R.id.et_monto_abono);
        android.widget.EditText etNotas = view.findViewById(R.id.et_notas_abono);
        android.widget.AutoCompleteTextView spinnerMetodo = view.findViewById(R.id.spinner_metodo_abono);

        tvSaldoActual.setText(getString(R.string.abono_saldo_actual_format, moneda(pedido.getSaldo())));
        spinnerMetodo.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, Abono.METODOS));
        spinnerMetodo.setText(Abono.METODO_EFECTIVO, false);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(R.string.abono_guardar, null)
                .setNegativeButton(R.string.accion_cancelar, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String montoTexto = etMonto.getText() != null ? etMonto.getText().toString().trim() : "";
            double monto;
            try {
                monto = Dinero.redondear(Double.parseDouble(montoTexto));
            } catch (NumberFormatException ex) {
                etMonto.setError(getString(R.string.abono_monto_invalido));
                return;
            }
            if (!Dinero.esPositivo(monto)) {
                etMonto.setError(getString(R.string.abono_monto_invalido));
                return;
            }
            if (Dinero.mayorQue(monto, pedido.getSaldo())) {
                etMonto.setError(getString(R.string.abono_excede_saldo));
                return;
            }

            String metodo = spinnerMetodo.getText() != null
                    ? spinnerMetodo.getText().toString() : Abono.METODO_EFECTIVO;
            String notas = etNotas.getText() != null ? etNotas.getText().toString().trim() : "";

            MutableLiveData<Boolean> resultado = new MutableLiveData<>();
            resultado.observe(this, ok -> {
                if (Boolean.TRUE.equals(ok)) {
                    if (tvAbonado != null) tvAbonado.setText(moneda(pedido.getTotalAbonado()));
                    if (tvSaldo != null) tvSaldo.setText(moneda(pedido.getSaldo()));
                    if (tvEstado != null) aplicarEstado(tvEstado, pedido.getEstado());
                    Snackbar.make(binding.rvPedidos, R.string.abono_guardado, Snackbar.LENGTH_SHORT).show();
                    cargarPedidos();
                    dialog.dismiss();
                }
            });
            viewModel.registrarAbono(pedido, monto, metodo, notas, resultado);
        }));

        dialog.show();
    }

    private void mostrarHistorialAbonos(Pedido pedido, android.widget.TextView tvAbonado,
                                        android.widget.TextView tvSaldo, android.widget.TextView tvEstado) {
        View view = getLayoutInflater().inflate(R.layout.dialog_historial_abonos, null);
        RecyclerView rvAbonos = view.findViewById(R.id.rv_abonos);
        android.widget.TextView tvTotal = view.findViewById(R.id.tv_total_abonado);
        android.widget.TextView tvVacio = view.findViewById(R.id.tv_abonos_vacio);

        tvTotal.setText(getString(R.string.abono_total_format, moneda(pedido.getTotalAbonado())));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(R.string.accion_cerrar, null)
                .create();

        AbonosAdapter adapter = new AbonosAdapter(abono ->
                confirmarEliminarAbono(pedido, abono, dialog, tvAbonado, tvSaldo, tvEstado));
        rvAbonos.setLayoutManager(new LinearLayoutManager(this));
        rvAbonos.setAdapter(adapter);

        viewModel.getAbonos(pedido.getId()).observe(this, abonos -> {
            adapter.actualizarLista(abonos);
            boolean vacio = abonos == null || abonos.isEmpty();
            tvVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
            rvAbonos.setVisibility(vacio ? View.GONE : View.VISIBLE);
        });

        dialog.show();
    }

    private void confirmarEliminarAbono(Pedido pedido, Abono abono, AlertDialog historialDialog,
                                        android.widget.TextView tvAbonado, android.widget.TextView tvSaldo,
                                        android.widget.TextView tvEstado) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.abono_eliminar_title)
                .setMessage(R.string.abono_eliminar_message)
                .setPositiveButton(R.string.accion_eliminar, (d, which) -> {
                    MutableLiveData<Boolean> resultado = new MutableLiveData<>();
                    resultado.observe(this, ok -> {
                        if (Boolean.TRUE.equals(ok)) {
                            tvAbonado.setText(moneda(pedido.getTotalAbonado()));
                            tvSaldo.setText(moneda(pedido.getSaldo()));
                            aplicarEstado(tvEstado, pedido.getEstado());
                            Snackbar.make(binding.rvPedidos, R.string.abono_eliminado, Snackbar.LENGTH_SHORT).show();
                            cargarPedidos();
                            historialDialog.dismiss();
                        }
                    });
                    viewModel.eliminarAbono(pedido, abono, resultado);
                })
                .setNegativeButton(R.string.accion_cancelar, null)
                .show();
    }

    private String moneda(double valor) {
        return String.format(Locale.getDefault(), "RD$ %,.2f", valor);
    }

    private void aplicarEstado(android.widget.TextView tvEstado, String estado) {
        int colorRes;
        if (Pedido.ESTADO_PAGADO.equalsIgnoreCase(estado)) {
            colorRes = R.color.status_paid;
        } else if (Pedido.ESTADO_ENTREGADO.equalsIgnoreCase(estado)) {
            colorRes = R.color.status_delivered;
        } else if (Pedido.ESTADO_CANCELADO.equalsIgnoreCase(estado)) {
            colorRes = R.color.status_cancelled;
        } else {
            colorRes = R.color.status_pending;
        }
        tvEstado.setText(estado);
        tvEstado.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));
        tvEstado.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private void enviarResumenCliente(Pedido pedido, java.text.SimpleDateFormat sdf) {
        String fechaRegistro = pedido.getFechaRegistro() != null ? sdf.format(pedido.getFechaRegistro()) : "N/A";
        String fechaEntrega = pedido.getFechaEntrega() != null ? sdf.format(pedido.getFechaEntrega()) : "N/A";

        String mensaje = "*Resumen del Pedido*\n\n"
                + getString(R.string.pedido_detalle_cliente) + ": " + pedido.getClienteNombre() + "\n"
                + getString(R.string.pedido_detalle_tienda) + ": " + pedido.getTienda() + "\n"
                + getString(R.string.pedido_detalle_fecha_compra) + ": " + fechaRegistro + "\n"
                + getString(R.string.pedido_detalle_fecha_entrega) + ": " + fechaEntrega + "\n"
                + getString(R.string.pedido_detalle_costo) + ": RD$ " + String.format(java.util.Locale.getDefault(), "%,.2f", pedido.getMontoCompra()) + "\n"
                + getString(R.string.pedido_detalle_venta) + ": RD$ " + String.format(java.util.Locale.getDefault(), "%,.2f", pedido.getTotalGeneral()) + "\n"
                + getString(R.string.pedido_detalle_ganancia) + ": RD$ " + String.format(java.util.Locale.getDefault(), "%,.2f", pedido.getGanancia()) + "\n"
                + "Estado: " + pedido.getEstado();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.pedido_share_subject, pedido.getClienteNombre()));
        intent.putExtra(Intent.EXTRA_TEXT, mensaje);
        startActivity(Intent.createChooser(intent, getString(R.string.pedido_enviar_resumen)));
    }

    @Override
    public void onPedidoLongClick(Pedido pedido) {
        if (pedido != null) {
            mostrarAccionesPedido(pedido);
        }
    }

    /** Hoja inferior con todas las acciones del pedido, en el orden común de la app. */
    private void mostrarAccionesPedido(Pedido pedido) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        String subtitulo = (pedido.getTienda() != null && !pedido.getTienda().isEmpty()
                ? pedido.getTienda() + " · " : "") + moneda(pedido.getTotalGeneral());

        HojaAcciones hoja = HojaAcciones.de(this, pedido.getClienteNombre())
                .subtitulo(subtitulo)
                .accion(R.drawable.ic_visibility, R.string.accion_ver_detalle, () -> onPedidoClick(pedido))
                .accion(R.drawable.ic_edit, R.string.accion_editar, () -> {
                    Intent i = new Intent(this, NuevoPedidoActivity.class);
                    i.putExtra("pedido_id", pedido.getId());
                    startActivity(i);
                })
                .accion(R.drawable.ic_swap, R.string.accion_cambiar_estado, () -> mostrarDialogoEstado(pedido));
        if (!Pedido.ESTADO_CANCELADO.equalsIgnoreCase(pedido.getEstado()) && Dinero.esPositivo(pedido.getSaldo())) {
            hoja.accion(R.drawable.ic_payments, R.string.abono_registrar,
                    () -> mostrarDialogoAbono(pedido, null, null, null));
        }
        hoja.accion(R.drawable.ic_share, R.string.pedido_enviar_resumen, () -> enviarResumenCliente(pedido, sdf))
                .peligrosa(R.drawable.ic_delete, R.string.accion_eliminar,
                        () -> eliminarConDeshacer(pedido))
                .mostrar();
    }

    private void actualizarTitulo() {
        if (filtroClienteNombre != null) {
            binding.toolbar.setTitle(getString(R.string.pedidos_title_cliente, filtroClienteNombre));
        } else {
            binding.toolbar.setTitle(R.string.pedidos_title);
        }
    }

    /** Oculta el pedido y lo borra al cerrarse el aviso, salvo que se pulse "Deshacer". */
    private void eliminarConDeshacer(Pedido pedido) {
        borrado.eliminar(binding.getRoot(), binding.bottomNav,
                getString(R.string.pedido_eliminado_deshacer, pedido.getClienteNombre()),
                () -> {
                    adapter.quitar(pedido);
                    binding.emptyState.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                },
                this::cargarPedidos,
                () -> viewModel.deletePedido(pedido));
    }

    @Override
    protected void onStop() {
        borrado.confirmarPendiente();
        super.onStop();
    }

    private void mostrarDialogoEstado(Pedido pedido) {
        final String[] valores = {
                Pedido.ESTADO_PENDIENTE,
                Pedido.ESTADO_ENTREGADO,
                Pedido.ESTADO_PAGADO,
                Pedido.ESTADO_CANCELADO
        };
        final String[] labels = getResources().getStringArray(R.array.pedido_estados_labels);
        int seleccionado = 0;
        for (int i = 0; i < valores.length; i++) {
            if (valores[i].equalsIgnoreCase(pedido.estado)) {
                seleccionado = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_change_estado_title)
                .setSingleChoiceItems(labels, seleccionado, (dialog, which) -> {
                    String nuevoEstado = valores[which];
                    if (!Pedido.ESTADO_PAGADO.equals(nuevoEstado)
                            && !Pedido.ESTADO_CANCELADO.equals(nuevoEstado)
                            && !Dinero.esPositivo(pedido.getSaldo())) {
                        Snackbar.make(binding.rvPedidos, R.string.abono_pedido_pagado,
                                Snackbar.LENGTH_SHORT).show();
                        dialog.dismiss();
                        return;
                    }
                    if (Pedido.ESTADO_PAGADO.equals(nuevoEstado) && Dinero.esPositivo(pedido.getSaldo())) {
                        MutableLiveData<Boolean> resultado = new MutableLiveData<>();
                        resultado.observe(this, ok -> {
                            if (Boolean.TRUE.equals(ok)) {
                                Snackbar.make(binding.rvPedidos,
                                        getString(R.string.pedidos_estado_actualizado, labels[which]),
                                        Snackbar.LENGTH_SHORT).show();
                                cargarPedidos();
                            }
                        });
                        viewModel.registrarAbono(pedido, pedido.getSaldo(),
                                Abono.METODO_EFECTIVO, "Pago completo", resultado);
                    } else {
                        pedido.estado = nuevoEstado;
                        viewModel.updatePedido(pedido);
                        Snackbar.make(binding.rvPedidos, getString(R.string.pedidos_estado_actualizado, labels[which]), Snackbar.LENGTH_SHORT).show();
                        adapter.notifyDataSetChanged();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.accion_cancelar, null)
                .show();
    }
}

