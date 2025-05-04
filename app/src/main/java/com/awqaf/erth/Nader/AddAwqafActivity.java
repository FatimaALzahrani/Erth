package com.awqaf.erth.Nader;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.awqaf.erth.R; // تأكد من أن معرفات الموارد صحيحة
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AddAwqafActivity extends AppCompatActivity {

    private static final String TAG = "AddAwqafActivity";

    private static final String FILE_TYPE_DOC_WAQF = "doc_waqf";
    private static final String FILE_TYPE_STUDY = "study";
    private static final String FILE_TYPE_LICENSE = "license";
    private static final String FILE_TYPE_RECORD = "record";

    private TextInputEditText etName, etDescription, etGoal,
            etCategory, etDateFrom, etDateTo,
            etSelfPercent, etSelfAmount, etReqPercent, etReqAmount;
    private TextInputEditText etDocWaqf, etStudy, etLicense, etRecord;
    private AutoCompleteTextView acPath;
    private MaterialButton btnSave, btnDraft;
    private View progressOverlay;

    private final Map<String, Uri> fileUris = new HashMap<>();

    private FirebaseAuth auth;
    private DatabaseReference dbRef;
    private StorageReference storageRoot;
    private boolean isFirebaseReady = false;

    private final Map<String, ActivityResultLauncher<Intent>> filePickers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_awqaf);

        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();

        bindViews();
        setButtonsEnabled(false);
        setupPathAutocomplete();
        setupDatePickers();
        registerFilePickers();
        setupFilePickerListeners();
        setupButtons();

        signInAnonymously();
    }

    private void signInAnonymously() {
        showProgress(true);
        auth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "Anonymous sign-in successful.");
                    initFirebaseReferences();
                    isFirebaseReady = true;
                    setButtonsEnabled(true);
                    showProgress(false);
                })
                .addOnFailureListener(e -> {
                    initFirebaseReferences();
                    isFirebaseReady = true;
                    setButtonsEnabled(true);
                    showProgress(false);
                    Log.e(TAG, "Anonymous sign-in failed.", e);
                    Toast.makeText(
                            AddAwqafActivity.this,
                            "فشل تسجيل الدخول المجهول: " + e.getMessage() + ". لا يمكن حفظ البيانات.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void initFirebaseReferences() {
        dbRef = FirebaseDatabase.getInstance().getReference("waqfProjects");
        storageRoot = FirebaseStorage.getInstance().getReference("waqf_attachments");
        Log.d(TAG, "Firebase Database and Storage references initialized.");
    }

    private void bindViews() {
        etName = findViewById(R.id.et_name);
        etDescription = findViewById(R.id.et_description);
        etGoal = findViewById(R.id.et_goal);
        acPath = findViewById(R.id.ac_path);
        etCategory = findViewById(R.id.et_category);
        etDateFrom = findViewById(R.id.date_from);
        etDateTo = findViewById(R.id.date_to);
        etSelfPercent = findViewById(R.id.et_self_percent);
        etSelfAmount = findViewById(R.id.et_self_amount);
        etReqPercent = findViewById(R.id.et_req_percent);
        etReqAmount = findViewById(R.id.et_req_amount);

        etDocWaqf = findViewById(R.id.et_doc_waqf);
        etStudy = findViewById(R.id.et_study);
        etLicense = findViewById(R.id.et_license);
        etRecord = findViewById(R.id.et_record);

        btnSave = findViewById(R.id.btn_send);
        btnDraft = findViewById(R.id.btn_draft);

        progressOverlay = findViewById(R.id.progress_overlay);
    }

    private void setupPathAutocomplete() {
        String[] paths = {"مسار أ", "مسار ب", "مسار ج"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, paths);
        acPath.setAdapter(adapter);
    }

    private void setupDatePickers() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        etDateFrom.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, y, m, d) -> etDateFrom.setText(String.format(java.util.Locale.US, "%02d-%02d-%04d", d, m + 1, y)),
                    year, month, day);
            datePickerDialog.show();
        });

        etDateTo.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, y, m, d) -> etDateTo.setText(String.format(java.util.Locale.US, "%02d-%02d-%04d", d, m + 1, y)),
                    year, month, day);
            // datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void registerFilePickers() {
        filePickers.put(FILE_TYPE_DOC_WAQF, createActivityResultLauncher(etDocWaqf, FILE_TYPE_DOC_WAQF));
        filePickers.put(FILE_TYPE_STUDY, createActivityResultLauncher(etStudy, FILE_TYPE_STUDY));
        filePickers.put(FILE_TYPE_LICENSE, createActivityResultLauncher(etLicense, FILE_TYPE_LICENSE));
        filePickers.put(FILE_TYPE_RECORD, createActivityResultLauncher(etRecord, FILE_TYPE_RECORD));
    }

    private ActivityResultLauncher<Intent> createActivityResultLauncher(TextInputEditText targetEditText, String fileType) {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            fileUris.put(fileType, uri);
                            String fileName = getFileNameFromUri(uri);
                            targetEditText.setText(fileName);
                            Log.d(TAG, "File selected for " + fileType + ": " + fileName);
                        } else {
                            Log.w(TAG, "Received null URI for " + fileType);
                            Toast.makeText(this, "لم يتم العثور على مسار الملف", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "File selection cancelled or failed for " + fileType);
                    }
                });
    }

    private void setupFilePickerListeners() {
        etDocWaqf.setOnClickListener(v -> launchFilePicker(FILE_TYPE_DOC_WAQF));
        etStudy.setOnClickListener(v -> launchFilePicker(FILE_TYPE_STUDY));
        etLicense.setOnClickListener(v -> launchFilePicker(FILE_TYPE_LICENSE));
        etRecord.setOnClickListener(v -> launchFilePicker(FILE_TYPE_RECORD));
    }

    private void launchFilePicker(String fileType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            ActivityResultLauncher<Intent> launcher = filePickers.get(fileType);
            if (launcher != null) {
                launcher.launch(Intent.createChooser(intent, "اختر مستند PDF"));
            } else {
                Log.e(TAG, "ActivityResultLauncher not found for type: " + fileType);
                Toast.makeText(this, "خطأ في إعداد منتقي الملفات", Toast.LENGTH_SHORT).show();
            }
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "يرجى تثبيت مدير ملفات.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int cut = path.lastIndexOf("/");
            if (cut != -1) {
                return path.substring(cut + 1);
            }
        }
        return "ملف مختار";
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> validateAndSaveData(false));
        btnDraft.setOnClickListener(v -> validateAndSaveData(true));
    }

    private void setButtonsEnabled(boolean enabled) {
        if (btnSave != null) {
            btnSave.setEnabled(enabled);
        }
        if (btnDraft != null) {
            btnDraft.setEnabled(enabled);
        }
    }

    private void validateAndSaveData(boolean isDraft) {
        if (!isFirebaseReady) {
            Toast.makeText(this, "Firebase ليست جاهزة بعد، يرجى الانتظار.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Save attempt failed: Firebase not ready.");
            if (auth.getCurrentUser() == null) {
                signInAnonymously();
            }
            return;
        }

        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("اسم الوقف مطلوب");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(description)) {
            etDescription.setError("وصف الوقف مطلوب");
            etDescription.requestFocus();
            return;
        }

        Double selfPercent = parseDoubleOrNull(etSelfPercent.getText().toString());
        Double selfAmount = parseDoubleOrNull(etSelfAmount.getText().toString());
        Double reqPercent = parseDoubleOrNull(etReqPercent.getText().toString());
        Double reqAmount = parseDoubleOrNull(etReqAmount.getText().toString());

        if (selfPercent != null && (selfPercent < 0 || selfPercent > 100)) {
            etSelfPercent.setError("النسبة يجب أن تكون بين 0 و 100");
            etSelfPercent.requestFocus();
            return;
        }
        if (reqPercent != null && (reqPercent < 0 || reqPercent > 100)) {
            etReqPercent.setError("النسبة يجب أن تكون بين 0 و 100");
            etReqPercent.requestFocus();
            return;
        }

        Map<String, Object> waqfData = new HashMap<>();
        waqfData.put("name", name);
        waqfData.put("description", description);
        waqfData.put("goal", etGoal.getText().toString().trim());
        waqfData.put("path", acPath.getText().toString().trim());
        waqfData.put("category", etCategory.getText().toString().trim());
        waqfData.put("dateFrom", etDateFrom.getText().toString());
        waqfData.put("dateTo", etDateTo.getText().toString());
        waqfData.put("selfPercent", selfPercent);
        waqfData.put("selfAmount", selfAmount);
        waqfData.put("reqPercent", reqPercent);
        waqfData.put("reqAmount", reqAmount);
        waqfData.put("isDraft", isDraft);
        waqfData.put("timestamp", System.currentTimeMillis());
        waqfData.put("status", isDraft ? "draft" : "submitted");

        uploadFilesAndSaveToDatabase(waqfData);
    }

    private Double parseDoubleOrNull(String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse double: " + text, e);
            return null;
        }
    }

    private void uploadFilesAndSaveToDatabase(Map<String, Object> waqfData) {
        if (dbRef == null || storageRoot == null) {
            Toast.makeText(this, "خطأ: لم يتم تهيئة اتصال Firebase.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Firebase references not initialized during save attempt!");
            isFirebaseReady = false;
            setButtonsEnabled(false);
            signInAnonymously();
            return;
        }

        showProgress(true);

        String projectId = dbRef.push().getKey();
        if (projectId == null) {
            showProgress(false);
            Toast.makeText(this, "فشل في إنشاء معرف فريد للمشروع.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to get unique key from Firebase.");
            return;
        }

        waqfData.put("projectId", projectId);

        List<Task<Uri>> uploadTasks = new ArrayList<>();
        Map<String, String> fileMetadata = new HashMap<>();

        for (Map.Entry<String, Uri> entry : fileUris.entrySet()) {
            String fileType = entry.getKey();
            Uri fileUri = entry.getValue();
            String originalFileName = getFileNameFromUri(fileUri);
            String fileExtension = getFileExtension(originalFileName);
            String storageFileName = fileType + (TextUtils.isEmpty(fileExtension) ? "" : "." + fileExtension);
            StorageReference fileRef = storageRoot.child(projectId).child(storageFileName);

            fileMetadata.put(fileType, originalFileName);

            uploadTasks.add(fileRef.putFile(fileUri).continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    if (task.getException() != null) {
                        throw task.getException();
                    } else {
                        throw new Exception("Unknown error during file upload for type: " + fileType);
                    }
                }
                return fileRef.getDownloadUrl();
            }).addOnSuccessListener(downloadUri -> {
                Log.d(TAG, "File uploaded successfully: " + fileType + " -> " + downloadUri.toString());
            }));
        }

        if (!fileMetadata.isEmpty()) {
            waqfData.put("fileMetadata", fileMetadata);
        }

        if (uploadTasks.isEmpty()) {
            saveDataToDatabase(projectId, waqfData);
        } else {
            Task<List<Uri>> allUploadsTask = Tasks.whenAllSuccess(uploadTasks);

            allUploadsTask.addOnSuccessListener(downloadUris -> {
                Log.d(TAG, "All files uploaded. Ready to save data.");
                saveDataToDatabase(projectId, waqfData);

            }).addOnFailureListener(e -> {
                showProgress(false);
                Log.e(TAG, "File upload failed.", e);
                Toast.makeText(this, "فشل رفع واحد أو أكثر من الملفات: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // deleteUploadedFilesOnError(projectId, fileMetadata.keySet());
            });
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            try {
                return fileName.substring(fileName.lastIndexOf(".") + 1);
            } catch (Exception e) {
                Log.w(TAG, "Could not get file extension from: " + fileName);
            }
        }
        return "";
    }

    private void saveDataToDatabase(String projectId, Map<String, Object> waqfData) {
        dbRef.child(projectId).setValue(waqfData)
                .addOnSuccessListener(aVoid -> {
                    showProgress(false);
                    Log.d(TAG, "Waqf data saved successfully for ID: " + projectId);
                    Toast.makeText(this, "تم حفظ بيانات الوقف بنجاح", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    Log.e(TAG, "Failed to save Waqf data to database.", e);
                    Toast.makeText(this, "خطأ في حفظ البيانات بقاعدة البيانات: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // deleteUploadedFilesOnError(projectId, fileMetadata.keySet());
                });
    }

    private void showProgress(final boolean show) {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        // setButtonsEnabled(!show && isFirebaseReady);
    }

}

