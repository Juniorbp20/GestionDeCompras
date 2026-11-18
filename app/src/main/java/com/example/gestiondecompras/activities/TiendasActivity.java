package com.example.gestiondecompras.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.daos.TiendaDao;
import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.databinding.ActivityFinListaBinding;
import com.example.gestiondecompras.models.Tienda;
import com.example.gestiondecompras.utils.HojaAcciones;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiendas donde se compra: crear, renombrar y eliminar (solo si no tienen pedidos). */
public class TiendasActivity extends AppCompatActivity {

    private ActivityFinListaBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFinListaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = AppDatabase.getInstance(this);

        binding.toolbar.setTitle(R.string.tiendas_titulo);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvAyuda.setText(R.string.tiendas_ayuda);
        binding.tvAyuda.setVisibility(View.VISIBLE);
        binding.tvVacio.setText(R.string.tiendas_vacio);
        binding.fabNuevo.setText(R.string.tienda_nueva_titulo);
        binding.fabNuevo.setOnClickListener(v -> editar(null));
        cargar();
    }

    private void cargar() {
        executor.execute(() -> {
            List<TiendaDao.TiendaConConteo> tiendas = db.tiendaDao().getTiendasConConteo();
            runOnUiThread(() -> mostrar(tiendas));
        });
    }

    private void mostrar(List<TiendaDao.TiendaConConteo> tiendas) {
        if (isFinishing()) return;
        binding.layoutLista.removeAllViews();
        boolean vacia = tiendas.isEmpty();
        binding.cardLista.setVisibility(vacia ? View.GONE : View.VISIBLE);
        binding.tvVacio.setVisibility(vacia ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (TiendaDao.TiendaConConteo t : tiendas) {
            View fila = inflater.inflate(R.layout.item_fin_gestion, binding.layoutLista, false);
            ImageView icono = fila.findViewById(R.id.ivIcono);
            icono.setImageResource(R.drawable.ic_cart);
            icono.setBackgroundResource(R.drawable.bg_icon_tile);
            icono.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_gold)));
            ((TextView) fila.findViewById(R.id.tvTitulo)).setText(t.nombre);
            TextView sub = fila.findViewById(R.id.tvSubtitulo);
            sub.setText(getResources().getQuantityString(R.plurals.tienda_pedidos, t.pedidos, t.pedidos));
            sub.setVisibility(View.VISIBLE);
            fila.findViewById(R.id.tvValor).setVisibility(View.GONE);
            fila.setOnClickListener(v -> editar(t));
            fila.findViewById(R.id.btnMas).setOnClickListener(v -> acciones(t));
            binding.layoutLista.addView(fila);
        }
    }

    private void acciones(TiendaDao.TiendaConConteo t) {
        HojaAcciones.de(this, t.nombre)
                .accion(R.drawable.ic_edit, R.string.tienda_renombrar, () -> editar(t))
                .peligrosa(R.drawable.ic_delete, R.string.accion_eliminar, () -> eliminar(t))
                .mostrar();
    }

    private void eliminar(TiendaDao.TiendaConConteo t) {
        executor.execute(() -> {
            // La tienda no se puede borrar si algún pedido la usa (los pedidos perderían su tienda)
            int pedidos = db.tiendaDao().contarPedidos(t.id);
            if (pedidos > 0) {
                runOnUiThread(() -> new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.tienda_no_eliminar_titulo)
                        .setMessage(getResources().getQuantityString(R.plurals.tienda_no_eliminar_mensaje, pedidos, pedidos))
                        .setPositiveButton(R.string.accion_entendido, null)
                        .show());
                return;
            }
            db.tiendaDao().eliminarPorId(t.id);
            runOnUiThread(() -> Toast.makeText(this, getString(R.string.tienda_eliminada, t.nombre), Toast.LENGTH_SHORT).show());
            cargar();
        });
    }

    /** Crea una tienda nueva ({@code existente == null}) o renombra la existente. */
    private void editar(@Nullable TiendaDao.TiendaConConteo existente) {
        View vista = getLayoutInflater().inflate(R.layout.dialog_nueva_tienda, null);
        TextInputLayout til = vista.findViewById(R.id.til_nombre_tienda);
        EditText campo = vista.findViewById(R.id.et_nombre_tienda);
        if (existente != null) {
            campo.setText(existente.nombre);
            campo.setSelection(campo.length());
        }
        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(existente != null ? R.string.tienda_renombrar : R.string.tienda_nueva_titulo)
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
                executor.execute(() -> {
                    Tienda igual = db.tiendaDao().buscarPorNombre(nombre);
                    if (igual != null && (existente == null || igual.id != existente.id)) {
                        runOnUiThread(() -> til.setError(getString(R.string.tienda_duplicada)));
                        return;
                    }
                    if (existente == null) {
                        Tienda nueva = new Tienda();
                        nueva.setNombre(nombre);
                        db.tiendaDao().insert(nueva);
                    } else {
                        db.runInTransaction(() -> {
                            db.tiendaDao().actualizarNombre(existente.id, nombre);
                            db.tiendaDao().renombrarEnPedidos(existente.id, nombre);
                        });
                    }
                    runOnUiThread(dialogo::dismiss);
                    cargar();
                });
            });
        });
        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialogo.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
