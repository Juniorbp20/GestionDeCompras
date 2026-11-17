package com.example.gestiondecompras.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.adapters.ClientesAdapter;
import com.example.gestiondecompras.adapters.MovimientosAdapter;
import com.example.gestiondecompras.adapters.PedidosAdapter;
import com.example.gestiondecompras.databinding.ActivityBusquedaBinding;
import com.example.gestiondecompras.models.Cliente;
import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.viewmodels.BusquedaViewModel;

import java.util.ArrayList;

/** Búsqueda global: clientes, pedidos y movimientos de Finanzas en un solo campo. */
public class BusquedaActivity extends AppCompatActivity {

    private ActivityBusquedaBinding binding;
    private BusquedaViewModel viewModel;
    private ClientesAdapter clientesAdapter;
    private PedidosAdapter pedidosAdapter;
    private MovimientosAdapter movimientosAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBusquedaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(BusquedaViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        configurarListas();

        binding.etBusqueda.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.buscar(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        viewModel.getResultados().observe(this, this::mostrar);

        binding.etBusqueda.requestFocus();
        binding.etBusqueda.post(() -> {
            InputMethodManager imm = getSystemService(InputMethodManager.class);
            if (imm != null) {
                imm.showSoftInput(binding.etBusqueda, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresca los resultados al volver de editar un elemento
        if (binding.etBusqueda.getText() != null) {
            viewModel.buscar(binding.etBusqueda.getText().toString());
        }
    }

    private void configurarListas() {
        clientesAdapter = new ClientesAdapter(new ArrayList<>(), new ClientesAdapter.OnClienteClickListener() {
            @Override public void onClienteClick(Cliente cliente) { verPedidosDe(cliente); }
            @Override public void onClienteLongClick(Cliente cliente) { verPedidosDe(cliente); }
        });
        binding.rvClientes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvClientes.setAdapter(clientesAdapter);

        pedidosAdapter = new PedidosAdapter(new PedidosAdapter.OnPedidoClickListener() {
            @Override public void onPedidoClick(Pedido pedido) { abrirPedido(pedido); }
            @Override public void onPedidoLongClick(Pedido pedido) { abrirPedido(pedido); }
        });
        binding.rvPedidos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPedidos.setAdapter(pedidosAdapter);

        movimientosAdapter = new MovimientosAdapter(new MovimientosAdapter.Listener() {
            @Override public void onMovimientoClick(Movimiento m) { abrirMovimiento(m); }
            @Override public void onMovimientoAcciones(Movimiento m) { abrirMovimiento(m); }
        });
        binding.rvGastos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvGastos.setAdapter(movimientosAdapter);
    }

    private void mostrar(BusquedaViewModel.Resultados r) {
        boolean buscando = r.texto.length() >= 2;
        clientesAdapter.actualizarLista(new ArrayList<Cliente>(r.clientes));
        pedidosAdapter.actualizarLista(new ArrayList<>(r.pedidos));
        movimientosAdapter.setCatalogos(r.categorias, r.cuentas);
        movimientosAdapter.setMovimientos(r.movimientos);

        mostrarSeccion(binding.tvTituloClientes, binding.rvClientes, !r.clientes.isEmpty());
        mostrarSeccion(binding.tvTituloPedidos, binding.rvPedidos, !r.pedidos.isEmpty());
        mostrarSeccion(binding.tvTituloGastos, binding.rvGastos, !r.movimientos.isEmpty());

        if (!buscando) {
            binding.tvBusquedaAyuda.setText(R.string.busqueda_ayuda);
            binding.tvBusquedaAyuda.setVisibility(View.VISIBLE);
        } else if (r.vacio()) {
            binding.tvBusquedaAyuda.setText(getString(R.string.busqueda_sin_resultados, r.texto));
            binding.tvBusquedaAyuda.setVisibility(View.VISIBLE);
        } else {
            binding.tvBusquedaAyuda.setVisibility(View.GONE);
        }
    }

    private void mostrarSeccion(View titulo, View lista, boolean visible) {
        titulo.setVisibility(visible ? View.VISIBLE : View.GONE);
        lista.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void verPedidosDe(Cliente cliente) {
        Intent intent = new Intent(this, ListaPedidosActivity.class);
        intent.putExtra("cliente_id", cliente.id);
        intent.putExtra("cliente_nombre", cliente.nombre);
        startActivity(intent);
    }

    private void abrirPedido(Pedido pedido) {
        Intent intent = new Intent(this, NuevoPedidoActivity.class);
        intent.putExtra("pedido_id", pedido.getId());
        startActivity(intent);
    }

    private void abrirMovimiento(Movimiento m) {
        Intent intent = new Intent(this, NuevoMovimientoActivity.class);
        intent.putExtra(NuevoMovimientoActivity.EXTRA_ID, m.id);
        startActivity(intent);
    }
}
