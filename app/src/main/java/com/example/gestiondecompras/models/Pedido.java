package com.example.gestiondecompras.models;

import com.example.gestiondecompras.utils.Dinero;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

import androidx.room.Index;

@Entity(tableName = "pedidos",
        foreignKeys = {
                @ForeignKey(entity = Cliente.class, parentColumns = "id", childColumns = "cliente_id"),
                @ForeignKey(entity = Tienda.class, parentColumns = "id", childColumns = "tienda_id"),
                @ForeignKey(entity = Tarjeta.class, parentColumns = "id", childColumns = "tarjeta_rel_id")
        },
        indices = {
                @Index("cliente_id"),
                @Index("tienda_id"),
                @Index("tarjeta_rel_id")
        })
public class Pedido {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @ColumnInfo(name = "cliente_id")
    public long clienteId;
    @ColumnInfo(name = "tienda_id")
    public Long tiendaId;
    @ColumnInfo(name = "tarjeta_rel_id")
    public Long tarjetaId;
    @ColumnInfo(name = "cliente_nombre", defaultValue = "")
    public String clienteNombre;
    @ColumnInfo(name = "tienda", defaultValue = "")
    public String tiendaNombre;
    @ColumnInfo(name = "tarjeta_alias", defaultValue = "")
    public String tarjetaAlias;
    @ColumnInfo(name = "monto_compra")
    public double montoCompra;
    public double ganancia;
    @ColumnInfo(name = "total_general")
    public double totalGeneral;
    @ColumnInfo(name = "fecha_registro_epoch", defaultValue = "0")
    public Long fechaRegistroEpoch = 0L;
    @ColumnInfo(name = "fecha_entrega")
    public Long fechaEntregaEpoch;
    public String notas;

    /** Monto de la compra en dólares (null si se compró en pesos). */
    @ColumnInfo(name = "monto_usd")
    public Double montoUsd;
    /** Tasa RD$ por US$ usada para convertir {@link #montoUsd}. */
    @ColumnInfo(name = "tasa_cambio")
    public Double tasaCambio;
    /** Número de rastreo del envío (opcional). */
    public String rastreo;
    /** Artículo o descripción de lo comprado (opcional). */
    public String articulo;
    /** Nombre del archivo de la foto del recibo en files/recibos (opcional). */
    @ColumnInfo(name = "foto_recibo")
    public String fotoRecibo;

    @NonNull
    @ColumnInfo(name = "estado", defaultValue = ESTADO_PENDIENTE)
    public String estado = ESTADO_PENDIENTE;

    @Ignore
    public double totalAbonado;

    public static final String ESTADO_PENDIENTE = "pendiente";
    public static final String ESTADO_ENTREGADO = "entregado";
    public static final String ESTADO_PAGADO = "pagado";
    public static final String ESTADO_CANCELADO = "cancelado";
    /** El pedido ya salió de la tienda pero aún no se ha entregado. */
    public static final String ESTADO_EN_CAMINO = "en camino";

    public String getArticulo() {
        return articulo;
    }

    public String getRastreo() {
        return rastreo;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getClienteId() {
        return clienteId;
    }

    public void setClienteId(long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public void setTiendaId(Long tiendaId) {
        this.tiendaId = tiendaId;
    }

    public Long getTarjetaId() {
        return tarjetaId;
    }

    public void setTarjetaId(Long tarjetaId) {
        this.tarjetaId = tarjetaId;
    }

    public String getClienteNombre() {
        return clienteNombre != null ? clienteNombre : "";
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getTienda() {
        return tiendaNombre != null ? tiendaNombre : "";
    }

    public void setTienda(String tiendaNombre) {
        this.tiendaNombre = tiendaNombre;
    }

    public String getTarjetaAlias() {
        return tarjetaAlias;
    }

    public void setTarjetaAlias(String tarjetaAlias) {
        this.tarjetaAlias = tarjetaAlias;
    }

    public double getMontoCompra() {
        return montoCompra;
    }

    public void setMontoCompra(double montoCompra) {
        this.montoCompra = montoCompra;
    }

    public double getGanancia() {
        return ganancia;
    }

    public void setGanancia(double ganancia) {
        this.ganancia = ganancia;
    }

    public double getTotalGeneral() {
        return totalGeneral;
    }

    public void setTotalGeneral(double totalGeneral) {
        this.totalGeneral = totalGeneral;
    }

    public Long getFechaRegistroEpoch() {
        return fechaRegistroEpoch;
    }

    public void setFechaRegistroEpoch(Long fechaRegistroEpoch) {
        this.fechaRegistroEpoch = fechaRegistroEpoch;
    }

    public Date getFechaRegistro() {
        return fechaRegistroEpoch != null ? new Date(fechaRegistroEpoch) : null;
    }

    public Long getFechaEntregaEpoch() {
        return fechaEntregaEpoch;
    }

    public void setFechaEntregaEpoch(Long fechaEntregaEpoch) {
        this.fechaEntregaEpoch = fechaEntregaEpoch;
    }

    public Date getFechaEntrega() {
        return fechaEntregaEpoch != null ? new Date(fechaEntregaEpoch) : null;
    }

    public void setFechaEntrega(Date fechaEntrega) {
        this.fechaEntregaEpoch = fechaEntrega != null ? fechaEntrega.getTime() : null;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    @NonNull
    public String getEstado() {
        return estado;
    }

    public void setEstado(@NonNull String estado) {
        this.estado = estado;
    }

    public double getTotalAbonado() {
        return totalAbonado;
    }

    public void setTotalAbonado(double totalAbonado) {
        this.totalAbonado = totalAbonado;
    }

    public double getSaldo() {
        return Dinero.saldo(totalGeneral, totalAbonado);
    }

    public boolean estaPagadoCompleto() {
        return Dinero.mayorOIgual(totalAbonado, totalGeneral);
    }
}
