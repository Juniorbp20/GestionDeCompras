package com.example.gestiondecompras.models.finanzas;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Ingreso o gasto registrado en Finanzas. */
@Entity(tableName = "fin_movimientos",
        foreignKeys = {
                @ForeignKey(entity = CategoriaFin.class, parentColumns = "id",
                        childColumns = "categoria_id", onDelete = ForeignKey.SET_NULL),
                @ForeignKey(entity = Cuenta.class, parentColumns = "id",
                        childColumns = "cuenta_id", onDelete = ForeignKey.SET_NULL)
        },
        indices = {@Index("categoria_id"), @Index("cuenta_id"), @Index("fecha_epoch")})
public class Movimiento {

    public static final String TIPO_INGRESO = CategoriaFin.TIPO_INGRESO;
    public static final String TIPO_GASTO = CategoriaFin.TIPO_GASTO;

    @PrimaryKey(autoGenerate = true)
    public long id;
    public String tipo;
    public double monto;
    @ColumnInfo(name = "fecha_epoch")
    public long fechaEpoch;
    public String descripcion;
    public String notas;
    @ColumnInfo(name = "categoria_id")
    public Long categoriaId;
    @ColumnInfo(name = "cuenta_id")
    public Long cuentaId;
    /** Movimiento recurrente que lo generó, si lo hay. */
    @ColumnInfo(name = "recurrente_id")
    public Long recurrenteId;

    public boolean esIngreso() {
        return TIPO_INGRESO.equals(tipo);
    }

    /** Monto con signo: positivo para ingresos, negativo para gastos. */
    public double montoConSigno() {
        return esIngreso() ? monto : -monto;
    }
}
