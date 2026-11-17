package com.example.gestiondecompras.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.example.gestiondecompras.databinding.DialogFinCuentaBinding;
import com.example.gestiondecompras.models.finanzas.Cuenta;
import com.example.gestiondecompras.servicios.FinanzasServicio;
import com.example.gestiondecompras.utils.Dinero;
import com.example.gestiondecompras.utils.HojaAcciones;
import com.example.gestiondecompras.utils.finanzas.UiFinanzas;
import com.example.gestiondecompras.utils.finanzas.VisualFinanzas;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Cuentas o métodos de pago (efectivo, banco, tarjeta...) con su saldo actual. */
public class CuentasActivity extends AppCompatActivity {

    private static final String[] TIPOS = {Cuenta.TIPO_EFECTIVO, Cuenta.TIPO_BANCO, Cuenta.TIPO_TARJETA, Cuenta.TIPO_OTRO};

    private ActivityFinListaBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFinListaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = AppDatabase.getInstance(this);

        binding.toolbar.setTitle(R.string.fin_cuentas);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.fabNuevo.setText(R.string.fin_nueva_cuenta);
        binding.fabNuevo.setOnClickListener(v -> editar(null));
        cargar();
    }

    private String nombreTipo(String tipo) {
        if (Cuenta.TIPO_BANCO.equals(tipo)) return getString(R.string.fin_cuenta_banco);
        if (Cuenta.TIPO_TARJETA.equals(tipo)) return getString(R.string.fin_cuenta_tarjeta);
        if (Cuenta.TIPO_EFECTIVO.equals(tipo)) return getString(R.string.fin_cuenta_efectivo);
        return getString(R.string.fin_cuenta_otro);
    }

    private void cargar() {
        executor.execute(() -> {
            List<FinanzasServicio.SaldoCuenta> saldos = new FinanzasServicio(db).saldosCuentas(true);
            runOnUiThread(() -> mostrar(saldos));
        });
    }

    private void mostrar(List<FinanzasServicio.SaldoCuenta> saldos) {
        if (isFinishing()) return;
        binding.layoutLista.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (FinanzasServicio.SaldoCuenta s : saldos) {
            Cuenta c = s.cuenta;
            View fila = inflater.inflate(R.layout.item_fin_gestion, binding.layoutLista, false);
            ImageView icono = fila.findViewById(R.id.ivIcono);
            icono.setImageResource(VisualFinanzas.iconoCuenta(c.tipo));
            icono.setBackgroundResource(R.drawable.bg_icon_tile);
            icono.setImageTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_gold)));
            ((TextView) fila.findViewById(R.id.tvTitulo)).setText(c.nombre);
            TextView sub = fila.findViewById(R.id.tvSubtitulo);
            sub.setText(c.archivada ? nombreTipo(c.tipo) + " · " + getString(R.string.fin_archivada) : nombreTipo(c.tipo));
            sub.setVisibility(View.VISIBLE);
            TextView valor = fila.findViewById(R.id.tvValor);
            valor.setText(UiFinanzas.moneda(s.saldo));
            valor.setTextColor(ContextCompat.getColor(this, s.saldo < 0 ? R.color.error_color : R.color.gray_dark));
            fila.setAlpha(c.archivada ? 0.55f : 1f);
            fila.setOnClickListener(v -> editar(c));
            fila.findViewById(R.id.btnMas).setOnClickListener(v -> acciones(c));
            binding.layoutLista.addView(fila);
        }
    }

    private void acciones(Cuenta c) {
        HojaAcciones.de(this, c.nombre)
                .accion(R.drawable.ic_edit, R.string.accion_editar, () -> editar(c))
                .accion(R.drawable.ic_category, c.archivada ? R.string.fin_reactivar : R.string.fin_archivar, () -> {
                    c.archivada = !c.archivada;
                    executor.execute(() -> {
                        db.cuentaDao().update(c);
                        cargar();
                    });
                })
                .peligrosa(R.drawable.ic_delete, R.string.accion_eliminar, () -> executor.execute(() -> {
                    boolean eliminada = new FinanzasServicio(db).eliminarOArchivarCuenta(c);
                    runOnUiThread(() -> Toast.makeText(this, eliminada ? R.string.fin_cuenta_eliminada
                            : R.string.fin_cuenta_archivada, Toast.LENGTH_LONG).show());
                    cargar();
                }))
                .mostrar();
    }

    private void editar(@Nullable Cuenta existente) {
        DialogFinCuentaBinding d = DialogFinCuentaBinding.inflate(getLayoutInflater());
        String[] nombres = new String[TIPOS.length];
        for (int i = 0; i < TIPOS.length; i++) nombres[i] = nombreTipo(TIPOS[i]);
        d.spTipo.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, nombres));
        final int[] tipoSel = {0};
        if (existente != null) {
            d.etNombre.setText(existente.nombre);
            d.etSaldo.setText(String.format(Locale.US, "%.2f", existente.saldoInicial));
            for (int i = 0; i < TIPOS.length; i++) if (TIPOS[i].equals(existente.tipo)) tipoSel[0] = i;
        }
        d.spTipo.setText(nombres[tipoSel[0]], false);
        d.spTipo.setOnItemClickListener((p, v, pos, id) -> tipoSel[0] = pos);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(existente != null ? R.string.fin_editar_cuenta : R.string.fin_nueva_cuenta)
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
            double saldo;
            try {
                saldo = Dinero.redondear(Double.parseDouble(String.valueOf(d.etSaldo.getText()).trim()));
            } catch (NumberFormatException e) {
                saldo = 0;
            }
            final double saldoFinal = saldo;
            executor.execute(() -> {
                Cuenta c = existente != null ? existente : new Cuenta();
                c.nombre = nombre;
                c.tipo = TIPOS[tipoSel[0]];
                c.saldoInicial = saldoFinal;
                if (existente == null) db.cuentaDao().insert(c);
                else db.cuentaDao().update(c);
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
