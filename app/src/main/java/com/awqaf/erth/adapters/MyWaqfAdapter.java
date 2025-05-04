package com.awqaf.erth.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.awqaf.erth.Nader.MyAwqafActivity;
import com.awqaf.erth.R;
import com.awqaf.erth.Nader.WaqfDetailsActivity;
import com.awqaf.erth.model.Waqf;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

public class MyWaqfAdapter extends RecyclerView.Adapter<MyWaqfAdapter.WaqfViewHolder> {

    private Context context;
    private List<Waqf> waqfList;
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00 ريال");

    // Constructor used by MyAwqafActivity
    public MyWaqfAdapter(Context context, List<Waqf> waqfList) {
        this.context = context;
        this.waqfList = waqfList;
    }

    // Remove the duplicate/unused constructor
    // public MyWaqfAdapter(MyAwqafActivity context, List<Waqf> filteredWaqfList) {
    // }

    @NonNull
    @Override
    public WaqfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use parent.getContext() for correct theme context
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.waqf_list_item, parent, false);
        return new WaqfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WaqfViewHolder holder, int position) {
        Waqf waqf = waqfList.get(position);
        if (waqf == null) return;

        if (waqf.waqfImageUrl != null && !waqf.waqfImageUrl.isEmpty()) {
            Glide.with(context)
                    .load(waqf.waqfImageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.ivWaqfImage);
        } else {
            holder.ivWaqfImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.tvWaqfName.setText(waqf.waqfName);
        holder.tvWaqfPath.setText(waqf.path);
        holder.tvWaqfStatus.setText(getDisplayStatus(waqf.status));

        double displayAmount = (waqf.paidAmount != null && waqf.paidAmount > 0) ? waqf.paidAmount : waqf.selfFundingAmount;
        holder.tvWaqfAmount.setText(currencyFormat.format(displayAmount));

        double amountForProgress = displayAmount;
        holder.tvProgressMin.setText(currencyFormat.format(0));
        holder.tvProgressMax.setText(currencyFormat.format(waqf.totalAmount));
        int progressPercentage = 0;
        if (waqf.totalAmount > 0) {
            progressPercentage = (int) ((amountForProgress / waqf.totalAmount) * 100);
        }
        holder.seekBarProgress.setMax(100);
        holder.seekBarProgress.setProgress(Math.max(0, Math.min(progressPercentage, 100)));

        holder.chipEdit.setOnClickListener(v -> {
            Toast.makeText(context, "Edit clicked for: " + waqf.waqfName, Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(context, AddWaqfActivity.class);
            // intent.putExtra("WAQF_ID", waqf.waqfId);
            // intent.putExtra("USER_ID", waqf.userId); // Pass userId if needed for editing
            // context.startActivity(intent);
        });

        holder.itemView.setOnClickListener(v -> {
            if (waqf.waqfId != null && waqf.userId != null) {
                Intent intent = new Intent(context, WaqfDetailsActivity.class);
                intent.putExtra(WaqfDetailsActivity.EXTRA_WAQF_ID, waqf.waqfId);
                intent.putExtra(WaqfDetailsActivity.EXTRA_USER_ID, waqf.userId);
                context.startActivity(intent);
            } else {
                Log.e("MyWaqfAdapter", "Waqf ID or User ID is null, cannot open details.");
                Toast.makeText(context, "لا يمكن فتح التفاصيل، بيانات غير كاملة", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return waqfList == null ? 0 : waqfList.size();
    }

    public void updateData(List<Waqf> newWaqfList) {
        this.waqfList = newWaqfList;
        notifyDataSetChanged();
    }

    private String getDisplayStatus(String status) {
        if (status == null) return "غير معروف";
        switch (status.toUpperCase(Locale.ROOT)) {
            case "PENDING": return "تحت المراجعة";
            case "APPROVED": return "معتمد";
            case "REJECTED": return "مرفوض";
            case "NEEDS_EDIT": return "يحتاج إلى تعديل";
            default: return status;
        }
    }

    public static class WaqfViewHolder extends RecyclerView.ViewHolder {
        ImageView ivWaqfImage;
        Chip chipEdit;
        TextView tvWaqfName, tvWaqfVersion, tvWaqfPath, tvWaqfStatus, tvWaqfAmount;
        TextView tvProgressMin, tvProgressMax, tvProgressLabel;
        SeekBar seekBarProgress;

        public WaqfViewHolder(@NonNull View itemView) {
            super(itemView);
            ivWaqfImage = itemView.findViewById(R.id.iv_waqf_item_image);
            chipEdit = itemView.findViewById(R.id.chip_edit_waqf);
            tvWaqfName = itemView.findViewById(R.id.tv_waqf_item_name);
            tvWaqfVersion = itemView.findViewById(R.id.tv_waqf_item_version); // Optional
            tvWaqfPath = itemView.findViewById(R.id.tv_waqf_item_path);
            tvWaqfStatus = itemView.findViewById(R.id.tv_waqf_item_status);
            tvWaqfAmount = itemView.findViewById(R.id.tv_waqf_item_amount);
            tvProgressMin = itemView.findViewById(R.id.tv_progress_min);
            tvProgressMax = itemView.findViewById(R.id.tv_progress_max);
            seekBarProgress = itemView.findViewById(R.id.sb_waqf_item_progress);
            tvProgressLabel = itemView.findViewById(R.id.tv_progress_label);
        }
    }
}

