package com.example.gestiondecompras.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.activities.ListaClientesActivity;
import com.example.gestiondecompras.activities.ListaGastosActivity;
import com.example.gestiondecompras.activities.ListaPedidosActivity;
import com.example.gestiondecompras.activities.MainActivity;
import com.example.gestiondecompras.activities.MasActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Barra de navegación inferior compartida por las cinco pantallas principales
 * (Inicio, Pedidos, Clientes, Finanzas y Más).
 *
 * Reglas de navegación:
 * - La pila de pantallas es siempre [Inicio] o [Inicio, pestaña]: al cambiar de pestaña
 *   se cierra la pestaña actual (salvo Inicio).
 * - Atrás en cualquier pestaña distinta de Inicio vuelve a Inicio.
 * - Volver a pulsar la pestaña actual ejecuta {@code alRepetir} (p. ej. subir la lista al principio).
 */
public final class NavegacionInferior {

    private NavegacionInferior() {
    }

    public static void configurar(@NonNull ComponentActivity activity,
                                  @NonNull BottomNavigationView barra,
                                  @IdRes int pestanaActual) {
        configurar(activity, barra, pestanaActual, null);
    }

    public static void configurar(@NonNull ComponentActivity activity,
                                  @NonNull BottomNavigationView barra,
                                  @IdRes int pestanaActual,
                                  @Nullable Runnable alRepetir) {
        barra.setVisibility(View.VISIBLE);
        barra.setSelectedItemId(pestanaActual);

        barra.setOnItemSelectedListener(item -> {
            int destino = item.getItemId();
            if (destino == pestanaActual) {
                return true;
            }
            irA(activity, destino, pestanaActual);
            // No se marca la nueva pestaña aquí: la pantalla destino la marcará al abrirse.
            return false;
        });

        barra.setOnItemReselectedListener(item -> {
            if (alRepetir != null) {
                alRepetir.run();
            }
        });

        if (pestanaActual != R.id.tab_inicio) {
            activity.getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    irA(activity, R.id.tab_inicio, pestanaActual);
                }
            });
        }
    }

    /** Abre la pestaña indicada sin animación y cierra la actual si no es Inicio. */
    public static void irA(@NonNull Activity activity, @IdRes int destino, @IdRes int pestanaActual) {
        Class<?> clase = claseDe(destino);
        if (clase == null) {
            return;
        }
        Intent intent = new Intent(activity, clase);
        if (destino == R.id.tab_inicio) {
            // Vuelve a la instancia existente de Inicio (o la crea si se llegó desde una notificación).
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
        if (pestanaActual != R.id.tab_inicio) {
            activity.finish();
            activity.overridePendingTransition(0, 0);
        }
    }

    @Nullable
    private static Class<?> claseDe(@IdRes int pestana) {
        if (pestana == R.id.tab_inicio) return MainActivity.class;
        if (pestana == R.id.tab_pedidos) return ListaPedidosActivity.class;
        if (pestana == R.id.tab_clientes) return ListaClientesActivity.class;
        if (pestana == R.id.tab_finanzas) return ListaGastosActivity.class;
        if (pestana == R.id.tab_mas) return MasActivity.class;
        return null;
    }
}
