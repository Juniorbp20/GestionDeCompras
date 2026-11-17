package com.example.gestiondecompras.fragments.finanzas;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.activities.FinanzasActivity;
import com.example.gestiondecompras.adapters.MovimientosAdapter;
import com.example.gestiondecompras.databinding.FragmentFinMovimientosBinding;
import com.example.gestiondecompras.databinding.SheetFinFiltrosBinding;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Cuenta;
import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.utils.Dinero;
import com.example.gestiondecompras.utils.finanzas.UiFinanzas;
import com.example.gestiondecompras.viewmodels.FinanzasViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Lista de movimientos con búsqueda, filtro por tipo y filtros avanzados. */
public class MovimientosFragment extends Fragment implements MovimientosAdapter.Listener {

    private FragmentFinMovimientosBinding binding;
    private FinanzasViewModel viewModel;
    private MovimientosAdapter adapter;
    private boolean actualizandoUi = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFinMovimientosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(FinanzasViewModel.class);

        adapter = new MovimientosAdapter(this);
        binding.rvMovimientos.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMovimientos.setAdapter(adapter);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                // Las cabeceras de día no se pueden deslizar
                return adapter.getMovimientoAt(vh.getBindingAdapterPosition()) == null ? 0 : super.getSwipeDirs(rv, vh);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                Movimiento m = adapter.getMovimientoAt(vh.getBindingAdapterPosition());
                if (m != null) {
                    ((FinanzasActivity) requireActivity()).eliminarConDeshacer(m);
                } else {
                    adapter.notifyItemChanged(vh.getBindingAdapterPosition());
                }
            }
        }).attachToRecyclerView(binding.rvMovimientos);

        binding.etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (actualizandoUi) return;
                FinanzasViewModel.Filtro f = viewModel.getFiltro();
                f.texto = s.toString();
                viewModel.setFiltro(f);
            }
        });
        binding.grupoTipo.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (actualizandoUi) return;
            FinanzasViewModel.Filtro f = viewModel.getFiltro();
            f.tipo = ids.contains(R.id.chipIngresos) ? Movimiento.TIPO_INGRESO
                    : ids.contains(R.id.chipGastos) ? Movimiento.TIPO_GASTO : "";
            viewModel.setFiltro(f);
        });
        binding.btnFiltros.setOnClickListener(v -> mostrarFiltros());
        binding.btnLimpiar.setOnClickListener(v -> {
            viewModel.setFiltro(new FinanzasViewModel.Filtro());
            sincronizarControles();
        });
        binding.btnVacioAccion.setOnClickListener(v -> ((FinanzasActivity) requireActivity()).nuevoMovimiento());

        viewModel.getMovimientos().observe(getViewLifecycleOwner(), this::mostrar);
        viewModel.getCategorias().observe(getViewLifecycleOwner(),
                c -> adapter.setCatalogos(c, viewModel.getCuentas().getValue()));
        viewModel.getCuentas().observe(getViewLifecycleOwner(),
                c -> adapter.setCatalogos(viewModel.getCategorias().getValue(), c));
        sincronizarControles();
    }

    /** Refleja en los controles el filtro actual (p. ej. al llegar desde el Resumen). */
    public void sincronizarControles() {
        if (binding == null) return;
        actualizandoUi = true;
        FinanzasViewModel.Filtro f = viewModel.getFiltro();
        if (!f.texto.equals(String.valueOf(binding.etBuscar.getText()))) {
            binding.etBuscar.setText(f.texto);
        }
        binding.grupoTipo.check(Movimiento.TIPO_INGRESO.equals(f.tipo) ? R.id.chipIngresos
                : Movimiento.TIPO_GASTO.equals(f.tipo) ? R.id.chipGastos : R.id.chipTodos);
        actualizandoUi = false;
    }

    public void subir() {
        if (binding != null) binding.rvMovimientos.smoothScrollToPosition(0);
    }

    public void quitar(Movimiento m) {
        adapter.quitar(m);
        actualizarVacio();
    }

    private void mostrar(List<Movimiento> lista) {
        if (binding == null) return;
        List<Movimiento> datos = lista != null ? lista : Collections.emptyList();
        adapter.setMovimientos(datos);

        double ingresos = 0, gastos = 0;
        for (Movimiento m : datos) {
            if (m.esIngreso()) ingresos = Dinero.sumar(ingresos, m.monto);
            else gastos = Dinero.sumar(gastos, m.monto);
        }
        binding.tvResumenFiltro.setText(getString(R.string.fin_resumen_filtro, datos.size(),
                UiFinanzas.moneda(ingresos), UiFinanzas.moneda(gastos)));
        FinanzasViewModel.Filtro f = viewModel.getFiltro();
        boolean hayFiltros = f.tieneFiltrosAvanzados() || !f.tipo.isEmpty() || !f.texto.trim().isEmpty();
        binding.btnLimpiar.setVisibility(hayFiltros ? View.VISIBLE : View.GONE);
        binding.tvVacio.setText(hayFiltros ? R.string.fin_sin_resultados : R.string.fin_sin_movimientos);
        binding.btnFiltros.setIconTintResource(f.tieneFiltrosAvanzados() ? R.color.accent_gold : R.color.gray_dark);
        actualizarVacio();
    }

    private void actualizarVacio() {
        boolean vacio = adapter.getCantidadMovimientos() == 0;
        binding.emptyState.setVisibility(vacio ? View.VISIBLE : View.GONE);
        binding.rvMovimientos.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    // ===== Filtros avanzados =====

    private void mostrarFiltros() {
        BottomSheetDialog hoja = new BottomSheetDialog(requireContext());
        SheetFinFiltrosBinding b = SheetFinFiltrosBinding.inflate(getLayoutInflater());
        FinanzasViewModel.Filtro f = viewModel.getFiltro();

        // Categorías del tipo elegido (o todas) y cuentas
        Map<Long, CategoriaFin> mapaCat = viewModel.getCategorias().getValue();
        Map<Long, Cuenta> mapaCuentas = viewModel.getCuentas().getValue();
        List<CategoriaFin> categorias = new ArrayList<>();
        if (mapaCat != null) {
            for (CategoriaFin c : mapaCat.values()) {
                if (f.tipo.isEmpty() || f.tipo.equals(c.tipo)) categorias.add(c);
            }
        }
        categorias.sort((a, c) -> a.nombre.compareToIgnoreCase(c.nombre));
        List<Cuenta> cuentas = mapaCuentas != null ? new ArrayList<>(mapaCuentas.values()) : new ArrayList<>();
        cuentas.sort((a, c) -> a.nombre.compareToIgnoreCase(c.nombre));

        List<String> nombresCat = new ArrayList<>();
        nombresCat.add(getString(R.string.fin_cualquiera));
        for (CategoriaFin c : categorias) nombresCat.add(c.nombre);
        List<String> nombresCuentas = new ArrayList<>();
        nombresCuentas.add(getString(R.string.fin_cualquiera));
        for (Cuenta c : cuentas) nombresCuentas.add(c.nombre);

        b.spCategoria.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, nombresCat));
        b.spCuenta.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, nombresCuentas));
        final int[] indiceCat = {0};
        final int[] indiceCuenta = {0};
        for (int i = 0; i < categorias.size(); i++) if (f.categoriaId != null && categorias.get(i).id == f.categoriaId) indiceCat[0] = i + 1;
        for (int i = 0; i < cuentas.size(); i++) if (f.cuentaId != null && cuentas.get(i).id == f.cuentaId) indiceCuenta[0] = i + 1;
        b.spCategoria.setText(nombresCat.get(indiceCat[0]), false);
        b.spCuenta.setText(nombresCuentas.get(indiceCuenta[0]), false);
        b.spCategoria.setOnItemClickListener((p, v, pos, id) -> indiceCat[0] = pos);
        b.spCuenta.setOnItemClickListener((p, v, pos, id) -> indiceCuenta[0] = pos);

        final Long[] desde = {f.desde};
        final Long[] hasta = {f.hasta};
        if (desde[0] != null) b.etDesde.setText(UiFinanzas.fechaCorta(desde[0]));
        if (hasta[0] != null) b.etHasta.setText(UiFinanzas.fechaCorta(hasta[0]));
        b.etDesde.setOnClickListener(v -> elegirFecha(desde[0], fecha -> {
            desde[0] = inicioDia(fecha);
            b.etDesde.setText(UiFinanzas.fechaCorta(desde[0]));
        }));
        b.etHasta.setOnClickListener(v -> elegirFecha(hasta[0], fecha -> {
            hasta[0] = finDia(fecha);
            b.etHasta.setText(UiFinanzas.fechaCorta(hasta[0]));
        }));
        if (f.montoMin != null) b.etMontoMin.setText(String.valueOf(f.montoMin));
        if (f.montoMax != null) b.etMontoMax.setText(String.valueOf(f.montoMax));

        b.btnLimpiarFiltros.setOnClickListener(v -> {
            FinanzasViewModel.Filtro limpio = viewModel.getFiltro();
            limpio.categoriaId = null; limpio.cuentaId = null; limpio.desde = null;
            limpio.hasta = null; limpio.montoMin = null; limpio.montoMax = null;
            viewModel.setFiltro(limpio);
            hoja.dismiss();
        });
        b.btnAplicarFiltros.setOnClickListener(v -> {
            FinanzasViewModel.Filtro nuevo = viewModel.getFiltro();
            nuevo.categoriaId = indiceCat[0] > 0 ? categorias.get(indiceCat[0] - 1).id : null;
            nuevo.cuentaId = indiceCuenta[0] > 0 ? cuentas.get(indiceCuenta[0] - 1).id : null;
            nuevo.desde = desde[0];
            nuevo.hasta = hasta[0];
            nuevo.montoMin = leerMonto(b.etMontoMin.getText());
            nuevo.montoMax = leerMonto(b.etMontoMax.getText());
            viewModel.setFiltro(nuevo);
            hoja.dismiss();
        });

        hoja.setContentView(b.getRoot());
        hoja.show();
    }

    private interface AlElegirFecha { void elegida(long fecha); }

    private void elegirFecha(@Nullable Long inicial, AlElegirFecha callback) {
        Calendar c = Calendar.getInstance();
        if (inicial != null) c.setTimeInMillis(inicial);
        new DatePickerDialog(requireContext(), (dp, y, mo, d) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(y, mo, d, 12, 0, 0);
            callback.elegida(sel.getTimeInMillis());
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private static long inicioDia(long fecha) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(fecha);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static long finDia(long fecha) {
        return inicioDia(fecha) + 86_400_000L - 1;
    }

    @Nullable
    private static Double leerMonto(@Nullable CharSequence texto) {
        if (texto == null || texto.toString().trim().isEmpty()) return null;
        try {
            return Dinero.redondear(Double.parseDouble(texto.toString().trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void onMovimientoClick(Movimiento m) {
        ((FinanzasActivity) requireActivity()).editar(m);
    }

    @Override
    public void onMovimientoAcciones(Movimiento m) {
        ((FinanzasActivity) requireActivity()).mostrarAcciones(m);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
