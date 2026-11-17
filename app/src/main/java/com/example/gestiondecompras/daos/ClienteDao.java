package com.example.gestiondecompras.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.gestiondecompras.models.Cliente;

import java.util.List;

@Dao
public interface ClienteDao {
    @Insert
    long insert(Cliente cliente);

    @Update
    int update(Cliente cliente);

    @Delete
    int delete(Cliente cliente);

    @Query("SELECT * FROM clientes")
    List<Cliente> getAllClientes();

    @Query("SELECT * FROM clientes WHERE id = :id LIMIT 1")
    Cliente getClienteById(long id);

    @Query("SELECT c.*, " +
           "(SELECT COUNT(*) FROM pedidos p WHERE p.cliente_id = c.id) as cantidadPedidos, " +
           "(SELECT COALESCE(SUM(total_general), 0) FROM pedidos p WHERE p.cliente_id = c.id) as totalCompras " +
           "FROM clientes c")
    List<com.example.gestiondecompras.models.ClienteWithMetrics> getAllClientesWithMetrics();
}
