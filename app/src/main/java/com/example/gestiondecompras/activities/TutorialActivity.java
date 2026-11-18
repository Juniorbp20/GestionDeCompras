package com.example.gestiondecompras.activities;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.databinding.ActivityTutorialBinding;

/**
 * Tutorial de bienvenida para el primer uso: páginas deslizables con los pasos básicos.
 * Se muestra una vez al abrir la app y se puede volver a ver desde Más → Ayuda.
 */
public class TutorialActivity extends AppCompatActivity {

    /** Preferencia (archivo "settings") que indica que el tutorial ya se mostró. */
    public static final String PREF_VISTO = "tutorial_bienvenida_visto";

    /** Página del tutorial: icono, título y texto. */
    private static final class Pagina {
        @DrawableRes final int icono;
        @StringRes final int titulo;
        @StringRes final int texto;

        Pagina(@DrawableRes int icono, @StringRes int titulo, @StringRes int texto) {
            this.icono = icono;
            this.titulo = titulo;
            this.texto = texto;
        }
    }

    private static final Pagina[] PAGINAS = {
            new Pagina(R.drawable.ic_cart, R.string.tutorial_1_titulo, R.string.tutorial_1_texto),
            new Pagina(R.drawable.ic_people, R.string.tutorial_2_titulo, R.string.tutorial_2_texto),
            new Pagina(R.drawable.ic_credit_card, R.string.tutorial_3_titulo, R.string.tutorial_3_texto),
            new Pagina(R.drawable.ic_add_circle, R.string.tutorial_4_titulo, R.string.tutorial_4_texto),
            new Pagina(R.drawable.ic_payments, R.string.tutorial_5_titulo, R.string.tutorial_5_texto),
            new Pagina(R.drawable.ic_wallet, R.string.tutorial_6_titulo, R.string.tutorial_6_texto),
            new Pagina(R.drawable.ic_cloud_upload, R.string.tutorial_7_titulo, R.string.tutorial_7_texto),
    };

    private ActivityTutorialBinding binding;

    /** Marca el tutorial como visto (para no volver a mostrarlo al abrir la app). */
    public static void marcarVisto(@NonNull Context context) {
        context.getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean(PREF_VISTO, true).apply();
    }

    public static boolean yaVisto(@NonNull Context context) {
        return context.getSharedPreferences("settings", MODE_PRIVATE).getBoolean(PREF_VISTO, false);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTutorialBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.pager.setAdapter(new PaginasAdapter());
        crearPuntos();
        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                actualizarControles(position);
            }
        });

        binding.btnSaltar.setOnClickListener(v -> terminar());
        binding.btnAnterior.setOnClickListener(v -> binding.pager.setCurrentItem(binding.pager.getCurrentItem() - 1));
        binding.btnSiguiente.setOnClickListener(v -> {
            int actual = binding.pager.getCurrentItem();
            if (actual >= PAGINAS.length - 1) {
                terminar();
            } else {
                binding.pager.setCurrentItem(actual + 1);
            }
        });

        // Atrás retrocede una página; en la primera, cierra el tutorial
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int actual = binding.pager.getCurrentItem();
                if (actual > 0) {
                    binding.pager.setCurrentItem(actual - 1);
                } else {
                    terminar();
                }
            }
        });
        actualizarControles(0);
    }

    private void terminar() {
        marcarVisto(this);
        finish();
    }

    private void crearPuntos() {
        int tam = getResources().getDimensionPixelSize(R.dimen.tutorial_punto);
        int margen = tam / 2;
        for (int i = 0; i < PAGINAS.length; i++) {
            View punto = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tam, tam);
            lp.setMargins(margen, 0, margen, 0);
            punto.setLayoutParams(lp);
            punto.setBackgroundResource(R.drawable.bg_tutorial_punto);
            binding.layoutPuntos.addView(punto);
        }
    }

    private void actualizarControles(int posicion) {
        int activo = ContextCompat.getColor(this, R.color.accent_gold);
        int inactivo = ContextCompat.getColor(this, R.color.gray_light);
        int tam = getResources().getDimensionPixelSize(R.dimen.tutorial_punto);
        for (int i = 0; i < binding.layoutPuntos.getChildCount(); i++) {
            View punto = binding.layoutPuntos.getChildAt(i);
            punto.setBackgroundTintList(ColorStateList.valueOf(i == posicion ? activo : inactivo));
            // El punto de la página actual es más ancho
            ViewGroup.LayoutParams lp = punto.getLayoutParams();
            lp.width = i == posicion ? tam * 3 : tam;
            punto.setLayoutParams(lp);
        }
        boolean ultima = posicion == PAGINAS.length - 1;
        binding.btnAnterior.setVisibility(posicion == 0 ? View.INVISIBLE : View.VISIBLE);
        binding.btnSiguiente.setText(ultima ? R.string.tutorial_empezar : R.string.tutorial_siguiente);
        binding.btnSaltar.setVisibility(ultima ? View.INVISIBLE : View.VISIBLE);
    }

    private final class PaginasAdapter extends RecyclerView.Adapter<PaginasAdapter.Holder> {

        final class Holder extends RecyclerView.ViewHolder {
            final ImageView icono;
            final TextView paso;
            final TextView titulo;
            final TextView texto;

            Holder(@NonNull View v) {
                super(v);
                icono = v.findViewById(R.id.ivIcono);
                paso = v.findViewById(R.id.tvPaso);
                titulo = v.findViewById(R.id.tvTitulo);
                texto = v.findViewById(R.id.tvTexto);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tutorial_pagina, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Pagina p = PAGINAS[position];
            h.icono.setImageResource(p.icono);
            // La primera página es la bienvenida; las demás son pasos numerados
            if (position == 0) {
                h.paso.setText(R.string.tutorial_bienvenida);
            } else {
                h.paso.setText(getString(R.string.tutorial_paso_de, position, PAGINAS.length - 1));
            }
            h.titulo.setText(p.titulo);
            h.texto.setText(p.texto);
        }

        @Override
        public int getItemCount() {
            return PAGINAS.length;
        }
    }
}
