package com.example.gestiondecompras.models.finanzas;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Categoría personalizable de ingresos o gastos. */
@Entity(tableName = "fin_categorias")
public class CategoriaFin {

    public static final String TIPO_INGRESO = "ingreso";
    public static final String TIPO_GASTO = "gasto";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public String nombre;
    /** {@link #TIPO_INGRESO} o {@link #TIPO_GASTO}. */
    public String tipo;
    /** Clave de color de la paleta (ver {@code utils.finanzas.VisualFinanzas}). */
    @ColumnInfo(name = "color_key")
    public String colorKey;
    /** Clave del icono (ver {@code utils.finanzas.VisualFinanzas}). */
    @ColumnInfo(name = "icono_key")
    public String iconoKey;
    public boolean archivada;
    public int orden;

    public boolean esIngreso() {
        return TIPO_INGRESO.equals(tipo);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
