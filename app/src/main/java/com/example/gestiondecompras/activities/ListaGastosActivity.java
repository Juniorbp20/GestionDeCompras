package com.example.gestiondecompras.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.adapters.GastosAdapter;
import com.example.gestiondecompras.databinding.ActivityListaGastosBinding;
import com.example.gestiondecompras.models.Gasto;
import com.example.gestiondecompras.models.GastoCategoria;
import com.example.gestiondecompras.models.ResumenFinanciero;
import com.example.gestiondecompras.utils.CategoriaGastoUtils;
import com.example.gestiondecompras.viewmodels.ListaGastosViewModel;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ListaGastosActivity extends AppCompatActivity implements GastosAdapter.OnGastoClickListener {

    public static final String EXTRA_GASTO_ID = "gasto_id";

    private static final Locale LOCALE_ES = new Locale("es", "DO");

    private ActivityListaGastosBinding binding;
    private ListaGastosViewModel viewModel;
    private GastosAdapter adapter;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat sdfMes = new SimpleDateFormat("MMMM yyyy", LOCALE_ES);

    private final Calendar mesMostrado = Calendar.getInstance();
    private Long fechaDesde;
    private Long fechaHasta;
    private boolean rangoPersonalizado = false;
    private String categoriaFiltro = "";
    private List<GastoCategoria> ultimasCategorias = Collections.emptyList();
    private boolean actualizandoFiltros = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListaGastosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ListaGastosViewModel.class);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupRecyclerView();
        setupCategoriasSpinner();
        setupFiltros();
        observeViewModel();
        seleccionarMesActual();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarGastos();
    }

    private void setupRecyclerView() {
        binding.rvGastos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GastosAdapter(this);
        binding.rvGastos.setAdapter(adapter);
    }

    private void setupCategoriasSpinner() {
        String[] opciones = new String[Gasto.CATEGORIAS.length + 1];
        opciones[0] = getString(R.string.finanzas_todas_categorias);
        System.arraycopy(Gasto.CATEGORIAS, 0, opciones, 1, Gasto.CATEGORIAS.length);

        ArrayAdapter<String> adapterCategorias = new ArrayAdapter<>(
                this, R.layout.item_dropdown, opciones);
        binding.spinnerCategoriaFiltro.setAdapter(adapterCategorias);
        binding.spinnerCategoriaFiltro.setText(opciones[0], false);

        binding.spinnerCategoriaFiltro.setOnItemClickListener((parent, view, position, id) -> {
            String seleccionada = position == 0 ? "" : Gasto.CATEGORIAS[position - 1];
            if (!seleccionada.equals(categoriaFiltro)) {
                seleccionarCategoria(seleccionada);
            }
        });
    }

    private void setupFiltros() {
        binding.fabNuevoGasto.setOnClickListener(v ->
                startActivity(new Intent(this, NuevoGastoActivity.class)));

        binding.etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!actualizandoFiltros) {
                    cargarGastos();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etFechaDesde.setOnClickListener(v -> mostrarDatePicker(true));
        binding.etFechaHasta.setOnClickListener(v -> mostrarDatePicker(false));
        binding.etFechaDesde.setOnLongClickListener(v -> {
            fechaDesde = null;
            binding.etFechaDesde.setText("");
            rangoPersonalizado = true;
            actualizarEtiquetaPeriodo();
            cargarGastos();
            return true;
        });
        binding.etFechaHasta.setOnLongClickListener(v -> {
            fechaHasta = null;
            binding.etFechaHasta.setText("");
            rangoPersonalizado = true;
            actualizarEtiquetaPeriodo();
            cargarGastos();
            return true;
        });

        binding.btnToggleFechas.setOnClickListener(v -> {
            boolean mostrar = binding.layoutFechas.getVisibility() != View.VISIBLE;
            binding.layoutFechas.setVisibility(mostrar ? View.VISIBLE : View.GONE);
            if (mostrar) {
                if (binding.etFechaDesde.getText().length() == 0 && fechaDesde != null) {
                    binding.etFechaDesde.setText(sdf.format(new Date(fechaDesde)));
                }
                if (binding.etFechaHasta.getText().length() == 0 && fechaHasta != null) {
                    binding.etFechaHasta.setText(sdf.format(new Date(fechaHasta)));
                }
            }
        });

        binding.btnLimpiarFiltros.setOnClickListener(v -> limpiarFiltros());

        binding.btnMesAnterior.setOnClickListener(v -> cambiarMes(-1));
        binding.btnMesSiguiente.setOnClickListener(v -> cambiarMes(1));
    }

    private void seleccionarMesActual() {
        mesMostrado.setTimeInMillis(System.currentTimeMillis());
        mesMostrado.set(Calendar.DAY_OF_MONTH, 1);
        fechaDesde = inicioDelMes(mesMostrado);
        fechaHasta = finDelMes(mesMostrado);
        rangoPersonalizado = false;
        binding.etFechaDesde.setText("");
        binding.etFechaHasta.setText("");
        actualizarEtiquetaPeriodo();
    }

    private void cambiarMes(int delta) {
        mesMostrado.add(Calendar.MONTH, delta);
        fechaDesde = inicioDelMes(mesMostrado);
        fechaHasta = finDelMes(mesMostrado);
        rangoPersonalizado = false;
        binding.etFechaDesde.setText("");
        binding.etFechaHasta.setText("");
        actualizarEtiquetaPeriodo();
        cargarGastos();
    }

    private void limpiarFiltros() {
        actualizandoFiltros = true;
        binding.etBuscar.setText("");
        categoriaFiltro = "";
        binding.spinnerCategoriaFiltro.setText(getString(R.string.finanzas_todas_categorias), false);
        binding.layoutFechas.setVisibility(View.GONE);
        seleccionarMesActual();
        actualizandoFiltros = false;
        renderCategorias();
        cargarGastos();
    }

    private long inicioDelMes(Calendar base) {
        Calendar cal = (Calendar) base.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long finDelMes(Calendar base) {
        Calendar cal = (Calendar) base.clone();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    private void actualizarEtiquetaPeriodo() {
        if (rangoPersonalizado) {
            if (fechaDesde != null && fechaHasta != null) {
                binding.tvMesActual.setText(sdf.format(new Date(fechaDesde)) + " - "
                        + sdf.format(new Date(fechaHasta)));
            } else if (fechaDesde != null) {
                binding.tvMesActual.setText(getString(R.string.gastos_desde) + ": "
                        + sdf.format(new Date(fechaDesde)));
            } else if (fechaHasta != null) {
                binding.tvMesActual.setText(getString(R.string.gastos_hasta) + ": "
                        + sdf.format(new Date(fechaHasta)));
            } else {
                binding.tvMesActual.setText(R.string.finanzas_todo_el_tiempo);
            }
        } else {
            binding.tvMesActual.setText(capitalizar(sdfMes.format(new Date(inicioDelMes(mesMostrado)))));
        }
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        return texto.substring(0, 1).toUpperCase(LOCALE_ES) + texto.substring(1);
    }

    private void mostrarDatePicker(boolean esDesde) {
        Calendar cal = Calendar.getInstance();
        if (esDesde && fechaDesde != null) {
            cal.setTimeInMillis(fechaDesde);
        } else if (!esDesde && fechaHasta != null) {
            cal.setTimeInMillis(fechaHasta);
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar seleccionada = Calendar.getInstance();
            seleccionada.clear();
            seleccionada.set(year, month, dayOfMonth);

            if (esDesde) {
                seleccionada.set(Calendar.HOUR_OF_DAY, 0);
                seleccionada.set(Calendar.MINUTE, 0);
                seleccionada.set(Calendar.SECOND, 0);
                seleccionada.set(Calendar.MILLISECOND, 0);
                fechaDesde = seleccionada.getTimeInMillis();
                binding.etFechaDesde.setText(sdf.format(new Date(fechaDesde)));

                if (fechaHasta != null && fechaHasta < fechaDesde) {
                    Calendar fin = (Calendar) seleccionada.clone();
                    fin.set(Calendar.HOUR_OF_DAY, 23);
                    fin.set(Calendar.MINUTE, 59);
                    fin.set(Calendar.SECOND, 59);
                    fin.set(Calendar.MILLISECOND, 999);
                    fechaHasta = fin.getTimeInMillis();
                    binding.etFechaHasta.setText(sdf.format(new Date(fechaHasta)));
                }
            } else {
                seleccionada.set(Calendar.HOUR_OF_DAY, 23);
                seleccionada.set(Calendar.MINUTE, 59);
                seleccionada.set(Calendar.SECOND, 59);
                seleccionada.set(Calendar.MILLISECOND, 999);
                fechaHasta = seleccionada.getTimeInMillis();
                binding.etFechaHasta.setText(sdf.format(new Date(fechaHasta)));

                if (fechaDesde != null && fechaDesde > fechaHasta) {
                    Calendar inicio = (Calendar) seleccionada.clone();
                    inicio.set(Calendar.HOUR_OF_DAY, 0);
                    inicio.set(Calendar.MINUTE, 0);
                    inicio.set(Calendar.SECOND, 0);
                    inicio.set(Calendar.MILLISECOND, 0);
                    fechaDesde = inicio.getTimeInMillis();
                    binding.etFechaDesde.setText(sdf.format(new Date(fechaDesde)));
                }
            }

            sincronizarMesConFecha(seleccionada.getTimeInMillis());
            rangoPersonalizado = true;
            actualizarEtiquetaPeriodo();
            cargarGastos();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void sincronizarMesConFecha(long millis) {
        mesMostrado.setTimeInMillis(millis);
        mesMostrado.set(Calendar.DAY_OF_MONTH, 1);
    }

    private void seleccionarCategoria(String categoria) {
        categoriaFiltro = categoria;
        binding.spinnerCategoriaFiltro.setText(categoria.isEmpty()
                ? getString(R.string.finanzas_todas_categorias) : categoria, false);
        renderCategorias();
        cargarGastos();
    }

    private void renderCategorias() {
        binding.layoutCategorias.removeAllViews();

        if (ultimasCategorias == null || ultimasCategorias.isEmpty()) {
            binding.tvCategoriasTitle.setVisibility(View.GONE);
            binding.scrollCategorias.setVisibility(View.GONE);
            binding.tvCategoriasHint.setVisibility(View.GONE);
            return;
        }

        binding.tvCategoriasTitle.setVisibility(View.VISIBLE);
        binding.scrollCategorias.setVisibility(View.VISIBLE);
        binding.tvCategoriasHint.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (GastoCategoria categoria : ultimasCategorias) {
            String nombreCategoria = categoria.categoria != null
                    ? categoria.categoria : Gasto.CAT_OTROS;

            View item = inflater.inflate(R.layout.item_categoria_gasto, binding.layoutCategorias, false);
            MaterialCardView card = (MaterialCardView) item;
            ImageView icono = item.findViewById(R.id.ivCategoriaChip);
            TextView nombre = item.findViewById(R.id.tvCategoriaChip);
            TextView monto = item.findViewById(R.id.tvMontoChip);

            int color = ContextCompat.getColor(this, CategoriaGastoUtils.color(nombreCategoria));
            icono.setImageResource(CategoriaGastoUtils.icono(nombreCategoria));
            icono.setImageTintList(ColorStateList.valueOf(color));
            nombre.setText(nombreCategoria);
            monto.setText(formatoMoneda(categoria.total));

            boolean seleccionada = nombreCategoria.equals(categoriaFiltro);
            card.setStrokeColor(ContextCompat.getColor(this,
                    seleccionada ? R.color.primary_color : R.color.card_stroke));
            card.setStrokeWidth(dp(seleccionada ? 2 : 1));

            item.setOnClickListener(v -> seleccionarCategoria(seleccionada ? "" : nombreCategoria));
            binding.layoutCategorias.addView(item);
        }
    }

    private void cargarGastos() {
        String busqueda = binding.etBuscar.getText() != null
                ? binding.etBuscar.getText().toString().trim() : "";
        viewModel.loadGastos(fechaDesde, fechaHasta, busqueda, categoriaFiltro);
    }

    private void observeViewModel() {
        viewModel.getGastos().observe(this, gastos -> {
            List<Gasto> lista = gastos != null ? gastos : Collections.emptyList();
            adapter.actualizarLista(lista);

            boolean vacio = lista.isEmpty();
            binding.rvGastos.setVisibility(vacio ? View.GONE : View.VISIBLE);
            binding.emptyState.setVisibility(vacio ? View.VISIBLE : View.GONE);
            binding.tvVacio.setText(tieneFiltrosActivos()
                    ? R.string.finanzas_sin_resultados : R.string.finanzas_sin_gastos);

            double totalFiltrado = 0;
            for (Gasto gasto : lista) {
                totalFiltrado += gasto.getMonto();
            }

            if (vacio) {
                binding.tvResumenFiltro.setText(getResources().getQuantityString(
                        R.plurals.finanzas_resumen_filtro, 0, 0, formatoMoneda(0)));
            } else {
                binding.tvResumenFiltro.setText(getResources().getQuantityString(
                        R.plurals.finanzas_resumen_filtro, lista.size(), lista.size(),
                        formatoMoneda(totalFiltrado)));
            }
        });

        viewModel.getResumen().observe(this, this::mostrarResumen);
    }

    private void mostrarResumen(ResumenFinanciero resumen) {
        if (resumen == null) {
            return;
        }

        binding.tvIngresosMes.setText(formatoMoneda(resumen.ingresos));
        binding.tvTotalMes.setText(formatoMoneda(resumen.gastos));
        binding.tvBalanceMes.setText(formatoMoneda(resumen.balance));
        binding.tvBalanceMes.setTextColor(ContextCompat.getColor(this,
                resumen.balance < 0 ? R.color.error_color : R.color.success_color));

        binding.tvCantidadMes.setText(getResources().getQuantityString(
                R.plurals.finanzas_cantidad_periodo,
                resumen.cantidadGastos, resumen.cantidadGastos));

        if (resumen.mostrarPromedio && resumen.gastos > 0) {
            binding.tvPromedioDiario.setVisibility(View.VISIBLE);
            binding.tvPromedioDiario.setText(getString(R.string.finanzas_promedio_diario,
                    formatoMoneda(resumen.promedioDiario)));
        } else {
            binding.tvPromedioDiario.setVisibility(View.GONE);
        }

        mostrarVariacion(resumen);

        ultimasCategorias = resumen.categorias != null
                ? new ArrayList<>(resumen.categorias) : Collections.emptyList();
        renderCategorias();
    }

    private void mostrarVariacion(ResumenFinanciero resumen) {
        if (!resumen.hayPeriodoAnterior || resumen.gastosPeriodoAnterior <= 0) {
            binding.tvVariacionGastos.setVisibility(View.GONE);
            return;
        }

        double variacion = resumen.variacionPorcentual;
        String valor = String.format(Locale.getDefault(), "%.1f%%", Math.abs(variacion));

        int texto;
        int color;
        if (Math.abs(variacion) < 0.05) {
            texto = R.string.finanzas_variacion_igual;
            color = R.color.gray;
            binding.tvVariacionGastos.setText(texto);
        } else if (variacion > 0) {
            texto = R.string.finanzas_variacion_mas;
            color = R.color.error_color;
            binding.tvVariacionGastos.setText(getString(texto, valor));
        } else {
            texto = R.string.finanzas_variacion_menos;
            color = R.color.success_color;
            binding.tvVariacionGastos.setText(getString(texto, valor));
        }

        binding.tvVariacionGastos.setTextColor(ContextCompat.getColor(this, color));
        binding.tvVariacionGastos.setVisibility(View.VISIBLE);
    }

    private boolean tieneFiltrosActivos() {
        String busqueda = binding.etBuscar.getText() != null
                ? binding.etBuscar.getText().toString().trim() : "";
        return !busqueda.isEmpty() || !categoriaFiltro.isEmpty() || rangoPersonalizado;
    }

    private String formatoMoneda(double monto) {
        return String.format(Locale.getDefault(), "RD$ %,.2f", monto);
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onGastoClick(Gasto gasto) {
        mostrarOpcionesGasto(gasto);
    }

    @Override
    public void onGastoLongClick(Gasto gasto) {
        mostrarOpcionesGasto(gasto);
    }

    private void mostrarOpcionesGasto(Gasto gasto) {
        CharSequence[] opciones = {
                getString(R.string.accion_editar),
                getString(R.string.accion_eliminar)
        };

        new AlertDialog.Builder(this)
                .setTitle(gasto.getDescripcion())
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        editarGasto(gasto);
                    } else if (which == 1) {
                        confirmarEliminar(gasto);
                    }
                })
                .setNegativeButton(R.string.accion_cancelar, null)
                .show();
    }

    private void editarGasto(Gasto gasto) {
        Intent intent = new Intent(this, NuevoGastoActivity.class);
        intent.putExtra(EXTRA_GASTO_ID, gasto.getId());
        startActivity(intent);
    }

    private void confirmarEliminar(Gasto gasto) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.gastos_eliminar_title)
                .setMessage(R.string.gastos_eliminar_message)
                .setPositiveButton(R.string.accion_eliminar, (d, w) -> {
                    viewModel.deleteGasto(gasto);
                    Toast.makeText(this, R.string.toast_gasto_eliminado, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.accion_cancelar, null)
                .show();
    }
}
