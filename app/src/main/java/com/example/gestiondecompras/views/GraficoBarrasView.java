package com.example.gestiondecompras.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.gestiondecompras.R;

import java.util.ArrayList;
import java.util.List;

/** Barras agrupadas de ingresos y gastos por periodo. Sin dependencias externas. */
public class GraficoBarrasView extends View {

    /** Grupo de dos barras con su etiqueta. */
    public static class Grupo {
        public final String etiqueta;
        public final double ingresos;
        public final double gastos;

        public Grupo(String etiqueta, double ingresos, double gastos) {
            this.etiqueta = etiqueta;
            this.ingresos = ingresos;
            this.gastos = gastos;
        }
    }

    private final Paint barraIngreso = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barraGasto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linea = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint etiqueta = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final List<Grupo> grupos = new ArrayList<>();

    public GraficoBarrasView(Context context) {
        this(context, null);
    }

    public GraficoBarrasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        barraIngreso.setColor(ContextCompat.getColor(context, R.color.success_color));
        barraGasto.setColor(ContextCompat.getColor(context, R.color.error_color));
        linea.setColor(ContextCompat.getColor(context, R.color.card_stroke));
        linea.setStrokeWidth(getResources().getDisplayMetrics().density);
        etiqueta.setColor(ContextCompat.getColor(context, R.color.gray));
        etiqueta.setTextAlign(Paint.Align.CENTER);
        etiqueta.setTextSize(10 * getResources().getDisplayMetrics().scaledDensity);
        Typeface media = ResourcesCompat.getFont(context, R.font.inter_medium);
        if (media != null) etiqueta.setTypeface(media);
    }

    public void setDatos(List<Grupo> datos) {
        grupos.clear();
        if (datos != null) grupos.addAll(datos);
        StringBuilder desc = new StringBuilder();
        for (Grupo g : grupos) {
            desc.append(g.etiqueta).append(": ").append(Math.round(g.ingresos)).append(" / ")
                    .append(Math.round(g.gastos)).append(". ");
        }
        setContentDescription(desc.toString());
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ancho = MeasureSpec.getSize(widthMeasureSpec);
        int alto = (int) (170 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(ancho, resolveSize(alto, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (grupos.isEmpty()) return;
        float d = getResources().getDisplayMetrics().density;
        float altoEtiqueta = 18 * d;
        float base = getHeight() - altoEtiqueta;
        float techo = 8 * d;
        canvas.drawLine(0, base, getWidth(), base, linea);

        double maximo = 0;
        for (Grupo g : grupos) maximo = Math.max(maximo, Math.max(g.ingresos, g.gastos));
        if (maximo <= 0) maximo = 1;

        float anchoGrupo = getWidth() / (float) grupos.size();
        float anchoBarra = Math.min(14 * d, anchoGrupo / 3.2f);
        float radio = 4 * d;
        for (int i = 0; i < grupos.size(); i++) {
            Grupo g = grupos.get(i);
            float centro = anchoGrupo * i + anchoGrupo / 2f;
            float hIng = (float) (g.ingresos / maximo * (base - techo));
            float hGas = (float) (g.gastos / maximo * (base - techo));
            rect.set(centro - anchoBarra - 1.5f * d, base - hIng, centro - 1.5f * d, base);
            canvas.drawRoundRect(rect, radio, radio, barraIngreso);
            rect.set(centro + 1.5f * d, base - hGas, centro + anchoBarra + 1.5f * d, base);
            canvas.drawRoundRect(rect, radio, radio, barraGasto);
            canvas.drawText(g.etiqueta, centro, getHeight() - 4 * d, etiqueta);
        }
    }
}
