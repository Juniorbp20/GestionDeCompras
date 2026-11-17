package com.example.gestiondecompras.fragments.finanzas;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.activities.CuentasActivity;
import com.example.gestiondecompras.activities.FinanzasActivity;
import com.example.gestiondecompras.activities.PresupuestosActivity;
import com.example.gestiondecompras.activities.RecurrentesActivity;
import com.example.gestiondecompras.databinding.FragmentFinResumenBinding;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.models.finanzas.Recurrente;
import com.example.gestiondecompras.servicios.FinanzasServicio;
import com.example.gestiondecompras.utils.finanzas.CalculosFinancieros;
import com.example.gestiondecompras.utils.finanzas.Periodo;
import com.example.gestiondecompras.utils.finanzas.UiFinanzas;
import com.example.gestiondecompras.utils.finanzas.VisualFinanzas;
import com.example.gestiondecompras.viewmodels.FinanzasViewModel;
import com.example.gestiondecompras.views.GraficoAnilloView;
import com.example.gestiondecompras.views.GraficoBarrasView;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Panel financiero del periodo: indicadores, gráficos, presupuestos, cuentas y movimientos recientes. */
public class ResumenFragment extends Fragment {

    private FragmentFinResumenBinding binding;
    private FinanzasViewModel viewModel;
    /** true = el anillo muestra ingresos por categoría; false = gastos. */
    private boolean mostrandoIngresos = false;
    private FinanzasViewModel.Panel ultimoPanel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFinResumenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(FinanzasViewModel.class);

        binding.btnAnterior.setOnClickListener(v -> viewModel.setPeriodo(viewModel.getPeriodo().mover(-1)));
        binding.btnSiguiente.setOnClickListener(v -> viewModel.setPeriodo(viewModel.getPeriodo().mover(1)));
        binding.grupoPeriodo.setOnCheckedStateChangeListener((grupo, ids) -> {
            Periodo.Tipo tipo = Periodo.Tipo.MES;
            if (ids.contains(R.id.chipTrimestre)) tipo = Periodo.Tipo.TRIMESTRE;
            else if (ids.contains(R.id.chipAnio)) tipo = Periodo.Tipo.ANIO;
            if (viewModel.getPeriodo().tipo != tipo) {
                viewModel.setPeriodo(Periodo.de(tipo, System.currentTimeMillis()));
            }
        });
        binding.btnAlternarTipo.setOnClickListener(v -> {
            mostrandoIngresos = !mostrandoIngresos;
            if (ultimoPanel != null) mostrarCategorias(ultimoPanel);
        });
        binding.btnConfigPresupuestos.setOnClickListener(v -> startActivity(new Intent(requireContext(), PresupuestosActivity.class)));
        binding.btnConfigCuentas.setOnClickListener(v -> startActivity(new Intent(requireContext(), CuentasActivity.class)));
        binding.btnConfigRecurrentes.setOnClickListener(v -> startActivity(new Intent(requireContext(), RecurrentesActivity.class)));
        binding.btnVerTodos.setOnClickListener(v -> ((FinanzasActivity) requireActivity()).irAMovimientos());

        viewModel.getPanel().observe(getViewLifecycleOwner(), this::mostrar);
        viewModel.getCategorias().observe(getViewLifecycleOwner(), c -> { if (ultimoPanel != null) mostrar(ultimoPanel); });
    }

    /** Sube el panel al principio (al volver a pulsar la pestaña). */
    public void subir() {
        if (binding != null) binding.scrollResumen.smoothScrollTo(0, 0);
    }

    private void mostrar(FinanzasViewModel.Panel p) {
        if (p == null || binding == null) return;
        ultimoPanel = p;
        FinanzasServicio.ResumenPeriodo r = p.resumen;

        binding.tvPeriodo.setText(p.periodo.etiqueta());
        binding.tvBalance.setText(UiFinanzas.monedaConSigno(r.balance));
        binding.tvIngresos.setText(UiFinanzas.moneda(r.ingresos));
        binding.tvGastos.setText(UiFinanzas.moneda(r.gastos));
        binding.tvAhorro.setText(r.ingresos > 0 ? UiFinanzas.porcentaje(r.tasaAhorro) + "%" : "—");

        if (r.variacionGastos == null) {
            binding.tvVariacion.setText(R.string.fin_variacion_sin_datos);
        } else if (Math.abs(r.variacionGastos) < 0.05) {
            binding.tvVariacion.setText(R.string.fin_variacion_igual);
        } else {
            String pct = UiFinanzas.porcentaje(Math.abs(r.variacionGastos));
            String base = getString(r.variacionGastos > 0 ? R.string.fin_variacion_mas : R.string.fin_variacion_menos, pct);
            binding.tvVariacion.setText(getString(R.string.fin_gastos) + ": " + base);
        }
        binding.tvPromedio.setText(getString(R.string.fin_promedio_diario, UiFinanzas.moneda(r.promedioDiarioGasto)));

        mostrarCategorias(p);
        mostrarEvolucion(p.evolucion);
        mostrarPresupuestos(p.presupuestos);
        mostrarSaldos(p.saldos);
        mostrarProximos(p.proximos);
        mostrarRecientes(p.recientes);
    }

    private void mostrarCategorias(FinanzasViewModel.Panel p) {
        List<FinanzasServicio.TotalCategoria> datos = mostrandoIngresos
                ? p.resumen.ingresosPorCategoria : p.resumen.gastosPorCategoria;
        double total = mostrandoIngresos ? p.resumen.ingresos : p.resumen.gastos;

        binding.tvTituloCategorias.setText(mostrandoIngresos ? R.string.fin_ingresos_por_categoria : R.string.fin_gastos_por_categoria);
        binding.btnAlternarTipo.setText(mostrandoIngresos ? R.string.fin_ver_gastos : R.string.fin_ver_ingresos);

        List<GraficoAnilloView.Segmento> segmentos = new ArrayList<>();
        binding.layoutLeyenda.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (FinanzasServicio.TotalCategoria t : datos) {
            int color = UiFinanzas.colorCategoria(requireContext(), t.categoria);
            segmentos.add(new GraficoAnilloView.Segmento(t.total, color));

            View fila = inflater.inflate(R.layout.item_fin_leyenda, binding.layoutLeyenda, false);
            fila.findViewById(R.id.vPunto).setBackgroundTintList(ColorStateList.valueOf(color));
            ((TextView) fila.findViewById(R.id.tvNombre)).setText(
                    t.categoria != null ? t.categoria.nombre : getString(R.string.fin_sin_categoria));
            ((TextView) fila.findViewById(R.id.tvPorcentaje)).setText(UiFinanzas.porcentaje(t.porcentaje) + "%");
            ((TextView) fila.findViewById(R.id.tvMonto)).setText(UiFinanzas.moneda(t.total));
            final Long categoriaId = t.categoria != null ? t.categoria.id : null;
            fila.setOnClickListener(v -> ((FinanzasActivity) requireActivity())
                    .verMovimientosDeCategoria(categoriaId, mostrandoIngresos, p.periodo));
            binding.layoutLeyenda.addView(fila);
        }
        binding.graficoAnillo.setDatos(segmentos, getString(R.string.fin_total), UiFinanzas.moneda(total));
        binding.tvSinCategorias.setVisibility(datos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void mostrarEvolucion(List<FinanzasServicio.PuntoMes> puntos) {
        String[] meses = new DateFormatSymbols(UiFinanzas.ES).getShortMonths();
        List<GraficoBarrasView.Grupo> grupos = new ArrayList<>();
        for (FinanzasServicio.PuntoMes p : puntos) {
            String etiqueta = meses[p.mes].replace(".", "");
            etiqueta = etiqueta.substring(0, 1).toUpperCase(UiFinanzas.ES) + etiqueta.substring(1);
            grupos.add(new GraficoBarrasView.Grupo(etiqueta, p.ingresos, p.gastos));
        }
        binding.graficoBarras.setDatos(grupos);
    }

    private void mostrarPresupuestos(List<FinanzasServicio.EstadoPresupuesto> lista) {
        binding.layoutPresupuestos.removeAllViews();
        binding.tvSinPresupuestos.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (FinanzasServicio.EstadoPresupuesto e : lista) {
            View fila = inflater.inflate(R.layout.item_fin_presupuesto, binding.layoutPresupuestos, false);
            UiFinanzas.pintarIcono(fila.findViewById(R.id.ivIcono), e.categoria);
            ((TextView) fila.findViewById(R.id.tvNombre)).setText(e.categoria.nombre);
            ((TextView) fila.findViewById(R.id.tvDetalle)).setText(getString(R.string.fin_presupuesto_detalle,
                    UiFinanzas.moneda(e.gastado), UiFinanzas.moneda(e.limite), UiFinanzas.porcentaje(e.porcentaje)));
            ProgressBar barra = fila.findViewById(R.id.pbProgreso);
            barra.setProgress((int) Math.min(100, Math.round(e.porcentaje)));
            int color = e.estado == CalculosFinancieros.EstadoPresupuesto.EXCEDIDO ? R.color.error_color
                    : e.estado == CalculosFinancieros.EstadoPresupuesto.ALERTA ? R.color.warning_color
                    : R.color.success_color;
            barra.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color)));
            fila.setOnClickListener(v -> startActivity(new Intent(requireContext(), PresupuestosActivity.class)));
            binding.layoutPresupuestos.addView(fila);
        }
    }

    private void mostrarSaldos(List<FinanzasServicio.SaldoCuenta> saldos) {
        binding.layoutCuentas.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (FinanzasServicio.SaldoCuenta s : saldos) {
            View fila = inflater.inflate(R.layout.item_fin_saldo, binding.layoutCuentas, false);
            ((ImageView) fila.findViewById(R.id.ivIcono)).setImageResource(VisualFinanzas.iconoCuenta(s.cuenta.tipo));
            ((TextView) fila.findViewById(R.id.tvNombre)).setText(s.cuenta.nombre);
            TextView monto = fila.findViewById(R.id.tvMonto);
            monto.setText(UiFinanzas.moneda(s.saldo));
            monto.setTextColor(ContextCompat.getColor(requireContext(), s.saldo < 0 ? R.color.error_color : R.color.gray_dark));
            fila.setOnClickListener(v -> ((FinanzasActivity) requireActivity()).verMovimientosDeCuenta(s.cuenta.id));
            binding.layoutCuentas.addView(fila);
        }
    }

    private void mostrarProximos(List<Recurrente> proximos) {
        binding.layoutProximos.removeAllViews();
        binding.tvSinProximos.setVisibility(proximos.isEmpty() ? View.VISIBLE : View.GONE);
        Map<Long, CategoriaFin> categorias = viewModel.getCategorias().getValue();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Recurrente r : proximos) {
            View fila = inflater.inflate(R.layout.item_movimiento, binding.layoutProximos, false);
            Movimiento vista = new Movimiento();
            vista.tipo = r.tipo;
            vista.monto = r.monto;
            vista.descripcion = r.descripcion;
            vista.categoriaId = r.categoriaId;
            vista.cuentaId = r.cuentaId;
            vista.fechaEpoch = r.proximaFechaEpoch;
            UiFinanzas.enlazarMovimiento(fila, vista, categorias, viewModel.getCuentas().getValue(), true);
            fila.findViewById(R.id.btnMas).setVisibility(View.GONE);
            fila.setPadding(0, fila.getPaddingTop(), 0, fila.getPaddingBottom());
            fila.setOnClickListener(v -> startActivity(new Intent(requireContext(), RecurrentesActivity.class)));
            binding.layoutProximos.addView(fila);
        }
    }

    private void mostrarRecientes(List<Movimiento> recientes) {
        binding.layoutRecientes.removeAllViews();
        binding.tvSinRecientes.setVisibility(recientes.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Movimiento m : recientes) {
            View fila = inflater.inflate(R.layout.item_movimiento, binding.layoutRecientes, false);
            UiFinanzas.enlazarMovimiento(fila, m, viewModel.getCategorias().getValue(), viewModel.getCuentas().getValue(), true);
            fila.setPadding(0, fila.getPaddingTop(), 0, fila.getPaddingBottom());
            fila.setOnClickListener(v -> ((FinanzasActivity) requireActivity()).editar(m));
            fila.findViewById(R.id.btnMas).setOnClickListener(v -> ((FinanzasActivity) requireActivity()).mostrarAcciones(m));
            binding.layoutRecientes.addView(fila);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
