package com.example.gestiondecompras.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.models.Gasto;
import com.example.gestiondecompras.models.Pedido;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Exporta pedidos y gastos a CSV y abre el selector para compartir o abrir el archivo. */
public final class ExportadorCsv {

    private ExportadorCsv() {
    }

    public static void exportarPedidos(Context context, List<Pedido> pedidos, String nombreBase) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String[] encabezado = {"Cliente", "Tienda", "Fecha compra", "Fecha entrega", "Estado",
                "Compra", "Ganancia", "Total", "Abonado", "Saldo", "Notas"};
        List<String[]> filas = new ArrayList<>();
        for (Pedido p : pedidos) {
            filas.add(new String[]{
                    p.getClienteNombre(),
                    p.getTienda(),
                    p.getFechaRegistro() != null ? sdf.format(p.getFechaRegistro()) : "",
                    p.getFechaEntrega() != null ? sdf.format(p.getFechaEntrega()) : "",
                    p.getEstado(),
                    CsvUtils.importe(p.getMontoCompra()),
                    CsvUtils.importe(p.getGanancia()),
                    CsvUtils.importe(p.getTotalGeneral()),
                    CsvUtils.importe(p.getTotalAbonado()),
                    CsvUtils.importe(p.getSaldo()),
                    p.getNotas()
            });
        }
        guardarYCompartir(context, CsvUtils.documento(encabezado, filas), nombreBase);
    }

    public static void exportarGastos(Context context, List<Gasto> gastos, String nombreBase) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String[] encabezado = {"Fecha", "Descripción", "Categoría", "Monto", "Notas"};
        List<String[]> filas = new ArrayList<>();
        for (Gasto g : gastos) {
            filas.add(new String[]{
                    sdf.format(new Date(g.getFechaEpoch())),
                    g.getDescripcion(),
                    g.getCategoria(),
                    CsvUtils.importe(g.getMonto()),
                    g.getNotas()
            });
        }
        guardarYCompartir(context, CsvUtils.documento(encabezado, filas), nombreBase);
    }

    private static void guardarYCompartir(Context context, String contenido, String nombreBase) {
        try {
            File carpeta = new File(context.getCacheDir(), "exports");
            if (!carpeta.exists() && !carpeta.mkdirs()) {
                throw new IOException("No se pudo crear la carpeta de exportación");
            }
            String fecha = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date());
            File archivo = new File(carpeta, nombreBase + "_" + fecha + ".csv");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8)) {
                w.write(contenido);
            }

            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", archivo);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, archivo.getName());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.exportar_csv)));
        } catch (IOException | IllegalArgumentException e) {
            Toast.makeText(context, R.string.exportar_csv_error, Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_app_disponible, Toast.LENGTH_SHORT).show();
        }
    }
}
