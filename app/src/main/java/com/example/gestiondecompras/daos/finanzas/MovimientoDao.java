package com.example.gestiondecompras.daos.finanzas;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.models.finanzas.TotalPorGrupo;

import java.util.List;

@Dao
public interface MovimientoDao {
    @Insert
    long insert(Movimiento movimiento);

    @Update
    int update(Movimiento movimiento);

    @Delete
    int delete(Movimiento movimiento);

    @Query("SELECT * FROM fin_movimientos WHERE id = :id LIMIT 1")
    Movimiento getById(long id);

    /**
     * Búsqueda con filtros combinables. Los filtros vacíos se ignoran:
     * tipo '' = ambos, ids/fechas/montos null = sin límite.
     */
    @Query("SELECT m.* FROM fin_movimientos m "
            + "LEFT JOIN fin_categorias c ON c.id = m.categoria_id "
            + "WHERE (:tipo = '' OR m.tipo = :tipo) "
            + "AND (:categoriaId IS NULL OR m.categoria_id = :categoriaId) "
            + "AND (:cuentaId IS NULL OR m.cuenta_id = :cuentaId) "
            + "AND (:desde IS NULL OR m.fecha_epoch >= :desde) "
            + "AND (:hasta IS NULL OR m.fecha_epoch <= :hasta) "
            + "AND (:montoMin IS NULL OR m.monto >= :montoMin) "
            + "AND (:montoMax IS NULL OR m.monto <= :montoMax) "
            + "AND (:texto = '' OR m.descripcion LIKE '%' || :texto || '%' "
            + "     OR m.notas LIKE '%' || :texto || '%' OR c.nombre LIKE '%' || :texto || '%') "
            + "ORDER BY m.fecha_epoch DESC, m.id DESC")
    List<Movimiento> buscar(String tipo, Long categoriaId, Long cuentaId, Long desde, Long hasta,
                            Double montoMin, Double montoMax, String texto);

    @Query("SELECT * FROM fin_movimientos ORDER BY fecha_epoch DESC, id DESC LIMIT :limite")
    List<Movimiento> recientes(int limite);

    @Query("SELECT COALESCE(SUM(monto), 0) FROM fin_movimientos "
            + "WHERE tipo = :tipo AND fecha_epoch >= :desde AND fecha_epoch <= :hasta")
    double total(String tipo, long desde, long hasta);

    @Query("SELECT COUNT(*) FROM fin_movimientos WHERE fecha_epoch >= :desde AND fecha_epoch <= :hasta")
    int cantidad(long desde, long hasta);

    @Query("SELECT categoria_id AS grupo_id, NULL AS clave, tipo, SUM(monto) AS total, COUNT(*) AS cantidad "
            + "FROM fin_movimientos WHERE tipo = :tipo AND fecha_epoch >= :desde AND fecha_epoch <= :hasta "
            + "GROUP BY categoria_id ORDER BY total DESC")
    List<TotalPorGrupo> totalesPorCategoria(String tipo, long desde, long hasta);

    @Query("SELECT cuenta_id AS grupo_id, NULL AS clave, tipo, SUM(monto) AS total, COUNT(*) AS cantidad "
            + "FROM fin_movimientos WHERE fecha_epoch >= :desde AND fecha_epoch <= :hasta "
            + "GROUP BY cuenta_id, tipo ORDER BY total DESC")
    List<TotalPorGrupo> totalesPorCuenta(long desde, long hasta);

    /** Totales por mes (clave "AAAA-MM", hora local) y tipo. */
    @Query("SELECT NULL AS grupo_id, strftime('%Y-%m', fecha_epoch / 1000, 'unixepoch', 'localtime') AS clave, "
            + "tipo, SUM(monto) AS total, COUNT(*) AS cantidad FROM fin_movimientos "
            + "WHERE fecha_epoch >= :desde AND fecha_epoch <= :hasta GROUP BY clave, tipo ORDER BY clave")
    List<TotalPorGrupo> totalesPorMes(long desde, long hasta);

    @Query("SELECT * FROM fin_movimientos WHERE tipo = :tipo AND fecha_epoch >= :desde AND fecha_epoch <= :hasta "
            + "ORDER BY monto DESC LIMIT :limite")
    List<Movimiento> mayores(String tipo, long desde, long hasta, int limite);

    @Query("SELECT COALESCE(SUM(monto), 0) FROM fin_movimientos WHERE tipo = 'gasto' "
            + "AND categoria_id = :categoriaId AND fecha_epoch >= :desde AND fecha_epoch <= :hasta")
    double gastoDeCategoria(long categoriaId, long desde, long hasta);

    @Query("SELECT COUNT(*) FROM fin_movimientos WHERE recurrente_id = :recurrenteId AND fecha_epoch = :fecha")
    int existeGenerado(long recurrenteId, long fecha);

    @Query("SELECT * FROM fin_movimientos WHERE descripcion LIKE '%' || :texto || '%' "
            + "OR notas LIKE '%' || :texto || '%' ORDER BY fecha_epoch DESC LIMIT :limite")
    List<Movimiento> buscarTexto(String texto, int limite);
}
