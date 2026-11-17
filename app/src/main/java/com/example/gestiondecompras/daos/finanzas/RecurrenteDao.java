package com.example.gestiondecompras.daos.finanzas;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.gestiondecompras.models.finanzas.Recurrente;

import java.util.List;

@Dao
public interface RecurrenteDao {
    @Insert
    long insert(Recurrente recurrente);

    @Update
    int update(Recurrente recurrente);

    @Delete
    int delete(Recurrente recurrente);

    @Query("SELECT * FROM fin_recurrentes WHERE id = :id LIMIT 1")
    Recurrente getById(long id);

    @Query("SELECT * FROM fin_recurrentes ORDER BY activo DESC, proxima_fecha_epoch")
    List<Recurrente> getTodos();

    @Query("SELECT * FROM fin_recurrentes WHERE activo = 1 AND proxima_fecha_epoch <= :hasta "
            + "ORDER BY proxima_fecha_epoch")
    List<Recurrente> getVencidos(long hasta);
}
