package com.example.gestiondecompras.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.databinding.ActivityNuevoMovimientoBinding;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Cuenta;
import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.models.finanzas.Recurrente;
import com.example.gestiondecompras.servicios.FinanzasServicio;
import com.example.gestiondecompras.utils.Dinero;
import com.example.gestiondecompras.utils.finanzas.UiFinanzas;
import com.example.gestiondecompras.utils.finanzas.VisualFinanzas;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Alta, edición y duplicado de ingresos y gastos. */
public class NuevoMovimientoActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "extra_movimiento_id";
    public static final String EXTRA_DUPLICAR_ID = "extra_movimiento_duplicar_id";
    public static final String EXTRA_TIPO = "extra_movimiento_tipo";

    private ActivityNuevoMovimientoBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AppDatabase db;

    /** Movimiento en edición (null = nuevo). */
    @Nullable private Movimiento edicion;
    private String tipo = Movimiento.TIPO_GASTO;
    private long fecha = System.currentTimeMillis();
    @Nullable private Long categoriaSeleccionada;
    private List<Cuenta> cuentas = new ArrayList<>();
    private int cuentaSeleccionada = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNuevoMovimientoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = AppDatabase.getInstance(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        String tipoExtra = getIntent().getStringExtra(EXTRA_TIPO);
        if (Movimiento.TIPO_INGRESO.equals(tipoExtra)) tipo = Movimiento.TIPO_INGRESO;
        binding.grupoTipo.check(Movimiento.TIPO_INGRESO.equals(tipo) ? R.id.btnTipoIngreso : R.id.btnTipoGasto);
        binding.grupoTipo.addOnButtonCheckedListener((grupo, id, marcado) -> {
            if (!marcado) return;
            String nuevo = id == R.id.btnTipoIngreso ? Movimiento.TIPO_INGRESO : Movimiento.TIPO_GASTO;
            if (!nuevo.equals(tipo)) {
                tipo = nuevo;
                categoriaSeleccionada = null;
                cargarCategorias();
            }
        });

        binding.etFecha.setText(UiFinanzas.fechaCorta(fecha));
        binding.etFecha.setOnClickListener(v -> elegirFecha());
        binding.swRepetir.setOnCheckedChangeListener((b, marcado) ->
                binding.grupoFrecuencia.setVisibility(marcado ? View.VISIBLE : View.GONE));
        binding.btnGestionarCategorias.setOnClickListener(v ->
                startActivity(new Intent(this, CategoriasFinActivity.class)));
        binding.spCuenta.setOnItemClickListener((p, v, pos, id) -> cuentaSeleccionada = pos);
        binding.btnGuardar.setOnClickListener(v -> guardar());

        long idEdicion = getIntent().getLongExtra(EXTRA_ID, -1);
        long idDuplicar = getIntent().getLongExtra(EXTRA_DUPLICAR_ID, -1);
        if (idEdicion > 0 || idDuplicar > 0) {
            boolean esDuplicado = idEdicion <= 0;
            binding.toolbar.setTitle(esDuplicado ? R.string.fin_nuevo_movimiento : R.string.fin_editar_movimiento);
            // La repetición solo se configura al crear; un recurrente existente se gestiona en su pantalla
            if (!esDuplicado) binding.cardRepetir.setVisibility(View.GONE);
            executor.execute(() -> {
                Movimiento m = db.movimientoDao().getById(esDuplicado ? idDuplicar : idEdicion);
                runOnUiThread(() -> cargarMovimiento(m, esDuplicado));
            });
        } else {
            cargarCategorias();
            cargarCuentas(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al volver de "Gestionar categorías" se refrescan los chips
        if (binding.grupoCategorias.getChildCount() > 0) cargarCategorias();
    }

    private void cargarMovimiento(@Nullable Movimiento m, boolean esDuplicado) {
        if (m == null || isFinishing()) {
            cargarCategorias();
            cargarCuentas(null);
            return;
        }
        if (!esDuplicado) edicion = m;
        tipo = m.tipo;
        binding.grupoTipo.check(m.esIngreso() ? R.id.btnTipoIngreso : R.id.btnTipoGasto);
        binding.etMonto.setText(String.format(java.util.Locale.US, "%.2f", m.monto));
        binding.etDescripcion.setText(m.descripcion);
        binding.etNotas.setText(m.notas);
        if (!esDuplicado) fecha = m.fechaEpoch;
        binding.etFecha.setText(UiFinanzas.fechaCorta(fecha));
        categoriaSeleccionada = m.categoriaId;
        cargarCategorias();
        cargarCuentas(m.cuentaId);
    }

    private void cargarCategorias() {
        final String tipoActual = tipo;
        executor.execute(() -> {
            List<CategoriaFin> lista = db.categoriaFinDao().getActivasPorTipo(tipoActual);
            // Si se edita un movimiento con una categoría archivada, se muestra igualmente
            if (categoriaSeleccionada != null) {
                boolean esta = false;
                for (CategoriaFin c : lista) if (c.id == categoriaSeleccionada) esta = true;
                if (!esta) {
                    CategoriaFin archivada = db.categoriaFinDao().getById(categoriaSeleccionada);
                    if (archivada != null && tipoActual.equals(archivada.tipo)) lista.add(archivada);
                }
            }
            runOnUiThread(() -> mostrarCategorias(lista));
        });
    }

    private void mostrarCategorias(List<CategoriaFin> lista) {
        if (isFinishing()) return;
        binding.grupoCategorias.removeAllViews();
        for (CategoriaFin c : lista) {
            Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
            chip.setText(c.nombre);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            int color = ContextCompat.getColor(this, VisualFinanzas.color(c.colorKey));
            chip.setChipIconResource(VisualFinanzas.icono(c.iconoKey));
            chip.setChipIconTint(ColorStateList.valueOf(color));
            chip.setChipIconVisible(true);
            int[][] estados = {{android.R.attr.state_checked}, {}};
            chip.setChipBackgroundColor(new ColorStateList(estados, new int[]{
                    ColorUtils.setAlphaComponent(color, 60),
                    ContextCompat.getColor(this, R.color.background_card)}));
            chip.setChipStrokeColor(new ColorStateList(estados, new int[]{color,
                    ContextCompat.getColor(this, R.color.card_stroke)}));
            chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
            chip.setId(View.generateViewId());
            chip.setTag(c.id);
            chip.setChecked(categoriaSeleccionada != null && c.id == categoriaSeleccionada);
            chip.setOnCheckedChangeListener((b, marcado) -> {
                if (marcado) categoriaSeleccionada = c.id;
                else if (categoriaSeleccionada != null && categoriaSeleccionada == c.id) categoriaSeleccionada = null;
            });
            binding.grupoCategorias.addView(chip);
        }
    }

    private void cargarCuentas(@Nullable Long cuentaId) {
        executor.execute(() -> {
            FinanzasServicio.asegurarCuentasBase(this, db);
            List<Cuenta> lista = db.cuentaDao().getActivas();
            if (cuentaId != null) {
                boolean esta = false;
                for (Cuenta c : lista) if (c.id == cuentaId) esta = true;
                if (!esta) {
                    Cuenta archivada = db.cuentaDao().getById(cuentaId);
                    if (archivada != null) lista.add(archivada);
                }
            }
            runOnUiThread(() -> {
                if (isFinishing()) return;
                cuentas = lista;
                List<String> nombres = new ArrayList<>();
                nombres.add(getString(R.string.fin_sin_cuenta));
                cuentaSeleccionada = 0;
                for (int i = 0; i < lista.size(); i++) {
                    nombres.add(lista.get(i).nombre);
                    if (cuentaId != null && lista.get(i).id == cuentaId) cuentaSeleccionada = i + 1;
                }
                // Por defecto al crear: la cuenta de efectivo (o la primera si no hay)
                if (cuentaId == null && edicion == null && !lista.isEmpty()) {
                    cuentaSeleccionada = 1;
                    for (int i = 0; i < lista.size(); i++) {
                        if (Cuenta.TIPO_EFECTIVO.equals(lista.get(i).tipo)) {
                            cuentaSeleccionada = i + 1;
                            break;
                        }
                    }
                }
                binding.spCuenta.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, nombres));
                binding.spCuenta.setText(nombres.get(cuentaSeleccionada), false);
            });
        });
    }

    private void elegirFecha() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(fecha);
        new DatePickerDialog(this, (dp, y, mo, d) -> {
            Calendar sel = Calendar.getInstance();
            sel.setTimeInMillis(fecha);
            sel.set(y, mo, d);
            fecha = sel.getTimeInMillis();
            binding.etFecha.setText(UiFinanzas.fechaCorta(fecha));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void guardar() {
        double monto;
        try {
            monto = Dinero.redondear(Double.parseDouble(String.valueOf(binding.etMonto.getText()).trim()));
        } catch (NumberFormatException e) {
            monto = 0;
        }
        binding.tilMonto.setError(null);
        binding.tilDescripcion.setError(null);
        if (!Dinero.esPositivo(monto)) {
            binding.tilMonto.setError(getString(R.string.fin_monto_invalido));
            return;
        }
        String descripcion = String.valueOf(binding.etDescripcion.getText()).trim();
        if (descripcion.isEmpty()) {
            binding.tilDescripcion.setError(getString(R.string.fin_descripcion_requerida));
            return;
        }
        if (categoriaSeleccionada == null) {
            Toast.makeText(this, R.string.fin_categoria_requerida, Toast.LENGTH_SHORT).show();
            return;
        }

        Movimiento m = edicion != null ? edicion : new Movimiento();
        m.tipo = tipo;
        m.monto = monto;
        m.descripcion = descripcion;
        String notas = String.valueOf(binding.etNotas.getText()).trim();
        m.notas = notas.isEmpty() ? null : notas;
        m.fechaEpoch = fecha;
        m.categoriaId = categoriaSeleccionada;
        m.cuentaId = cuentaSeleccionada > 0 && cuentaSeleccionada <= cuentas.size()
                ? cuentas.get(cuentaSeleccionada - 1).id : null;

        boolean repetir = edicion == null && binding.swRepetir.isChecked();
        String frecuencia = frecuenciaElegida();
        binding.btnGuardar.setEnabled(false);
        executor.execute(() -> {
            FinanzasServicio servicio = new FinanzasServicio(db);
            if (repetir) servicio.guardarConRecurrencia(m, frecuencia);
            else servicio.guardarMovimiento(m);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.fin_movimiento_guardado, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private String frecuenciaElegida() {
        int id = binding.grupoFrecuencia.getCheckedChipId();
        if (id == R.id.chipSemanal) return Recurrente.FREC_SEMANAL;
        if (id == R.id.chipQuincenal) return Recurrente.FREC_QUINCENAL;
        if (id == R.id.chipAnual) return Recurrente.FREC_ANUAL;
        return Recurrente.FREC_MENSUAL;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
