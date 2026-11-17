package com.example.gestiondecompras.models.finanzas;

import androidx.room.ColumnInfo;

/** Resultado de agregación: total y cantidad de movimientos por categoría, cuenta o mes. */
public class TotalPorGrupo {
    /** Id del grupo (categoría o cuenta); null si el movimiento no tiene. */
    @ColumnInfo(name = "grupo_id")
    public Long grupoId;
    /** Clave textual del grupo (p. ej. "2026-11" para meses). */
    public String clave;
    public String tipo;
    public double total;
    public int cantidad;
}
