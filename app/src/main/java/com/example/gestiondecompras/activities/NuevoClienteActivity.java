package com.example.gestiondecompras.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.databinding.ActivityNuevoClienteBinding;
import com.example.gestiondecompras.models.Cliente;
import com.example.gestiondecompras.viewmodels.NuevoClienteViewModel;

/** Alta y edición de clientes. Para editar se pasa {@link #EXTRA_CLIENTE_ID}. */
public class NuevoClienteActivity extends AppCompatActivity {

    public static final String EXTRA_CLIENTE_ID = "extra_cliente_id";
    /** Id del cliente guardado, devuelto en el resultado de la pantalla. */
    public static final String EXTRA_ID_GUARDADO = "extra_cliente_id_guardado";

    private ActivityNuevoClienteBinding binding;
    private NuevoClienteViewModel viewModel;
    /** Cliente cargado en modo edición (conserva campos que el formulario no muestra, como el email). */
    @Nullable
    private Cliente clienteEdicion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNuevoClienteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(NuevoClienteViewModel.class);

        setupToolbar();
        setupSaveButton();
        cargarSiEsEdicion();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSaveButton() {
        binding.btnGuardar.setOnClickListener(v -> saveCliente());
    }

    private void cargarSiEsEdicion() {
        long id = getIntent().getLongExtra(EXTRA_CLIENTE_ID, -1L);
        if (id <= 0) {
            return;
        }
        binding.toolbar.setTitle(R.string.cliente_editar_title);
        viewModel.getClienteEdicion().observe(this, cliente -> {
            if (cliente == null) {
                return;
            }
            clienteEdicion = cliente;
            binding.etNombre.setText(cliente.nombre);
            binding.etApellido.setText(cliente.apellido);
            binding.etTelefono.setText(cliente.telefono);
            binding.etDireccion.setText(cliente.direccion);
            binding.cbActivo.setChecked(cliente.activo);
        });
        viewModel.cargarCliente(id);
    }

    private void saveCliente() {
        String nombre = binding.etNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            binding.etNombre.setError(getString(R.string.cliente_nombre_requerido));
            return;
        }

        Cliente cliente = clienteEdicion != null ? clienteEdicion : new Cliente();
        cliente.nombre = nombre;
        cliente.apellido = binding.etApellido.getText().toString().trim();
        cliente.telefono = binding.etTelefono.getText().toString().trim();
        cliente.direccion = binding.etDireccion.getText().toString().trim();
        cliente.activo = binding.cbActivo.isChecked();

        binding.btnGuardar.setEnabled(false);
        boolean esEdicion = clienteEdicion != null;
        androidx.lifecycle.MutableLiveData<Long> idGuardado = new androidx.lifecycle.MutableLiveData<>();
        idGuardado.observe(this, id -> {
            // Se devuelve el id para que la pantalla anterior (p. ej. Nuevo pedido) lo seleccione solo
            setResult(RESULT_OK, new android.content.Intent().putExtra(EXTRA_ID_GUARDADO, id));
            Toast.makeText(this, esEdicion ? R.string.cliente_actualizado : R.string.cliente_guardado,
                    Toast.LENGTH_SHORT).show();
            finish();
        });
        viewModel.saveCliente(cliente, idGuardado);
    }
}
