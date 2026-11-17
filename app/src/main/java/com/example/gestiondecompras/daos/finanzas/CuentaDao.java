package com.example.gestiondecompras.daos.finanzas;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.gestiondecompras.models.finanzas.Cuenta;

import java.util.List;

@Dao
public interface CuentaDao {
    @Insert
    long insert(Cuenta cuenta);

    @Update
    int update(Cuenta cuenta);

    @Query("DELETE FROM fin_cuentas WHERE id = :id")
    int eliminar(long id);

    @Query("SELECT * FROM fin_cuentas WHERE id = :id LIMIT 1")
    Cuenta getById(long id);

    @Query("SELECT * FROM fin_cuentas ORDER BY archivada, nombre")
    List<Cuenta> getTodas();

    @Query("SELECT * FROM fin_cuentas WHERE archivada = 0 ORDER BY nombre")
    List<Cuenta> getActivas();

    /** Busca una cuenta (activa o archivada) por nombre, sin distinguir mayúsculas ni espacios. */
    @Query("SELECT * FROM fin_cuentas WHERE TRIM(LOWER(nombre)) = TRIM(LOWER(:nombre)) LIMIT 1")
    Cuenta buscarPorNombre(String nombre);

    @Query("SELECT COUNT(*) FROM fin_movimientos WHERE cuenta_id = :id")
    int contarMovimientos(long id);

    /** Ingresos menos gastos de la cuenta (sin el saldo inicial). */
    @Query("SELECT COALESCE(SUM(CASE WHEN tipo = 'ingreso' THEN monto ELSE -monto END), 0) "
            + "FROM fin_movimientos WHERE cuenta_id = :id")
    double movimientoNeto(long id);
}
