package com.example.gestiondecompras.utils;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gestiondecompras.R;
import com.google.android.material.snackbar.Snackbar;

/**
 * Eliminación con opción de deshacer.
 *
 * El elemento se oculta de la lista al momento y se muestra un aviso con "Deshacer".
 * El borrado real en la base de datos solo se ejecuta cuando el aviso se cierra sin
 * deshacer, cuando se elimina otro elemento o cuando la pantalla deja de verse
 * ({@link #confirmarPendiente()} debe llamarse en {@code onStop}).
 */
public final class BorradoConDeshacer {

    private static final int DURACION_MS = 5000;

    @Nullable
    private Runnable borradoPendiente;

    /**
     * @param raiz      vista sobre la que se muestra el aviso
     * @param anclaje   vista sobre la que se coloca el aviso (p. ej. la barra inferior), o null
     * @param mensaje   texto del aviso
     * @param ocultar   quita el elemento de la lista visible
     * @param restaurar vuelve a mostrarlo si el usuario pulsa "Deshacer"
     * @param borrar    borrado definitivo en la base de datos
     */
    public void eliminar(@NonNull View raiz, @Nullable View anclaje, @NonNull CharSequence mensaje,
                         @NonNull Runnable ocultar, @NonNull Runnable restaurar, @NonNull Runnable borrar) {
        // Un borrado anterior todavía pendiente se hace definitivo antes de empezar otro
        confirmarPendiente();

        ocultar.run();
        borradoPendiente = borrar;

        Snackbar aviso = Snackbar.make(raiz, mensaje, DURACION_MS)
                .setAction(R.string.accion_deshacer, v -> {
                    if (borradoPendiente == borrar) {
                        borradoPendiente = null;
                        restaurar.run();
                    }
                });
        if (anclaje != null && anclaje.getVisibility() == View.VISIBLE) {
            aviso.setAnchorView(anclaje);
        }
        aviso.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                if (event != DISMISS_EVENT_ACTION && borradoPendiente == borrar) {
                    confirmarPendiente();
                }
            }
        });
        aviso.show();
    }

    /** Ejecuta el borrado pendiente, si lo hay. */
    public void confirmarPendiente() {
        Runnable borrar = borradoPendiente;
        borradoPendiente = null;
        if (borrar != null) {
            borrar.run();
        }
    }
}
