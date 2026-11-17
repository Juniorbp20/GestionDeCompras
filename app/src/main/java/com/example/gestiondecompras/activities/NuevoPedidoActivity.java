package com.example.gestiondecompras.activities;

import com.example.gestiondecompras.utils.Dinero;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.databinding.ActivityNuevoPedidoBinding;
import com.example.gestiondecompras.models.Abono;
import com.example.gestiondecompras.models.Cliente;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.models.Tarjeta;
import com.example.gestiondecompras.models.Tienda;
import com.example.gestiondecompras.viewmodels.NuevoPedidoViewModel;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class NuevoPedidoActivity extends AppCompatActivity {

    private ActivityNuevoPedidoBinding binding;
    private NuevoPedidoViewModel viewModel;
    private final Calendar fechaRegistro = Calendar.getInstance();
    private final Calendar fechaEntrega = Calendar.getInstance();
    private long pedidoIdEdicion = 0;
    private double originalMonto = 0; // Guardamos el monto original para ediciones
    private double totalAbonadoOriginal = 0;
    private String estadoOriginal = Pedido.ESTADO_PENDIENTE;
    private Cliente selectedCliente;
    private Tienda selectedTienda;
    private Tarjeta selectedTarjeta;
    
    private final TextWatcher totalWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { updateTotalGeneral(); }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNuevoPedidoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(NuevoPedidoViewModel.class);

        setupToolbar();
        setupSpinners();
        setupDatePickers();
        setupSaveButton();
        setupInputWatchers();
        updateTotalGeneral();
        
        if (getIntent().hasExtra("pedido_id")) {
             pedidoIdEdicion = getIntent().getLongExtra("pedido_id", 0);
             if (pedidoIdEdicion > 0) {
                 loadPedidoData(pedidoIdEdicion);
             }
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        viewModel.getClientes().observe(this, clientes -> {
            if (clientes != null) {
                ArrayAdapter<Cliente> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown, clientes);
                binding.spinnerCliente.setAdapter(adapter);
            }
        });

        viewModel.getTiendas().observe(this, tiendas -> {
            if (tiendas != null) {
                ArrayAdapter<Tienda> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown, tiendas);
                binding.spinnerTienda.setAdapter(adapter);
            }
        });

        viewModel.getTarjetas().observe(this, tarjetas -> {
            if (tarjetas != null) {
                ArrayAdapter<Tarjeta> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown, tarjetas);
                binding.spinnerTarjeta.setAdapter(adapter);
            }
        });

        binding.spinnerCliente.setOnItemClickListener((parent, view, position, id) ->
                selectedCliente = (Cliente) parent.getItemAtPosition(position));
        binding.spinnerTienda.setOnItemClickListener((parent, view, position, id) ->
                selectedTienda = (Tienda) parent.getItemAtPosition(position));
        binding.spinnerTarjeta.setOnItemClickListener((parent, view, position, id) ->
                selectedTarjeta = (Tarjeta) parent.getItemAtPosition(position));
    }

    private void setupDatePickers() {
        binding.btnFechaRegistro.setOnClickListener(v -> showDatePickerDialog(fechaRegistro, binding.etFechaRegistro));
        binding.btnFechaRegistro.setEndIconOnClickListener(v -> showDatePickerDialog(fechaRegistro, binding.etFechaRegistro));
        binding.etFechaRegistro.setOnClickListener(v -> showDatePickerDialog(fechaRegistro, binding.etFechaRegistro));
        binding.btnFechaEntrega.setOnClickListener(v -> showDatePickerDialog(fechaEntrega, binding.etFechaEntrega));
        binding.btnFechaEntrega.setEndIconOnClickListener(v -> showDatePickerDialog(fechaEntrega, binding.etFechaEntrega));
        binding.etFechaEntrega.setOnClickListener(v -> showDatePickerDialog(fechaEntrega, binding.etFechaEntrega));
        binding.btnNuevaTarjeta.setOnClickListener(v -> startActivity(new Intent(this, NuevaTarjetaActivity.class)));
    }

    private void showDatePickerDialog(Calendar calendar, androidx.appcompat.widget.AppCompatEditText field) {
        @SuppressLint("DefaultLocale") DatePickerDialog dialog = new DatePickerDialog(this,
                (datePicker, year, month, day) -> {
                    calendar.set(year, month, day);
                    field.setText(String.format("%d/%d/%d", day, month + 1, year));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void setupSaveButton() {
        binding.btnGuardar.setOnClickListener(v -> savePedido());
    }

    private void setupInputWatchers() {
        if (!binding.rbGananciaFija.isChecked() && !binding.rbGananciaPorcentaje.isChecked()) {
            binding.rbGananciaFija.setChecked(true);
        }
        binding.etMontoCompra.addTextChangedListener(totalWatcher);
        binding.etGanancia.addTextChangedListener(totalWatcher);
        binding.rgGanancia.addOnButtonCheckedListener((group, checkedId, isChecked) -> updateTotalGeneral());
        binding.switchPagadoCompleto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.etAbonoInicial.setEnabled(!isChecked);
            if (isChecked) {
                updateTotalGeneral();
            } else {
                binding.etAbonoInicial.setText("");
            }
        });
    }

    private void updateTotalGeneral() {
        String montoTexto = binding.etMontoCompra.getText() != null
                ? binding.etMontoCompra.getText().toString().trim() : "";
        String gananciaTexto = binding.etGanancia.getText() != null
                ? binding.etGanancia.getText().toString().trim() : "";

        if (montoTexto.isEmpty() || gananciaTexto.isEmpty()) {
            binding.tvTotalGeneral.setText(getString(R.string.total_general_amount, 0.0));
            return;
        }

        try {
            double monto = Double.parseDouble(montoTexto);
            double ganancia = Double.parseDouble(gananciaTexto);
            boolean isGananciaFija = binding.rbGananciaFija.isChecked();
            double totalGanancia = isGananciaFija ? ganancia : monto * (ganancia / 100);
            double totalGeneral = monto + totalGanancia;
            binding.tvTotalGeneral.setText(getString(R.string.total_general_amount, totalGeneral));
            if (binding.switchPagadoCompleto.isChecked()) {
                binding.etAbonoInicial.setText(String.format(Locale.getDefault(), "%.2f", totalGeneral));
            }
        } catch (NumberFormatException ex) {
            binding.tvTotalGeneral.setText(getString(R.string.total_general_amount, 0.0));
        }
    }

    @SuppressLint({"SetTextI18n", "SimpleDateFormat"})
    private void loadPedidoData(long id) {
        viewModel.getPedido(id).observe(this, pedido -> {
            if (pedido != null) {
                Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.pedido_editar_titulo, pedido.getId()));
                binding.btnGuardar.setText(R.string.pedido_actualizar);
                
                originalMonto = pedido.getMontoCompra(); // Guardar para validar cambios
                totalAbonadoOriginal = pedido.getTotalAbonado();
                estadoOriginal = pedido.getEstado();
                binding.etMontoCompra.setText(String.valueOf(pedido.getMontoCompra()));

                binding.rbGananciaFija.setChecked(true);
                binding.etGanancia.setText(String.valueOf(pedido.getGanancia()));
                binding.etNotas.setText(pedido.getNotas());

                binding.layoutAbonoInicial.setVisibility(View.GONE);
                binding.tvAbonoInfo.setVisibility(View.VISIBLE);
                binding.tvAbonoInfo.setText(getString(R.string.abono_info_format,
                        String.format(Locale.getDefault(), "RD$ %,.2f", totalAbonadoOriginal),
                        String.format(Locale.getDefault(), "RD$ %,.2f", pedido.getSaldo())));
                
                if (pedido.fechaRegistroEpoch != null) {
                    fechaRegistro.setTimeInMillis(pedido.fechaRegistroEpoch);
                    binding.etFechaRegistro.setText(new java.text.SimpleDateFormat("dd/MM/yyyy").format(fechaRegistro.getTime()));
                }
                 if (pedido.fechaEntregaEpoch != null) {
                    fechaEntrega.setTimeInMillis(pedido.fechaEntregaEpoch);
                    binding.etFechaEntrega.setText(new java.text.SimpleDateFormat("dd/MM/yyyy").format(fechaEntrega.getTime()));
                }
                
                selectDropdownItem(binding.spinnerCliente, pedido.clienteId);
                selectDropdownItem(binding.spinnerTienda, pedido.tiendaId);
                selectDropdownItem(binding.spinnerTarjeta, pedido.tarjetaId);
            }
        });
    }

    private void selectDropdownItem(android.widget.AutoCompleteTextView field, Long id) {
        if (id == null) return;
        android.widget.ListAdapter adapter = field.getAdapter();
        if (adapter == null) return;
        for (int i = 0; i < adapter.getCount(); i++) {
             Object item = adapter.getItem(i);
             if (item instanceof Cliente && ((Cliente)item).id == id) {
                 field.setText(item.toString(), false);
                 selectedCliente = (Cliente) item;
                 return;
             }
             if (item instanceof Tienda && ((Tienda)item).id == id) {
                 field.setText(item.toString(), false);
                 selectedTienda = (Tienda) item;
                 return;
             }
             if (item instanceof Tarjeta && ((Tarjeta)item).id == id) {
                 field.setText(item.toString(), false);
                 selectedTarjeta = (Tarjeta) item;
                 return;
             }
        }
    }

    private void savePedido() {
        Cliente cliente = selectedCliente;
        Tienda tienda = selectedTienda;
        Tarjeta tarjeta = selectedTarjeta;

        if (cliente == null || tienda == null || tarjeta == null) {
            Toast.makeText(this, R.string.pedido_faltan_selecciones, Toast.LENGTH_SHORT).show();
            return;
        }

        String montoTexto = Objects.requireNonNull(binding.etMontoCompra.getText()).toString().trim();
        String gananciaTexto = Objects.requireNonNull(binding.etGanancia.getText()).toString().trim();
        if (montoTexto.isEmpty() || gananciaTexto.isEmpty()) {
            Toast.makeText(this, R.string.pedido_faltan_montos, Toast.LENGTH_SHORT).show();
            return;
        }

        double monto;
        double ganancia;
        try {
            monto = Dinero.redondear(Double.parseDouble(montoTexto));
            ganancia = Double.parseDouble(gananciaTexto);
        } catch (NumberFormatException ex) {
            Toast.makeText(this, R.string.pedido_valores_invalidos, Toast.LENGTH_SHORT).show();
            return;
        }

        // VALIDACIÓN INTELIGENTE DE LÍMITE DE TARJETA
        double diferencia = (pedidoIdEdicion > 0) ? Dinero.restar(monto, originalMonto) : monto;
        if (tarjeta.getLimiteCredito() > 0
                && Dinero.mayorQue(Dinero.sumar(tarjeta.getDeudaActual(), diferencia), tarjeta.getLimiteCredito())) {
            Toast.makeText(this, R.string.pedido_supera_limite, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isGananciaFija = binding.rbGananciaFija.isChecked();
        double totalGanancia = Dinero.redondear(isGananciaFija ? ganancia : monto * (ganancia / 100));
        double totalGeneral = Dinero.sumar(monto, totalGanancia);

        double abonoInicial = 0;
        if (pedidoIdEdicion == 0) {
            if (binding.switchPagadoCompleto.isChecked()) {
                abonoInicial = totalGeneral;
            } else {
                String abonoTexto = Objects.requireNonNull(binding.etAbonoInicial.getText()).toString().trim();
                if (!abonoTexto.isEmpty()) {
                    try {
                        abonoInicial = Dinero.redondear(Double.parseDouble(abonoTexto));
                    } catch (NumberFormatException ex) {
                        Toast.makeText(this, R.string.abono_monto_invalido, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            if (abonoInicial < 0 || Dinero.mayorQue(abonoInicial, totalGeneral)) {
                Toast.makeText(this, R.string.abono_excede_total, Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (Dinero.mayorQue(totalAbonadoOriginal, totalGeneral)) {
            Toast.makeText(this, R.string.abono_total_menor_abonado, Toast.LENGTH_SHORT).show();
            return;
        }

        Pedido pedido = new Pedido();
        if (pedidoIdEdicion > 0) {
            pedido.id = pedidoIdEdicion;
        }
        pedido.clienteId = cliente.id;
        pedido.setClienteNombre(cliente.getNombre());
        pedido.tiendaId = tienda.id;
        pedido.setTienda(tienda.getNombre());
        pedido.tarjetaId = tarjeta.id;
        pedido.setTarjetaAlias(tarjeta.getAlias());
        pedido.montoCompra = monto;
        pedido.ganancia = totalGanancia;
        pedido.totalGeneral = totalGeneral;
        pedido.fechaRegistroEpoch = fechaRegistro.getTimeInMillis();
        pedido.fechaEntregaEpoch = fechaEntrega.getTimeInMillis();
        pedido.notas = Objects.requireNonNull(binding.etNotas.getText()).toString();

        if (pedidoIdEdicion > 0) {
            boolean estabaPagado = Pedido.ESTADO_PAGADO.equalsIgnoreCase(estadoOriginal);
            boolean sigueCompleto = Dinero.mayorOIgual(totalAbonadoOriginal, totalGeneral);
            if (sigueCompleto && !Pedido.ESTADO_CANCELADO.equalsIgnoreCase(estadoOriginal)) {
                pedido.estado = Pedido.ESTADO_PAGADO;
            } else if (estabaPagado) {
                pedido.estado = Pedido.ESTADO_PENDIENTE;
            } else {
                pedido.estado = estadoOriginal;
            }
        } else {
            pedido.estado = Dinero.mayorOIgual(abonoInicial, totalGeneral)
                    ? Pedido.ESTADO_PAGADO
                    : Pedido.ESTADO_PENDIENTE;
        }

        viewModel.savePedido(pedido, tarjeta, abonoInicial, Abono.METODO_EFECTIVO);
        Toast.makeText(this, pedidoIdEdicion > 0 ? R.string.pedido_actualizado : R.string.pedido_guardado, Toast.LENGTH_SHORT).show();
        finish();
    }
}
