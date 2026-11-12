package com.example.gestiondecompras.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.gestiondecompras.models.Gasto;
import com.example.gestiondecompras.models.GastoCategoria;

import java.util.List;

@Dao
public interface GastoDao {
    @Insert
    long insert(Gasto gasto);

    @Update
    int update(Gasto gasto);

    @Delete
    int delete(Gasto gasto);

    @Query("SELECT * FROM gastos ORDER BY fechaEpoch DESC")
    List<Gasto> getAllGastos();

    @Query("SELECT * FROM gastos WHERE (:desde IS NULL OR fechaEpoch >= :desde) "
            + "AND (:hasta IS NULL OR fechaEpoch <= :hasta) "
            + "AND (:categoria = '' OR categoria = :categoria) "
            + "AND (:busqueda = '' OR descripcion LIKE '%' || :busqueda || '%' "
            + "OR categoria LIKE '%' || :busqueda || '%' "
            + "OR notas LIKE '%' || :busqueda || '%') "
            + "ORDER BY fechaEpoch DESC")
    List<Gasto> getGastosFiltrados(Long desde, Long hasta, String busqueda, String categoria);

    @Query("SELECT COALESCE(SUM(monto), 0) FROM gastos WHERE fechaEpoch >= :desde AND fechaEpoch <= :hasta")
    double getTotalRango(long desde, long hasta);

    @Query("SELECT COUNT(*) FROM gastos WHERE fechaEpoch >= :desde AND fechaEpoch <= :hasta")
    int getCantidadRango(long desde, long hasta);

    @Query("SELECT categoria, COUNT(*) AS cantidad, SUM(monto) AS total FROM gastos "
            + "WHERE fechaEpoch >= :desde AND fechaEpoch <= :hasta "
            + "GROUP BY categoria ORDER BY total DESC")
    List<GastoCategoria> getTotalesPorCategoria(long desde, long hasta);
}
