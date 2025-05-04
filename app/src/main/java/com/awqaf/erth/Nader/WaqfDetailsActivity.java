package com.awqaf.erth.Nader;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.awqaf.erth.R;
import com.awqaf.erth.model.Waqf; // *** Ensure you have this Waqf model class ***
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WaqfDetailsActivity extends AppCompatActivity {

    private static final String TAG = "WaqfDetailsActivity";
    public static final String EXTRA_WAQF_ID = "waqf_id";
    public static final String EXTRA_USER_ID = "user_id";

    private Waqf currentWaqf;
    private DatabaseReference waqfRef;
    private ValueEventListener waqfListener;

    // UI Elements
    private Toolbar toolbar;
    private TextView tvToolbarTitle;
    private ImageButton btnBack;

    private ImageView ivWaqfHeaderImage;
    private TextView tvWaqfTag, tvWaqfDetailsName, tvWaqfDetailsCompany;
    private ProgressBar progressCircular;
    private TextView tvProgressPercentage;

    // Info Cards
    private View infoFundingAmountCard, infoEndPeriodCard, infoOpportunityDurationCard;
    private View infoAnnualReturnCard, infoSukukDateCard, infoIssuanceAmountCard;

    // Calculator
    private SeekBar seekbarSukukCount;
    private TextView tvSukukCountValue, tvInvestmentAmount, tvNetProfit;
    private TextView tvCalcPaymentDate, tvCalcNominalValue, tvCalcNetProfit, tvCalcTotalAmount;

    // Documents
    private View docSummaryCard, docSukukCard, docProspectusCard, docLegalCard;

    // Guarantees
    private View guaranteeAssociationCard, guaranteeAuthorityCard, guaranteeMunicipalCard;

    // Project Info
    private TextView tvProjectType, tvProjectUnits, tvProjectEndDate;
    private TextView tvProjectWaqifFunding, tvProjectNazirFunding;

    // Map
    private ImageView ivMapPlaceholder;

    // Risk Disclaimer
    private TextView tvRiskDisclaimer;

    // Bottom Button
    private MaterialButton btnEndowNow;

    // Formats
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00 "); // Corrected format
    private DecimalFormat numberFormat = new DecimalFormat("#,##0");
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()); // Format from Firebase

    // Map Constants
    private final String STATIC_MAP_API_KEY = "AIzaSyBTaCexUeNkxSZISfVH1L0GOKdM9-7TIbE";
    private final int MAP_ZOOM_LEVEL = 15;
    private final String MAP_SIZE = "600x300";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waqf_details);

        String waqfId = getIntent().getStringExtra(EXTRA_WAQF_ID);
        String userId = getIntent().getStringExtra(EXTRA_USER_ID);

        if (TextUtils.isEmpty(waqfId) || TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "معرف الوقف أو المستخدم غير صحيح", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Waqf ID or User ID is missing in Intent extras.");
            finish();
            return;
        }

        waqfRef = FirebaseDatabase.getInstance().getReference("waqfs").child(userId).child(waqfId);

        initializeUI();
        setupToolbar();
        fetchWaqfDetails();
        setupCalculator(); // Initial setup, might be updated when data loads
    }

    private void initializeUI() {
        toolbar = findViewById(R.id.toolbar);
        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        btnBack = findViewById(R.id.btn_back);

        ivWaqfHeaderImage = findViewById(R.id.iv_waqf_header_image);
        tvWaqfTag = findViewById(R.id.tv_waqf_tag);
        tvWaqfDetailsName = findViewById(R.id.tv_waqf_details_name);
        tvWaqfDetailsCompany = findViewById(R.id.tv_waqf_details_company);
        progressCircular = findViewById(R.id.progress_circular);
        tvProgressPercentage = findViewById(R.id.tv_progress_percentage);

        // Info Cards
        infoFundingAmountCard = findViewById(R.id.info_funding_amount);
        infoEndPeriodCard = findViewById(R.id.info_end_period);
        infoOpportunityDurationCard = findViewById(R.id.info_opportunity_duration);
        infoAnnualReturnCard = findViewById(R.id.info_annual_return);
        infoSukukDateCard = findViewById(R.id.info_sukuk_date);
        infoIssuanceAmountCard = findViewById(R.id.info_issuance_amount);

        // Calculator
        seekbarSukukCount = findViewById(R.id.seekbar_sukuk_count);
        tvSukukCountValue = findViewById(R.id.tv_sukuk_count_value);
        tvInvestmentAmount = findViewById(R.id.tv_investment_amount);
        tvNetProfit = findViewById(R.id.tv_net_profit);
        tvCalcPaymentDate = findViewById(R.id.tv_calc_payment_date);
        tvCalcNominalValue = findViewById(R.id.tv_calc_nominal_value);
        tvCalcNetProfit = findViewById(R.id.tv_calc_net_profit);
        tvCalcTotalAmount = findViewById(R.id.tv_calc_total_amount);

        // Documents
        docSummaryCard = findViewById(R.id.doc_summary);
        docSukukCard = findViewById(R.id.doc_sukuk);
        docProspectusCard = findViewById(R.id.doc_prospectus);
        docLegalCard = findViewById(R.id.doc_legal);

        // Guarantees
        guaranteeAssociationCard = findViewById(R.id.guarantee_association);
        guaranteeAuthorityCard = findViewById(R.id.guarantee_authority);
        guaranteeMunicipalCard = findViewById(R.id.guarantee_municipal);

        // Project Info
        tvProjectType = findViewById(R.id.tv_project_type);
        tvProjectUnits = findViewById(R.id.tv_project_units);
        tvProjectEndDate = findViewById(R.id.tv_project_end_date);
        tvProjectWaqifFunding = findViewById(R.id.tv_project_waqif_funding);
        tvProjectNazirFunding = findViewById(R.id.tv_project_nazir_funding);

        ivMapPlaceholder = findViewById(R.id.iv_map);

        tvRiskDisclaimer = findViewById(R.id.tv_risk_disclaimer);

        btnEndowNow = findViewById(R.id.btn_endow_now);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void fetchWaqfDetails() {
        Log.d(TAG, "Fetching details for Waqf ID: " + waqfRef.getKey());

        if (waqfListener != null) {
            waqfRef.removeEventListener(waqfListener);
        }

        waqfListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentWaqf = snapshot.getValue(Waqf.class);
                    if (currentWaqf != null) {
                        if (currentWaqf.waqfId == null) {
                            currentWaqf.waqfId = snapshot.getKey();
                        }
                        Log.d(TAG, "Successfully fetched Waqf: " + currentWaqf.waqfName);
                        bindDataToUI();
                    } else {
                        Log.e(TAG, "Failed to parse Waqf object.");
                        Toast.makeText(WaqfDetailsActivity.this, "فشل في قراءة بيانات الوقف", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Waqf data does not exist for ID: " + waqfRef.getKey());
                    Toast.makeText(WaqfDetailsActivity.this, "بيانات الوقف غير موجودة", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase database error: " + error.getMessage());
                Toast.makeText(WaqfDetailsActivity.this, "خطأ في تحميل البيانات: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
        waqfRef.addValueEventListener(waqfListener);
    }

    private void bindDataToUI() {
        if (currentWaqf == null) return;

        tvToolbarTitle.setText(currentWaqf.waqfName);
        tvWaqfDetailsName.setText(currentWaqf.waqfName);
        if (!TextUtils.isEmpty(currentWaqf.description)) {
            tvWaqfDetailsCompany.setText(currentWaqf.description);
            tvWaqfDetailsCompany.setVisibility(View.VISIBLE);
        } else {
            tvWaqfDetailsCompany.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(currentWaqf.waqfImageUrl)) {
            Glide.with(this).load(currentWaqf.waqfImageUrl).placeholder(R.drawable.icon).error(R.drawable.icon).into(ivWaqfHeaderImage);
        } else {
            ivWaqfHeaderImage.setImageResource(R.drawable.icon);
        }
        tvWaqfTag.setText(getDisplayStatus(currentWaqf.status));
        tvWaqfTag.setVisibility(View.VISIBLE);

        double amountForProgress = (currentWaqf.paidAmount != null && currentWaqf.paidAmount > 0) ? currentWaqf.paidAmount : currentWaqf.selfFundingAmount;
        int progressPercentage = 0;
        if (currentWaqf.totalAmount > 0) {
            progressPercentage = (int) ((amountForProgress / currentWaqf.totalAmount) * 100);
        }
        progressCircular.setProgress(Math.max(0, Math.min(progressPercentage, 100)));
        tvProgressPercentage.setText(String.format(Locale.ENGLISH, "%d%%", Math.max(0, Math.min(progressPercentage, 100))));

        setInfoCardData(infoFundingAmountCard, "مبلغ التمويل", currencyFormat.format(currentWaqf.requiredFundingAmount));
        setInfoCardData(infoEndPeriodCard, "نهاية المدة", formatDate(currentWaqf.dateTo)); // Use dateTo for end period
        setInfoCardData(infoOpportunityDurationCard, "مدة الفرصة", calculateDuration(currentWaqf.dateFrom, currentWaqf.dateTo));
        if (!TextUtils.isEmpty(currentWaqf.goal)) {
            setInfoCardData(infoAnnualReturnCard, "الهدف", currentWaqf.goal);
            infoAnnualReturnCard.setVisibility(View.VISIBLE);
        } else {
            infoAnnualReturnCard.setVisibility(View.GONE);
        }
        setInfoCardData(infoSukukDateCard, "تاريخ اطفاء الصكوك", formatDate(currentWaqf.dateTo)); // Assuming dateTo is sukuk date
        setInfoCardData(infoIssuanceAmountCard, "مبلغ الاصدار", currencyFormat.format(currentWaqf.totalAmount));

        setDocumentItem(docSummaryCard, "ملخص الوقف", currentWaqf.feasibilityStudyUrl); // Assuming feasibilityStudyUrl exists
        setDocumentItem(docSukukCard, "صك الوقف", currentWaqf.waqfDocUrl);
        docProspectusCard.setVisibility(View.GONE); // No clear alternative for Prospectus
        setDocumentItem(docLegalCard, "مستندات قانونية", currentWaqf.commercialRegisterUrl); // Assuming commercialRegisterUrl exists

        guaranteeAssociationCard.setVisibility(View.GONE); // No clear alternative
        guaranteeAuthorityCard.setVisibility(View.GONE); // No clear alternative
        setGuaranteeItem(guaranteeMunicipalCard, "رخصة بلدي", currentWaqf.municipalLicenseUrl); // Assuming municipalLicenseUrl exists

        if (!TextUtils.isEmpty(currentWaqf.path)) {
            tvProjectType.setText(currentWaqf.path);
//            findViewById(R.id.label_project_type).setVisibility(View.VISIBLE);
            tvProjectType.setVisibility(View.VISIBLE);
        } else {
//            findViewById(R.id.label_project_type).setVisibility(View.GONE);
            tvProjectType.setVisibility(View.GONE);
        }
//        findViewById(R.id.label_project_units).setVisibility(View.GONE);
        tvProjectUnits.setVisibility(View.GONE);

        tvProjectEndDate.setText(formatDate(currentWaqf.dateTo));
        tvProjectWaqifFunding.setText(String.format(Locale.ENGLISH, "%.0f%%", currentWaqf.selfFundingPercentage));
        tvProjectNazirFunding.setText(String.format(Locale.ENGLISH, "%.0f%%", currentWaqf.requiredFundingPercentage));

        loadMapImage(currentWaqf.latitude, currentWaqf.longitude);
        ivMapPlaceholder.setOnClickListener(v -> openMap(currentWaqf.latitude, currentWaqf.longitude, currentWaqf.locationName));

        // tvRiskDisclaimer.setText(currentWaqf.riskDisclaimer);

        double sukukValue = (currentWaqf.totalAmount > 0) ? currentWaqf.totalAmount / 100.0 : 1000;
        int maxSukuk = 100;
        seekbarSukukCount.setMax(maxSukuk);
        updateCalculator(seekbarSukukCount.getProgress(), sukukValue);

        btnEndowNow.setOnClickListener(v -> {
            Toast.makeText(this, "أوقف الآن clicked!", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, InvestmentActivity.class);
            // intent.putExtra(InvestmentActivity.EXTRA_WAQF_ID, currentWaqf.waqfId);
            // startActivity(intent);
        });
    }

    private void setInfoCardData(View cardView, String title, String value) {
        if (cardView == null) return;
        TextView tvTitle = cardView.findViewById(R.id.tv_info_title);
        TextView tvValue = cardView.findViewById(R.id.tv_info_value);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvValue != null) tvValue.setText(!TextUtils.isEmpty(value) ? value : "-");
    }

    private void setDocumentItem(View itemView, String title, final String url) {
        setItemClickListener(itemView, title, url, R.id.tv_doc_title);
    }

    private void setGuaranteeItem(View itemView, String title, final String url) {
        setItemClickListener(itemView, title, url, R.id.tv_guarantee_title);
    }

    private void setItemClickListener(View itemView, String title, final String url, int titleResId) {
        if (itemView == null) return;
        TextView tvTitle = itemView.findViewById(titleResId);
        if (tvTitle != null) tvTitle.setText(title);

        if (!TextUtils.isEmpty(url)) {
            itemView.setOnClickListener(v -> openUrl(url));
            itemView.setAlpha(1.0f);
            itemView.setVisibility(View.VISIBLE);
        } else {
            itemView.setOnClickListener(null);
            itemView.setVisibility(View.GONE);
        }
    }

    private void openUrl(String url) {
        if (TextUtils.isEmpty(url)) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening URL: " + url, e);
            Toast.makeText(this, "لا يمكن فتح الرابط", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatDate(String dateString) {
        if (TextUtils.isEmpty(dateString)) return "-";
        try {
            Date date = inputDateFormat.parse(dateString);
            return displayDateFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dateString, e);
            return dateString;
        }
    }

    private String calculateDuration(String startDateStr, String endDateStr) {
        if (TextUtils.isEmpty(startDateStr) || TextUtils.isEmpty(endDateStr)) return "-";
        try {
            Date startDate = inputDateFormat.parse(startDateStr);
            Date endDate = inputDateFormat.parse(endDateStr);
            long diffInMillis = endDate.getTime() - startDate.getTime();
            long diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
            if (diffInDays < 0) return "-";
            if (diffInDays < 30) {
                return diffInDays + " يوم";
            } else {
                long months = diffInDays / 30;
                return months + " شهر";
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error calculating duration", e);
            return "-";
        }
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

    private void setupCalculator() {
        seekbarSukukCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (currentWaqf != null) {
                    double sukukValue = (currentWaqf.totalAmount > 0) ? currentWaqf.totalAmount / 100.0 : 1000;
                    updateCalculator(progress, sukukValue);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        updateCalculator(0, 1000);
    }

    private void updateCalculator(int sukukCount, double sukukValue) {
        tvSukukCountValue.setText(String.valueOf(sukukCount));
        double investment = sukukCount * sukukValue;
        double netProfitValue = 0.0;

        tvInvestmentAmount.setText(currencyFormat.format(investment));
        tvNetProfit.setText(currencyFormat.format(netProfitValue));

        tvCalcPaymentDate.setText(formatDate(currentWaqf != null ? currentWaqf.dateTo : null));
        tvCalcNominalValue.setText(currencyFormat.format(sukukValue));
        tvCalcNetProfit.setText(currencyFormat.format(netProfitValue));
        tvCalcTotalAmount.setText(currencyFormat.format(investment + netProfitValue));
    }

    private void loadMapImage(double latitude, double longitude) {
        if (TextUtils.isEmpty(STATIC_MAP_API_KEY) || STATIC_MAP_API_KEY.equals("YOUR_STATIC_MAPS_API_KEY")) {
            Log.w(TAG, "Static Maps API Key is missing.");
            ivMapPlaceholder.setImageResource(R.drawable.map);
            return;
        }
        String marker = String.format(Locale.ENGLISH, "&markers=color:red%%7C%f,%f", latitude, longitude);
        String mapUrl = String.format(Locale.ENGLISH,
                "https://maps.googleapis.com/maps/api/staticmap?center=%f,%f&zoom=%d&size=%s&maptype=roadmap%s&key=%s",
                latitude, longitude, MAP_ZOOM_LEVEL, MAP_SIZE, marker, STATIC_MAP_API_KEY);

        Log.d(TAG, "Loading map image: " + mapUrl);
        Glide.with(this)
                .load(mapUrl)
                .placeholder(R.drawable.map)
                .error(R.drawable.map)
                .into(ivMapPlaceholder);
    }

    private void openMap(double latitude, double longitude, String label) {
        String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)", latitude, longitude, latitude, longitude, Uri.encode(!TextUtils.isEmpty(label) ? label : "موقع الوقف"));
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            String webUri = String.format(Locale.ENGLISH, "https://www.google.com/maps/search/?api=1&query=%f,%f", latitude, longitude);
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
            if (webIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(webIntent);
            } else {
                Toast.makeText(this, "لا يوجد تطبيق خرائط مناسب", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (waqfRef != null && waqfListener != null) {
            waqfRef.removeEventListener(waqfListener);
        }
    }
}

