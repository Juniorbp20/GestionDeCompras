package com.example.gestiondecompras.models.finanzas;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Cuenta o método de pago de Finanzas (efectivo, banco, tarjeta...). */
@Entity(tableName = "fin_cuentas")
public class Cuenta {

    public static final String TIPO_EFECTIVO = "efectivo";
    public static final String TIPO_BANCO = "banco";
    public static final String TIPO_TARJETA = "tarjeta";
    public static final String TIPO_OTRO = "otro";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public String nombre;
    public String tipo;
    @ColumnInfo(name = "saldo_inicial")
    public double saldoInicial;
    public boolean archivada;

    @Override
    public String toString() {
        return nombre;
    }
}
