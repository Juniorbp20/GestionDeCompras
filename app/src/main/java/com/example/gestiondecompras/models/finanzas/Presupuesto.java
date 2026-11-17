package com.example.gestiondecompras.models.finanzas;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Límite mensual de gasto para una categoría. */
@Entity(tableName = "fin_presupuestos",
        foreignKeys = @ForeignKey(entity = CategoriaFin.class, parentColumns = "id",
                childColumns = "categoria_id", onDelete = ForeignKey.CASCADE),
        indices = @Index(value = "categoria_id", unique = true))
public class Presupuesto {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @ColumnInfo(name = "categoria_id")
    public long categoriaId;
    @ColumnInfo(name = "monto_mensual")
    public double montoMensual;
}
