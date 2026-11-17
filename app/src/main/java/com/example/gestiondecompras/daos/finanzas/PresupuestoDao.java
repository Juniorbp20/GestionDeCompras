package com.example.gestiondecompras.daos.finanzas;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.gestiondecompras.models.finanzas.Presupuesto;

import java.util.List;

@Dao
public interface PresupuestoDao {
    /** Crea o reemplaza el presupuesto de la categoría (índice único por categoría). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long guardar(Presupuesto presupuesto);

    @Query("DELETE FROM fin_presupuestos WHERE categoria_id = :categoriaId")
    int eliminarDeCategoria(long categoriaId);

    @Query("SELECT * FROM fin_presupuestos")
    List<Presupuesto> getTodos();

    @Query("SELECT * FROM fin_presupuestos WHERE categoria_id = :categoriaId LIMIT 1")
    Presupuesto getDeCategoria(long categoriaId);
}
