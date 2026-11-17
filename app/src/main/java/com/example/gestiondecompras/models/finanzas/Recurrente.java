package com.example.gestiondecompras.models.finanzas;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Ingreso o gasto que se registra solo con una frecuencia fija. */
@Entity(tableName = "fin_recurrentes",
        foreignKeys = {
                @ForeignKey(entity = CategoriaFin.class, parentColumns = "id",
                        childColumns = "categoria_id", onDelete = ForeignKey.SET_NULL),
                @ForeignKey(entity = Cuenta.class, parentColumns = "id",
                        childColumns = "cuenta_id", onDelete = ForeignKey.SET_NULL)
        },
        indices = {@Index("categoria_id"), @Index("cuenta_id")})
public class Recurrente {

    public static final String FREC_SEMANAL = "semanal";
    public static final String FREC_QUINCENAL = "quincenal";
    public static final String FREC_MENSUAL = "mensual";
    public static final String FREC_ANUAL = "anual";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public String tipo;
    public double monto;
    public String descripcion;
    public String notas;
    @ColumnInfo(name = "categoria_id")
    public Long categoriaId;
    @ColumnInfo(name = "cuenta_id")
    public Long cuentaId;
    public String frecuencia;
    @ColumnInfo(name = "proxima_fecha_epoch")
    public long proximaFechaEpoch;
    /** Día del mes de referencia (1-31) para frecuencias mensual/anual; evita que el 31 derive al 28. */
    @ColumnInfo(name = "dia_ancla")
    public int diaAncla;
    public boolean activo;
}
