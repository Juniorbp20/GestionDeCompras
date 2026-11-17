package com.example.gestiondecompras.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class CsvUtilsTest {

    @Test
    public void escapar_entrecomillaSoloCuandoHaceFalta() {
        assertEquals("Ana", CsvUtils.escapar("Ana"));
        assertEquals("\"Pérez, Ana\"", CsvUtils.escapar("Pérez, Ana"));
        assertEquals("\"Dijo \"\"hola\"\"\"", CsvUtils.escapar("Dijo \"hola\""));
        assertEquals("\"línea 1\nlínea 2\"", CsvUtils.escapar("línea 1\nlínea 2"));
        assertEquals("", CsvUtils.escapar(null));
    }

    @Test
    public void escapar_neutralizaFormulasPeroNoNumerosNegativos() {
        assertEquals("'=SUMA(A1)", CsvUtils.escapar("=SUMA(A1)"));
        assertEquals("'@cmd", CsvUtils.escapar("@cmd"));
        assertEquals("-150.00", CsvUtils.escapar("-150.00"));
    }

    @Test
    public void importe_usaPuntoDecimalSinMiles() {
        assertEquals("1234.50", CsvUtils.importe(1234.5));
        assertEquals("0.30", CsvUtils.importe(0.1 + 0.2));
    }

    @Test
    public void documento_incluyeBomEncabezadoYFilas() {
        String csv = CsvUtils.documento(new String[]{"Cliente", "Total"},
                Arrays.asList(new String[]{"Ana", "10.00"}, new String[]{"Luis, Jr.", "5.00"}));
        assertTrue(csv.startsWith(CsvUtils.BOM));
        assertEquals(CsvUtils.BOM + "Cliente,Total\r\nAna,10.00\r\n\"Luis, Jr.\",5.00\r\n", csv);
        assertEquals(CsvUtils.BOM + "A\r\n", CsvUtils.documento(new String[]{"A"}, Collections.emptyList()));
    }
}
