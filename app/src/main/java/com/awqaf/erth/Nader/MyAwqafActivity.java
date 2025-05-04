package com.awqaf.erth.Nader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.awqaf.erth.R;
import com.awqaf.erth.adapters.MyWaqfAdapter;
import com.awqaf.erth.model.Waqf;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyAwqafActivity extends AppCompatActivity {

    private static final String TAG = "MyAwqafActivity";

    private ChipGroup chipGroupFilters;
    private Chip chipAll, chipApproved, chipPending;
    private RecyclerView rvMyAwqaf;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAdd;

    private MyWaqfAdapter myWaqfAdapter;
    private List<Waqf> allWaqfList;
    private List<Waqf> filteredWaqfList;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ValueEventListener waqfListener;
    private DatabaseReference userWaqfsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_awqaf);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initializeUI();
        setupRecyclerView();
        setupFilterChips();
        setupFab();

        allWaqfList = new ArrayList<>();
        filteredWaqfList = new ArrayList<>();

        loadWaqfData();
    }

    private void initializeUI() {
        chipGroupFilters = findViewById(R.id.chip_group_filters);
        chipAll = findViewById(R.id.chip_all);
        chipApproved = findViewById(R.id.chip_approved);
        chipPending = findViewById(R.id.chip_pending);
        rvMyAwqaf = findViewById(R.id.rv_my_awqaf);
        fabAdd = findViewById(R.id.fab_add);
    }

    private void setupRecyclerView() {
        rvMyAwqaf.setLayoutManager(new LinearLayoutManager(this));
        myWaqfAdapter = new MyWaqfAdapter(this, filteredWaqfList);
        rvMyAwqaf.setAdapter(myWaqfAdapter);
    }

    private void setupFilterChips() {
        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            filterWaqfList(checkedId);
        });
    }

    private void setupFab() {
        fabAdd.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MyAwqafActivity.this, AddWaqfActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting AddWaqfActivity", e);
                Toast.makeText(this, "لا يمكن فتح شاشة إضافة وقف", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWaqfData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "المستخدم غير مسجل دخوله", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        Log.d(TAG, "Loading Waqf data for user: " + userId);

        userWaqfsRef = mDatabase.child("waqfs").child(userId);

        if (waqfListener != null) {
            userWaqfsRef.removeEventListener(waqfListener);
        }

        waqfListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allWaqfList.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Waqf waqf = snapshot.getValue(Waqf.class);
                        if (waqf != null) {
                            if (waqf.waqfId == null) {
                                waqf.waqfId = snapshot.getKey();
                            }
                            allWaqfList.add(waqf);
                            Log.d(TAG, "Fetched Waqf: " + waqf.waqfName);
                        }
                    }
                    Log.d(TAG, "Total Waqfs fetched: " + allWaqfList.size());
                    filterWaqfList(chipGroupFilters.getCheckedChipId());
                } else {
                    Log.d(TAG, "No Waqf data found for user: " + userId);
                    filteredWaqfList.clear();
                    myWaqfAdapter.notifyDataSetChanged();
                    Toast.makeText(MyAwqafActivity.this, "لا توجد أوقاف لعرضها", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading Waqf data", databaseError.toException());
                // showLoading(false);
                Toast.makeText(MyAwqafActivity.this, "فشل تحميل البيانات", Toast.LENGTH_SHORT).show();
            }
        };

        userWaqfsRef.addValueEventListener(waqfListener);
    }

    private void filterWaqfList(int checkedId) {
        filteredWaqfList.clear();
        String filterStatus = null;

        if (checkedId == R.id.chip_approved) {
            filterStatus = "APPROVED";
        } else if (checkedId == R.id.chip_pending) {
            filterStatus = "PENDING";
        } else if (checkedId == R.id.chip_all) {
        } else {
            Log.w(TAG, "Unknown or no chip checked, defaulting to show all.");
        }

        Log.d(TAG, "Filtering list for status: " + (filterStatus == null ? "ALL" : filterStatus));

        if (filterStatus == null) {
            filteredWaqfList.addAll(allWaqfList);
        } else {
            for (Waqf waqf : allWaqfList) {
                if (waqf.status != null && waqf.status.equalsIgnoreCase(filterStatus)) {
                    filteredWaqfList.add(waqf);
                }
            }
        }

        Log.d(TAG, "Filtered list size: " + filteredWaqfList.size());
        myWaqfAdapter.updateData(filteredWaqfList);

        if (filteredWaqfList.isEmpty() && !allWaqfList.isEmpty()) {
            Toast.makeText(this, "لا توجد أوقاف تطابق هذا الفلتر", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userWaqfsRef != null && waqfListener != null) {
            userWaqfsRef.removeEventListener(waqfListener);
            Log.d(TAG, "Removed Firebase listener.");
        }
    }
    public void notification(View view) {
        Intent intent = new Intent(MyAwqafActivity.this, NotificationsActivity.class);
        startActivity(intent);
    }

    public void Add(View view) {
        Intent intent = new Intent(MyAwqafActivity.this, AddWaqfActivity.class);
        startActivity(intent);
    }

    public void Nader(View view) {
        Intent intent = new Intent(MyAwqafActivity.this, NaderActivity.class);
        startActivity(intent);
    }

}

