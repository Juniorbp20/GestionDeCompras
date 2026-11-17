package com.example.gestiondecompras.utils.finanzas;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Cuenta;
import com.example.gestiondecompras.models.finanzas.Movimiento;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/** Formato y presentación compartidos por las pantallas de Finanzas. */
public final class UiFinanzas {

    public static final Locale ES = Locale.forLanguageTag("es-DO");

    private UiFinanzas() {
    }

    public static String moneda(double monto) {
        return String.format(Locale.getDefault(), "RD$ %,.2f", monto);
    }

    /** "+ RD$ 100.00" para ingresos y "- RD$ 100.00" para gastos. */
    public static String montoConSigno(Movimiento m) {
        return (m.esIngreso() ? "+ " : "- ") + moneda(m.monto);
    }

    /** Importe con signo según sea positivo o negativo. */
    public static String monedaConSigno(double monto) {
        return (monto < 0 ? "- " : "+ ") + moneda(Math.abs(monto));
    }

    public static String fechaCorta(long epoch) {
        return new SimpleDateFormat("dd MMM yyyy", ES).format(new Date(epoch));
    }

    public static String fechaLarga(long epoch) {
        String s = new SimpleDateFormat("EEEE d 'de' MMMM", ES).format(new Date(epoch));
        return s.substring(0, 1).toUpperCase(ES) + s.substring(1);
    }

    public static String porcentaje(double valor) {
        // "25.0" -> "25", "12.5" -> "12.5" (con punto o coma según el idioma)
        return String.format(Locale.getDefault(), "%.1f", valor).replaceAll("[.,]0$", "");
    }

    public static int colorCategoria(Context context, @Nullable CategoriaFin c) {
        return ContextCompat.getColor(context, VisualFinanzas.color(c != null ? c.colorKey : null));
    }

    /** Pinta un icono de categoría: icono del color de la categoría sobre fondo suave del mismo color. */
    public static void pintarIcono(ImageView icono, @Nullable CategoriaFin c) {
        int color = colorCategoria(icono.getContext(), c);
        icono.setImageResource(VisualFinanzas.icono(c != null ? c.iconoKey : null));
        icono.setImageTintList(ColorStateList.valueOf(color));
        icono.setBackgroundTintList(ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 38)));
    }

    /** Rellena una fila item_movimiento. */
    public static void enlazarMovimiento(@NonNull View fila, @NonNull Movimiento m,
                                         @Nullable Map<Long, CategoriaFin> categorias,
                                         @Nullable Map<Long, Cuenta> cuentas, boolean conFecha) {
        Context ctx = fila.getContext();
        CategoriaFin cat = m.categoriaId != null && categorias != null ? categorias.get(m.categoriaId) : null;
        Cuenta cuenta = m.cuentaId != null && cuentas != null ? cuentas.get(m.cuentaId) : null;

        pintarIcono(fila.findViewById(R.id.ivIcono), cat);
        TextView desc = fila.findViewById(R.id.tvDescripcion);
        String descripcion = m.descripcion != null && !m.descripcion.isEmpty() ? m.descripcion
                : (cat != null ? cat.nombre : ctx.getString(R.string.fin_sin_categoria));
        desc.setText(descripcion);

        StringBuilder detalle = new StringBuilder(cat != null ? cat.nombre : ctx.getString(R.string.fin_sin_categoria));
        if (cuenta != null) detalle.append(" · ").append(cuenta.nombre);
        if (conFecha) detalle.append(" · ").append(fechaCorta(m.fechaEpoch));
        if (m.recurrenteId != null) detalle.append(" · ↻");
        ((TextView) fila.findViewById(R.id.tvDetalle)).setText(detalle);

        TextView monto = fila.findViewById(R.id.tvMonto);
        monto.setText(montoConSigno(m));
        monto.setTextColor(ContextCompat.getColor(ctx, m.esIngreso() ? R.color.success_color : R.color.error_color));
    }
}
