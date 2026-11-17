package com.example.gestiondecompras.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ContactoUtilsTest {

    @Test
    public void numeroParaLlamar_quitaSimbolosYConservaPrefijoInternacional() {
        assertEquals("8095551234", ContactoUtils.numeroParaLlamar("(809) 555-1234"));
        assertEquals("+18095551234", ContactoUtils.numeroParaLlamar("+1 809 555 1234"));
        assertEquals("", ContactoUtils.numeroParaLlamar(null));
    }

    @Test
    public void numeroWhatsApp_agregaCodigoDePaisDominicano() {
        assertEquals("18095551234", ContactoUtils.numeroWhatsApp("809-555-1234"));
        assertEquals("18295551234", ContactoUtils.numeroWhatsApp("829 555 1234"));
        assertEquals("18495551234", ContactoUtils.numeroWhatsApp("(849)5551234"));
    }

    @Test
    public void numeroWhatsApp_respetaNumerosInternacionalesYRechazaInvalidos() {
        assertEquals("18095551234", ContactoUtils.numeroWhatsApp("+1 809 555 1234"));
        assertEquals("34612345678", ContactoUtils.numeroWhatsApp("+34 612 345 678"));
        assertEquals("", ContactoUtils.numeroWhatsApp("12345"));
        assertEquals("", ContactoUtils.numeroWhatsApp(null));
    }
}
