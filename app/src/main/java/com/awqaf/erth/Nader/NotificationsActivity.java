package com.awqaf.erth.Nader; 

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.awqaf.erth.R;
import com.awqaf.erth.adapters.NotificationsAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsActivity extends AppCompatActivity implements NotificationsAdapter.OnNotificationInteractionListener {

    private static final String TAG = "NotificationsActivity";

    private RecyclerView recyclerViewNotifications;
    private NotificationsAdapter adapter;
    private EditText etSearch;
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipUnread;
    private MaterialButton btnDeleteAll;
    private ImageView ivBack, ivForward;
    private FrameLayout progressBar;
    private TextView tvLoginRequired;

    private FirebaseAuth mAuth;
    private DatabaseReference userNotificationsRef;
    private ValueEventListener notificationsListener;
    private List<Notification> allNotifications = new ArrayList<>();

    private String currentSearchQuery = "";
    private boolean currentShowAll = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        setupToolbar();
        bindViews();
        setupRecyclerView();
        setupSearch();
        setupFilters();
        setupDeleteButton();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            userNotificationsRef = FirebaseDatabase.getInstance().getReference("notifications").child(userId);
            Log.d(TAG, "Fetching notifications for user: " + userId + " from path: " + userNotificationsRef.toString());
            if (tvLoginRequired != null) tvLoginRequired.setVisibility(View.GONE);
            attachDatabaseReadListener();
        } else {
            Log.w(TAG, "User not logged in. Cannot fetch notifications.");
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (tvLoginRequired != null) {
                tvLoginRequired.setVisibility(View.VISIBLE);
                tvLoginRequired.setText("يرجى تسجيل الدخول لعرض التنبيهات.");
            }
            recyclerViewNotifications.setVisibility(View.GONE);
            btnDeleteAll.setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detachDatabaseReadListener();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        ivBack = findViewById(R.id.iv_back_arrow);
        ivBack.setOnClickListener(v -> onBackPressed());
    }

    private void bindViews() {
        recyclerViewNotifications = findViewById(R.id.rv_notifications);
        etSearch = findViewById(R.id.et_search);
        chipGroupFilter = findViewById(R.id.chip_group_filter);
        chipAll = findViewById(R.id.chip_all);
        chipUnread = findViewById(R.id.chip_unread);
        btnDeleteAll = findViewById(R.id.btn_delete_all);
        progressBar = findViewById(R.id.progress_bar);
        tvLoginRequired = findViewById(R.id.tv_login_required);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
    }

    private void setupRecyclerView() {
        adapter = new NotificationsAdapter(this, this);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                adapter.applyFilter();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            currentShowAll = (checkedId == R.id.chip_all);
            Log.d(TAG, "Filter changed: Show All = " + currentShowAll);
            adapter.applyFilter();
        });
        currentShowAll = chipAll.isChecked();
    }

    private void setupDeleteButton() {
        btnDeleteAll.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("تأكيد الحذف")
                    .setMessage("هل أنت متأكد من رغبتك في حذف جميع التنبيهات المعروضة؟")
                    .setPositiveButton("حذف", (dialog, which) -> adapter.deleteAllFiltered())
                    .setNegativeButton("إلغاء", null)
                    .show();
        });
    }

    private void attachDatabaseReadListener() {
        if (userNotificationsRef == null) return;
        if (notificationsListener == null) {
            notificationsListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    allNotifications.clear();
                    Log.d(TAG, "User notifications received. Count: " + dataSnapshot.getChildrenCount());
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Notification notification = snapshot.getValue(Notification.class);
                            if (notification != null) {
                                notification.setId(snapshot.getKey());
                                allNotifications.add(notification);
                            } else {
                                Log.w(TAG, "Parsed user notification is null for key: " + snapshot.getKey());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing user notification for key: " + snapshot.getKey(), e);
                        }
                    }
                    adapter.updateList(allNotifications);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Log.d(TAG, "Adapter updated with " + allNotifications.size() + " user notifications.");
                    if (allNotifications.isEmpty() && tvLoginRequired != null && mAuth.getCurrentUser() != null) {
                        tvLoginRequired.setText("لا توجد تنبيهات لعرضها.");
                        tvLoginRequired.setVisibility(View.VISIBLE);
                    } else if (tvLoginRequired != null) {
                        tvLoginRequired.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Firebase user notifications read failed: ", databaseError.toException());
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(NotificationsActivity.this, "فشل تحميل التنبيهات: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                    if (tvLoginRequired != null) {
                        tvLoginRequired.setText("حدث خطأ أثناء تحميل التنبيهات.");
                        tvLoginRequired.setVisibility(View.VISIBLE);
                    }
                }
            };
            userNotificationsRef.addValueEventListener(notificationsListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (notificationsListener != null && userNotificationsRef != null) {
            userNotificationsRef.removeEventListener(notificationsListener);
            notificationsListener = null;
            Log.d(TAG, "Firebase user notifications listener detached.");
        }
    }

    @Override
    public void onNotificationClick(Notification notification) {
        if (userNotificationsRef == null) return;
        Log.d(TAG, "Notification clicked: " + notification.getTitle() + " (ID: " + notification.getId() + ")");
        if (!notification.isRead()) {
            userNotificationsRef.child(notification.getId()).child("read").setValue(true)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification marked as read: " + notification.getId()))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to mark notification as read: " + notification.getId(), e));
        }
        Toast.makeText(this, "Clicked: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewDetailsClick(Notification notification) {
        Log.d(TAG, "View Details clicked for: " + notification.getTitle());

        if (notification.getStatus() == Notification.Status.REJECTED &&
                !TextUtils.isEmpty(notification.getRejectionReason())) {

            new AlertDialog.Builder(this)
                    .setTitle("سبب الرفض")
                    .setMessage(notification.getRejectionReason())
                    .setPositiveButton("حسناً", null)
                    .show();
        } else if (notification.getStatus() == Notification.Status.REJECTED) {
            Toast.makeText(this, "لا يوجد سبب رفض مسجل لهذا التنبيه.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "التفاصيل متاحة فقط للتنبيهات المرفوضة.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteNotification(String notificationId) {
        if (userNotificationsRef == null) return;
        Log.d(TAG, "Requesting delete for notification ID: " + notificationId);
        userNotificationsRef.child(notificationId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Notification deleted successfully: " + notificationId);
                    Toast.makeText(this, "تم حذف التنبيه", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete notification: " + notificationId, e);
                    Toast.makeText(this, "فشل حذف التنبيه: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDeleteNotifications(List<String> notificationIds) {
        if (userNotificationsRef == null || notificationIds.isEmpty()) return;
        Log.d(TAG, "Requesting delete for multiple notifications. Count: " + notificationIds.size());
        Map<String, Object> updates = new HashMap<>();
        for (String id : notificationIds) {
            updates.put(id, null);
        }
        userNotificationsRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Multiple notifications deleted successfully. Count: " + notificationIds.size());
                    Toast.makeText(this, "تم حذف التنبيهات المحددة", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete multiple notifications.", e);
                    Toast.makeText(this, "فشل حذف بعض التنبيهات: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public String getCurrentFilterQuery() {
        return currentSearchQuery;
    }

    public boolean getCurrentFilterShowAll() {
        return currentShowAll;
    }
}

