package com.example.gestiondecompras.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.databinding.ActivityFinListaBinding;
import com.example.gestiondecompras.databinding.DialogFinCategoriaBinding;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.servicios.FinanzasServicio;
import com.example.gestiondecompras.utils.HojaAcciones;
import com.example.gestiondecompras.utils.finanzas.UiFinanzas;
import com.example.gestiondecompras.utils.finanzas.VisualFinanzas;
import com.google.android.material.chip.Chip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Categorías personalizables de ingresos y gastos. */
public class CategoriasFinActivity extends AppCompatActivity {

    private ActivityFinListaBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String tipo = CategoriaFin.TIPO_GASTO;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFinListaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = AppDatabase.getInstance(this);

        binding.toolbar.setTitle(R.string.fin_categorias);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.grupoTipo.setVisibility(View.VISIBLE);
        binding.grupoTipo.setOnCheckedStateChangeListener((g, ids) -> {
            tipo = ids.contains(R.id.chipIngreso) ? CategoriaFin.TIPO_INGRESO : CategoriaFin.TIPO_GASTO;
            cargar();
        });
        binding.fabNuevo.setText(R.string.fin_nueva_categoria);
        binding.fabNuevo.setOnClickListener(v -> editar(null));
        cargar();
    }

    private void cargar() {
        final String t = tipo;
        executor.execute(() -> {
            List<CategoriaFin> lista = db.categoriaFinDao().getPorTipo(t);
            Map<Long, Integer> conteos = new HashMap<>();
            for (CategoriaFin c : lista) conteos.put(c.id, db.categoriaFinDao().contarMovimientos(c.id));
            runOnUiThread(() -> mostrar(lista, conteos));
        });
    }

    private void mostrar(List<CategoriaFin> lista, Map<Long, Integer> conteos) {
        if (isFinishing()) return;
        binding.layoutLista.removeAllViews();
        binding.cardLista.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (CategoriaFin c : lista) {
            View fila = inflater.inflate(R.layout.item_fin_gestion, binding.layoutLista, false);
            UiFinanzas.pintarIcono(fila.findViewById(R.id.ivIcono), c);
            ((TextView) fila.findViewById(R.id.tvTitulo)).setText(c.nombre);
            int n = conteos.containsKey(c.id) ? conteos.get(c.id) : 0;
            TextView sub = fila.findViewById(R.id.tvSubtitulo);
            String texto = getResources().getQuantityString(R.plurals.fin_cantidad_movimientos, n, n);
            if (c.archivada) texto = getString(R.string.fin_archivada) + " · " + texto;
            sub.setText(texto);
            sub.setVisibility(View.VISIBLE);
            fila.findViewById(R.id.tvValor).setVisibility(View.GONE);
            fila.setAlpha(c.archivada ? 0.55f : 1f);
            fila.setOnClickListener(v -> editar(c));
            fila.findViewById(R.id.btnMas).setOnClickListener(v -> acciones(c));
            binding.layoutLista.addView(fila);
        }
    }

    private void acciones(CategoriaFin c) {
        HojaAcciones.de(this, c.nombre)
                .accion(R.drawable.ic_edit, R.string.accion_editar, () -> editar(c))
                .accion(R.drawable.ic_category, c.archivada ? R.string.fin_reactivar : R.string.fin_archivar, () -> {
                    c.archivada = !c.archivada;
                    executor.execute(() -> {
                        db.categoriaFinDao().update(c);
                        cargar();
                    });
                })
                .peligrosa(R.drawable.ic_delete, R.string.accion_eliminar, () -> executor.execute(() -> {
                    boolean eliminada = new FinanzasServicio(db).eliminarOArchivarCategoria(c);
                    runOnUiThread(() -> Toast.makeText(this, eliminada ? R.string.fin_categoria_eliminada
                            : R.string.fin_categoria_archivada, Toast.LENGTH_LONG).show());
                    cargar();
                }))
                .mostrar();
    }

    /** Diálogo de creación (c == null) o edición. */
    private void editar(@Nullable CategoriaFin existente) {
        DialogFinCategoriaBinding d = DialogFinCategoriaBinding.inflate(getLayoutInflater());
        final String[] color = {existente != null ? existente.colorKey : VisualFinanzas.COLORES[0]};
        final String[] icono = {existente != null ? existente.iconoKey : VisualFinanzas.ICONOS[0]};
        if (existente != null) d.etNombre.setText(existente.nombre);

        int[][] estados = {{android.R.attr.state_checked}, {}};
        for (String clave : VisualFinanzas.COLORES) {
            int c = ContextCompat.getColor(this, VisualFinanzas.color(clave));
            Chip chip = new Chip(this);
            chip.setCheckable(true);
            chip.setChipBackgroundColor(ColorStateList.valueOf(c));
            chip.setCheckedIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
            chip.setChipStrokeColor(new ColorStateList(estados, new int[]{
                    ContextCompat.getColor(this, R.color.gray_dark), c}));
            chip.setChipStrokeWidth(2 * getResources().getDisplayMetrics().density);
            chip.setText("");
            chip.setContentDescription(clave);
            chip.setId(View.generateViewId());
            chip.setChecked(clave.equals(color[0]));
            chip.setOnCheckedChangeListener((b, m) -> { if (m) color[0] = clave; });
            d.grupoColores.addView(chip);
        }
        for (String clave : VisualFinanzas.ICONOS) {
            Chip chip = new Chip(this);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setChipIconResource(VisualFinanzas.icono(clave));
            chip.setChipIconVisible(true);
            chip.setChipIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray_dark)));
            chip.setTextStartPadding(0);
            chip.setTextEndPadding(0);
            chip.setText("");
            chip.setContentDescription(clave);
            chip.setChipBackgroundColor(new ColorStateList(estados, new int[]{
                    ContextCompat.getColor(this, R.color.accent_gold_container),
                    ContextCompat.getColor(this, R.color.background_card)}));
            chip.setId(View.generateViewId());
            chip.setChecked(clave.equals(icono[0]));
            chip.setOnCheckedChangeListener((b, m) -> { if (m) icono[0] = clave; });
            d.grupoIconos.addView(chip);
        }

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(existente != null ? R.string.fin_editar_categoria : R.string.fin_nueva_categoria)
                .setView(d.getRoot())
                .setPositiveButton(R.string.fin_guardar, null)
                .setNegativeButton(R.string.accion_cancelar, null)
                .create();
        dialogo.setOnShowListener(x -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nombre = String.valueOf(d.etNombre.getText()).trim();
            if (nombre.isEmpty()) {
                d.tilNombre.setError(getString(R.string.fin_nombre_requerido));
                return;
            }
            final String t = tipo;
            executor.execute(() -> {
                CategoriaFin c = existente != null ? existente : new CategoriaFin();
                c.nombre = nombre;
                c.colorKey = color[0];
                c.iconoKey = icono[0];
                if (existente == null) {
                    c.tipo = t;
                    c.orden = db.categoriaFinDao().ordenMaximo(t) + 1;
                    db.categoriaFinDao().insert(c);
                } else {
                    db.categoriaFinDao().update(c);
                }
                cargar();
            });
            dialogo.dismiss();
        }));
        dialogo.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
