package com.example.gestiondecompras.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.gestiondecompras.models.Tienda;

import java.util.List;

@Dao
public interface TiendaDao {
    @Insert
    long insert(Tienda tienda);

    @Update
    int update(Tienda tienda);

    @Delete
    int delete(Tienda tienda);

    @Query("SELECT * FROM tiendas ORDER BY nombre COLLATE NOCASE")
    List<Tienda> getAllTiendas();

    @Query("SELECT * FROM tiendas WHERE TRIM(LOWER(nombre)) = TRIM(LOWER(:nombre)) LIMIT 1")
    Tienda buscarPorNombre(String nombre);
}
