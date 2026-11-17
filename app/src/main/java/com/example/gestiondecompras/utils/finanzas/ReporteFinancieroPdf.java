package com.example.gestiondecompras.utils.finanzas;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.servicios.FinanzasServicio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Genera el reporte financiero en PDF (A4) y lo abre con el visor del teléfono. */
public final class ReporteFinancieroPdf {

    private static final int ANCHO = 595;
    private static final int ALTO = 842;
    private static final int MARGEN = 40;
    private static final int MARINO = Color.rgb(20, 33, 61);
    private static final int DORADO = Color.rgb(184, 144, 31);
    private static final int GRIS = Color.rgb(107, 112, 128);
    private static final int TEXTO = Color.rgb(26, 31, 46);
    private static final int VERDE = Color.rgb(31, 138, 91);
    private static final int ROJO = Color.rgb(192, 57, 43);
    private static final int LINEA = Color.rgb(236, 231, 220);

    private final Context context;
    private PdfDocument doc;
    private PdfDocument.Page pagina;
    private Canvas canvas;
    private int numeroPagina = 0;
    private float y;
    private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ReporteFinancieroPdf(Context context) {
        this.context = context;
    }

    public static void generarYAbrir(Context context, FinanzasServicio.Reporte r, String etiquetaRango,
                                     Map<Long, CategoriaFin> categorias) {
        new ReporteFinancieroPdf(context).generar(r, etiquetaRango, categorias);
    }

    private void generar(FinanzasServicio.Reporte r, String rango, Map<Long, CategoriaFin> categorias) {
        doc = new PdfDocument();
        try {
            nuevaPagina();
            cabecera(rango);
            resumen(r);
            tabla(context.getString(R.string.fin_gastos_por_categoria), r.gastosPorCategoria, r.gastos);
            tabla(context.getString(R.string.fin_ingresos_por_categoria), r.ingresosPorCategoria, r.ingresos);
            cuentas(r.porCuenta);
            meses(r.porMes);
            mayores(r.mayoresGastos, categorias);
            doc.finishPage(pagina);

            File carpeta = new File(context.getCacheDir(), "pdfs");
            if (!carpeta.exists() && !carpeta.mkdirs()) throw new IOException("carpeta");
            String fecha = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date());
            File archivo = new File(carpeta, "reporte_financiero_" + fecha + ".pdf");
            try (FileOutputStream out = new FileOutputStream(archivo)) {
                doc.writeTo(out);
            }
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", archivo);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.reportes_pdf_abrir)));
        } catch (IOException | IllegalArgumentException e) {
            Toast.makeText(context, R.string.fin_rep_pdf_error, Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.reportes_pdf_open_error, Toast.LENGTH_LONG).show();
        } finally {
            doc.close();
        }
    }

    // ===== Página =====

    private void nuevaPagina() {
        if (pagina != null) doc.finishPage(pagina);
        numeroPagina++;
        pagina = doc.startPage(new PdfDocument.PageInfo.Builder(ANCHO, ALTO, numeroPagina).create());
        canvas = pagina.getCanvas();
        y = MARGEN;
        // Pie con número de página
        estilo(9, GRIS, false);
        texto.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.valueOf(numeroPagina), ANCHO / 2f, ALTO - 20, texto);
        texto.setTextAlign(Paint.Align.LEFT);
    }

    private void asegurarEspacio(float alto) {
        if (y + alto > ALTO - MARGEN) nuevaPagina();
    }

    private void estilo(float tam, int color, boolean negrita) {
        texto.setTextSize(tam);
        texto.setColor(color);
        texto.setTypeface(Typeface.create(Typeface.SANS_SERIF, negrita ? Typeface.BOLD : Typeface.NORMAL));
    }

    private static String moneda(double v) {
        return String.format(Locale.getDefault(), "RD$ %,.2f", v);
    }

    // ===== Secciones =====

    private void cabecera(String rango) {
        relleno.setColor(MARINO);
        canvas.drawRoundRect(new RectF(MARGEN, y, ANCHO - MARGEN, y + 70), 12, 12, relleno);
        relleno.setColor(DORADO);
        canvas.drawRect(MARGEN + 18, y + 52, MARGEN + 58, y + 55, relleno);
        estilo(18, Color.WHITE, true);
        canvas.drawText(context.getString(R.string.fin_rep_pdf_titulo), MARGEN + 18, y + 30, texto);
        estilo(10, Color.rgb(184, 194, 220), false);
        canvas.drawText(rango, MARGEN + 18, y + 46, texto);
        String generado = context.getString(R.string.fin_rep_pdf_generado,
                new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
        texto.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(generado, ANCHO - MARGEN - 18, y + 46, texto);
        texto.setTextAlign(Paint.Align.LEFT);
        y += 90;
    }

    private void resumen(FinanzasServicio.Reporte r) {
        float ancho = (ANCHO - 2 * MARGEN - 20) / 3f;
        String[] etiquetas = {context.getString(R.string.fin_ingresos), context.getString(R.string.fin_gastos),
                context.getString(R.string.fin_rep_balance)};
        String[] valores = {moneda(r.ingresos), moneda(r.gastos), moneda(r.balance)};
        int[] colores = {VERDE, ROJO, r.balance < 0 ? ROJO : MARINO};
        for (int i = 0; i < 3; i++) {
            float x = MARGEN + i * (ancho + 10);
            relleno.setColor(Color.rgb(247, 245, 240));
            canvas.drawRoundRect(new RectF(x, y, x + ancho, y + 56), 10, 10, relleno);
            estilo(9, GRIS, true);
            canvas.drawText(etiquetas[i].toUpperCase(Locale.getDefault()), x + 12, y + 20, texto);
            estilo(14, colores[i], true);
            canvas.drawText(valores[i], x + 12, y + 42, texto);
        }
        y += 70;
        estilo(10, GRIS, false);
        String detalle = context.getResources().getQuantityString(R.plurals.fin_cantidad_movimientos, r.cantidad, r.cantidad)
                + "  ·  " + context.getString(R.string.fin_tasa_ahorro, UiFinanzas.porcentaje(r.tasaAhorro));
        canvas.drawText(detalle, MARGEN, y, texto);
        y += 20;
    }

    private void titulo(String titulo) {
        asegurarEspacio(50);
        y += 10;
        estilo(13, MARINO, true);
        canvas.drawText(titulo, MARGEN, y, texto);
        y += 8;
        relleno.setColor(LINEA);
        canvas.drawRect(MARGEN, y, ANCHO - MARGEN, y + 1, relleno);
        y += 16;
    }

    /** Fila con hasta 4 columnas alineadas (la primera a la izquierda, el resto a la derecha). */
    private void fila(String c1, String c2, String c3, String c4, int color4, boolean negrita) {
        asegurarEspacio(18);
        estilo(10, TEXTO, negrita);
        canvas.drawText(recortar(c1, 32), MARGEN, y, texto);
        texto.setTextAlign(Paint.Align.RIGHT);
        estilo(10, GRIS, negrita);
        if (c2 != null) canvas.drawText(c2, MARGEN + 300, y, texto);
        if (c3 != null) canvas.drawText(c3, MARGEN + 410, y, texto);
        estilo(10, color4, true);
        if (c4 != null) canvas.drawText(c4, ANCHO - MARGEN, y, texto);
        texto.setTextAlign(Paint.Align.LEFT);
        y += 17;
    }

    private static String recortar(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private void tabla(String titulo, List<FinanzasServicio.TotalCategoria> datos, double total) {
        if (datos.isEmpty()) return;
        titulo(titulo);
        for (FinanzasServicio.TotalCategoria t : datos) {
            String nombre = t.categoria != null ? t.categoria.nombre : context.getString(R.string.fin_sin_categoria);
            fila(nombre, UiFinanzas.porcentaje(t.porcentaje) + "%", String.valueOf(t.cantidad), moneda(t.total), TEXTO, false);
        }
        fila(context.getString(R.string.fin_total), null, null, moneda(total), MARINO, true);
    }

    private void cuentas(List<FinanzasServicio.TotalCuenta> datos) {
        if (datos.isEmpty()) return;
        titulo(context.getString(R.string.fin_rep_por_cuenta));
        fila("", context.getString(R.string.fin_rep_col_ingresos), context.getString(R.string.fin_rep_col_gastos),
                context.getString(R.string.fin_rep_col_neto), GRIS, true);
        for (FinanzasServicio.TotalCuenta t : datos) {
            String nombre = t.cuenta != null ? t.cuenta.nombre : context.getString(R.string.fin_sin_cuenta);
            fila(nombre, moneda(t.ingresos), moneda(t.gastos), moneda(t.neto), t.neto < 0 ? ROJO : VERDE, false);
        }
    }

    private void meses(List<FinanzasServicio.PuntoMes> datos) {
        if (datos.size() < 2) return;
        titulo(context.getString(R.string.fin_rep_por_mes));
        fila("", context.getString(R.string.fin_rep_col_ingresos), context.getString(R.string.fin_rep_col_gastos),
                context.getString(R.string.fin_rep_col_neto), GRIS, true);
        String[] nombres = new java.text.DateFormatSymbols(UiFinanzas.ES).getMonths();
        for (FinanzasServicio.PuntoMes p : datos) {
            String mes = nombres[p.mes];
            mes = mes.substring(0, 1).toUpperCase(UiFinanzas.ES) + mes.substring(1) + " " + p.anio;
            double neto = p.ingresos - p.gastos;
            fila(mes, moneda(p.ingresos), moneda(p.gastos), moneda(neto), neto < 0 ? ROJO : VERDE, false);
        }
    }

    private void mayores(List<Movimiento> datos, Map<Long, CategoriaFin> categorias) {
        if (datos.isEmpty()) return;
        titulo(context.getString(R.string.fin_rep_mayores_gastos));
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        for (Movimiento m : datos) {
            CategoriaFin c = m.categoriaId != null && categorias != null ? categorias.get(m.categoriaId) : null;
            fila(m.descripcion, sdf.format(new Date(m.fechaEpoch)), c != null ? recortar(c.nombre, 16) : "",
                    moneda(m.monto), ROJO, false);
        }
    }
}
