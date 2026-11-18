package com.example.gestiondecompras.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.adapters.PedidosAdapter;
import com.example.gestiondecompras.databinding.ActivityReportesBinding;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.viewmodels.ReportesViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportesActivity extends AppCompatActivity {

    private ActivityReportesBinding binding;
    private ReportesViewModel viewModel;
    private PedidosAdapter porCobrarAdapter;
    private PedidosAdapter pagadosAdapter;
    private List<Pedido> porCobrarActual = Collections.emptyList();
    private List<Pedido> pagadosActual = Collections.emptyList();

    private Calendar fechaDesde = null;
    private Calendar fechaHasta = null;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private double totalCobradoVal = 0;
    private double totalPendienteVal = 0;
    private double totalVentasVal = 0;
    private double totalGananciaVal = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ReportesViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setTitle(R.string.reportes_title);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupRecyclerViews();
        setupFilters();
        observeViewModel();
        binding.btnExportPagados.setOnClickListener(v -> exportReporteCompleto());
        binding.btnExportCsv.setOnClickListener(v -> {
            java.util.List<Pedido> todos = new java.util.ArrayList<>(porCobrarActual);
            todos.addAll(pagadosActual);
            if (todos.isEmpty()) {
                Toast.makeText(this, R.string.exportar_csv_vacio, Toast.LENGTH_SHORT).show();
                return;
            }
            com.example.gestiondecompras.utils.ExportadorCsv.exportarPedidos(this, todos, "reporte_pedidos");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        recargarReportes();
    }

    private void setupRecyclerViews() {
        porCobrarAdapter = new PedidosAdapter(null);
        binding.rvReportePedidos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReportePedidos.setAdapter(porCobrarAdapter);
        binding.rvReportePedidos.setClickable(false);
        binding.rvReportePedidos.setFocusable(false);

        pagadosAdapter = new PedidosAdapter(null);
        binding.rvPagados.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPagados.setAdapter(pagadosAdapter);
        binding.rvPagados.setClickable(false);
        binding.rvPagados.setFocusable(false);
    }

    private void setupFilters() {
        binding.etBuscarCliente.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { recargarReportes(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        binding.etFechaDesde.setOnClickListener(v -> pickDate(true));
        binding.btnFechaDesde.setEndIconOnClickListener(v -> pickDate(true));
        binding.etFechaDesde.setOnLongClickListener(v -> {
            limpiarFecha(true);
            return true;
        });

        binding.etFechaHasta.setOnClickListener(v -> pickDate(false));
        binding.btnFechaHasta.setEndIconOnClickListener(v -> pickDate(false));
        binding.etFechaHasta.setOnLongClickListener(v -> {
            limpiarFecha(false);
            return true;
        });
    }

    private void pickDate(boolean esDesde) {
        Calendar base = esDesde
                ? (fechaDesde != null ? fechaDesde : Calendar.getInstance())
                : (fechaHasta != null ? fechaHasta : Calendar.getInstance());
        new android.app.DatePickerDialog(this, (picker, y, m, d) -> {
            Calendar c = Calendar.getInstance();
            c.set(y, m, d, 0, 0, 0);
            if (esDesde) fechaDesde = c; else fechaHasta = c;
            binding.etFechaDesde.setText(fechaDesde != null ? sdf.format(fechaDesde.getTime()) : "");
            binding.etFechaHasta.setText(fechaHasta != null ? sdf.format(fechaHasta.getTime()) : "");
            recargarReportes();
        }, base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void limpiarFecha(boolean esDesde) {
        if (esDesde) fechaDesde = null; else fechaHasta = null;
        binding.etFechaDesde.setText(fechaDesde != null ? sdf.format(fechaDesde.getTime()) : "");
        binding.etFechaHasta.setText(fechaHasta != null ? sdf.format(fechaHasta.getTime()) : "");
        recargarReportes();
    }

    private void recargarReportes() {
        String busqueda = binding.etBuscarCliente.getText() != null
                ? binding.etBuscarCliente.getText().toString().trim() : "";
        Long desde = fechaDesde != null ? fechaDesde.getTimeInMillis() : null;
        Long hasta = fechaHasta != null ? fechaHasta.getTimeInMillis() : null;
        viewModel.loadReportes(desde, hasta, busqueda);
    }

    private void observeViewModel() {
        viewModel.getTotalCobrado().observe(this, total -> {
            if (total != null) {
                totalCobradoVal = total;
                binding.tvTotalCobrado.setText(formatoMoneda(total));
            }
        });

        viewModel.getTotalPendiente().observe(this, total -> {
            if (total != null) {
                totalPendienteVal = total;
                binding.tvTotalPendiente.setText(formatoMoneda(total));
            }
        });

        viewModel.getVentasGeneradas().observe(this, total -> {
            if (total != null) {
                totalVentasVal = total;
                binding.tvTotalVentas.setText(formatoMoneda(total));
            }
        });

        viewModel.getGananciaProyectada().observe(this, total -> {
            if (total != null) {
                totalGananciaVal = total;
                binding.tvTotalGanancia.setText(formatoMoneda(total));
            }
        });

        viewModel.getPedidos().observe(this, pedidos -> {
            porCobrarActual = pedidos != null ? pedidos : Collections.emptyList();
            porCobrarAdapter.actualizarLista(porCobrarActual);
            binding.tvPorPagarVacio.setVisibility(porCobrarActual.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getPedidosPagados().observe(this, pagados -> {
            pagadosActual = pagados != null ? pagados : Collections.emptyList();
            pagadosAdapter.actualizarLista(pagadosActual);
            binding.tvPagadosVacio.setVisibility(pagadosActual.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private String formatoMoneda(double valor) {
        return String.format(Locale.getDefault(), "RD$ %,.2f", valor);
    }

    // ================== EXPORTAR PDF ==================

    private static final int PAGE_W = 595;
    private static final int PAGE_H = 842;
    private static final int MARGIN = 40;
    private static final int CONTENT_BOTTOM = PAGE_H - 60;
    private static final int COLOR_HEADER = Color.rgb(33, 150, 243);
    private static final int COLOR_DARK = Color.rgb(55, 71, 79);
    private static final int COLOR_GRAY = Color.rgb(117, 117, 117);
    private static final int COLOR_DIVIDER = Color.rgb(224, 229, 236);
    private static final int COLOR_GREEN = Color.rgb(76, 175, 80);
    private static final int COLOR_BLUE = Color.rgb(33, 150, 243);
    private static final int COLOR_ORANGE = Color.rgb(255, 152, 0);
    private static final int COLOR_EN_CAMINO = Color.rgb(107, 79, 168);
    private static final int COLOR_RED = Color.rgb(244, 67, 54);
    private static final int COLOR_LIGHT_BG = Color.rgb(243, 247, 252);

    private PdfDocument doc;
    private Paint paint;
    private Canvas canvas;
    private int y;
    private int pageNumber;
    private PdfDocument.Page currentPage;

    private void exportReporteCompleto() {
        if (porCobrarActual.isEmpty() && pagadosActual.isEmpty()) {
            Toast.makeText(this, R.string.reportes_pdf_error, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            doc = new PdfDocument();
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            pageNumber = 0;

            newPage();

            // --- ENCABEZADO TITULAR ---
            canvas.drawRect(0, 0, PAGE_W, 78, paintFill(COLOR_HEADER));
            paint.setTextSize(19f);
            paint.setColor(Color.WHITE);
            paint.setFakeBoldText(true);
            canvas.drawText(getString(R.string.app_name), MARGIN, 32, paint);
            paint.setTextSize(12f);
            paint.setFakeBoldText(false);
            canvas.drawText("Reporte de Pedidos", MARGIN, 52, paint);
            canvas.drawText("Generado: " + sdf.format(new Date()), MARGIN, 68, paint);
            y = 78 + 24;

            // --- SECCION POR PAGAR ---
            if (!porCobrarActual.isEmpty()) {
                drawSectionHeader("PEDIDOS POR COBRAR", COLOR_BLUE, porCobrarActual.size());
                for (Pedido p : porCobrarActual) {
                    ensureSpace(100);
                    drawPedido(p, false);
                }
            }

            // --- SECCION PAGADOS ---
            if (!pagadosActual.isEmpty()) {
                drawSectionHeader("PEDIDOS PAGADOS", COLOR_GREEN, pagadosActual.size());
                for (Pedido p : pagadosActual) {
                    ensureSpace(100);
                    drawPedido(p, true);
                }
            }

            // --- RESUMEN ---
            ensureSpace(110);
            drawResumen();

            doc.finishPage(currentPage);

            File dir = new File(getCacheDir(), "pdfs");
            if (!dir.exists() && !dir.mkdirs()) {
                Toast.makeText(this, R.string.reportes_pdf_error, Toast.LENGTH_SHORT).show();
                return;
            }
            String fileName = "reporte_pedidos_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
            File file = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            doc.writeTo(fos);
            fos.close();
            abrirPdf(file);
        } catch (IOException e) {
            Toast.makeText(this, R.string.reportes_pdf_error, Toast.LENGTH_SHORT).show();
        } finally {
            if (doc != null) doc.close();
        }
    }

    private void abrirPdf(File file) {
        try {
            android.net.Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.reportes_pdf_abrir)));
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, R.string.reportes_pdf_open_error, Toast.LENGTH_LONG).show();
        }
    }

    private void newPage() {
        pageNumber++;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create();
        currentPage = doc.startPage(pageInfo);
        canvas = currentPage.getCanvas();
        y = MARGIN;
    }

    private void ensureSpace(int needed) {
        if (y + needed > CONTENT_BOTTOM) {
            doc.finishPage(currentPage);
            newPage();
        }
    }

    private void drawSectionHeader(String title, int color, int count) {
        canvas.drawRoundRect(new RectF(MARGIN, y, PAGE_W - MARGIN, y + 28), 14, 14, paintFill(color));
        paint.setTextSize(12f);
        paint.setColor(Color.WHITE);
        paint.setFakeBoldText(true);
        canvas.drawText(title + "  (" + count + ")", MARGIN + 12, y + 19, paint);
        paint.setFakeBoldText(false);
        y += 42;
    }

    private void drawPedido(Pedido p, boolean pagado) {
        int rowBottom = y + 84;

        canvas.drawRoundRect(new RectF(MARGIN, y, PAGE_W - MARGIN, rowBottom), 10, 10, paintFill(COLOR_LIGHT_BG));

        // Nombre del cliente + chip estado
        paint.setTextSize(12f);
        paint.setFakeBoldText(true);
        paint.setColor(COLOR_DARK);
        canvas.drawText("Cliente: " + (p.getClienteNombre() != null ? p.getClienteNombre() : "—"), MARGIN + 12, y + 20, paint);
        paint.setFakeBoldText(false);

        String estado = p.getEstado() != null ? p.getEstado().toUpperCase() : "";
        int chipColor = estadoColor(estado);
        float chipW = paint.measureText(estado) + 18;
        RectF chip = new RectF(PAGE_W - MARGIN - chipW - 12, y + 8, PAGE_W - MARGIN - 12, y + 26);
        canvas.drawRoundRect(chip, 9, 9, paintFill(chipColor));
        paint.setTextSize(9f);
        paint.setColor(Color.WHITE);
        paint.setFakeBoldText(true);
        float tx = chip.centerX() - paint.measureText(estado) / 2f;
        canvas.drawText(estado, tx, y + 19, paint);
        paint.setFakeBoldText(false);

        // Tienda
        paint.setTextSize(10f);
        paint.setColor(COLOR_GRAY);
        canvas.drawText("Tienda: " + (p.getTienda() != null ? p.getTienda() : "—"), MARGIN + 12, y + 36, paint);

        // Fechas
        String reg = p.getFechaRegistro() != null ? sdf.format(p.getFechaRegistro()) : "—";
        String ent = p.getFechaEntrega() != null ? sdf.format(p.getFechaEntrega()) : "—";
        canvas.drawText("Registro: " + reg + "     Entrega: " + ent, MARGIN + 12, y + 51, paint);

        // Montos: compra/ganancia izquierda, total derecha
        canvas.drawText("Compra: " + moneda(p.getMontoCompra()) + "   Ganancia: " + moneda(p.getGanancia()),
                MARGIN + 12, y + 66, paint);
        paint.setColor(pagado ? COLOR_GREEN : COLOR_BLUE);
        paint.setFakeBoldText(true);
        String totalTxt = "Total: " + moneda(p.getTotalGeneral());
        canvas.drawText(totalTxt, PAGE_W - MARGIN - 12 - paint.measureText(totalTxt), y + 66, paint);
        paint.setFakeBoldText(false);
        paint.setColor(COLOR_GRAY);

        canvas.drawText("Abonado: " + moneda(p.getTotalAbonado()) + "     Saldo: " + moneda(p.getSaldo()),
                MARGIN + 12, y + 80, paint);

        y = rowBottom + 10;

        // Divisor
        canvas.drawLine(MARGIN, y - 5, PAGE_W - MARGIN, y - 5, paintStroke(COLOR_DIVIDER, 1f));
    }

    private void drawResumen() {
        canvas.drawRoundRect(new RectF(MARGIN, y, PAGE_W - MARGIN, y + 96), 12, 12, paintFill(COLOR_LIGHT_BG));
        paint.setTextSize(13f);
        paint.setColor(COLOR_DARK);
        paint.setFakeBoldText(true);
        canvas.drawText("RESUMEN", MARGIN + 14, y + 24, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(11f);

        int colX = MARGIN + 14;
        int row = y + 46;

        paint.setColor(COLOR_GRAY);
        canvas.drawText("Total cobrado:", colX, row, paint);
        paint.setColor(COLOR_GREEN);
        paint.setFakeBoldText(true);
        String val1 = moneda(totalCobradoVal);
        canvas.drawText(val1, PAGE_W - MARGIN - 14 - paint.measureText(val1), row, paint);
        paint.setFakeBoldText(false);

        row += 18;
        paint.setColor(COLOR_GRAY);
        canvas.drawText("Total por cobrar:", colX, row, paint);
        paint.setColor(COLOR_BLUE);
        paint.setFakeBoldText(true);
        String val2 = moneda(totalPendienteVal);
        canvas.drawText(val2, PAGE_W - MARGIN - 14 - paint.measureText(val2), row, paint);
        paint.setFakeBoldText(false);

        row += 18;
        paint.setColor(COLOR_GRAY);
        canvas.drawText("Ventas generadas:", colX, row, paint);
        paint.setColor(COLOR_DARK);
        paint.setFakeBoldText(true);
        String val3 = moneda(totalVentasVal);
        canvas.drawText(val3, PAGE_W - MARGIN - 14 - paint.measureText(val3), row, paint);
        paint.setFakeBoldText(false);

        row += 18;
        paint.setColor(COLOR_GRAY);
        canvas.drawText("Ganancia:", colX, row, paint);
        paint.setColor(COLOR_GREEN);
        paint.setFakeBoldText(true);
        String val4 = moneda(totalGananciaVal);
        canvas.drawText(val4, PAGE_W - MARGIN - 14 - paint.measureText(val4), row, paint);
        paint.setFakeBoldText(false);

        y = y + 96 + 10;
    }

    private int estadoColor(String estado) {
        if (Pedido.ESTADO_PAGADO.equalsIgnoreCase(estado)) return COLOR_GREEN;
        if (Pedido.ESTADO_ENTREGADO.equalsIgnoreCase(estado)) return COLOR_BLUE;
        if (Pedido.ESTADO_CANCELADO.equalsIgnoreCase(estado)) return COLOR_RED;
        if (Pedido.ESTADO_EN_CAMINO.equalsIgnoreCase(estado)) return COLOR_EN_CAMINO;
        return COLOR_ORANGE;
    }

    private String moneda(double v) {
        return String.format(Locale.getDefault(), "RD$ %,.2f", v);
    }

    private Paint paintFill(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        return p;
    }

    private Paint paintStroke(int color, float width) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(color);
        p.setStrokeWidth(width);
        return p;
    }
}