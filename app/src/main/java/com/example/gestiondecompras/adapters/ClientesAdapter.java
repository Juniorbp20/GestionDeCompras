package com.example.gestiondecompras.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.models.Cliente;

import java.util.List;

public class ClientesAdapter extends RecyclerView.Adapter<ClientesAdapter.ClienteViewHolder> {

    public interface OnClienteClickListener {
        void onClienteClick(Cliente cliente);
        void onClienteLongClick(Cliente cliente);
    }

    private List<Cliente> clientes;
    private final OnClienteClickListener listener;

    public ClientesAdapter(List<Cliente> clientes, OnClienteClickListener listener) {
        this.clientes = clientes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cliente, parent, false);
        return new ClienteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ClienteViewHolder holder, int position) {
        Cliente c = clientes.get(position);
        holder.bind(c);
        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onClienteClick(c); });
        holder.itemView.setOnLongClickListener(v -> { if (listener != null) listener.onClienteLongClick(c); return true; });
        View btnMas = holder.itemView.findViewById(R.id.btnMas);
        if (btnMas != null) {
            btnMas.setVisibility(listener != null ? View.VISIBLE : View.GONE);
            btnMas.setOnClickListener(v -> { if (listener != null) listener.onClienteLongClick(c); });
        }
    }

    @Override
    public int getItemCount() { return clientes != null ? clientes.size() : 0; }

    public void actualizarLista(List<Cliente> nuevos) {
        this.clientes = nuevos;
        notifyDataSetChanged();
    }

    /** Devuelve hasta dos iniciales del nombre (p. ej. "Ana Pérez" -> "AP"). */
    static String iniciales(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String parte : partes) {
            if (!parte.isEmpty()) {
                sb.appendCodePoint(parte.codePointAt(0));
            }
            if (sb.length() >= 2) {
                break;
            }
        }
        return sb.toString().toUpperCase(java.util.Locale.getDefault());
    }

    static class ClienteViewHolder extends RecyclerView.ViewHolder {
        TextView tvInicial, tvNombre, tvTelefono, tvEmail, tvCantidadPedidos, tvTotalCompras;
        public ClienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInicial = itemView.findViewById(R.id.tvInicial);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvTelefono = itemView.findViewById(R.id.tvTelefono);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvCantidadPedidos = itemView.findViewById(R.id.tvCantidadPedidos);
            tvTotalCompras = itemView.findViewById(R.id.tvTotalCompras);
        }
        void bind(Cliente c) {
            tvNombre.setText(c.getNombre());
            String apellido = c.getApellido() != null ? c.getApellido() : "";
            tvInicial.setText(iniciales(c.getNombre() + " " + apellido));
            if (c.getTelefono() != null && !c.getTelefono().isEmpty()) { tvTelefono.setText(c.getTelefono()); tvTelefono.setVisibility(View.VISIBLE);} else tvTelefono.setVisibility(View.GONE);
            if (c.getEmail() != null && !c.getEmail().isEmpty()) { tvEmail.setText(c.getEmail()); tvEmail.setVisibility(View.VISIBLE);} else tvEmail.setVisibility(View.GONE);
            
            if (c instanceof com.example.gestiondecompras.models.ClienteWithMetrics) {
                com.example.gestiondecompras.models.ClienteWithMetrics cm = (com.example.gestiondecompras.models.ClienteWithMetrics) c;
                tvCantidadPedidos.setText(cm.cantidadPedidos + " pedidos");
                tvTotalCompras.setText(String.format(java.util.Locale.getDefault(), "RD$ %,.2f", cm.totalCompras));
            } else {
                tvCantidadPedidos.setText("0 pedidos");
                tvTotalCompras.setText("RD$ 0.00");
            }
        }
    }
}