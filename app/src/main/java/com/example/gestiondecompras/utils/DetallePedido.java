package com.example.gestiondecompras.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.models.Pedido;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Lógica común del detalle de un pedido (layout dialog_detalle_pedido), usada por
 * Pedidos y por el Calendario para no duplicar código.
 */
public final class DetallePedido {

    private DetallePedido() {
    }

    public static String moneda(double monto) {
        return String.format(Locale.getDefault(), "RD$ %,.2f", monto);
    }

    private static SimpleDateFormat formatoFecha() {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    /** Rellena los campos del layout dialog_detalle_pedido con los datos del pedido. */
    public static void rellenar(@NonNull View vista, @NonNull Pedido pedido) {
        Context context = vista.getContext();
        SimpleDateFormat sdf = formatoFecha();
        String sinFecha = context.getString(R.string.sin_dato);

        ((TextView) vista.findViewById(R.id.tv_detalle_producto)).setText(pedido.getClienteNombre());
        ((TextView) vista.findViewById(R.id.tv_detalle_tienda)).setText(pedido.getTienda());
        ((TextView) vista.findViewById(R.id.tv_detalle_fecha_compra)).setText(
                pedido.getFechaRegistro() != null ? sdf.format(pedido.getFechaRegistro()) : sinFecha);
        ((TextView) vista.findViewById(R.id.tv_detalle_fecha_entrega)).setText(
                pedido.getFechaEntrega() != null ? sdf.format(pedido.getFechaEntrega()) : sinFecha);
        ((TextView) vista.findViewById(R.id.tv_detalle_costo)).setText(moneda(pedido.getMontoCompra()));
        ((TextView) vista.findViewById(R.id.tv_detalle_venta)).setText(moneda(pedido.getTotalGeneral()));
        ((TextView) vista.findViewById(R.id.tv_detalle_ganancia)).setText(moneda(pedido.getGanancia()));

        // Notas del pedido: solo se muestran si existen
        String notas = pedido.getNotas() != null ? pedido.getNotas().trim() : "";
        View bloqueNotas = vista.findViewById(R.id.layout_detalle_notas);
        if (bloqueNotas != null) {
            bloqueNotas.setVisibility(notas.isEmpty() ? View.GONE : View.VISIBLE);
            ((TextView) vista.findViewById(R.id.tv_detalle_notas)).setText(notas);
        }
        actualizarPagos(vista, pedido);
    }

    /** Actualiza abonado, saldo y estado (tras registrar o eliminar un abono). */
    public static void actualizarPagos(@NonNull View vista, @NonNull Pedido pedido) {
        ((TextView) vista.findViewById(R.id.tv_detalle_abonado)).setText(moneda(pedido.getTotalAbonado()));
        ((TextView) vista.findViewById(R.id.tv_detalle_saldo)).setText(moneda(pedido.getSaldo()));
        aplicarEstado(vista.findViewById(R.id.tv_detalle_estado), pedido.getEstado());
    }

    /** Muestra el estado con su color (texto blanco sobre el color del estado). */
    public static void aplicarEstado(TextView tvEstado, String estado) {
        if (tvEstado == null) {
            return;
        }
        Context context = tvEstado.getContext();
        tvEstado.setText(estado);
        tvEstado.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, colorEstado(estado))));
        tvEstado.setTextColor(ContextCompat.getColor(context, R.color.white));
    }

    public static int colorEstado(String estado) {
        if (Pedido.ESTADO_PAGADO.equalsIgnoreCase(estado)) return R.color.status_paid;
        if (Pedido.ESTADO_ENTREGADO.equalsIgnoreCase(estado)) return R.color.status_delivered;
        if (Pedido.ESTADO_CANCELADO.equalsIgnoreCase(estado)) return R.color.status_cancelled;
        return R.color.status_pending;
    }

    /** Texto del recordatorio de pago que se envía al cliente por WhatsApp. */
    public static String mensajeRecordatorio(@NonNull Context context, @NonNull Pedido pedido) {
        String tienda = pedido.getTienda() != null && !pedido.getTienda().isEmpty()
                ? pedido.getTienda() : context.getString(R.string.recordatorio_tu_compra);
        return context.getString(R.string.recordatorio_mensaje, pedido.getClienteNombre(),
                moneda(pedido.getSaldo()), tienda);
    }

    /** Abre WhatsApp con el recordatorio de pago dirigido al teléfono del cliente. */
    public static void recordarPagoPorWhatsApp(@NonNull Context context, @NonNull Pedido pedido, String telefono) {
        String numero = ContactoUtils.numeroWhatsApp(telefono);
        if (numero.isEmpty()) {
            Toast.makeText(context, R.string.recordatorio_sin_telefono, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String texto = java.net.URLEncoder.encode(mensajeRecordatorio(context, pedido), "UTF-8");
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://wa.me/" + numero + "?text=" + texto));
            context.startActivity(intent);
        } catch (ActivityNotFoundException | java.io.UnsupportedEncodingException e) {
            Toast.makeText(context, R.string.no_app_disponible, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Comparte con el cliente el resumen del pedido. No incluye el costo ni la ganancia:
     * es información interna del negocio que el cliente no debe ver.
     */
    public static void compartirResumen(@NonNull Context context, @NonNull Pedido pedido) {
        SimpleDateFormat sdf = formatoFecha();
        String sinFecha = context.getString(R.string.sin_dato);
        String fechaRegistro = pedido.getFechaRegistro() != null ? sdf.format(pedido.getFechaRegistro()) : sinFecha;
        String fechaEntrega = pedido.getFechaEntrega() != null ? sdf.format(pedido.getFechaEntrega()) : sinFecha;

        String mensaje = context.getString(R.string.resumen_titulo) + "\n\n"
                + context.getString(R.string.pedido_detalle_cliente) + ": " + pedido.getClienteNombre() + "\n"
                + context.getString(R.string.pedido_detalle_tienda) + ": " + pedido.getTienda() + "\n"
                + context.getString(R.string.pedido_detalle_fecha_compra) + ": " + fechaRegistro + "\n"
                + context.getString(R.string.pedido_detalle_fecha_entrega) + ": " + fechaEntrega + "\n"
                + context.getString(R.string.resumen_total) + ": " + moneda(pedido.getTotalGeneral()) + "\n"
                + context.getString(R.string.pedido_detalle_abonado) + ": " + moneda(pedido.getTotalAbonado()) + "\n"
                + context.getString(R.string.pedido_detalle_saldo) + ": " + moneda(pedido.getSaldo()) + "\n"
                + context.getString(R.string.resumen_estado) + ": " + pedido.getEstado();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.pedido_share_subject, pedido.getClienteNombre()));
        intent.putExtra(Intent.EXTRA_TEXT, mensaje);
        try {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.pedido_enviar_resumen)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_app_disponible, Toast.LENGTH_SHORT).show();
        }
    }
}
