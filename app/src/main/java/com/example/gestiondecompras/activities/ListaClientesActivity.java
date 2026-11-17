package com.example.gestiondecompras.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.utils.ContactoUtils;
import com.example.gestiondecompras.utils.HojaAcciones;
import com.example.gestiondecompras.utils.NavegacionInferior;
import com.example.gestiondecompras.models.Cliente;
import com.example.gestiondecompras.adapters.ClientesAdapter;
import com.example.gestiondecompras.databinding.ActivityListaClientesBinding;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.viewmodels.ListaClientesViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListaClientesActivity extends AppCompatActivity implements ClientesAdapter.OnClienteClickListener {

    private ActivityListaClientesBinding binding;
    private ListaClientesViewModel viewModel;
    private ClientesAdapter adapter;

    private final List<Cliente> fuente = new ArrayList<>();
    private final List<Cliente> visibles = new ArrayList<>();
    private String filtroTodos;
    private String filtroTelefono;
    private String filtroEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListaClientesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ListaClientesViewModel.class);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        binding.toolbar.setNavigationIcon(null);
        binding.toolbar.setTitle(R.string.clientes_title);

        setupRecyclerView();
        setupFilters();
        setupFab();
        observeViewModel();

        NavegacionInferior.configurar(this, binding.bottomNav, R.id.tab_clientes,
                () -> binding.rvClientes.smoothScrollToPosition(0));
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadClientes();
    }

    private void setupRecyclerView() {
        binding.rvClientes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClientesAdapter(visibles, this);
        binding.rvClientes.setAdapter(adapter);
    }

    private void setupFilters() {
        filtroTodos = getString(R.string.clientes_filter_all);
        filtroTelefono = getString(R.string.clientes_filter_phone);
        filtroEmail = getString(R.string.clientes_filter_email);

        String[] opciones = new String[]{filtroTodos, filtroTelefono, filtroEmail};
        ArrayAdapter<String> spAdapter = new ArrayAdapter<>(this, R.layout.item_dropdown, opciones);
        binding.spinnerFiltro.setAdapter(spAdapter);
        binding.spinnerFiltro.setText(filtroTodos, false);

        binding.spinnerFiltro.setOnItemClickListener((parent, view, position, id) -> aplicarFiltros());

        binding.etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { aplicarFiltros(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFab() {
        binding.fabNuevo.setOnClickListener(this::showFabMenu);
        binding.btnVacioAccion.setOnClickListener(v ->
                startActivity(new Intent(this, NuevoClienteActivity.class)));
    }

    private void showFabMenu(View v) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_fab_clientes, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.accion_nuevo_cliente) {
                startActivity(new Intent(this, NuevoClienteActivity.class));
                return true;
            } else if (item.getItemId() == R.id.accion_importar_contacto) {
                checkPermissionAndPick();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void observeViewModel() {
        viewModel.getClientes().observe(this, clientes -> {
            if (clientes != null) {
                fuente.clear();
                fuente.addAll(clientes);
                aplicarFiltros();
            }
        });
    }

    private void aplicarFiltros() {
        String q = binding.etBuscar.getText().toString().toLowerCase(Locale.getDefault()).trim();
        String filtro = binding.spinnerFiltro.getText() != null
                ? binding.spinnerFiltro.getText().toString() : filtroTodos;

        visibles.clear();
        for (Cliente c : fuente) {
            boolean porTexto = q.isEmpty()
                    || (c.nombre != null && c.nombre.toLowerCase(Locale.getDefault()).contains(q))
                    || (c.telefono != null && c.telefono.toLowerCase(Locale.getDefault()).contains(q))
                    || (c.email != null && c.email.toLowerCase(Locale.getDefault()).contains(q));

            boolean porFiltro = true;
            if (filtroTelefono.equals(filtro)) porFiltro = c.telefono != null && !c.telefono.isEmpty();
            else if (filtroEmail.equals(filtro)) porFiltro = c.email != null && !c.email.isEmpty();

            if (porTexto && porFiltro) visibles.add(c);
        }

        adapter.notifyDataSetChanged();
        binding.emptyState.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClienteClick(Cliente cliente) {
        Intent i = new Intent(this, ListaPedidosActivity.class);
        i.putExtra("cliente_id", cliente.id);
        i.putExtra("cliente_nombre", cliente.nombre);
        startActivity(i);
    }

    @Override
    public void onClienteLongClick(Cliente cliente) {
        String nombreCompleto = cliente.nombre
                + (cliente.apellido != null && !cliente.apellido.isEmpty() ? " " + cliente.apellido : "");
        boolean tieneTelefono = cliente.telefono != null && !cliente.telefono.trim().isEmpty();

        HojaAcciones hoja = HojaAcciones.de(this, nombreCompleto)
                .subtitulo(tieneTelefono ? cliente.telefono : cliente.email)
                .accion(R.drawable.ic_list, R.string.clientes_ver_pedidos, () -> onClienteClick(cliente))
                .accion(R.drawable.ic_edit, R.string.accion_editar, () -> {
                    Intent intent = new Intent(this, NuevoClienteActivity.class);
                    intent.putExtra(NuevoClienteActivity.EXTRA_CLIENTE_ID, cliente.id);
                    startActivity(intent);
                });
        if (tieneTelefono) {
            hoja.accion(R.drawable.ic_phone, R.string.accion_llamar, () -> llamar(cliente.telefono))
                .accion(R.drawable.ic_chat, R.string.accion_whatsapp, () -> abrirWhatsApp(cliente.telefono));
        }
        hoja.peligrosa(R.drawable.ic_delete, R.string.accion_eliminar, () -> confirmarEliminarCliente(cliente))
            .mostrar();
    }

    private void llamar(String telefono) {
        Intent intent = new Intent(Intent.ACTION_DIAL,
                android.net.Uri.parse("tel:" + ContactoUtils.numeroParaLlamar(telefono)));
        abrirSiEsPosible(intent);
    }

    private void abrirWhatsApp(String telefono) {
        String numero = ContactoUtils.numeroWhatsApp(telefono);
        if (numero.isEmpty()) {
            Toast.makeText(this, R.string.no_app_disponible, Toast.LENGTH_SHORT).show();
            return;
        }
        abrirSiEsPosible(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/" + numero)));
    }

    private void abrirSiEsPosible(Intent intent) {
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_app_disponible, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmarEliminarCliente(Cliente cliente) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_cliente_title)
                .setMessage(getString(R.string.dialog_delete_cliente_message, cliente.nombre))
                .setPositiveButton(R.string.accion_eliminar, (d, w) -> viewModel.deleteCliente(cliente))
                .setNegativeButton(R.string.accion_cancelar, null)
                .show();
    }
    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    pickContact();
                } else {
                    Toast.makeText(this, R.string.contactos_permiso_denegado, Toast.LENGTH_SHORT).show();
                }
            });

    private final androidx.activity.result.ActivityResultLauncher<Void> pickContactLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.PickContact(), contactUri -> {
                if (contactUri != null) {
                    importarContacto(contactUri);
                }
            });

    // Old menu code removed

    /** Importa el contacto; si ya existe un cliente con el mismo teléfono, pregunta antes. */
    private void importarSinDuplicar(Cliente nuevo) {
        androidx.lifecycle.MutableLiveData<Cliente> duplicado = new androidx.lifecycle.MutableLiveData<>();
        duplicado.observe(this, existente -> {
            if (existente == null) return;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.contacto_duplicado_titulo)
                    .setMessage(getString(R.string.contacto_duplicado_mensaje, existente.nombre,
                            existente.telefono != null ? existente.telefono : ""))
                    .setPositiveButton(R.string.contacto_importar_igual, (d, w) -> {
                        viewModel.insertCliente(nuevo);
                        Toast.makeText(this, getString(R.string.contacto_importado, nuevo.nombre), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.accion_cancelar, null)
                    .show();
        });
        viewModel.importarCliente(nuevo, duplicado, () ->
                Toast.makeText(this, getString(R.string.contacto_importado, nuevo.nombre), Toast.LENGTH_SHORT).show());
    }

    private void checkPermissionAndPick() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pickContact();
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS);
        }
    }

    private void pickContact() {
        pickContactLauncher.launch(null);
    }

    private void importarContacto(android.net.Uri contactUri) {
        try (android.database.Cursor cursor = getContentResolver().query(contactUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID);
                int nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME);
                int hasPhoneIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER);

                if (idIndex != -1 && nameIndex != -1 && hasPhoneIndex != -1) {
                    String id = cursor.getString(idIndex);
                    String name = cursor.getString(nameIndex);
                    String hasPhone = cursor.getString(hasPhoneIndex);

                    String phoneNumber = "";
                    String emailAddress = "";

                    if ("1".equals(hasPhone)) {
                        try (android.database.Cursor phones = getContentResolver().query(
                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id},
                                null)) {
                            if (phones != null && phones.moveToFirst()) {
                                int numberIndex = phones.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                                if (numberIndex != -1) {
                                    phoneNumber = phones.getString(numberIndex);
                                }
                            }
                        }
                    }

                    try (android.database.Cursor emails = getContentResolver().query(
                            android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            android.provider.ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            new String[]{id},
                            null)) {
                        if (emails != null && emails.moveToFirst()) {
                            int emailIndex = emails.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Email.DATA);
                            if (emailIndex != -1) {
                                emailAddress = emails.getString(emailIndex);
                            }
                        }
                    }

                    Cliente nuevoCliente = new Cliente();
                    nuevoCliente.nombre = name;
                    nuevoCliente.telefono = phoneNumber;
                    nuevoCliente.email = emailAddress;
                    nuevoCliente.activo = true;
                    // nuevoCliente.fechaRegistro = System.currentTimeMillis(); // Field does not exist

                    importarSinDuplicar(nuevoCliente);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.contacto_error_importar, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
