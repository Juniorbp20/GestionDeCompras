package com.example.gestiondecompras.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.databinding.ActivityFinanzasBinding;
import com.example.gestiondecompras.fragments.finanzas.MovimientosFragment;
import com.example.gestiondecompras.fragments.finanzas.ReportesFragment;
import com.example.gestiondecompras.fragments.finanzas.ResumenFragment;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.utils.BorradoConDeshacer;
import com.example.gestiondecompras.utils.ExportadorCsv;
import com.example.gestiondecompras.utils.HojaAcciones;
import com.example.gestiondecompras.utils.NavegacionInferior;
import com.example.gestiondecompras.utils.finanzas.Periodo;
import com.example.gestiondecompras.utils.finanzas.UiFinanzas;
import com.example.gestiondecompras.viewmodels.FinanzasViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

/** Pestaña Finanzas: Resumen (panel), Movimientos (búsqueda) y Reportes. */
public class FinanzasActivity extends AppCompatActivity {

    private static final String TAG_RESUMEN = "resumen";
    private static final String TAG_MOVIMIENTOS = "movimientos";
    private static final String TAG_REPORTES = "reportes";
    private static final String ESTADO_PESTANA = "pestana";

    private ActivityFinanzasBinding binding;
    private FinanzasViewModel viewModel;
    private final BorradoConDeshacer borrado = new BorradoConDeshacer();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFinanzasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(FinanzasViewModel.class);

        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.fin_tab_resumen));
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.fin_tab_movimientos));
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.fin_tab_reportes));

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.contenedor, new ResumenFragment(), TAG_RESUMEN)
                    .add(R.id.contenedor, new MovimientosFragment(), TAG_MOVIMIENTOS)
                    .add(R.id.contenedor, new ReportesFragment(), TAG_REPORTES)
                    .commitNow();
        }
        int inicial = savedInstanceState != null ? savedInstanceState.getInt(ESTADO_PESTANA, 0) : 0;
        mostrarPestana(inicial);
        TabLayout.Tab tab = binding.tabs.getTabAt(inicial);
        if (tab != null) tab.select();

        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { mostrarPestana(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { subirPestana(tab.getPosition()); }
        });

        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.accion_categorias) {
                startActivity(new Intent(this, CategoriasFinActivity.class));
            } else if (id == R.id.accion_cuentas) {
                startActivity(new Intent(this, CuentasActivity.class));
            } else if (id == R.id.accion_presupuestos) {
                startActivity(new Intent(this, PresupuestosActivity.class));
            } else if (id == R.id.accion_recurrentes) {
                startActivity(new Intent(this, RecurrentesActivity.class));
            } else if (id == R.id.accion_exportar_csv) {
                exportarCsv();
            } else {
                return false;
            }
            return true;
        });

        binding.fabNuevoMovimiento.setOnClickListener(v -> nuevoMovimiento());
        NavegacionInferior.configurar(this, binding.bottomNav, R.id.tab_finanzas,
                () -> subirPestana(binding.tabs.getSelectedTabPosition()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.recargar();
    }

    @Override
    protected void onStop() {
        borrado.confirmarPendiente();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_PESTANA, binding.tabs.getSelectedTabPosition());
    }

    private void mostrarPestana(int posicion) {
        String[] tags = {TAG_RESUMEN, TAG_MOVIMIENTOS, TAG_REPORTES};
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        for (int i = 0; i < tags.length; i++) {
            Fragment f = getSupportFragmentManager().findFragmentByTag(tags[i]);
            if (f == null) continue;
            if (i == posicion) t.show(f); else t.hide(f);
        }
        t.commitAllowingStateLoss();
        // El botón de nuevo movimiento no aparece en Reportes
        if (posicion == 2) binding.fabNuevoMovimiento.hide(); else binding.fabNuevoMovimiento.show();
    }

    private void subirPestana(int posicion) {
        Fragment f = getSupportFragmentManager().findFragmentByTag(
                posicion == 0 ? TAG_RESUMEN : posicion == 1 ? TAG_MOVIMIENTOS : TAG_REPORTES);
        if (f instanceof ResumenFragment) ((ResumenFragment) f).subir();
        else if (f instanceof MovimientosFragment) ((MovimientosFragment) f).subir();
        else if (f instanceof ReportesFragment) ((ReportesFragment) f).subir();
    }

    // ===== Navegación entre pestañas =====

    public void irAMovimientos() {
        TabLayout.Tab tab = binding.tabs.getTabAt(1);
        if (tab != null) tab.select();
    }

    /** Abre Movimientos filtrado por una categoría dentro del periodo del panel. */
    public void verMovimientosDeCategoria(@Nullable Long categoriaId, boolean ingresos, Periodo periodo) {
        FinanzasViewModel.Filtro f = new FinanzasViewModel.Filtro();
        f.tipo = ingresos ? Movimiento.TIPO_INGRESO : Movimiento.TIPO_GASTO;
        f.categoriaId = categoriaId;
        f.desde = periodo.inicio;
        f.hasta = periodo.fin;
        aplicarFiltroYMostrar(f);
    }

    public void verMovimientosDeCuenta(long cuentaId) {
        FinanzasViewModel.Filtro f = new FinanzasViewModel.Filtro();
        f.cuentaId = cuentaId;
        aplicarFiltroYMostrar(f);
    }

    private void aplicarFiltroYMostrar(FinanzasViewModel.Filtro f) {
        viewModel.setFiltro(f);
        Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG_MOVIMIENTOS);
        if (frag instanceof MovimientosFragment) ((MovimientosFragment) frag).sincronizarControles();
        irAMovimientos();
    }

    // ===== Acciones sobre movimientos =====

    public void nuevoMovimiento() {
        startActivity(new Intent(this, NuevoMovimientoActivity.class));
    }

    public void editar(Movimiento m) {
        Intent i = new Intent(this, NuevoMovimientoActivity.class);
        i.putExtra(NuevoMovimientoActivity.EXTRA_ID, m.id);
        startActivity(i);
    }

    public void mostrarAcciones(Movimiento m) {
        CategoriaFin cat = m.categoriaId != null && viewModel.getCategorias().getValue() != null
                ? viewModel.getCategorias().getValue().get(m.categoriaId) : null;
        String subtitulo = UiFinanzas.montoConSigno(m) + " · " + UiFinanzas.fechaCorta(m.fechaEpoch)
                + (cat != null ? " · " + cat.nombre : "");
        HojaAcciones.de(this, m.descripcion)
                .subtitulo(subtitulo)
                .accion(R.drawable.ic_edit, R.string.accion_editar, () -> editar(m))
                .accion(R.drawable.ic_add_circle, R.string.fin_duplicar, () -> {
                    Intent i = new Intent(this, NuevoMovimientoActivity.class);
                    i.putExtra(NuevoMovimientoActivity.EXTRA_DUPLICAR_ID, m.id);
                    startActivity(i);
                })
                .peligrosa(R.drawable.ic_delete, R.string.accion_eliminar, () -> eliminarConDeshacer(m))
                .mostrar();
    }

    /** Oculta el movimiento y lo borra al cerrarse el aviso, salvo que se pulse "Deshacer". */
    public void eliminarConDeshacer(Movimiento m) {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG_MOVIMIENTOS);
        borrado.eliminar(binding.getRoot(), binding.fabNuevoMovimiento,
                getString(R.string.fin_movimiento_eliminado),
                () -> { if (frag instanceof MovimientosFragment) ((MovimientosFragment) frag).quitar(m); },
                viewModel::cargarMovimientos,
                () -> {
                    viewModel.eliminar(m);
                    viewModel.cargarMovimientos();
                });
    }

    private void exportarCsv() {
        List<Movimiento> visibles = viewModel.getMovimientos().getValue();
        if (visibles == null || visibles.isEmpty()) {
            Toast.makeText(this, R.string.exportar_csv_vacio, Toast.LENGTH_SHORT).show();
            return;
        }
        ExportadorCsv.exportarMovimientos(this, visibles, viewModel.getCategorias().getValue(),
                viewModel.getCuentas().getValue(), "movimientos");
    }
}
