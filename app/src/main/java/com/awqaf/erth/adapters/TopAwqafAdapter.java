package com.awqaf.erth.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.awqaf.erth.R;
import com.awqaf.erth.model.AwqafModel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TopAwqafAdapter extends RecyclerView.Adapter<TopAwqafAdapter.AwqafViewHolder> {

    private List<AwqafModel> awqafList;
    private Context context;

    public TopAwqafAdapter(Context context, List<AwqafModel> awqafList) {
        this.context = context;
        this.awqafList = awqafList;
    }

    @NonNull
    @Override
    public AwqafViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_top_awqaf, parent, false);
        return new AwqafViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AwqafViewHolder holder, int position) {
        AwqafModel model = awqafList.get(position);
        holder.tvName.setText(model.getName());
        holder.tvVersion.setText(model.getVersion());
        holder.seekBar.setProgress(model.getProgress());
        holder.seekBar.setMax(100);
        holder.tvMin.setText("ريال " + formatAmount(model.getMinValue()));
        holder.tvMax.setText("ريال " + formatAmount(model.getMaxValue()));

        int imageResId = context.getResources().getIdentifier(model.getImageUrl(), "drawable", context.getPackageName());
        holder.imageView.setImageResource(imageResId);
    }

    @Override
    public int getItemCount() {
        return awqafList.size();
    }

    public static class AwqafViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvVersion, tvMin, tvMax;
        SeekBar seekBar;
        ImageView imageView;

        public AwqafViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_awqaf_name);
            tvVersion = itemView.findViewById(R.id.tv_awqaf_version);
            tvMin = itemView.findViewById(R.id.tv_min_value);
            tvMax = itemView.findViewById(R.id.tv_max_value);
            seekBar = itemView.findViewById(R.id.seekBar_progress);
            imageView = itemView.findViewById(R.id.image_awqaf);
        }
    }

    private String formatAmount(double amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }
}
