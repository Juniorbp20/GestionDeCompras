package com.example.gestiondecompras.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.example.gestiondecompras.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Hoja inferior con las acciones de un elemento (pedido, cliente, gasto, tarjeta).
 *
 * Uso:
 * <pre>
 * HojaAcciones.de(context, titulo)
 *         .subtitulo("RD$ 1,200.00")
 *         .accion(R.drawable.ic_edit, R.string.accion_editar, () -> editar())
 *         .peligrosa(R.drawable.ic_delete, R.string.accion_eliminar, () -> eliminar())
 *         .mostrar();
 * </pre>
 * Las acciones peligrosas se muestran siempre al final, separadas y en rojo.
 */
public final class HojaAcciones {

    private static final class Accion {
        final int icono;
        final CharSequence texto;
        final Runnable alPulsar;
        final boolean peligrosa;

        Accion(int icono, CharSequence texto, Runnable alPulsar, boolean peligrosa) {
            this.icono = icono;
            this.texto = texto;
            this.alPulsar = alPulsar;
            this.peligrosa = peligrosa;
        }
    }

    private final Context context;
    private final CharSequence titulo;
    @Nullable
    private CharSequence subtitulo;
    private final List<Accion> acciones = new ArrayList<>();

    private HojaAcciones(@NonNull Context context, @Nullable CharSequence titulo) {
        this.context = context;
        this.titulo = titulo;
    }

    public static HojaAcciones de(@NonNull Context context, @Nullable CharSequence titulo) {
        return new HojaAcciones(context, titulo);
    }

    public HojaAcciones subtitulo(@Nullable CharSequence subtitulo) {
        this.subtitulo = subtitulo;
        return this;
    }

    public HojaAcciones accion(@DrawableRes int icono, @StringRes int texto, @NonNull Runnable alPulsar) {
        acciones.add(new Accion(icono, context.getString(texto), alPulsar, false));
        return this;
    }

    public HojaAcciones peligrosa(@DrawableRes int icono, @StringRes int texto, @NonNull Runnable alPulsar) {
        acciones.add(new Accion(icono, context.getString(texto), alPulsar, true));
        return this;
    }

    public void mostrar() {
        BottomSheetDialog dialogo = new BottomSheetDialog(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View vista = inflater.inflate(R.layout.sheet_acciones, null, false);

        TextView tvTitulo = vista.findViewById(R.id.tvHojaTitulo);
        TextView tvSubtitulo = vista.findViewById(R.id.tvHojaSubtitulo);
        LinearLayout contenedor = vista.findViewById(R.id.layoutHojaAcciones);
        LinearLayout contenedorPeligro = vista.findViewById(R.id.layoutHojaPeligro);
        View separador = vista.findViewById(R.id.separadorHojaPeligro);

        boolean hayTitulo = titulo != null && titulo.length() > 0;
        tvTitulo.setText(hayTitulo ? titulo : context.getString(R.string.acciones_mas_opciones));
        boolean haySubtitulo = subtitulo != null && subtitulo.length() > 0;
        tvSubtitulo.setVisibility(haySubtitulo ? View.VISIBLE : View.GONE);
        if (haySubtitulo) {
            tvSubtitulo.setText(subtitulo);
        }

        boolean hayPeligrosas = false;
        for (Accion accion : acciones) {
            if (accion.peligrosa) {
                hayPeligrosas = true;
                agregarFila(inflater, contenedorPeligro, accion, dialogo);
            } else {
                agregarFila(inflater, contenedor, accion, dialogo);
            }
        }
        separador.setVisibility(hayPeligrosas ? View.VISIBLE : View.GONE);
        contenedorPeligro.setVisibility(hayPeligrosas ? View.VISIBLE : View.GONE);

        dialogo.setContentView(vista);
        dialogo.show();
    }

    private void agregarFila(LayoutInflater inflater, LinearLayout contenedor, Accion accion,
                             BottomSheetDialog dialogo) {
        View fila = inflater.inflate(R.layout.item_accion_hoja, contenedor, false);
        ImageView icono = fila.findViewById(R.id.ivAccionIcono);
        TextView texto = fila.findViewById(R.id.tvAccionTexto);

        icono.setImageResource(accion.icono);
        texto.setText(accion.texto);
        if (accion.peligrosa) {
            int rojo = ContextCompat.getColor(context, R.color.error_color);
            texto.setTextColor(rojo);
            icono.setImageTintList(ColorStateList.valueOf(rojo));
            icono.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.dashboard_badge_red_bg)));
        }

        fila.setOnClickListener(v -> {
            dialogo.dismiss();
            accion.alPulsar.run();
        });
        contenedor.addView(fila);
    }
}
