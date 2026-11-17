package com.example.gestiondecompras.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
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
import com.example.gestiondecompras.models.finanzas.Presupuesto;
import com.example.gestiondecompras.servicios.FinanzasServicio;
import com.example.gestiondecompras.utils.Dinero;
import com.example.gestiondecompras.utils.finanzas.CalculosFinancieros;
import com.example.gestiondecompras.utils.finanzas.UiFinanzas;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Presupuesto mensual por categoría de gasto y su progreso en el mes actual. */
public class PresupuestosActivity extends AppCompatActivity {

    private ActivityFinListaBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Fila de la pantalla: categoría con su presupuesto (si lo tiene) y lo gastado este mes. */
    private static class FilaPresupuesto {
        CategoriaFin categoria;
        Double limite;
        double gastado;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFinListaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = AppDatabase.getInstance(this);

        binding.toolbar.setTitle(R.string.fin_presupuestos);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvAyuda.setText(R.string.fin_presupuesto_ayuda);
        binding.tvAyuda.setVisibility(View.VISIBLE);
        binding.fabNuevo.setVisibility(View.GONE);
        cargar();
    }

    private void cargar() {
        executor.execute(() -> {
            long[] mes = FinanzasServicio.rangoMes(System.currentTimeMillis(), 0);
            Map<Long, Double> limites = new HashMap<>();
            for (Presupuesto p : db.presupuestoDao().getTodos()) limites.put(p.categoriaId, p.montoMensual);
            List<CategoriaFin> categorias = db.categoriaFinDao().getActivasPorTipo(CategoriaFin.TIPO_GASTO);
            java.util.ArrayList<FilaPresupuesto> filas = new java.util.ArrayList<>();
            for (CategoriaFin c : categorias) {
                FilaPresupuesto f = new FilaPresupuesto();
                f.categoria = c;
                f.limite = limites.get(c.id);
                f.gastado = Dinero.redondear(db.movimientoDao().gastoDeCategoria(c.id, mes[0], mes[1]));
                filas.add(f);
            }
            runOnUiThread(() -> mostrar(filas));
        });
    }

    private void mostrar(List<FilaPresupuesto> filas) {
        if (isFinishing()) return;
        binding.layoutLista.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (FilaPresupuesto f : filas) {
            View fila = inflater.inflate(R.layout.item_fin_gestion, binding.layoutLista, false);
            UiFinanzas.pintarIcono(fila.findViewById(R.id.ivIcono), f.categoria);
            ((TextView) fila.findViewById(R.id.tvTitulo)).setText(f.categoria.nombre);
            TextView sub = fila.findViewById(R.id.tvSubtitulo);
            TextView valor = fila.findViewById(R.id.tvValor);
            ProgressBar barra = fila.findViewById(R.id.pbProgreso);
            fila.findViewById(R.id.btnMas).setVisibility(View.GONE);
            if (f.limite != null) {
                double pct = CalculosFinancieros.porcentajePresupuesto(f.gastado, f.limite);
                sub.setText(getString(R.string.fin_presupuesto_detalle, UiFinanzas.moneda(f.gastado),
                        UiFinanzas.moneda(f.limite), UiFinanzas.porcentaje(pct)));
                barra.setVisibility(View.VISIBLE);
                barra.setProgress((int) Math.min(100, Math.round(pct)));
                CalculosFinancieros.EstadoPresupuesto estado = CalculosFinancieros.estadoPresupuesto(f.gastado, f.limite);
                int color = estado == CalculosFinancieros.EstadoPresupuesto.EXCEDIDO ? R.color.error_color
                        : estado == CalculosFinancieros.EstadoPresupuesto.ALERTA ? R.color.warning_color : R.color.success_color;
                barra.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, color)));
                valor.setText(UiFinanzas.moneda(f.limite));
            } else {
                sub.setText(UiFinanzas.moneda(f.gastado));
                valor.setText(R.string.fin_sin_limite);
                valor.setTextColor(ContextCompat.getColor(this, R.color.gray));
            }
            sub.setVisibility(View.VISIBLE);
            fila.setOnClickListener(v -> editar(f));
            binding.layoutLista.addView(fila);
        }
    }

    private void editar(FilaPresupuesto f) {
        DialogFinMontoBinding d = DialogFinMontoBinding.inflate(getLayoutInflater());
        if (f.limite != null) d.etMonto.setText(String.format(Locale.US, "%.2f", f.limite));
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(f.categoria.nombre)
                .setView(d.getRoot())
                .setPositiveButton(R.string.fin_guardar, null)
                .setNegativeButton(R.string.accion_cancelar, null);
        if (f.limite != null) {
            b.setNeutralButton(R.string.fin_quitar_presupuesto, (dlg, w) -> executor.execute(() -> {
                db.presupuestoDao().eliminarDeCategoria(f.categoria.id);
                cargar();
            }));
        }
        AlertDialog dialogo = b.create();
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
            final double montoFinal = monto;
            executor.execute(() -> {
                Presupuesto p = db.presupuestoDao().getDeCategoria(f.categoria.id);
                if (p == null) p = new Presupuesto();
                p.categoriaId = f.categoria.id;
                p.montoMensual = montoFinal;
                db.presupuestoDao().guardar(p);
                runOnUiThread(() -> Toast.makeText(this, R.string.fin_presupuesto_guardado, Toast.LENGTH_SHORT).show());
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
