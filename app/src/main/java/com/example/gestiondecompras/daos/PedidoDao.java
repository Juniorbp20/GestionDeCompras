package com.example.gestiondecompras.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.gestiondecompras.models.DashboardRow;
import com.example.gestiondecompras.models.Pedido;

import java.util.List;

@Dao
public interface PedidoDao {
    @Insert
    long insert(Pedido p);

    @Update
    int update(Pedido p);

    @Delete
    int delete(Pedido p);

    @Query("SELECT * FROM pedidos WHERE id = :id LIMIT 1")
    Pedido getPedidoById(long id);

    @Query("SELECT * FROM pedidos WHERE (:estado = '' OR estado = :estado) AND (:busqueda = '' OR cliente_nombre LIKE '%' || :busqueda || '%') AND (:clienteId IS NULL OR cliente_id = :clienteId) ORDER BY fecha_registro_epoch DESC")
    List<Pedido> getPedidosFiltrados(String estado, String busqueda, Integer clienteId);

    @Query("SELECT * FROM pedidos WHERE date(fecha_registro_epoch/1000,'unixepoch')=date(:epoch/1000,'unixepoch') ORDER BY fecha_registro_epoch ASC")
    List<Pedido> pedidosPorDia(long epoch);

    @Query("SELECT * FROM pedidos WHERE fecha_entrega IS NOT NULL AND date(fecha_entrega/1000,'unixepoch')=date(:epoch/1000,'unixepoch') ORDER BY fecha_entrega ASC")
    List<Pedido> getEntregasPorDia(long epoch);

    @Query("SELECT * FROM pedidos WHERE estado = 'pendiente' AND fecha_entrega IS NOT NULL " +
           "AND fecha_entrega >= :desde AND fecha_entrega <= :hasta ORDER BY fecha_entrega ASC")
    List<Pedido> getEntregasPendientesEnRango(long desde, long hasta);

    @Query("SELECT * FROM pedidos WHERE date(fecha_registro_epoch/1000,'unixepoch')=date(:epoch/1000,'unixepoch') AND (:busqueda = '' OR cliente_nombre LIKE '%' || :busqueda || '%') ORDER BY fecha_registro_epoch ASC")
    List<Pedido> pedidosPorDiaYCliente(long epoch, String busqueda);

    @Query("SELECT * FROM pedidos WHERE (:estado = '' OR estado = :estado) ORDER BY fecha_registro_epoch DESC")
    List<Pedido> findByEstado(String estado);

    @Query("SELECT " +
           "COALESCE(SUM(CASE WHEN p.estado != 'cancelado' THEN p.total_general ELSE 0 END), 0) - " +
           "COALESCE((SELECT SUM(a.monto) FROM abonos a INNER JOIN pedidos p2 ON p2.id = a.pedido_id " +
           "WHERE p2.estado != 'cancelado'), 0) AS totalPendiente, " +
           "COALESCE((SELECT SUM(a.monto) FROM abonos a INNER JOIN pedidos p2 ON p2.id = a.pedido_id " +
           "WHERE p2.estado != 'cancelado'), 0) AS totalPagado, " +
           "COUNT(CASE WHEN date(p.fecha_registro_epoch/1000,'unixepoch')=date('now') THEN 1 END) AS pedidosHoy " +
           "FROM pedidos p")
    DashboardRow getDashboard();

    @Query("SELECT cliente_nombre AS clienteNombre, SUM(ganancia) AS totalGanancia FROM pedidos WHERE estado != 'cancelado' GROUP BY cliente_id ORDER BY totalGanancia DESC")
    List<com.example.gestiondecompras.models.ClienteGanancia> getGananciasPorCliente();

    @Query("SELECT COUNT(*) FROM pedidos WHERE estado = 'pendiente' AND fecha_entrega < :todayEpoch")
    int getOverdueOrdersCount(long todayEpoch);

    @Query("SELECT COUNT(*) FROM pedidos WHERE estado = 'pendiente' AND fecha_entrega IS NOT NULL AND fecha_entrega < :inicioHoy")
    int countAtrasados(long inicioHoy);

    @Query("SELECT COUNT(*) FROM pedidos WHERE estado != 'cancelado' " +
           "AND fecha_registro_epoch >= :desde AND fecha_registro_epoch <= :hasta")
    int countPedidosEnRango(long desde, long hasta);

    @Query("SELECT COALESCE(SUM(total_general), 0) FROM pedidos WHERE estado != 'cancelado' " +
           "AND fecha_registro_epoch >= :desde AND fecha_registro_epoch <= :hasta")
    double totalVentasEnRango(long desde, long hasta);

    @Query("SELECT COALESCE(SUM(ganancia), 0) FROM pedidos WHERE estado != 'cancelado' " +
           "AND fecha_registro_epoch >= :desde AND fecha_registro_epoch <= :hasta")
    double totalGananciaEnRango(long desde, long hasta);

    @Query("SELECT COUNT(*) FROM pedidos WHERE estado != 'cancelado' AND fecha_entrega IS NOT NULL " +
           "AND fecha_entrega >= :desde AND fecha_entrega <= :hasta")
    int countEntregasEnRango(long desde, long hasta);

    @Query("SELECT COUNT(*) FROM pedidos p WHERE p.estado != 'pagado' AND p.estado != 'cancelado' " +
           "AND p.total_general - COALESCE((SELECT SUM(a.monto) FROM abonos a WHERE a.pedido_id = p.id), 0) > 0.001")
    int countPorCobrar();

    @Query("SELECT COALESCE(SUM(p.total_general - COALESCE((SELECT SUM(a.monto) FROM abonos a WHERE a.pedido_id = p.id), 0)), 0) " +
           "FROM pedidos p WHERE p.estado != 'pagado' AND p.estado != 'cancelado' " +
           "AND p.total_general - COALESCE((SELECT SUM(a.monto) FROM abonos a WHERE a.pedido_id = p.id), 0) > 0.001")
    double totalPorCobrarGlobal();

    @Query("SELECT * FROM pedidos WHERE estado = 'pendiente' AND fecha_entrega IS NOT NULL AND fecha_entrega < :todayEpoch ORDER BY fecha_entrega ASC")
    List<Pedido> getPedidosAtrasados(long todayEpoch);

    @Query("SELECT * FROM pedidos WHERE fecha_entrega IS NOT NULL AND fecha_entrega >= :fromEpoch ORDER BY fecha_entrega ASC LIMIT :limit")
    List<Pedido> getProximosPedidos(long fromEpoch, int limit);

    @Query("SELECT * FROM pedidos WHERE estado != 'pagado' AND estado != 'cancelado' " +
           "AND (:desde IS NULL OR fecha_registro_epoch >= :desde) " +
           "AND (:hasta IS NULL OR fecha_registro_epoch <= :hasta) " +
           "AND (:busqueda = '' OR cliente_nombre LIKE '%' || :busqueda || '%') " +
           "ORDER BY fecha_registro_epoch DESC")
    List<Pedido> getPedidosPorCobrar(Long desde, Long hasta, String busqueda);

    @Query("SELECT * FROM pedidos WHERE estado = 'pagado' " +
           "AND (:desde IS NULL OR fecha_registro_epoch >= :desde) " +
           "AND (:hasta IS NULL OR fecha_registro_epoch <= :hasta) " +
           "AND (:busqueda = '' OR cliente_nombre LIKE '%' || :busqueda || '%') " +
           "ORDER BY fecha_registro_epoch DESC")
    List<Pedido> getPedidosPagadosFiltrados(Long desde, Long hasta, String busqueda);

    @Query("SELECT * FROM pedidos WHERE estado = 'pagado' ORDER BY fecha_registro_epoch DESC")
    List<Pedido> getPedidosPagados();

    @Query("SELECT COUNT(*) FROM pedidos WHERE tarjeta_rel_id = :tarjetaId AND estado = 'pendiente'")
    int countPedidosPorTarjeta(long tarjetaId);

    @Query("SELECT * FROM pedidos WHERE estado != 'pagado' AND estado != 'cancelado' ORDER BY fecha_registro_epoch DESC")
    List<Pedido> getPedidosNoPagados();
}
