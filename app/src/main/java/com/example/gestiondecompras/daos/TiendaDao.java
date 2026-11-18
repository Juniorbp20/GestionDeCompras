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

    /** Tienda con la cantidad de pedidos que la usan (pantalla de Tiendas). */
    class TiendaConConteo {
        public long id;
        public String nombre;
        public int pedidos;
    }

    @Query("SELECT t.id AS id, t.nombre AS nombre, "
            + "(SELECT COUNT(*) FROM pedidos p WHERE p.tienda_id = t.id) AS pedidos "
            + "FROM tiendas t ORDER BY t.nombre COLLATE NOCASE")
    List<TiendaConConteo> getTiendasConConteo();

    @Query("SELECT COUNT(*) FROM pedidos WHERE tienda_id = :tiendaId")
    int contarPedidos(long tiendaId);

    @Query("UPDATE tiendas SET nombre = :nombre WHERE id = :tiendaId")
    int actualizarNombre(long tiendaId, String nombre);

    /** Los pedidos guardan una copia del nombre de la tienda: se actualiza al renombrar. */
    @Query("UPDATE pedidos SET tienda = :nombre WHERE tienda_id = :tiendaId")
    int renombrarEnPedidos(long tiendaId, String nombre);

    @Query("DELETE FROM tiendas WHERE id = :tiendaId")
    int eliminarPorId(long tiendaId);
}
