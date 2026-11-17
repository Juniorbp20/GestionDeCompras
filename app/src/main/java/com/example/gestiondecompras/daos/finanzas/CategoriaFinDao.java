package com.example.gestiondecompras.daos.finanzas;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.gestiondecompras.models.finanzas.CategoriaFin;

import java.util.List;

@Dao
public interface CategoriaFinDao {
    @Insert
    long insert(CategoriaFin categoria);

    @Update
    int update(CategoriaFin categoria);

    @Query("DELETE FROM fin_categorias WHERE id = :id")
    int eliminar(long id);

    @Query("SELECT * FROM fin_categorias WHERE id = :id LIMIT 1")
    CategoriaFin getById(long id);

    @Query("SELECT * FROM fin_categorias ORDER BY tipo, archivada, orden, nombre")
    List<CategoriaFin> getTodas();

    @Query("SELECT * FROM fin_categorias WHERE tipo = :tipo AND archivada = 0 ORDER BY orden, nombre")
    List<CategoriaFin> getActivasPorTipo(String tipo);

    @Query("SELECT * FROM fin_categorias WHERE tipo = :tipo ORDER BY archivada, orden, nombre")
    List<CategoriaFin> getPorTipo(String tipo);

    @Query("SELECT COUNT(*) FROM fin_movimientos WHERE categoria_id = :id")
    int contarMovimientos(long id);

    @Query("SELECT COALESCE(MAX(orden), 0) FROM fin_categorias WHERE tipo = :tipo")
    int ordenMaximo(String tipo);
}
