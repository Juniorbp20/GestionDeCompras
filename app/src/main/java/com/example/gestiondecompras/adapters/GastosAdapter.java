package com.example.gestiondecompras.adapters;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.models.Gasto;
import com.example.gestiondecompras.utils.CategoriaGastoUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GastosAdapter extends RecyclerView.Adapter<GastosAdapter.ViewHolder> {

    public interface OnGastoClickListener {
        void onGastoClick(Gasto gasto);
        void onGastoLongClick(Gasto gasto);
    }

    private final List<Gasto> gastos = new ArrayList<>();
    private final OnGastoClickListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public GastosAdapter(OnGastoClickListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void actualizarLista(List<Gasto> lista) {
        gastos.clear();
        if (lista != null) {
            gastos.addAll(lista);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gasto, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Gasto gasto = gastos.get(position);

        String descripcion = gasto.getDescripcion();
        if (descripcion == null || descripcion.trim().isEmpty()) {
            descripcion = gasto.getCategoria();
        }
        holder.tvDescripcion.setText(descripcion);
        holder.tvCategoria.setText(gasto.getCategoria() + " - " + sdf.format(new Date(gasto.getFechaEpoch())));
        holder.tvMonto.setText(String.format(Locale.getDefault(), "RD$ %,.2f", gasto.getMonto()));

        int color = ContextCompat.getColor(holder.itemView.getContext(),
                CategoriaGastoUtils.color(gasto.getCategoria()));
        holder.ivIcono.setImageResource(CategoriaGastoUtils.icono(gasto.getCategoria()));
        holder.ivIcono.setBackgroundTintList(ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(color, 38)));
        holder.ivIcono.setImageTintList(ColorStateList.valueOf(color));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGastoClick(gasto);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onGastoLongClick(gasto);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return gastos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final androidx.appcompat.widget.AppCompatImageView ivIcono;
        final TextView tvDescripcion;
        final TextView tvCategoria;
        final TextView tvMonto;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcono = itemView.findViewById(R.id.ivIcono);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvCategoria = itemView.findViewById(R.id.tvCategoria);
            tvMonto = itemView.findViewById(R.id.tvMonto);
        }
    }
}
