package com.example.gestiondecompras.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.gestiondecompras.models.Tarjeta;

import java.util.List;

@Dao
public interface TarjetaDao {
    @Insert
    long insert(Tarjeta tarjeta);

    @Update
    int update(Tarjeta tarjeta);

    @Delete
    int delete(Tarjeta tarjeta);

    @Query("SELECT * FROM tarjetas")
    List<Tarjeta> getAllTarjetas();

    @Query("SELECT * FROM tarjetas WHERE id = :id LIMIT 1")
    Tarjeta getTarjetaById(long id);
}
