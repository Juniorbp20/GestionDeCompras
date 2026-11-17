package com.example.gestiondecompras.servicios;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.Abono;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.models.Tarjeta;
import com.example.gestiondecompras.utils.Dinero;

/**
 * Reglas de negocio de pedidos y abonos. Recibe la base de datos para poder probarse
 * con una base en memoria. Todos los métodos son síncronos: llamarlos fuera del hilo principal.
 *
 * La deuda de cada tarjeta se mantiene automáticamente: una compra suma su monto a la tarjeta
 * con la que se pagó; editarla ajusta la diferencia (o la mueve si cambió de tarjeta) y
 * eliminarla la descuenta.
 */
public class PedidosServicio {

    private final AppDatabase db;

    public PedidosServicio(@NonNull AppDatabase db) {
        this.db = db;
    }

    /** Crea o actualiza un pedido y ajusta la deuda de las tarjetas implicadas. Devuelve su id. */
    public long guardarPedido(@NonNull Pedido pedido, double abonoInicial, @Nullable String metodoAbono) {
        return db.runInTransaction(() -> {
            pedido.montoCompra = Dinero.redondear(pedido.montoCompra);
            pedido.ganancia = Dinero.redondear(pedido.ganancia);
            pedido.totalGeneral = Dinero.redondear(pedido.totalGeneral);

            if (pedido.getId() > 0) {
                Pedido anterior = db.pedidoDao().getPedidoById(pedido.getId());
                if (anterior != null) {
                    // Se revierte la compra anterior y se aplica la nueva (cubre cambio de monto y de tarjeta)
                    ajustarDeuda(anterior.tarjetaId, -anterior.getMontoCompra());
                }
                ajustarDeuda(pedido.tarjetaId, pedido.getMontoCompra());
                db.pedidoDao().update(pedido);
                return pedido.getId();
            }

            long id = db.pedidoDao().insert(pedido);
            pedido.id = id;
            ajustarDeuda(pedido.tarjetaId, pedido.getMontoCompra());

            if (Dinero.esPositivo(abonoInicial)) {
                Abono abono = new Abono();
                abono.pedidoId = id;
                abono.monto = Dinero.redondear(abonoInicial);
                abono.fechaEpoch = System.currentTimeMillis();
                abono.metodo = metodoAbono != null ? metodoAbono : Abono.METODO_EFECTIVO;
                db.abonoDao().insert(abono);
            }
            return id;
        });
    }

    /** Elimina el pedido (sus abonos se borran en cascada) y descuenta la compra de su tarjeta. */
    public void eliminarPedido(@NonNull Pedido pedido) {
        db.runInTransaction(() -> {
            Pedido actual = db.pedidoDao().getPedidoById(pedido.getId());
            Pedido referencia = actual != null ? actual : pedido;
            ajustarDeuda(referencia.tarjetaId, -referencia.getMontoCompra());
            db.pedidoDao().delete(referencia);
        });
    }

    /** Registra un abono y marca el pedido como pagado si queda saldado. */
    public void registrarAbono(@NonNull Pedido pedido, double monto, String metodo, String notas) {
        db.runInTransaction(() -> {
            Abono abono = new Abono();
            abono.pedidoId = pedido.getId();
            abono.monto = Dinero.redondear(monto);
            abono.fechaEpoch = System.currentTimeMillis();
            abono.metodo = metodo;
            abono.notas = notas;
            db.abonoDao().insert(abono);

            pedido.totalAbonado = db.abonoDao().getTotalAbonado(pedido.getId());
            if (pedido.estaPagadoCompleto()) {
                pedido.estado = Pedido.ESTADO_PAGADO;
            }
            db.pedidoDao().update(pedido);
        });
    }

    /** Elimina un abono; si el pedido estaba pagado y deja de estarlo, vuelve a pendiente. */
    public void eliminarAbono(@NonNull Pedido pedido, @NonNull Abono abono) {
        db.runInTransaction(() -> {
            db.abonoDao().delete(abono);
            pedido.totalAbonado = db.abonoDao().getTotalAbonado(pedido.getId());
            if (Pedido.ESTADO_PAGADO.equalsIgnoreCase(pedido.getEstado()) && !pedido.estaPagadoCompleto()) {
                pedido.estado = Pedido.ESTADO_PENDIENTE;
            }
            db.pedidoDao().update(pedido);
        });
    }

    /** Suma (o resta, si es negativo) un monto a la deuda de la tarjeta; nunca queda por debajo de 0. */
    private void ajustarDeuda(@Nullable Long tarjetaId, double monto) {
        if (tarjetaId == null || tarjetaId <= 0 || Dinero.esCero(monto)) {
            return;
        }
        Tarjeta tarjeta = db.tarjetaDao().getTarjetaById(tarjetaId);
        if (tarjeta == null) {
            return;
        }
        double nueva = Dinero.sumar(tarjeta.getDeudaActual(), monto);
        tarjeta.setDeudaActual(Math.max(0, nueva));
        db.tarjetaDao().update(tarjeta);
    }
}
