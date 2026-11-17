package com.example.gestiondecompras.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Cuenta;
import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.utils.Dinero;
import com.example.gestiondecompras.utils.finanzas.RecurrenciaUtils;
import com.example.gestiondecompras.utils.finanzas.UiFinanzas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Lista de movimientos agrupada por día, con el neto de cada día en la cabecera. */
public class MovimientosAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onMovimientoClick(Movimiento m);
        void onMovimientoAcciones(Movimiento m);
    }

    private static final int TIPO_DIA = 0;
    private static final int TIPO_MOVIMIENTO = 1;

    /** Elemento de la lista: cabecera de día o movimiento. */
    private static final class Fila {
        final long dia;
        final double neto;
        final Movimiento movimiento;

        Fila(long dia, double neto, Movimiento movimiento) {
            this.dia = dia;
            this.neto = neto;
            this.movimiento = movimiento;
        }
    }

    private final Listener listener;
    private final List<Fila> filas = new ArrayList<>();
    private Map<Long, CategoriaFin> categorias = Collections.emptyMap();
    private Map<Long, Cuenta> cuentas = Collections.emptyMap();

    public MovimientosAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setCatalogos(Map<Long, CategoriaFin> categorias, Map<Long, Cuenta> cuentas) {
        this.categorias = categorias != null ? categorias : Collections.emptyMap();
        this.cuentas = cuentas != null ? cuentas : Collections.emptyMap();
        notifyDataSetChanged();
    }

    public void setMovimientos(List<Movimiento> movimientos) {
        filas.clear();
        if (movimientos != null) {
            int i = 0;
            while (i < movimientos.size()) {
                long dia = RecurrenciaUtils.inicioDelDia(movimientos.get(i).fechaEpoch);
                int j = i;
                double neto = 0;
                while (j < movimientos.size() && RecurrenciaUtils.inicioDelDia(movimientos.get(j).fechaEpoch) == dia) {
                    neto = Dinero.sumar(neto, movimientos.get(j).montoConSigno());
                    j++;
                }
                filas.add(new Fila(dia, neto, null));
                for (int k = i; k < j; k++) {
                    filas.add(new Fila(dia, 0, movimientos.get(k)));
                }
                i = j;
            }
        }
        notifyDataSetChanged();
    }

    /** Quita un movimiento de la lista visible (para "Deshacer"). */
    public void quitar(Movimiento m) {
        List<Movimiento> restantes = new ArrayList<>();
        for (Fila f : filas) {
            if (f.movimiento != null && f.movimiento.id != m.id) restantes.add(f.movimiento);
        }
        setMovimientos(restantes);
    }

    public Movimiento getMovimientoAt(int posicion) {
        return posicion >= 0 && posicion < filas.size() ? filas.get(posicion).movimiento : null;
    }

    public int getCantidadMovimientos() {
        int n = 0;
        for (Fila f : filas) if (f.movimiento != null) n++;
        return n;
    }

    @Override
    public int getItemViewType(int position) {
        return filas.get(position).movimiento == null ? TIPO_DIA : TIPO_MOVIMIENTO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_DIA) {
            return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_fin_dia, parent, false)) { };
        }
        return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_movimiento, parent, false)) { };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Fila f = filas.get(position);
        View v = holder.itemView;
        if (f.movimiento == null) {
            ((TextView) v.findViewById(R.id.tvFecha)).setText(UiFinanzas.fechaLarga(f.dia));
            ((TextView) v.findViewById(R.id.tvSubtotal)).setText(UiFinanzas.monedaConSigno(f.neto));
            return;
        }
        Movimiento m = f.movimiento;
        UiFinanzas.enlazarMovimiento(v, m, categorias, cuentas, false);
        v.setOnClickListener(x -> listener.onMovimientoClick(m));
        v.setOnLongClickListener(x -> {
            listener.onMovimientoAcciones(m);
            return true;
        });
        v.findViewById(R.id.btnMas).setOnClickListener(x -> listener.onMovimientoAcciones(m));
    }

    @Override
    public int getItemCount() {
        return filas.size();
    }
}
