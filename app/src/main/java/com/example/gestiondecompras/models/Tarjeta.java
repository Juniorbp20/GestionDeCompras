package com.example.gestiondecompras.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tarjetas")
public class Tarjeta {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String banco;
    public String alias;
    @androidx.room.ColumnInfo(name = "limite")
    public double limite;
    @androidx.room.ColumnInfo(name = "deuda")
    public double deudaActual;
    @androidx.room.ColumnInfo(name = "dia_corte")
    public int diaCorte;
    @androidx.room.ColumnInfo(name = "dia_pago", defaultValue = "0")
    public int diaPago; // 1-31; 0 = sin configurar
    @androidx.room.ColumnInfo(name = "fecha_vencimiento")
    public String fechaVencimiento; // MM-YY
    public String notas;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public double getLimite() {
        return limite;
    }

    public void setLimite(double limite) {
        this.limite = limite;
    }

    public double getLimiteCredito() {
        return limite;
    }

    public void setLimiteCredito(double limite) {
        this.limite = limite;
    }

    public double getDeudaActual() {
        return deudaActual;
    }

    public void setDeudaActual(double deudaActual) {
        this.deudaActual = deudaActual;
    }

    public double getDisponible() {
        return Math.max(0, limite - deudaActual);
    }

    public int getDiaCorte() {
        return diaCorte;
    }

    public void setDiaCorte(int diaCorte) {
        this.diaCorte = diaCorte;
    }

    public int getDiaPago() {
        return diaPago;
    }

    public void setDiaPago(int diaPago) {
        this.diaPago = diaPago;
    }

    public String getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(String fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    /**
     * Compatibilidad con el c&oacute;digo existente que mostraba &quot;d&iacute;a de vencimiento&quot;.
     * Para el nuevo esquema (MM-YY) devolvemos la cadena completa.
     */
    public String getDiaVencimiento() {
        return fechaVencimiento;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    @Override
    public String toString() {
        if (alias != null && !alias.isEmpty()) {
            return alias;
        }
        return banco != null ? banco : "";
    }

    public void setDiaVencimiento(int anInt) {
    }
}
