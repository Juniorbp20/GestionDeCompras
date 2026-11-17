package com.example.gestiondecompras.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

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

    /** Selector de contactos del sistema: da acceso solo al contacto elegido, sin pedir permiso. */
    private final ActivityResultLauncher<Intent> lanzadorContacto = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null
                        && resultado.getData().getData() != null) {
                    rellenarDesdeContacto(resultado.getData().getData());
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNuevoClienteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(NuevoClienteViewModel.class);

        setupToolbar();
        setupSaveButton();
        setupContactos();
        cargarSiEsEdicion();
    }

    private void setupContactos() {
        binding.btnElegirContacto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            try {
                lanzadorContacto.launch(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.no_app_disponible, Toast.LENGTH_SHORT).show();
            }
        });
        binding.btnGuardarContacto.setOnClickListener(v -> guardarEnContactos());
    }

    /** Rellena nombre, apellido y teléfono con el contacto elegido. */
    private void rellenarDesdeContacto(Uri uri) {
        String[] columnas = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        try (Cursor cursor = getContentResolver().query(uri, columnas, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                Toast.makeText(this, R.string.contacto_error_importar, Toast.LENGTH_SHORT).show();
                return;
            }
            String nombreCompleto = cursor.getString(0);
            String telefono = cursor.getString(1);
            if (!TextUtils.isEmpty(nombreCompleto)) {
                String[] partes = nombreCompleto.trim().split("\\s+", 2);
                binding.etNombre.setText(partes[0]);
                binding.etApellido.setText(partes.length > 1 ? partes[1] : "");
                binding.etNombre.setError(null);
            }
            if (!TextUtils.isEmpty(telefono)) {
                binding.etTelefono.setText(telefono.trim());
            }
            Toast.makeText(this, getString(R.string.cliente_contacto_cargado,
                    nombreCompleto != null ? nombreCompleto.trim() : ""), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.contacto_error_importar, Toast.LENGTH_SHORT).show();
        }
    }

    /** Abre la app de contactos del teléfono con los datos del cliente ya escritos. */
    private void guardarEnContactos() {
        String nombre = texto(binding.etNombre);
        if (nombre.isEmpty()) {
            binding.etNombre.setError(getString(R.string.cliente_nombre_requerido));
            binding.etNombre.requestFocus();
            return;
        }
        String apellido = texto(binding.etApellido);
        String telefono = texto(binding.etTelefono);
        String direccion = texto(binding.etDireccion);

        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.NAME,
                apellido.isEmpty() ? nombre : nombre + " " + apellido);
        if (!telefono.isEmpty()) {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, telefono);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        }
        if (!direccion.isEmpty()) {
            intent.putExtra(ContactsContract.Intents.Insert.POSTAL, direccion);
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_app_disponible, Toast.LENGTH_SHORT).show();
        }
    }

    private static String texto(android.widget.EditText campo) {
        return campo.getText() != null ? campo.getText().toString().trim() : "";
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
