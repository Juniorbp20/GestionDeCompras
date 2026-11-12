package com.example.gestiondecompras.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.models.Tarjeta;

import java.util.List;
public class TarjetasAdapter extends RecyclerView.Adapter<TarjetasAdapter.TarjetaViewHolder> {

    public interface OnTarjetaClickListener {
        void onTarjetaClick(Tarjeta tarjeta);
        void onTarjetaLongClick(Tarjeta tarjeta);
    }

    private List<Tarjeta> tarjetas;
    private final OnTarjetaClickListener listener;

    public TarjetasAdapter(OnTarjetaClickListener listener) {
        this.tarjetas = new java.util.ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public TarjetaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tarjeta, parent, false);
        return new TarjetaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TarjetaViewHolder holder, int position) {
        Tarjeta t = tarjetas.get(position);
        holder.bind(t);
        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onTarjetaClick(t); });
        holder.itemView.setOnLongClickListener(v -> { if (listener != null) listener.onTarjetaLongClick(t); return true; });
    }

    @Override
    public int getItemCount() { return tarjetas != null ? tarjetas.size() : 0; }

    public void actualizarLista(List<Tarjeta> nuevas) {
        this.tarjetas = nuevas;
        notifyDataSetChanged();
    }

    static class TarjetaViewHolder extends RecyclerView.ViewHolder {
        TextView tvBanco, tvAlias, tvLimite, tvDeuda, tvDisponible, tvCorte, tvPago, tvVence;
        ProgressBar pbUso;
        public TarjetaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBanco = itemView.findViewById(R.id.tvBanco);
            tvAlias = itemView.findViewById(R.id.tvAlias);
            tvLimite = itemView.findViewById(R.id.tvLimite);
            tvDeuda = itemView.findViewById(R.id.tvDeuda);
            tvDisponible = itemView.findViewById(R.id.tvDisponible);
            tvCorte = itemView.findViewById(R.id.tvCorte);
            tvPago = itemView.findViewById(R.id.tvPago);
            tvVence = itemView.findViewById(R.id.tvVence);
            pbUso = itemView.findViewById(R.id.pbUso);
        }
        void bind(Tarjeta t) {
            android.content.Context context = itemView.getContext();
            tvBanco.setText(t.getBanco());
            tvAlias.setText(t.getAlias());
            tvLimite.setText(context.getString(R.string.tarjeta_limite_format, t.getLimiteCredito()));
            tvDeuda.setText(context.getString(R.string.tarjeta_deuda_format, t.getDeudaActual()));
            double disponible = Math.max(0, t.getLimiteCredito() - t.getDeudaActual());
            tvDisponible.setText(context.getString(R.string.tarjeta_disponible_format, disponible));
            tvCorte.setText(context.getString(R.string.tarjeta_corte_format, t.getDiaCorte()));
            tvPago.setText(t.getDiaPago() > 0
                    ? context.getString(R.string.tarjeta_pago_format, t.getDiaPago())
                    : context.getString(R.string.tarjeta_pago_sin_configurar));
            tvVence.setText(context.getString(R.string.tarjeta_vence_format, t.getDiaVencimiento()));
            int progress = 0;
            if (t.getLimiteCredito() > 0) {
                progress = (int) Math.min(100, Math.round((t.getDeudaActual() / t.getLimiteCredito()) * 100));
            }
            pbUso.setProgress(progress);
        }
    }
}
