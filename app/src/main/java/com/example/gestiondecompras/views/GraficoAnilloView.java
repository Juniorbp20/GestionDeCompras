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

/** Gráfico de anillo (dona) con un texto central. Sin dependencias externas. */
public class GraficoAnilloView extends View {

    /** Porción del anillo. */
    public static class Segmento {
        public final double valor;
        public final int color;

        public Segmento(double valor, int color) {
            this.valor = valor;
            this.color = color;
        }
    }

    private final Paint pintura = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinturaFondo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textoValor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textoTitulo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF area = new RectF();
    private final List<Segmento> segmentos = new ArrayList<>();
    private String titulo = "";
    private String valor = "";

    public GraficoAnilloView(Context context) {
        this(context, null);
    }

    public GraficoAnilloView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float d = getResources().getDisplayMetrics().density;
        pintura.setStyle(Paint.Style.STROKE);
        pintura.setStrokeCap(Paint.Cap.BUTT);
        pinturaFondo.setStyle(Paint.Style.STROKE);
        pinturaFondo.setColor(ContextCompat.getColor(context, R.color.card_stroke));

        Typeface negrita = ResourcesCompat.getFont(context, R.font.inter_bold);
        Typeface media = ResourcesCompat.getFont(context, R.font.inter_medium);
        textoValor.setTextAlign(Paint.Align.CENTER);
        textoValor.setColor(ContextCompat.getColor(context, R.color.gray_dark));
        textoValor.setTextSize(18 * getResources().getDisplayMetrics().scaledDensity);
        if (negrita != null) textoValor.setTypeface(negrita);
        textoTitulo.setTextAlign(Paint.Align.CENTER);
        textoTitulo.setColor(ContextCompat.getColor(context, R.color.gray));
        textoTitulo.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        if (media != null) textoTitulo.setTypeface(media);
        setMinimumHeight((int) (160 * d));
    }

    public void setDatos(List<Segmento> datos, String titulo, String valor) {
        segmentos.clear();
        if (datos != null) segmentos.addAll(datos);
        this.titulo = titulo != null ? titulo : "";
        this.valor = valor != null ? valor : "";
        setContentDescription(this.titulo + " " + this.valor);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ancho = MeasureSpec.getSize(widthMeasureSpec);
        int alto = (int) (160 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(ancho, resolveSize(Math.min(ancho, alto), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float grosor = 18 * d;
        float lado = Math.min(getWidth(), getHeight()) - grosor - 4 * d;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        area.set(cx - lado / 2, cy - lado / 2, cx + lado / 2, cy + lado / 2);
        pintura.setStrokeWidth(grosor);
        pinturaFondo.setStrokeWidth(grosor);

        canvas.drawArc(area, 0, 360, false, pinturaFondo);

        double total = 0;
        for (Segmento s : segmentos) total += Math.max(0, s.valor);
        if (total > 0) {
            float inicio = -90f;
            float separacion = segmentos.size() > 1 ? 1.5f : 0f;
            for (Segmento s : segmentos) {
                float barrido = (float) (Math.max(0, s.valor) / total * 360f);
                if (barrido <= 0) continue;
                pintura.setColor(s.color);
                canvas.drawArc(area, inicio, Math.max(0.5f, barrido - separacion), false, pintura);
                inicio += barrido;
            }
        }

        float yValor = cy + textoValor.getTextSize() / 3f;
        canvas.drawText(valor, cx, yValor, textoValor);
        canvas.drawText(titulo, cx, yValor - textoValor.getTextSize() - 2 * d, textoTitulo);
    }
}
