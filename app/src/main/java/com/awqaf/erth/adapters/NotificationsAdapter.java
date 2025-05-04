package com.awqaf.erth.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.awqaf.erth.Nader.Notification;
import com.awqaf.erth.Nader.NotificationsActivity;
import com.awqaf.erth.R;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;
    private List<Notification> notificationListFiltered;
    private Context context;
    private OnNotificationInteractionListener listener;

    public interface OnNotificationInteractionListener {
        void onNotificationClick(Notification notification);
        void onViewDetailsClick(Notification notification);
        void onDeleteNotification(String notificationId);
        void onDeleteNotifications(List<String> notificationIds);
    }

    public NotificationsAdapter(Context context, OnNotificationInteractionListener listener) {
        this.context = context;
        this.notificationList = new ArrayList<>();
        this.notificationListFiltered = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationListFiltered.get(position);
        holder.bind(notification, listener);
    }

    @Override
    public int getItemCount() {
        return notificationListFiltered.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStatus;
        TextView tvTimestamp;
        TextView tvTitle;
        TextView tvSecondaryInfo;
        TextView tvViewDetails;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStatus = itemView.findViewById(R.id.iv_notification_status);
            tvTimestamp = itemView.findViewById(R.id.tv_notification_timestamp);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvSecondaryInfo = itemView.findViewById(R.id.tv_notification_secondary);
            tvViewDetails = itemView.findViewById(R.id.tv_view_details);
        }

        public void bind(final Notification notification, final OnNotificationInteractionListener listener) {
            tvTimestamp.setText(notification.getTimestamp());
            tvTitle.setText(notification.getTitle());
            tvSecondaryInfo.setText(notification.getSecondaryInfo());

            switch (notification.getStatus()) {
                case APPROVED:
                    ivStatus.setImageResource(R.drawable.ic_status_approved);
                    break;
                case PENDING:
                    ivStatus.setImageResource(R.drawable.ic_status_pending);
                    break;
                case REJECTED:
                    ivStatus.setImageResource(R.drawable.ic_status_rejected);
                    break;
            }

            tvViewDetails.setVisibility(notification.isShowDetailsOption() ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });

            tvViewDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewDetailsClick(notification);
                }
            });

            itemView.setAlpha(notification.isRead() ? 0.7f : 1.0f);
        }
    }

    public void updateList(List<Notification> newList) {
        notificationList.clear();
        notificationList.addAll(newList);
        applyFilter();
    }

    public void applyFilter() {
        String query = ((NotificationsActivity) context).getCurrentFilterQuery();
        boolean showAll = ((NotificationsActivity) context).getCurrentFilterShowAll();

        notificationListFiltered.clear();
        String lowerCaseQuery = query.toLowerCase().trim();

        for (Notification item : notificationList) {
            boolean matchesQuery = TextUtils.isEmpty(lowerCaseQuery) ||
                    (item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerCaseQuery)) ||
                    (item.getSecondaryInfo() != null && item.getSecondaryInfo().toLowerCase().contains(lowerCaseQuery));

            boolean matchesReadStatus = showAll || !item.isRead();

            if (matchesQuery && matchesReadStatus) {
                notificationListFiltered.add(item);
            }
        }
        notifyDataSetChanged();
    }

    public void deleteAllFiltered() {
        if (notificationListFiltered.isEmpty()) {
            Toast.makeText(context, "لا توجد تنبيهات لحذفها", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> idsToDelete = notificationListFiltered.stream()
                .map(Notification::getId)
                .collect(Collectors.toList());

        if (listener != null && !idsToDelete.isEmpty()) {
            listener.onDeleteNotifications(idsToDelete);
            Toast.makeText(context, "جاري حذف التنبيهات المحددة...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "خطأ: لا يمكن بدء عملية الحذف", Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteItemAt(int position) {
        if (position >= 0 && position < notificationListFiltered.size()) {
            Notification itemToDelete = notificationListFiltered.get(position);
            String idToDelete = itemToDelete.getId();

            if (listener != null && idToDelete != null) {
                listener.onDeleteNotification(idToDelete);
                Toast.makeText(context, "جاري حذف التنبيه...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "خطأ: لا يمكن حذف التنبيه (المعرف غير موجود)", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

