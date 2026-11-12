package com.example.gestiondecompras.models;

import java.util.Collections;
import java.util.List;

/**
 * Resumen financiero del periodo seleccionado en la pantalla de Finanzas.
 */
public class ResumenFinanciero {

    public double ingresos;
    public double gastos;
    public double balance;
    public int cantidadGastos;

    /** Solo se calcula cuando el periodo tiene fechas completas. */
    public boolean mostrarPromedio;
    public double promedioDiario;

    /** Comparación contra un periodo anterior de la misma duración. */
    public boolean hayPeriodoAnterior;
    public double gastosPeriodoAnterior;
    public double variacionPorcentual;

    public List<GastoCategoria> categorias = Collections.emptyList();
}
