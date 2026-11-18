package com.example.gestiondecompras.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.gestiondecompras.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Recorre el tutorial de bienvenida completo y comprueba que queda marcado como visto. */
@RunWith(AndroidJUnit4.class)
public class TutorialActivityTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                .remove(TutorialActivity.PREF_VISTO).commit();
    }

    @Test
    public void recorrerTodasLasPaginas_yEmpezar_cierraYMarcaVisto() {
        assertFalse(TutorialActivity.yaVisto(context));
        try (ActivityScenario<TutorialActivity> escenario = ActivityScenario.launch(TutorialActivity.class)) {
            onView(withId(R.id.btnSiguiente)).check(matches(withText(R.string.tutorial_siguiente)));
            for (int i = 0; i < 6; i++) {
                onView(withId(R.id.btnSiguiente)).perform(click());
            }
            onView(withId(R.id.btnSiguiente)).check(matches(withText(R.string.tutorial_empezar)));
            onView(withId(R.id.btnSiguiente)).perform(click());
            escenario.getResult(); // espera a que la pantalla se cierre
        }
        assertTrue(TutorialActivity.yaVisto(context));
    }

    @Test
    public void saltar_cierraYMarcaVisto() {
        try (ActivityScenario<TutorialActivity> escenario = ActivityScenario.launch(TutorialActivity.class)) {
            onView(withId(R.id.btnSaltar)).perform(click());
            escenario.getResult(); // espera a que la pantalla se cierre
        }
        assertTrue(TutorialActivity.yaVisto(context));
    }
}
