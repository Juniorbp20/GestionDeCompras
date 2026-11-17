package com.example.gestiondecompras.fragments.finanzas;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.databinding.FragmentFinReportesBinding;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.servicios.FinanzasServicio;
import com.example.gestiondecompras.utils.ExportadorCsv;
import com.example.gestiondecompras.utils.finanzas.Periodo;
import com.example.gestiondecompras.utils.finanzas.ReporteFinancieroPdf;
import com.example.gestiondecompras.utils.finanzas.UiFinanzas;
import com.example.gestiondecompras.viewmodels.FinanzasViewModel;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Reporte financiero de un rango: categorías, cuentas, evolución mensual, mayores gastos y exportación. */
public class ReportesFragment extends Fragment {

    private FragmentFinReportesBinding binding;
    private FinanzasViewModel viewModel;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());

    private long desde;
    private long hasta;
    @Nullable private FinanzasServicio.Reporte ultimo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFinReportesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(FinanzasViewModel.class);
        aplicarRango(Periodo.de(Periodo.Tipo.MES, System.currentTimeMillis()));

        binding.grupoRango.setOnCheckedStateChangeListener((g, ids) -> {
            long ahora = System.currentTimeMillis();
            if (ids.contains(R.id.chipMesAnterior)) aplicarRango(Periodo.de(Periodo.Tipo.MES, ahora).anterior());
            else if (ids.contains(R.id.chipTrimestre)) aplicarRango(Periodo.de(Periodo.Tipo.TRIMESTRE, ahora));
            else if (ids.contains(R.id.chipAnio)) aplicarRango(Periodo.de(Periodo.Tipo.ANIO, ahora));
            else if (ids.contains(R.id.chipPersonalizado)) elegirRangoPersonalizado();
            else aplicarRango(Periodo.de(Periodo.Tipo.MES, ahora));
        });
        binding.chipPersonalizado.setOnClickListener(v -> {
            if (binding.chipPersonalizado.isChecked()) elegirRangoPersonalizado();
        });
        binding.btnExportarPdf.setOnClickListener(v -> {
            if (ultimo == null || ultimo.cantidad == 0) {
                Toast.makeText(requireContext(), R.string.exportar_csv_vacio, Toast.LENGTH_SHORT).show();
                return;
            }
            ReporteFinancieroPdf.generarYAbrir(requireContext(), ultimo, etiquetaRango(),
                    viewModel.getCategorias().getValue());
        });
        binding.btnExportarCsv.setOnClickListener(v -> exportarCsv());

        // Al registrar o borrar movimientos se recalcula el reporte
        viewModel.getPanel().observe(getViewLifecycleOwner(), p -> calcular());
    }

    public void subir() {
        if (binding != null) binding.scrollReportes.smoothScrollTo(0, 0);
    }

    private void aplicarRango(Periodo p) {
        desde = p.inicio;
        hasta = p.fin;
        calcular();
    }

    private void elegirRangoPersonalizado() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(desde);
        new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
            Calendar ini = Calendar.getInstance();
            ini.set(y, m, d, 0, 0, 0);
            ini.set(Calendar.MILLISECOND, 0);
            Calendar fin = Calendar.getInstance();
            fin.setTimeInMillis(Math.max(hasta, ini.getTimeInMillis()));
            new DatePickerDialog(requireContext(), (dp2, y2, m2, d2) -> {
                Calendar f = Calendar.getInstance();
                f.set(y2, m2, d2, 23, 59, 59);
                f.set(Calendar.MILLISECOND, 999);
                desde = ini.getTimeInMillis();
                hasta = Math.max(f.getTimeInMillis(), desde);
                calcular();
            }, fin.get(Calendar.YEAR), fin.get(Calendar.MONTH), fin.get(Calendar.DAY_OF_MONTH)).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String etiquetaRango() {
        return getString(R.string.fin_rep_rango, UiFinanzas.fechaCorta(desde), UiFinanzas.fechaCorta(hasta));
    }

    private void calcular() {
        if (binding == null) return;
        binding.tvRango.setText(etiquetaRango());
        final long d = desde, h = hasta;
        executor.execute(() -> {
            FinanzasServicio.Reporte r = viewModel.getServicio().reporte(d, h);
            principal.post(() -> mostrar(r));
        });
    }

    private void mostrar(FinanzasServicio.Reporte r) {
        if (binding == null || r.desde != desde || r.hasta != hasta) return;
        ultimo = r;
        binding.tvRepIngresos.setText(UiFinanzas.moneda(r.ingresos));
        binding.tvRepGastos.setText(UiFinanzas.moneda(r.gastos));
        binding.tvRepBalance.setText(UiFinanzas.monedaConSigno(r.balance));
        binding.tvRepDetalle.setText(getString(R.string.fin_rep_detalle,
                getResources().getQuantityString(R.plurals.fin_cantidad_movimientos, r.cantidad, r.cantidad),
                UiFinanzas.porcentaje(r.tasaAhorro)));

        boolean vacio = r.cantidad == 0;
        binding.tvRepVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);

        llenarCategorias(binding.layoutGastosCat, r.gastosPorCategoria, r.gastos);
        binding.cardLayoutGastosCat.setVisibility(r.gastosPorCategoria.isEmpty() ? View.GONE : View.VISIBLE);
        llenarCategorias(binding.layoutIngresosCat, r.ingresosPorCategoria, r.ingresos);
        binding.cardLayoutIngresosCat.setVisibility(r.ingresosPorCategoria.isEmpty() ? View.GONE : View.VISIBLE);

        binding.layoutCuentasRep.removeAllViews();
        if (!r.porCuenta.isEmpty()) {
            fila(binding.layoutCuentasRep, "", getString(R.string.fin_rep_col_ingresos),
                    getString(R.string.fin_rep_col_gastos), getString(R.string.fin_rep_col_neto), R.color.gray, true);
            for (FinanzasServicio.TotalCuenta t : r.porCuenta) {
                fila(binding.layoutCuentasRep, t.cuenta != null ? t.cuenta.nombre : getString(R.string.fin_sin_cuenta),
                        corta(t.ingresos), corta(t.gastos), UiFinanzas.monedaConSigno(t.neto),
                        t.neto < 0 ? R.color.error_color : R.color.success_color, false);
            }
        }
        binding.cardLayoutCuentasRep.setVisibility(r.porCuenta.isEmpty() ? View.GONE : View.VISIBLE);

        binding.layoutMeses.removeAllViews();
        if (r.porMes.size() > 1) {
            String[] meses = new DateFormatSymbols(UiFinanzas.ES).getShortMonths();
            fila(binding.layoutMeses, "", getString(R.string.fin_rep_col_ingresos),
                    getString(R.string.fin_rep_col_gastos), getString(R.string.fin_rep_col_neto), R.color.gray, true);
            for (FinanzasServicio.PuntoMes p : r.porMes) {
                String mes = meses[p.mes].replace(".", "");
                mes = mes.substring(0, 1).toUpperCase(UiFinanzas.ES) + mes.substring(1) + " " + p.anio;
                double neto = p.ingresos - p.gastos;
                fila(binding.layoutMeses, mes, corta(p.ingresos), corta(p.gastos), UiFinanzas.monedaConSigno(neto),
                        neto < 0 ? R.color.error_color : R.color.success_color, false);
            }
        }
        binding.cardLayoutMeses.setVisibility(r.porMes.size() > 1 ? View.VISIBLE : View.GONE);

        binding.layoutMayores.removeAllViews();
        Map<Long, CategoriaFin> categorias = viewModel.getCategorias().getValue();
        for (Movimiento m : r.mayoresGastos) {
            CategoriaFin c = m.categoriaId != null && categorias != null ? categorias.get(m.categoriaId) : null;
            fila(binding.layoutMayores, m.descripcion, null, c != null ? c.nombre : "",
                    UiFinanzas.moneda(m.monto), R.color.error_color, false);
        }
        binding.cardLayoutMayores.setVisibility(r.mayoresGastos.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void llenarCategorias(LinearLayout contenedor, List<FinanzasServicio.TotalCategoria> datos, double total) {
        contenedor.removeAllViews();
        for (FinanzasServicio.TotalCategoria t : datos) {
            fila(contenedor, t.categoria != null ? t.categoria.nombre : getString(R.string.fin_sin_categoria),
                    UiFinanzas.porcentaje(t.porcentaje) + "%", String.valueOf(t.cantidad),
                    UiFinanzas.moneda(t.total), R.color.gray_dark, false);
        }
        if (!datos.isEmpty()) {
            fila(contenedor, getString(R.string.fin_total), null, null, UiFinanzas.moneda(total), R.color.primary_color, true);
        }
    }

    /** Importe sin decimales para columnas estrechas. */
    private static String corta(double monto) {
        return String.format(java.util.Locale.getDefault(), "%,.0f", monto);
    }

    private void fila(LinearLayout contenedor, String c1, @Nullable String c2, @Nullable String c3, String c4,
                      int colorC4, boolean cabecera) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.item_fin_tabla, contenedor, false);
        ((TextView) v.findViewById(R.id.tvCol1)).setText(c1);
        ((TextView) v.findViewById(R.id.tvCol2)).setText(c2 != null ? c2 : "");
        ((TextView) v.findViewById(R.id.tvCol3)).setText(c3 != null ? c3 : "");
        TextView col4 = v.findViewById(R.id.tvCol4);
        col4.setText(c4);
        col4.setTextColor(ContextCompat.getColor(requireContext(), colorC4));
        if (cabecera) {
            ((TextView) v.findViewById(R.id.tvCol1)).setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            col4.setTextSize(11);
        }
        contenedor.addView(v);
    }

    private void exportarCsv() {
        final long d = desde, h = hasta;
        final android.content.Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            List<Movimiento> lista = AppDatabase.getInstance(app)
                    .movimientoDao().buscar("", null, null, d, h, null, null, "");
            principal.post(() -> {
                if (!isAdded()) return;
                if (lista.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.exportar_csv_vacio, Toast.LENGTH_SHORT).show();
                    return;
                }
                ExportadorCsv.exportarMovimientos(requireContext(), lista, viewModel.getCategorias().getValue(),
                        viewModel.getCuentas().getValue(), "reporte_financiero");
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
