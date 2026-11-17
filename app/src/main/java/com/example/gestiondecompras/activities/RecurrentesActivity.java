package com.example.gestiondecompras.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.databinding.ActivityFinListaBinding;
import com.example.gestiondecompras.databinding.DialogFinMontoBinding;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Recurrente;
import com.example.gestiondecompras.servicios.FinanzasServicio;
import com.example.gestiondecompras.utils.Dinero;
import com.example.gestiondecompras.utils.HojaAcciones;
import com.example.gestiondecompras.utils.finanzas.UiFinanzas;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Ingresos y gastos que se registran solos (alquiler, sueldo, servicios...). */
public class RecurrentesActivity extends AppCompatActivity {

    private ActivityFinListaBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFinListaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = AppDatabase.getInstance(this);

        binding.toolbar.setTitle(R.string.fin_recurrentes);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvAyuda.setText(R.string.fin_recurrente_ayuda);
        binding.tvAyuda.setVisibility(View.VISIBLE);
        binding.fabNuevo.setText(R.string.fin_nuevo_recurrente);
        // Un recurrente se crea registrando un movimiento con "Repetir automáticamente"
        binding.fabNuevo.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, NuevoMovimientoActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargar();
    }

    private String nombreFrecuencia(String f) {
        if (Recurrente.FREC_SEMANAL.equals(f)) return getString(R.string.fin_frec_semanal);
        if (Recurrente.FREC_QUINCENAL.equals(f)) return getString(R.string.fin_frec_quincenal);
        if (Recurrente.FREC_ANUAL.equals(f)) return getString(R.string.fin_frec_anual);
        return getString(R.string.fin_frec_mensual);
    }

    private void cargar() {
        executor.execute(() -> {
            List<Recurrente> lista = db.recurrenteDao().getTodos();
            Map<Long, CategoriaFin> categorias = new FinanzasServicio(db).mapaCategorias();
            runOnUiThread(() -> mostrar(lista, categorias));
        });
    }

    private void mostrar(List<Recurrente> lista, Map<Long, CategoriaFin> categorias) {
        if (isFinishing()) return;
        binding.layoutLista.removeAllViews();
        binding.cardLista.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvVacio.setText(R.string.fin_sin_recurrentes);
        binding.tvVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Recurrente r : lista) {
            View fila = inflater.inflate(R.layout.item_fin_gestion, binding.layoutLista, false);
            CategoriaFin cat = r.categoriaId != null ? categorias.get(r.categoriaId) : null;
            UiFinanzas.pintarIcono(fila.findViewById(R.id.ivIcono), cat);
            ((TextView) fila.findViewById(R.id.tvTitulo)).setText(r.descripcion);
            TextView sub = fila.findViewById(R.id.tvSubtitulo);
            String detalle = nombreFrecuencia(r.frecuencia) + " · "
                    + (r.activo ? getString(R.string.fin_proxima, UiFinanzas.fechaCorta(r.proximaFechaEpoch))
                    : getString(R.string.fin_pausado));
            sub.setText(detalle);
            sub.setVisibility(View.VISIBLE);
            TextView valor = fila.findViewById(R.id.tvValor);
            boolean ingreso = CategoriaFin.TIPO_INGRESO.equals(r.tipo);
            valor.setText((ingreso ? "+ " : "- ") + UiFinanzas.moneda(r.monto));
            valor.setTextColor(ContextCompat.getColor(this, ingreso ? R.color.success_color : R.color.error_color));
            fila.setAlpha(r.activo ? 1f : 0.55f);
            fila.setOnClickListener(v -> acciones(r));
            fila.findViewById(R.id.btnMas).setOnClickListener(v -> acciones(r));
            binding.layoutLista.addView(fila);
        }
    }

    private void acciones(Recurrente r) {
        HojaAcciones.de(this, r.descripcion)
                .subtitulo(nombreFrecuencia(r.frecuencia) + " · " + UiFinanzas.moneda(r.monto))
                .accion(R.drawable.ic_edit, R.string.fin_monto, () -> editarMonto(r))
                .accion(R.drawable.ic_repeat, r.activo ? R.string.fin_pausar : R.string.fin_reanudar, () -> {
                    r.activo = !r.activo;
                    executor.execute(() -> {
                        db.recurrenteDao().update(r);
                        cargar();
                    });
                })
                .peligrosa(R.drawable.ic_delete, R.string.accion_eliminar, () -> executor.execute(() -> {
                    // Los movimientos ya generados se conservan
                    db.recurrenteDao().delete(r);
                    runOnUiThread(() -> Toast.makeText(this, R.string.fin_recurrente_eliminado, Toast.LENGTH_SHORT).show());
                    cargar();
                }))
                .mostrar();
    }

    private void editarMonto(Recurrente r) {
        DialogFinMontoBinding d = DialogFinMontoBinding.inflate(getLayoutInflater());
        d.tilMonto.setHint(R.string.fin_monto);
        d.etMonto.setText(String.format(Locale.US, "%.2f", r.monto));
        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(r.descripcion)
                .setView(d.getRoot())
                .setPositiveButton(R.string.fin_guardar, null)
                .setNegativeButton(R.string.accion_cancelar, null)
                .create();
        dialogo.setOnShowListener(x -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double monto;
            try {
                monto = Dinero.redondear(Double.parseDouble(String.valueOf(d.etMonto.getText()).trim()));
            } catch (NumberFormatException e) {
                monto = 0;
            }
            if (!Dinero.esPositivo(monto)) {
                d.tilMonto.setError(getString(R.string.fin_monto_invalido));
                return;
            }
            r.monto = monto;
            executor.execute(() -> {
                db.recurrenteDao().update(r);
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
