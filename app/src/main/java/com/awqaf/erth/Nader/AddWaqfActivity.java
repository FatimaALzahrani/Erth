package com.awqaf.erth.Nader;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.awqaf.erth.R;
import com.awqaf.erth.model.Waqf;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddWaqfActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "AddWaqfActivity";
    private static final int PICK_FILE_REQUEST_CODE = 101;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 102;
    private static final int PICK_WAQF_IMAGE_REQUEST_CODE = 103;
    private static final float DEFAULT_ZOOM = 15f;
    private static final LatLng DEFAULT_LOCATION = new LatLng(24.7136, 46.6753);

    private ImageView ivWaqfImage;
    private Button btnSelectWaqfImage;
    private TextInputEditText etWaqfName, etWaqfDescription, etWaqfGoal, etTotalAmount;
    private TextInputEditText etSelfFundingPercentage, etSelfFundingAmount, etRequiredFundingPercentage, etRequiredFundingAmount;
    private TextInputEditText etDateFrom, etDateTo;
    private AutoCompleteTextView actvWaqfPath;
    private CheckBox cbRegistered;
    private Button btnUploadWaqfDoc, btnUploadFeasibilityStudy, btnUploadMunicipalLicense, btnUploadCommercialRegister, btnUploadAdditionalDocs;
    private AppCompatButton btnSubmit, btnSaveDraft;
    private MapView mapView;
    private TextView tvSelectedLocation, tvMortgageError;
    private RadioGroup rgMortgage;
    private TextInputLayout tilWaqfName, tilWaqfDescription, tilWaqfGoal, tilWaqfPath, tilDateFrom, tilDateTo, tilTotalAmount;
    private TextInputLayout tilSelfFundingPercentage;

    private Calendar calendarFrom, calendarTo;
    private SimpleDateFormat dateFormat;
    private ProgressDialog progressDialog;
    private DecimalFormat numberFormat = new DecimalFormat("#,##0.00");

    private Uri waqfImageUri = null;
    private Uri waqfDocUri, feasibilityStudyUri, municipalLicenseUri, commercialRegisterUri, additionalDocsUri;
    private String currentlyPickingFor = null;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LatLng selectedLatLng = null;
    private String selectedLocationName = null;
    private Marker currentMarker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_waqf);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        initializeUI();
        setupSpinner();
        setupDatePickers();
        setupButtonClickListeners();
        setupTextWatchers();

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("جاري التحميل...");
        progressDialog.setCancelable(false);
    }

    private void initializeUI() {
        ivWaqfImage = findViewById(R.id.iv_waqf_image);
        btnSelectWaqfImage = findViewById(R.id.btn_select_waqf_image);
        etWaqfName = findViewById(R.id.et_waqf_name);
        etWaqfDescription = findViewById(R.id.et_waqf_description);
        etWaqfGoal = findViewById(R.id.et_waqf_goal);
        actvWaqfPath = findViewById(R.id.actv_waqf_path);
        mapView = findViewById(R.id.map_view);
        tvSelectedLocation = findViewById(R.id.tv_selected_location);
        etDateFrom = findViewById(R.id.et_date_from);
        etDateTo = findViewById(R.id.et_date_to);
        etTotalAmount = findViewById(R.id.et_total_amount);
        etSelfFundingPercentage = findViewById(R.id.et_self_funding_percentage);
        etSelfFundingAmount = findViewById(R.id.et_self_funding_amount);
        etRequiredFundingPercentage = findViewById(R.id.et_required_funding_percentage);
        etRequiredFundingAmount = findViewById(R.id.et_required_funding_amount);
        rgMortgage = findViewById(R.id.rg_mortgage);
        tvMortgageError = findViewById(R.id.tv_mortgage_error);
        cbRegistered = findViewById(R.id.cb_registered);
        btnUploadWaqfDoc = findViewById(R.id.btn_upload_waqf_doc);
        btnUploadFeasibilityStudy = findViewById(R.id.btn_upload_feasibility_study);
        btnUploadMunicipalLicense = findViewById(R.id.btn_upload_municipal_license);
        btnUploadCommercialRegister = findViewById(R.id.btn_upload_commercial_register);
        btnUploadAdditionalDocs = findViewById(R.id.btn_upload_additional_docs);
        btnSubmit = findViewById(R.id.btn_submit);
        btnSaveDraft = findViewById(R.id.btn_save_draft);

        tilWaqfName = findViewById(R.id.til_waqf_name);
        tilWaqfDescription = findViewById(R.id.til_waqf_description);
        tilWaqfGoal = findViewById(R.id.til_waqf_goal);
        tilWaqfPath = findViewById(R.id.til_waqf_path);
        tilDateFrom = findViewById(R.id.til_date_from);
        tilDateTo = findViewById(R.id.til_date_to);
        tilTotalAmount = findViewById(R.id.til_total_amount);
        tilSelfFundingPercentage = findViewById(R.id.til_self_funding_percentage);

        dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        calendarFrom = Calendar.getInstance();
        calendarTo = Calendar.getInstance();
    }

    private void setupSpinner() {
        String[] paths = {"تعليمي", "صحي", "اجتماعي", "ديني", "أخرى"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, paths);
        actvWaqfPath.setAdapter(adapter);
    }

    private void setupDatePickers() {
        DatePickerDialog.OnDateSetListener dateSetListenerFrom = (view, year, month, dayOfMonth) -> {
            calendarFrom.set(Calendar.YEAR, year);
            calendarFrom.set(Calendar.MONTH, month);
            calendarFrom.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            etDateFrom.setText(dateFormat.format(calendarFrom.getTime()));
            tilDateFrom.setError(null);
        };

        DatePickerDialog.OnDateSetListener dateSetListenerTo = (view, year, month, dayOfMonth) -> {
            calendarTo.set(Calendar.YEAR, year);
            calendarTo.set(Calendar.MONTH, month);
            calendarTo.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            etDateTo.setText(dateFormat.format(calendarTo.getTime()));
            tilDateTo.setError(null);
        };

        etDateFrom.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(AddWaqfActivity.this,
                    dateSetListenerFrom,
                    calendarFrom.get(Calendar.YEAR),
                    calendarFrom.get(Calendar.MONTH),
                    calendarFrom.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });

        etDateTo.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(AddWaqfActivity.this,
                    dateSetListenerTo,
                    calendarTo.get(Calendar.YEAR),
                    calendarTo.get(Calendar.MONTH),
                    calendarTo.get(Calendar.DAY_OF_MONTH));
            if (!TextUtils.isEmpty(etDateFrom.getText())) {
                try {
                    Date dateFromParsed = dateFormat.parse(etDateFrom.getText().toString());
                    if (dateFromParsed != null) {
                        datePickerDialog.getDatePicker().setMinDate(dateFromParsed.getTime());
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing from date", e);
                }
            } else {
                datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            }
            datePickerDialog.show();
        });
    }

    private void setupButtonClickListeners() {
        btnSubmit.setOnClickListener(v -> submitWaqfData());
        btnSaveDraft.setOnClickListener(v -> saveWaqfAsDraft());

        btnSelectWaqfImage.setOnClickListener(v -> openImagePicker());

        btnUploadWaqfDoc.setOnClickListener(v -> { currentlyPickingFor = "waqfDoc"; openFilePicker(); });
        btnUploadFeasibilityStudy.setOnClickListener(v -> { currentlyPickingFor = "feasibilityStudy"; openFilePicker(); });
        btnUploadMunicipalLicense.setOnClickListener(v -> { currentlyPickingFor = "municipalLicense"; openFilePicker(); });
        btnUploadCommercialRegister.setOnClickListener(v -> { currentlyPickingFor = "commercialRegister"; openFilePicker(); });
        btnUploadAdditionalDocs.setOnClickListener(v -> { currentlyPickingFor = "additionalDocs"; openFilePicker(); });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        Log.d(TAG, "Map is ready.");
        requestLocationPermission();
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setOnMapClickListener(latLng -> {
            Log.d(TAG, "Map clicked at: " + latLng.latitude + ", " + latLng.longitude);
            updateMarkerAndLocation(latLng);
        });
        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override public void onMarkerDragStart(@NonNull Marker marker) { }
            @Override public void onMarkerDrag(@NonNull Marker marker) { }
            @Override public void onMarkerDragEnd(@NonNull Marker marker) {
                Log.d(TAG, "Marker dragged to: " + marker.getPosition().latitude + ", " + marker.getPosition().longitude);
                updateMarkerAndLocation(marker.getPosition());
            }
        });
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 10f));
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission already granted.");
            enableMyLocation();
        } else {
            Log.d(TAG, "Requesting location permission.");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (googleMap == null) return;
        try {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM));
                } else {
                    Log.d(TAG, "Current location is null. Using default.");
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Security Exception in enableMyLocation: " + e.getMessage());
        }
    }

    private void updateMarkerAndLocation(LatLng latLng) {
        selectedLatLng = latLng;
        if (currentMarker != null) currentMarker.remove();
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("الموقع المختار").draggable(true);
        currentMarker = googleMap.addMarker(markerOptions);
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                selectedLocationName = address.getAddressLine(0) != null ? address.getAddressLine(0) : "";
                tvSelectedLocation.setText(selectedLocationName);
                Log.d(TAG, "Selected Address: " + selectedLocationName);
            } else {
                selectedLocationName = "Lat: " + numberFormat.format(latLng.latitude) + ", Lng: " + numberFormat.format(latLng.longitude);
                tvSelectedLocation.setText(selectedLocationName);
                Log.d(TAG, "No address found for the location.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder service not available or error", e);
            selectedLocationName = "Lat: " + numberFormat.format(latLng.latitude) + ", Lng: " + numberFormat.format(latLng.longitude);
            tvSelectedLocation.setText(selectedLocationName);
            Toast.makeText(this, "لا يمكن الحصول على العنوان", Toast.LENGTH_SHORT).show();
        }
        tvSelectedLocation.setError(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted by user.");
                enableMyLocation();
            } else {
                Log.w(TAG, "Location permission denied by user.");
                Toast.makeText(this, "إذن الموقع مطلوب لعرض موقعك الحالي", Toast.LENGTH_LONG).show();
                if (googleMap != null) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 10f));
                }
            }
        }
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState); }

    private void setupTextWatchers() {
        TextWatcher calculationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calculateFundingFields(); }
        };
        etTotalAmount.addTextChangedListener(calculationWatcher);
        etSelfFundingPercentage.addTextChangedListener(calculationWatcher);
    }

    private void calculateFundingFields() {
        try {
            String totalAmountStr = etTotalAmount.getText().toString();
            String selfFundPercentStr = etSelfFundingPercentage.getText().toString();
            if (!TextUtils.isEmpty(totalAmountStr) && !TextUtils.isEmpty(selfFundPercentStr)) {
                double totalAmount = Double.parseDouble(totalAmountStr);
                double selfFundPercent = Double.parseDouble(selfFundPercentStr);
                if (selfFundPercent >= 0 && selfFundPercent <= 100) {
                    double selfFundAmount = totalAmount * (selfFundPercent / 100.0);
                    double requiredPercent = 100.0 - selfFundPercent;
                    double requiredAmount = totalAmount - selfFundAmount;
                    etSelfFundingAmount.setText(numberFormat.format(selfFundAmount));
                    etRequiredFundingPercentage.setText(String.format(Locale.US, "%.2f", requiredPercent));
                    etRequiredFundingAmount.setText(numberFormat.format(requiredAmount));
                    tilTotalAmount.setError(null);
                    tilSelfFundingPercentage.setError(null);
                } else {
                    tilSelfFundingPercentage.setError("النسبة يجب أن تكون بين 0 و 100");
                    clearCalculatedFields();
                }
            } else {
                clearCalculatedFields();
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing numbers for calculation", e);
            clearCalculatedFields();
        }
    }

    private void clearCalculatedFields() {
        etSelfFundingAmount.setText("");
        etRequiredFundingPercentage.setText("");
        etRequiredFundingAmount.setText("");
    }

    private boolean validateInput() {
        boolean isValid = true;
        tilWaqfName.setError(null); tilWaqfDescription.setError(null); tilWaqfGoal.setError(null);
        tilWaqfPath.setError(null); tvSelectedLocation.setError(null); tilDateFrom.setError(null);
        tilDateTo.setError(null); tilTotalAmount.setError(null); tilSelfFundingPercentage.setError(null);
        tvMortgageError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(etWaqfName.getText())) { tilWaqfName.setError("اسم الوقف مطلوب"); isValid = false; }
        if (TextUtils.isEmpty(etWaqfDescription.getText())) { tilWaqfDescription.setError("الوصف التفصيلي مطلوب"); isValid = false; }
        if (TextUtils.isEmpty(etWaqfGoal.getText())) { tilWaqfGoal.setError("الهدف مطلوب"); isValid = false; }
        if (TextUtils.isEmpty(actvWaqfPath.getText())) { tilWaqfPath.setError("المسار مطلوب"); isValid = false; }
        if (selectedLatLng == null) { tvSelectedLocation.setError("يرجى تحديد الموقع على الخريطة"); isValid = false; }
        if (TextUtils.isEmpty(etDateFrom.getText())) { tilDateFrom.setError("تاريخ البدء مطلوب"); isValid = false; }
        if (TextUtils.isEmpty(etDateTo.getText())) { tilDateTo.setError("تاريخ الانتهاء مطلوب"); isValid = false; }
        if (!TextUtils.isEmpty(etDateFrom.getText()) && !TextUtils.isEmpty(etDateTo.getText())) {
            try {
                Date dateFromParsed = dateFormat.parse(etDateFrom.getText().toString());
                Date dateToParsed = dateFormat.parse(etDateTo.getText().toString());
                if (dateFromParsed != null && dateToParsed != null && dateFromParsed.after(dateToParsed)) {
                    tilDateTo.setError("تاريخ الانتهاء يجب أن يكون بعد تاريخ البدء"); isValid = false;
                }
            } catch (ParseException e) { tilDateTo.setError("تنسيق التاريخ غير صالح"); isValid = false; }
        }
        if (TextUtils.isEmpty(etTotalAmount.getText())) { tilTotalAmount.setError("إجمالي المبلغ مطلوب"); isValid = false; }
        if (TextUtils.isEmpty(etSelfFundingPercentage.getText())) {
            tilSelfFundingPercentage.setError("نسبة التمويل الذاتي مطلوبة"); isValid = false;
        } else {
            try {
                double percent = Double.parseDouble(etSelfFundingPercentage.getText().toString());
                if (percent < 0 || percent > 100) { tilSelfFundingPercentage.setError("النسبة يجب أن تكون بين 0 و 100"); isValid = false; }
            } catch (NumberFormatException e) { tilSelfFundingPercentage.setError("قيمة غير صالحة"); isValid = false; }
        }
        if (isValid && (TextUtils.isEmpty(etSelfFundingAmount.getText()) || TextUtils.isEmpty(etRequiredFundingAmount.getText()))) {
            calculateFundingFields();
            if (TextUtils.isEmpty(etSelfFundingAmount.getText())) { Toast.makeText(this, "خطأ في حساب مبالغ التمويل", Toast.LENGTH_SHORT).show(); isValid = false; }
        }
        if (rgMortgage.getCheckedRadioButtonId() == -1) { tvMortgageError.setVisibility(View.VISIBLE); isValid = false; }
        if (feasibilityStudyUri == null) { Toast.makeText(this, "يرجى إرفاق دراسة الجدوى", Toast.LENGTH_SHORT).show(); isValid = false; }
        if (municipalLicenseUri == null) { Toast.makeText(this, "يرجى إرفاق رخصة بلدي", Toast.LENGTH_SHORT).show(); isValid = false; }
        if (commercialRegisterUri == null) { Toast.makeText(this, "يرجى إرفاق السجل التجاري", Toast.LENGTH_SHORT).show(); isValid = false; }

        return isValid;
    }

    private void submitWaqfData() {
        if (!validateInput()) {
            Toast.makeText(this, "يرجى ملء جميع الحقول المطلوبة (*)، تحديد الموقع، وإرفاق المستندات", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "الرجاء تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        progressDialog.setMessage("جاري رفع الملفات وحفظ البيانات...");
        progressDialog.show();

        String waqfId = mDatabase.child("waqfs").child(userId).push().getKey();
        if (waqfId == null) {
            Toast.makeText(this, "حدث خطأ في إنشاء معرف الوقف", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        List<Task<Uri>> uploadTasks = new ArrayList<>();
        Map<String, Uri> fileMap = new HashMap<>();
        if (waqfImageUri != null) fileMap.put("waqfImage", waqfImageUri); // Add Waqf image to map
        if (waqfDocUri != null) fileMap.put("waqfDoc", waqfDocUri);
        if (feasibilityStudyUri != null) fileMap.put("feasibilityStudy", feasibilityStudyUri);
        if (municipalLicenseUri != null) fileMap.put("municipalLicense", municipalLicenseUri);
        if (commercialRegisterUri != null) fileMap.put("commercialRegister", commercialRegisterUri);
        if (additionalDocsUri != null) fileMap.put("additionalDocs", additionalDocsUri);

        Map<String, String> downloadUrls = new HashMap<>();

        for (Map.Entry<String, Uri> entry : fileMap.entrySet()) {
            String fileType = entry.getKey();
            Uri fileUri = entry.getValue();
            String fileName = fileType + "_" + UUID.randomUUID().toString();
            StorageReference fileRef = mStorageRef.child("waqfs").child(userId).child(waqfId).child(fileName);

            UploadTask uploadTask = fileRef.putFile(fileUri);
            Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return fileRef.getDownloadUrl();
            }).addOnSuccessListener(downloadUri -> {
                downloadUrls.put(fileType + "Url", downloadUri.toString());
                Log.d(TAG, fileType + " Upload Success. URL: " + downloadUri.toString());
            }).addOnFailureListener(e -> {
                Log.e(TAG, fileType + " Upload Failed", e);
            });
            uploadTasks.add(urlTask);
        }

        Tasks.whenAllSuccess(uploadTasks).addOnSuccessListener(results -> {
            Log.d(TAG, "All files uploaded successfully. Saving Waqf data...");
            saveWaqfToDatabase(userId, waqfId, downloadUrls);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "One or more file uploads failed.", e);

            Log.w(TAG, "Proceeding to save data despite some file upload failures.");
            saveWaqfToDatabase(userId, waqfId, downloadUrls); // Attempt to save with URLs obtained so far
        });
    }

    private void saveWaqfToDatabase(String userId, String waqfId, Map<String, String> downloadUrls) {
        String waqfName = etWaqfName.getText().toString().trim();
        String description = etWaqfDescription.getText().toString().trim();
        String goal = etWaqfGoal.getText().toString().trim();
        String path = actvWaqfPath.getText().toString().trim();
        String dateFrom = etDateFrom.getText().toString().trim();
        String dateTo = etDateTo.getText().toString().trim();
        boolean registered = cbRegistered.isChecked();
        boolean hasMortgage = rgMortgage.getCheckedRadioButtonId() == R.id.rb_mortgage_yes;
        String status = "PENDING";
        long createdAt = System.currentTimeMillis();

        double totalAmount = 0, selfFundPercent = 0, selfFundAmount = 0, reqPercent = 0, reqAmount = 0;
        try {
            totalAmount = Double.parseDouble(etTotalAmount.getText().toString());
            selfFundPercent = Double.parseDouble(etSelfFundingPercentage.getText().toString());
            selfFundAmount = numberFormat.parse(etSelfFundingAmount.getText().toString()).doubleValue();
            reqPercent = Double.parseDouble(etRequiredFundingPercentage.getText().toString());
            reqAmount = numberFormat.parse(etRequiredFundingAmount.getText().toString()).doubleValue();
        } catch (ParseException | NumberFormatException e) {
            Log.e(TAG, "Error parsing final numeric values before saving", e);
            Toast.makeText(this, "خطأ في تنسيق الأرقام", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        Waqf waqf = new Waqf(
                waqfId, userId, waqfName, description, goal, path,
                selectedLocationName, selectedLatLng != null ? selectedLatLng.latitude : 0.0,
                selectedLatLng != null ? selectedLatLng.longitude : 0.0,
                dateFrom, dateTo, totalAmount, selfFundPercent, selfFundAmount,
                reqPercent, reqAmount, registered, hasMortgage, status, createdAt,
                downloadUrls.getOrDefault("waqfImageUrl", null),
                downloadUrls.getOrDefault("waqfDocUrl", null),
                downloadUrls.getOrDefault("feasibilityStudyUrl", null),
                downloadUrls.getOrDefault("municipalLicenseUrl", null),
                downloadUrls.getOrDefault("commercialRegisterUrl", null),
                downloadUrls.getOrDefault("additionalDocsUrl", null)
        );

        DatabaseReference waqfRef = mDatabase.child("waqfs").child(userId).child(waqfId);
        waqfRef.setValue(waqf).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Waqf data saved successfully.");
                addNotification(userId, waqfId);
            } else {
                Log.e(TAG, "Error saving Waqf data", task.getException());
                progressDialog.dismiss();
                Toast.makeText(this, "حدث خطأ أثناء حفظ بيانات الوقف", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addNotification(String userId, String waqfId) {
        String notificationId = mDatabase.child("notifications").child(userId).push().getKey();
        if (notificationId == null) {
            Log.e(TAG, "Error creating notification ID");
            showSuccessDialog(generateRequestNumber(waqfId));
            return;
        }
        String requestNumber = generateRequestNumber(waqfId);
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Calendar.getInstance().getTime());
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("read", false);
        notificationData.put("secondaryInfo", "رقم الطلب: " + requestNumber);
        notificationData.put("showDetailsOption", false);
        notificationData.put("statusString", "PENDING");
        notificationData.put("timestamp", timestamp);
        notificationData.put("title", "تم ارسال طلب تمويل لوقف جديد وبانتظار المراجعة");
        notificationData.put("waqfId", waqfId);
        DatabaseReference notificationRef = mDatabase.child("notifications").child(userId).child(notificationId);
        notificationRef.setValue(notificationData).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Log.d(TAG, "Notification added successfully.");
            } else {
                Log.e(TAG, "Error adding notification", task.getException());
                Toast.makeText(this, "حدث خطأ أثناء إضافة الإشعار", Toast.LENGTH_SHORT).show();
            }
            showSuccessDialog(requestNumber);
        });
    }

    private String generateRequestNumber(String waqfId) {
        if (waqfId != null && waqfId.length() > 6) {
            return waqfId.substring(waqfId.length() - 6).toUpperCase();
        } else {
            return String.valueOf(System.currentTimeMillis() % 1000000);
        }
    }

    private void showSuccessDialog(String requestNumber) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_success, null);
        TextView tvRequestNumber = dialogView.findViewById(R.id.tv_request_number);
        Button btnClose = dialogView.findViewById(R.id.btn_close_dialog);
        tvRequestNumber.setText("جاري مراجعة طلبك رقم " + requestNumber);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog successDialog = builder.create();
        btnClose.setOnClickListener(v -> {
            successDialog.dismiss();
            finish();
        });
        if (!isFinishing()) {
            successDialog.show();
            Log.d(TAG, "Showing success dialog for request: " + requestNumber);
        } else {
            Log.e(TAG, "Activity is finishing, cannot show success dialog.");
        }
    }

    private void saveWaqfAsDraft() {
        Toast.makeText(this, "حفظ كمسودة (غير منفذ)", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Save as Draft clicked.");
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File"), PICK_FILE_REQUEST_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // intent.setType("image/*");
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Waqf Image"), PICK_WAQF_IMAGE_REQUEST_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Cannot open image picker.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri selectedUri = data.getData();
            if (requestCode == PICK_FILE_REQUEST_CODE) {
                Log.d(TAG, "File Selected: " + selectedUri.getPath() + " for " + currentlyPickingFor);
                assignFileUri(selectedUri);
                updateFileButtonStatus(currentlyPickingFor, selectedUri);
            } else if (requestCode == PICK_WAQF_IMAGE_REQUEST_CODE) {
                Log.d(TAG, "Waqf Image Selected: " + selectedUri.getPath());
                waqfImageUri = selectedUri;
                try {
                    ivWaqfImage.setImageURI(waqfImageUri);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting image URI to ImageView", e);
                    Toast.makeText(this, "Error displaying image", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Log.w(TAG, "File/Image selection cancelled or failed. RequestCode: " + requestCode);
        }
    }

    private void assignFileUri(Uri uri) {
        if (currentlyPickingFor == null) return;
        switch (currentlyPickingFor) {
            case "waqfDoc": waqfDocUri = uri; break;
            case "feasibilityStudy": feasibilityStudyUri = uri; break;
            case "municipalLicense": municipalLicenseUri = uri; break;
            case "commercialRegister": commercialRegisterUri = uri; break;
            case "additionalDocs": additionalDocsUri = uri; break;
        }
    }

    private void updateFileButtonStatus(String field, Uri uri) {
        String fileName = getFileName(uri);
        Button buttonToUpdate = null;
        switch (field) {
            case "waqfDoc": buttonToUpdate = btnUploadWaqfDoc; break;
            case "feasibilityStudy": buttonToUpdate = btnUploadFeasibilityStudy; break;
            case "municipalLicense": buttonToUpdate = btnUploadMunicipalLicense; break;
            case "commercialRegister": buttonToUpdate = btnUploadCommercialRegister; break;
            case "additionalDocs": buttonToUpdate = btnUploadAdditionalDocs; break;
        }
        if (buttonToUpdate != null && fileName != null) {
            buttonToUpdate.setText(fileName);
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri != null && "content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) { result = cursor.getString(nameIndex); }
                }
            }
        }
        if (result == null && uri != null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) { result = result.substring(cut + 1); }
        }
        return result;
    }
    public void notification(View view) {
        Intent intent = new Intent(AddWaqfActivity.this, NotificationsActivity.class);
        startActivity(intent);
    }

    public void MyAwaqaf(View view) {
        Intent intent = new Intent(AddWaqfActivity.this, MyAwqafActivity.class);
        startActivity(intent);
    }
    public void Nader(View view) {
        Intent intent = new Intent(AddWaqfActivity.this, NaderActivity.class);
        startActivity(intent);
    }
}

