package com.example.gestiondecompras.activities;

import com.example.gestiondecompras.utils.Dinero;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.databinding.ActivityNuevoPedidoBinding;
import com.example.gestiondecompras.models.Abono;
import com.example.gestiondecompras.models.Cliente;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.models.Tarjeta;
import com.example.gestiondecompras.models.Tienda;
import com.example.gestiondecompras.viewmodels.NuevoPedidoViewModel;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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
    private boolean fechaEntregaElegida = false;

    /** Ids que deben seleccionarse en cuanto llegue (o se recargue) la lista correspondiente. */
    private Long clientePendiente;
    private Long tiendaPendiente;
    private Long tarjetaPendiente;

    /** Opción "+ Añadir…" que se muestra al final de cada desplegable. */
    private static final class OpcionNueva {
        private final String texto;
        OpcionNueva(String texto) { this.texto = texto; }
        @NonNull @Override public String toString() { return texto; }
    }

    private final ActivityResultLauncher<Intent> lanzadorCliente = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                    long id = resultado.getData().getLongExtra(NuevoClienteActivity.EXTRA_ID_GUARDADO, 0);
                    if (id > 0) clientePendiente = id;
                }
                viewModel.recargarListas();
            });

    private final ActivityResultLauncher<Intent> lanzadorTarjeta = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                    long id = resultado.getData().getLongExtra(NuevaTarjetaActivity.EXTRA_ID_GUARDADO, 0);
                    if (id > 0) tarjetaPendiente = id;
                }
                viewModel.recargarListas();
            });

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
        }
        if (pedidoIdEdicion > 0) {
            loadPedidoData(pedidoIdEdicion);
        } else {
            binding.etFechaRegistro.setText(formatoFecha(fechaRegistro));
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        viewModel.getClientes().observe(this, clientes -> {
            if (clientes == null) return;
            asignarLista(binding.spinnerCliente, clientes, getString(R.string.opcion_nuevo_cliente));
            Long id = clientePendiente != null ? clientePendiente : (selectedCliente != null ? selectedCliente.id : null);
            clientePendiente = null;
            selectedCliente = null;
            for (Cliente c : clientes) {
                if (id != null && c.id == id) {
                    selectedCliente = c;
                    binding.tilCliente.setError(null);
                }
            }
            binding.spinnerCliente.setText(selectedCliente != null ? selectedCliente.toString() : "", false);
        });

        viewModel.getTiendas().observe(this, tiendas -> {
            if (tiendas == null) return;
            asignarLista(binding.spinnerTienda, tiendas, getString(R.string.opcion_nueva_tienda));
            Long id = tiendaPendiente != null ? tiendaPendiente : (selectedTienda != null ? selectedTienda.id : null);
            tiendaPendiente = null;
            selectedTienda = null;
            for (Tienda t : tiendas) {
                if (id != null && t.id == id) {
                    selectedTienda = t;
                    binding.tilTienda.setError(null);
                }
            }
            binding.spinnerTienda.setText(selectedTienda != null ? selectedTienda.toString() : "", false);
        });

        viewModel.getTarjetas().observe(this, tarjetas -> {
            if (tarjetas == null) return;
            asignarLista(binding.spinnerTarjeta, tarjetas, getString(R.string.opcion_nueva_tarjeta));
            Long id = tarjetaPendiente != null ? tarjetaPendiente : (selectedTarjeta != null ? selectedTarjeta.id : null);
            tarjetaPendiente = null;
            selectedTarjeta = null;
            for (Tarjeta t : tarjetas) {
                if (id != null && t.id == id) {
                    selectedTarjeta = t;
                    binding.tilTarjeta.setError(null);
                }
            }
            binding.spinnerTarjeta.setText(selectedTarjeta != null ? selectedTarjeta.toString() : "", false);
        });

        binding.spinnerCliente.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item instanceof OpcionNueva) {
                binding.spinnerCliente.setText(selectedCliente != null ? selectedCliente.toString() : "", false);
                abrirNuevoCliente();
            } else {
                selectedCliente = (Cliente) item;
                binding.tilCliente.setError(null);
            }
        });
        binding.spinnerTienda.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item instanceof OpcionNueva) {
                binding.spinnerTienda.setText(selectedTienda != null ? selectedTienda.toString() : "", false);
                mostrarDialogoNuevaTienda();
            } else {
                selectedTienda = (Tienda) item;
                binding.tilTienda.setError(null);
            }
        });
        binding.spinnerTarjeta.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item instanceof OpcionNueva) {
                binding.spinnerTarjeta.setText(selectedTarjeta != null ? selectedTarjeta.toString() : "", false);
                abrirNuevaTarjeta();
            } else {
                selectedTarjeta = (Tarjeta) item;
                binding.tilTarjeta.setError(null);
            }
        });

        binding.btnNuevoCliente.setOnClickListener(v -> abrirNuevoCliente());
        binding.btnNuevaTarjeta.setOnClickListener(v -> abrirNuevaTarjeta());
    }

    /** Pone la lista en el desplegable con la opción "+ Añadir…" al final. */
    private void asignarLista(AutoCompleteTextView campo, List<?> elementos, String textoNuevo) {
        List<Object> items = new ArrayList<>(elementos);
        items.add(new OpcionNueva(textoNuevo));
        campo.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, items));
    }

    private void abrirNuevoCliente() {
        lanzadorCliente.launch(new Intent(this, NuevoClienteActivity.class));
    }

    private void abrirNuevaTarjeta() {
        lanzadorTarjeta.launch(new Intent(this, NuevaTarjetaActivity.class));
    }

    private void mostrarDialogoNuevaTienda() {
        View vista = getLayoutInflater().inflate(R.layout.dialog_nueva_tienda, null);
        TextInputLayout til = vista.findViewById(R.id.til_nombre_tienda);
        EditText campo = vista.findViewById(R.id.et_nombre_tienda);
        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.tienda_nueva_titulo)
                .setView(vista)
                .setNegativeButton(R.string.accion_cancelar, null)
                .setPositiveButton(R.string.guardar, null)
                .create();
        dialogo.setOnShowListener(d -> {
            campo.requestFocus();
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String nombre = campo.getText() != null ? campo.getText().toString().trim() : "";
                if (nombre.isEmpty()) {
                    til.setError(getString(R.string.tienda_nombre));
                    return;
                }
                MutableLiveData<Tienda> resultado = new MutableLiveData<>();
                resultado.observe(this, tienda -> {
                    if (tienda == null) return;
                    selectedTienda = tienda;
                    binding.tilTienda.setError(null);
                    binding.spinnerTienda.setText(tienda.toString(), false);
                    Toast.makeText(this, getString(R.string.tienda_agregada, tienda.getNombre()), Toast.LENGTH_SHORT).show();
                });
                viewModel.crearTienda(nombre, resultado);
                dialogo.dismiss();
            });
        });
        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialogo.show();
    }

    private void setupDatePickers() {
        binding.btnFechaRegistro.setOnClickListener(v -> showDatePickerDialog(fechaRegistro, binding.etFechaRegistro, false));
        binding.btnFechaRegistro.setEndIconOnClickListener(v -> showDatePickerDialog(fechaRegistro, binding.etFechaRegistro, false));
        binding.etFechaRegistro.setOnClickListener(v -> showDatePickerDialog(fechaRegistro, binding.etFechaRegistro, false));
        binding.btnFechaEntrega.setOnClickListener(v -> showDatePickerDialog(fechaEntrega, binding.etFechaEntrega, true));
        binding.btnFechaEntrega.setEndIconOnClickListener(v -> showDatePickerDialog(fechaEntrega, binding.etFechaEntrega, true));
        binding.etFechaEntrega.setOnClickListener(v -> showDatePickerDialog(fechaEntrega, binding.etFechaEntrega, true));
    }

    private static String formatoFecha(Calendar calendario) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendario.getTime());
    }

    private void showDatePickerDialog(Calendar calendar, EditText field, boolean esEntrega) {
        DatePickerDialog dialog = new DatePickerDialog(this,
                (datePicker, year, month, day) -> {
                    calendar.set(year, month, day);
                    field.setText(formatoFecha(calendar));
                    if (esEntrega) fechaEntregaElegida = true;
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
        binding.etAbonoInicial.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { binding.tilAbono.setError(null); }
        });
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

        if (!montoTexto.isEmpty()) binding.tilMonto.setError(null);
        if (!gananciaTexto.isEmpty()) binding.tilGanancia.setError(null);
        if (montoTexto.isEmpty() || gananciaTexto.isEmpty()) {
            binding.tvTotalGeneral.setText(getString(R.string.pedido_total_monto, 0.0));
            return;
        }

        try {
            double monto = Double.parseDouble(montoTexto);
            double ganancia = Double.parseDouble(gananciaTexto);
            boolean isGananciaFija = binding.rbGananciaFija.isChecked();
            double totalGanancia = isGananciaFija ? ganancia : monto * (ganancia / 100);
            double totalGeneral = monto + totalGanancia;
            binding.tvTotalGeneral.setText(getString(R.string.pedido_total_monto, totalGeneral));
            if (binding.switchPagadoCompleto.isChecked()) {
                binding.etAbonoInicial.setText(String.format(Locale.getDefault(), "%.2f", totalGeneral));
            }
        } catch (NumberFormatException ex) {
            binding.tvTotalGeneral.setText(getString(R.string.pedido_total_monto, 0.0));
        }
    }

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
                    binding.etFechaRegistro.setText(formatoFecha(fechaRegistro));
                }
                if (pedido.fechaEntregaEpoch != null) {
                    fechaEntrega.setTimeInMillis(pedido.fechaEntregaEpoch);
                    binding.etFechaEntrega.setText(formatoFecha(fechaEntrega));
                    fechaEntregaElegida = true;
                }

                // Si las listas aún no han llegado, se seleccionan al llegar
                clientePendiente = pedido.clienteId;
                tiendaPendiente = pedido.tiendaId;
                tarjetaPendiente = pedido.tarjetaId;
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
                 clientePendiente = null;
                 return;
             }
             if (item instanceof Tienda && ((Tienda)item).id == id) {
                 field.setText(item.toString(), false);
                 selectedTienda = (Tienda) item;
                 tiendaPendiente = null;
                 return;
             }
             if (item instanceof Tarjeta && ((Tarjeta)item).id == id) {
                 field.setText(item.toString(), false);
                 selectedTarjeta = (Tarjeta) item;
                 tarjetaPendiente = null;
                 return;
             }
        }
    }

    private void savePedido() {
        Cliente cliente = selectedCliente;
        Tienda tienda = selectedTienda;
        Tarjeta tarjeta = selectedTarjeta;

        String montoTexto = Objects.requireNonNull(binding.etMontoCompra.getText()).toString().trim();
        String gananciaTexto = Objects.requireNonNull(binding.etGanancia.getText()).toString().trim();

        boolean valido = true;
        binding.tilCliente.setError(cliente == null ? getString(R.string.pedido_elige_cliente) : null);
        binding.tilTienda.setError(tienda == null ? getString(R.string.pedido_elige_tienda) : null);
        binding.tilTarjeta.setError(tarjeta == null ? getString(R.string.pedido_elige_tarjeta) : null);
        binding.tilMonto.setError(montoTexto.isEmpty() ? getString(R.string.pedido_monto_requerido) : null);
        binding.tilGanancia.setError(gananciaTexto.isEmpty() ? getString(R.string.pedido_ganancia_requerida) : null);
        if (cliente == null || tienda == null || tarjeta == null || montoTexto.isEmpty() || gananciaTexto.isEmpty()) {
            valido = false;
        }
        if (!valido) {
            Toast.makeText(this, R.string.pedido_faltan_selecciones, Toast.LENGTH_SHORT).show();
            return;
        }

        double monto;
        double ganancia;
        try {
            monto = Dinero.redondear(Double.parseDouble(montoTexto));
            ganancia = Double.parseDouble(gananciaTexto);
        } catch (NumberFormatException ex) {
            binding.tilMonto.setError(getString(R.string.pedido_valores_invalidos));
            return;
        }

        // VALIDACIÓN INTELIGENTE DE LÍMITE DE TARJETA
        double diferencia = (pedidoIdEdicion > 0) ? Dinero.restar(monto, originalMonto) : monto;
        if (tarjeta.getLimiteCredito() > 0
                && Dinero.mayorQue(Dinero.sumar(tarjeta.getDeudaActual(), diferencia), tarjeta.getLimiteCredito())) {
            binding.tilTarjeta.setError(getString(R.string.pedido_supera_limite));
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
                        binding.tilAbono.setError(getString(R.string.abono_monto_invalido));
                        return;
                    }
                }
            }
            if (abonoInicial < 0 || Dinero.mayorQue(abonoInicial, totalGeneral)) {
                binding.tilAbono.setError(getString(R.string.abono_excede_total));
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
        // Sin fecha de entrega elegida se usa la de compra
        pedido.fechaEntregaEpoch = fechaEntregaElegida ? fechaEntrega.getTimeInMillis() : fechaRegistro.getTimeInMillis();
        pedido.notas = Objects.requireNonNull(binding.etNotas.getText()).toString().trim();

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
