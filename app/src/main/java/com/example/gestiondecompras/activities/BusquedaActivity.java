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
import com.example.gestiondecompras.adapters.GastosAdapter;
import com.example.gestiondecompras.adapters.PedidosAdapter;
import com.example.gestiondecompras.databinding.ActivityBusquedaBinding;
import com.example.gestiondecompras.models.Cliente;
import com.example.gestiondecompras.models.Gasto;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.viewmodels.BusquedaViewModel;

import java.util.ArrayList;

/** Búsqueda global: clientes, pedidos y gastos en un solo campo. */
public class BusquedaActivity extends AppCompatActivity {

    private ActivityBusquedaBinding binding;
    private BusquedaViewModel viewModel;
    private ClientesAdapter clientesAdapter;
    private PedidosAdapter pedidosAdapter;
    private GastosAdapter gastosAdapter;

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

        gastosAdapter = new GastosAdapter(new GastosAdapter.OnGastoClickListener() {
            @Override public void onGastoClick(Gasto gasto) { abrirGasto(gasto); }
            @Override public void onGastoLongClick(Gasto gasto) { abrirGasto(gasto); }
        });
        binding.rvGastos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvGastos.setAdapter(gastosAdapter);
    }

    private void mostrar(BusquedaViewModel.Resultados r) {
        boolean buscando = r.texto.length() >= 2;
        clientesAdapter.actualizarLista(new ArrayList<Cliente>(r.clientes));
        pedidosAdapter.actualizarLista(new ArrayList<>(r.pedidos));
        gastosAdapter.actualizarLista(r.gastos);

        mostrarSeccion(binding.tvTituloClientes, binding.rvClientes, !r.clientes.isEmpty());
        mostrarSeccion(binding.tvTituloPedidos, binding.rvPedidos, !r.pedidos.isEmpty());
        mostrarSeccion(binding.tvTituloGastos, binding.rvGastos, !r.gastos.isEmpty());

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

    private void abrirGasto(Gasto gasto) {
        Intent intent = new Intent(this, NuevoGastoActivity.class);
        intent.putExtra(NuevoGastoActivity.EXTRA_GASTO_ID, gasto.getId());
        startActivity(intent);
    }
}
