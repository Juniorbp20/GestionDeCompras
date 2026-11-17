package com.example.gestiondecompras.servicios;

import androidx.annotation.NonNull;

import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.Tarjeta;
import com.example.gestiondecompras.utils.Dinero;

/** Reglas de negocio de tarjetas. Métodos síncronos: llamarlos fuera del hilo principal. */
public class TarjetasServicio {

    public enum ResultadoEliminacion { ELIMINADA, EN_USO }

    public enum ResultadoPago { REGISTRADO, MONTO_INVALIDO, EXCEDE_DEUDA }

    private final AppDatabase db;

    public TarjetasServicio(@NonNull AppDatabase db) {
        this.db = db;
    }

    /**
     * Elimina la tarjeta si no tiene pedidos pendientes. Los pedidos antiguos se conservan
     * desvinculados de la tarjeta (su clave foránea impide borrarla de otro modo).
     */
    public ResultadoEliminacion eliminar(@NonNull Tarjeta tarjeta) {
        return db.runInTransaction(() -> {
            if (db.pedidoDao().countPedidosPorTarjeta(tarjeta.id) > 0) {
                return ResultadoEliminacion.EN_USO;
            }
            db.pedidoDao().desvincularTarjeta(tarjeta.id);
            db.tarjetaDao().delete(tarjeta);
            return ResultadoEliminacion.ELIMINADA;
        });
    }

    /** Registra un pago a la tarjeta, reduciendo su deuda. */
    public ResultadoPago registrarPago(long tarjetaId, double monto) {
        return db.runInTransaction(() -> {
            double pago = Dinero.redondear(monto);
            if (!Dinero.esPositivo(pago)) {
                return ResultadoPago.MONTO_INVALIDO;
            }
            Tarjeta tarjeta = db.tarjetaDao().getTarjetaById(tarjetaId);
            if (tarjeta == null) {
                return ResultadoPago.MONTO_INVALIDO;
            }
            if (Dinero.mayorQue(pago, tarjeta.getDeudaActual())) {
                return ResultadoPago.EXCEDE_DEUDA;
            }
            tarjeta.setDeudaActual(Dinero.restar(tarjeta.getDeudaActual(), pago));
            db.tarjetaDao().update(tarjeta);
            return ResultadoPago.REGISTRADO;
        });
    }
}
