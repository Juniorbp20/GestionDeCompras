package com.example.gestiondecompras.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import com.google.android.material.card.MaterialCardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.models.Pedido;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PedidosAdapter extends RecyclerView.Adapter<PedidosAdapter.PedidoViewHolder> {

    public interface OnPedidoClickListener {
        void onPedidoClick(Pedido pedido);
        void onPedidoLongClick(Pedido pedido);
    }

    private List<Pedido> pedidos;
    private final OnPedidoClickListener listener;

    public PedidosAdapter(OnPedidoClickListener listener) {
        this.pedidos = new java.util.ArrayList<>();
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override public long getItemId(int position) {
        // Asegúrate que getId() sea único y > 0; si puede ser 0, usa -position
        return pedidos.get(position).getId();
    }

    @NonNull @Override
    public PedidoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pedido, parent, false);
        return new PedidoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PedidoViewHolder holder, int position) {
        holder.bind(pedidos.get(position));
    }

    @Override public int getItemCount() { return pedidos == null ? 0 : pedidos.size(); }

    public void actualizarLista(List<Pedido> nuevos) {
        this.pedidos = nuevos;
        notifyDataSetChanged();
    }

    /** Quita un pedido de la lista visible (sin tocar la base de datos). */
    public void quitar(Pedido pedido) {
        if (pedidos == null || pedido == null) return;
        int pos = pedidos.indexOf(pedido);
        if (pos >= 0) {
            pedidos = new java.util.ArrayList<>(pedidos);
            pedidos.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    public Pedido getPedidoAt(int position) {
        if (pedidos == null || position < 0 || position >= pedidos.size()) return null;
        return pedidos.get(position);
    }

    class PedidoViewHolder extends RecyclerView.ViewHolder {
        TextView tvCliente, tvTienda, tvTotal, tvFechaEntrega, tvGanancia, tvMontoCompra, tvAbonadoSaldo;
        Chip chipEstado;
        MaterialCardView card;
        View viewAccentEstado;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        PedidoViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardView);
            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvTienda = itemView.findViewById(R.id.tvTienda);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvFechaEntrega = itemView.findViewById(R.id.tvFechaEntrega);
            tvGanancia = itemView.findViewById(R.id.tvGanancia);
            tvMontoCompra = itemView.findViewById(R.id.tvMontoCompra);
            tvAbonadoSaldo = itemView.findViewById(R.id.tvAbonadoSaldo);
            chipEstado = itemView.findViewById(R.id.tvEstado);
            viewAccentEstado = itemView.findViewById(R.id.viewAccentEstado);

            // Click SIEMPRE toma el item por posición actual:
            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPedidoClick(pedidos.get(pos));
                }
            });
            itemView.setOnLongClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPedidoLongClick(pedidos.get(pos));
                }
                return true;
            });

            // Click en estado (cambio rápido de estado)
            // Botón ⋮: mismas acciones que la pulsación larga (oculto si no hay acciones)
            View btnMas = itemView.findViewById(R.id.btnMas);
            if (btnMas != null) {
                btnMas.setVisibility(listener != null ? View.VISIBLE : View.GONE);
                btnMas.setOnClickListener(v -> {
                    int pos = getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && listener != null) {
                        listener.onPedidoLongClick(pedidos.get(pos));
                    }
                });
            }

            chipEstado.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPedidoLongClick(pedidos.get(pos));
                }
            });
        }

        void bind(Pedido p) {
            Context context = itemView.getContext();
            tvCliente.setText(p.getClienteNombre());
            // "Amazon · Audífonos" cuando el pedido tiene artículo
            tvTienda.setText(com.example.gestiondecompras.utils.DetallePedido.tiendaYArticulo(p));
            tvTotal.setText(String.format(Locale.getDefault(), "RD$ %,.2f", p.getTotalGeneral()));
            tvMontoCompra.setText(String.format(Locale.getDefault(), context.getString(R.string.item_pedido_compra_format), p.getMontoCompra()));
            tvGanancia.setText(String.format(Locale.getDefault(), context.getString(R.string.item_pedido_ganancia_format), p.getGanancia()));
            String fechaEntrega = p.getFechaEntrega() == null
                    ? context.getString(R.string.item_pedido_sin_fecha)
                    : dateFormat.format(p.getFechaEntrega());
            tvFechaEntrega.setText(context.getString(R.string.item_pedido_entrega_format, fechaEntrega));

            boolean mostrarAbono = p.getTotalAbonado() > 0
                    || (!Pedido.ESTADO_PAGADO.equalsIgnoreCase(p.getEstado())
                    && !Pedido.ESTADO_CANCELADO.equalsIgnoreCase(p.getEstado()));
            tvAbonadoSaldo.setVisibility(mostrarAbono ? View.VISIBLE : View.GONE);
            if (mostrarAbono) {
                tvAbonadoSaldo.setText(context.getString(R.string.item_pedido_abono_format,
                        String.format(Locale.getDefault(), "RD$ %,.2f", p.getTotalAbonado()),
                        String.format(Locale.getDefault(), "RD$ %,.2f", p.getSaldo())));
            }

            // Estado en estilo tonal: fondo suave y texto del color del estado
            int colorEstado = ContextCompat.getColor(context, obtenerColorEstado(p.getEstado()));
            int fondoEstado = ContextCompat.getColor(context, obtenerFondoEstado(p.getEstado()));
            chipEstado.setText(formatearEstado(p.getEstado()));
            chipEstado.setChipBackgroundColor(ColorStateList.valueOf(fondoEstado));
            chipEstado.setTextColor(colorEstado);
            if (viewAccentEstado != null) {
                viewAccentEstado.setBackgroundTintList(ColorStateList.valueOf(colorEstado));
            }
        }

        private String formatearEstado(String estado) {
            if (estado == null || estado.isEmpty()) {
                return "";
            }
            return estado.substring(0, 1).toUpperCase(Locale.getDefault())
                    + estado.substring(1).toLowerCase(Locale.getDefault());
        }

        private int obtenerFondoEstado(String estado) {
            if (Pedido.ESTADO_PAGADO.equalsIgnoreCase(estado)) {
                return R.color.status_paid_bg;
            }
            if (Pedido.ESTADO_ENTREGADO.equalsIgnoreCase(estado)) {
                return R.color.status_delivered_bg;
            }
            if (Pedido.ESTADO_CANCELADO.equalsIgnoreCase(estado)) {
                return R.color.status_cancelled_bg;
            }
            if (Pedido.ESTADO_EN_CAMINO.equalsIgnoreCase(estado)) {
                return R.color.status_en_camino_bg;
            }
            return R.color.status_pending_bg;
        }

        private int obtenerColorEstado(String estado) {
            if (Pedido.ESTADO_PAGADO.equalsIgnoreCase(estado)) {
                return R.color.status_paid;
            }
            if (Pedido.ESTADO_ENTREGADO.equalsIgnoreCase(estado)) {
                return R.color.status_delivered;
            }
            if (Pedido.ESTADO_CANCELADO.equalsIgnoreCase(estado)) {
                return R.color.status_cancelled;
            }
            if (Pedido.ESTADO_EN_CAMINO.equalsIgnoreCase(estado)) {
                return R.color.status_en_camino;
            }
            // Pendiente (y cualquier otro)
            return R.color.status_pending;
        }
    }
}

