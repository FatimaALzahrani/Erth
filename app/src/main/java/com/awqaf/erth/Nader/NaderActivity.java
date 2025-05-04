package com.awqaf.erth.Nader;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

// Import WaqfDetailsActivity
import com.awqaf.erth.Nader.WaqfDetailsActivity;
import com.awqaf.erth.R;
import com.awqaf.erth.model.Waqf;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class NaderActivity extends AppCompatActivity {

    private static final String TAG = "NaderActivity";

    private TextView tvTotalAwqaf, tvApprovedAwqaf, tvUnderReview, tvNeedEdit;

    private FrameLayout topWaqfContainer;
    private View includedTopWaqfLayout;
    private TextView tvNoActiveWaqf;
    private ImageView ivWaqfImage;
    private Chip chipEditWaqf;
    private TextView tvWaqfName, tvWaqfVersion, tvWaqfPath, tvWaqfStatus, tvWaqfAmount;
    private TextView tvProgressMin, tvProgressMax;
    private SeekBar seekBarProgress;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userWaqfsRef;
    private ValueEventListener statsListener;

    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00 SAR");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nader);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "المستخدم غير مسجل دخوله", Toast.LENGTH_LONG).show();
            // finish();
            return;
        }
        String userId = currentUser.getUid();
        userWaqfsRef = FirebaseDatabase.getInstance().getReference("waqfs").child(userId);

        initializeUI();
        fetchWaqfData();
    }

    private void initializeUI() {
        tvTotalAwqaf = findViewById(R.id.tv_total_awqaf);
        tvApprovedAwqaf = findViewById(R.id.tv_approved_awqaf);
        tvUnderReview = findViewById(R.id.tv_under_review);
        tvNeedEdit = findViewById(R.id.tv_need_edit);

        topWaqfContainer = findViewById(R.id.top_waqf_container);
        includedTopWaqfLayout = findViewById(R.id.included_top_waqf_item);
        tvNoActiveWaqf = findViewById(R.id.tv_no_active_waqf);

        ivWaqfImage = includedTopWaqfLayout.findViewById(R.id.iv_waqf_item_image);
        chipEditWaqf = includedTopWaqfLayout.findViewById(R.id.chip_edit_waqf);
        tvWaqfName = includedTopWaqfLayout.findViewById(R.id.tv_waqf_item_name);
        tvWaqfVersion = includedTopWaqfLayout.findViewById(R.id.tv_waqf_item_version);
        tvWaqfPath = includedTopWaqfLayout.findViewById(R.id.tv_waqf_item_path);
        tvWaqfStatus = includedTopWaqfLayout.findViewById(R.id.tv_waqf_item_status);
        tvWaqfAmount = includedTopWaqfLayout.findViewById(R.id.tv_waqf_item_amount);
        tvProgressMin = includedTopWaqfLayout.findViewById(R.id.tv_progress_min);
        tvProgressMax = includedTopWaqfLayout.findViewById(R.id.tv_progress_max);
        seekBarProgress = includedTopWaqfLayout.findViewById(R.id.sb_waqf_item_progress);

        ImageButton btnProfile = findViewById(R.id.btn_profile);
        ImageButton btnNotifications = findViewById(R.id.btn_notifications);
    }

    private void fetchWaqfData() {
        Log.d(TAG, "Fetching Waqf data for user: " + userWaqfsRef.getKey());
        if (statsListener != null) {
            userWaqfsRef.removeEventListener(statsListener);
        }

        statsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int totalCount = 0;
                int approvedCount = 0;
                int pendingCount = 0;
                int needsEditCount = 0;
                List<Waqf> userWaqfs = new ArrayList<>();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Waqf waqf = snapshot.getValue(Waqf.class);
                        if (waqf != null) {
                            if (waqf.waqfId == null) {
                                waqf.waqfId = snapshot.getKey();
                            }
                            if (waqf.userId == null && currentUser != null) {
                                waqf.userId = currentUser.getUid();
                            }
                            userWaqfs.add(waqf);
                            totalCount++;
                            String status = waqf.status != null ? waqf.status.toUpperCase(Locale.ROOT) : "";
                            switch (status) {
                                case "APPROVED": approvedCount++; break;
                                case "PENDING": pendingCount++; break;
                                case "NEEDS_EDIT": needsEditCount++; break;
                            }
                        }
                    }
                    Log.d(TAG, "Fetched " + totalCount + " waqfs.");
                } else {
                    Log.d(TAG, "No Waqf data found for user.");
                }
                updateStatisticsUI(totalCount, approvedCount, pendingCount, needsEditCount);
                displayTopActiveWaqf(userWaqfs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading Waqf data", databaseError.toException());
                Toast.makeText(NaderActivity.this, "فشل تحميل البيانات: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                updateStatisticsUI(0, 0, 0, 0);
                displayTopActiveWaqf(new ArrayList<>());
            }
        };
        userWaqfsRef.addValueEventListener(statsListener);
    }

    private void updateStatisticsUI(int total, int approved, int pending, int needsEdit) {
        tvTotalAwqaf.setText(String.valueOf(total));
        tvApprovedAwqaf.setText(String.valueOf(approved));
        tvUnderReview.setText(String.valueOf(pending));
        tvNeedEdit.setText(String.valueOf(needsEdit));
    }

    private void displayTopActiveWaqf(List<Waqf> waqfList) {
        if (waqfList == null || waqfList.isEmpty()) {
            includedTopWaqfLayout.setVisibility(View.GONE);
            tvNoActiveWaqf.setVisibility(View.VISIBLE);
            return;
        }

        final Waqf topWaqf = Collections.max(waqfList, Comparator.comparingDouble(w -> {
            double amount = (w.paidAmount != null && w.paidAmount > 0) ? w.paidAmount : w.selfFundingAmount;
            return amount;
        }));

        if (topWaqf == null) {
            includedTopWaqfLayout.setVisibility(View.GONE);
            tvNoActiveWaqf.setVisibility(View.VISIBLE);
            return;
        }

        tvNoActiveWaqf.setVisibility(View.GONE);
        includedTopWaqfLayout.setVisibility(View.VISIBLE);

        if (topWaqf.waqfImageUrl != null && !topWaqf.waqfImageUrl.isEmpty()) {
            Glide.with(this).load(topWaqf.waqfImageUrl).placeholder(android.R.drawable.ic_menu_gallery).error(android.R.drawable.ic_menu_report_image).into(ivWaqfImage);
        } else {
            ivWaqfImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        tvWaqfName.setText(topWaqf.waqfName);
        tvWaqfPath.setText(topWaqf.path);
        tvWaqfStatus.setText(getDisplayStatus(topWaqf.status));

        double amountForProgress = (topWaqf.paidAmount != null && topWaqf.paidAmount > 0) ? topWaqf.paidAmount : topWaqf.selfFundingAmount;
        tvWaqfAmount.setText(currencyFormat.format(amountForProgress));
        tvProgressMin.setText(currencyFormat.format(0));
        tvProgressMax.setText(currencyFormat.format(topWaqf.totalAmount));
        int progressPercentage = 0;
        if (topWaqf.totalAmount > 0) {
            progressPercentage = (int) ((amountForProgress / topWaqf.totalAmount) * 100);
        }
        seekBarProgress.setMax(100);
        seekBarProgress.setProgress(Math.max(0, Math.min(progressPercentage, 100)));

        chipEditWaqf.setOnClickListener(v -> {
            Toast.makeText(this, "Edit clicked for: " + topWaqf.waqfName, Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, AddWaqfActivity.class);
            // intent.putExtra("WAQF_ID", topWaqf.waqfId);
            // intent.putExtra("USER_ID", topWaqf.userId);
            // startActivity(intent);
        });

        includedTopWaqfLayout.setOnClickListener(v -> {
            if (topWaqf.waqfId != null && topWaqf.userId != null) {
                Intent intent = new Intent(NaderActivity.this, WaqfDetailsActivity.class);
                intent.putExtra(WaqfDetailsActivity.EXTRA_WAQF_ID, topWaqf.waqfId);
                intent.putExtra(WaqfDetailsActivity.EXTRA_USER_ID, topWaqf.userId);
                startActivity(intent);
            } else {
                Log.e(TAG, "Top Waqf ID or User ID is null, cannot open details.");
                Toast.makeText(NaderActivity.this, "لا يمكن فتح التفاصيل، بيانات غير كاملة", Toast.LENGTH_SHORT).show();
            }
        });
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

    public void notification(View view) {
         Intent intent = new Intent(NaderActivity.this, NotificationsActivity.class);
         startActivity(intent);
//        Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show(); // Placeholder
    }

    public void Add(View view) {
         Intent intent = new Intent(NaderActivity.this, AddWaqfActivity.class);
         startActivity(intent);
//        Toast.makeText(this, "Add clicked", Toast.LENGTH_SHORT).show(); // Placeholder
    }

    public void MyAwaqaf(View view) {
        Intent intent = new Intent(NaderActivity.this, MyAwqafActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userWaqfsRef != null && statsListener != null) {
            userWaqfsRef.removeEventListener(statsListener);
            Log.d(TAG, "Removed Firebase listener.");
        }
    }
}

