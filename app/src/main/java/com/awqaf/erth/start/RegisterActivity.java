package com.awqaf.erth.start;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.awqaf.erth.R;
import com.awqaf.erth.utils.FirebaseErrorHandler;
import com.awqaf.erth.auth.DirectPhoneAuthSolution;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputEditText phoneInput, idInput, birthdateInput;
    private RadioGroup roleSelection;
    private RadioButton radioWaqif, radioNazir;
    private AppCompatButton continueButton;
    private ProgressBar progressBar;
    private TextView errorTextView;

    private FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    private int selectedYear, selectedMonth, selectedDay;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Log.d(TAG, "onCreate: Initializing RegisterActivity");

        phoneInput = findViewById(R.id.phone_input);
        idInput = findViewById(R.id.id_input);
        birthdateInput = findViewById(R.id.birthdate_input);
        roleSelection = findViewById(R.id.role_selection);
        radioWaqif = findViewById(R.id.radio_waqif);
        radioNazir = findViewById(R.id.radio_nazir);
        continueButton = findViewById(R.id.continue_button);

        progressBar = findViewById(R.id.progressBar);
        errorTextView = findViewById(R.id.errorTextView);

        if (progressBar == null) {
            Log.w(TAG, "progressBar is not found in layout");
        }

        if (errorTextView == null) {
            Log.w(TAG, "errorTextView is not found in layout");
        }

        mAuth = FirebaseAuth.getInstance();

        // FirebaseAuth.getInstance().getFirebaseAuthSettings()
        //    .setAppVerificationDisabledForTesting(false);

        setupBirthdatePicker();
        setupContinueButton();
        setupVerificationCallbacks();

        Log.d(TAG, "onCreate: RegisterActivity initialized successfully");
    }

    private void setupBirthdatePicker() {
        birthdateInput.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(RegisterActivity.this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        selectedYear = year1;
                        selectedMonth = monthOfYear;
                        selectedDay = dayOfMonth;
                        String formattedDate = String.format("%d-%02d-%02d", year1, (monthOfYear + 1), dayOfMonth);
                        birthdateInput.setText(formattedDate);
                    }, year, month, day);
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void setupContinueButton() {
        continueButton.setOnClickListener(v -> {
            if (validateInput()) {
                phoneNumber = phoneInput.getText().toString().trim();

                if (!phoneNumber.startsWith("+")) {
                    phoneNumber = "+966" + phoneNumber;
                }

                Log.d(TAG, "Starting verification for phone number: " + phoneNumber);
                startPhoneNumberVerification(phoneNumber);
            }
        });
    }

    private boolean validateInput() {
        phoneInput.setError(null);
        idInput.setError(null);
        birthdateInput.setError(null);
        if (errorTextView != null) errorTextView.setVisibility(View.GONE);

        String phone = phoneInput.getText().toString().trim();
        String id = idInput.getText().toString().trim();
        String birthdate = birthdateInput.getText().toString().trim();
        int selectedRoleId = roleSelection.getCheckedRadioButtonId();

        boolean isValid = true;

        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("رقم الجوال مطلوب");
            isValid = false;
        } else if (phone.length() < 9) {
            phoneInput.setError("رقم الجوال غير صحيح");
            isValid = false;
        }

        if (TextUtils.isEmpty(id)) {
            idInput.setError("رقم الهوية مطلوب");
            isValid = false;
        } else if (id.length() < 10) {
            idInput.setError("رقم الهوية غير صحيح");
            isValid = false;
        }

        if (TextUtils.isEmpty(birthdate)) {
            birthdateInput.setError("تاريخ الميلاد مطلوب");
            Toast.makeText(this, "الرجاء اختيار تاريخ الميلاد", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (selectedRoleId == -1) {
            Toast.makeText(this, "الرجاء اختيار نوع التسجيل (واقف أو ناظر)", Toast.LENGTH_SHORT).show();
            isValid = false;
            if (errorTextView != null) {
                errorTextView.setText("الرجاء اختيار نوع التسجيل");
                errorTextView.setVisibility(View.VISIBLE);
            }
        }

        return isValid;
    }

    private void setupVerificationCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
                hideLoading();
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);
                hideLoading();

                if (errorTextView != null) {
                    FirebaseErrorHandler.handleFirebaseException(e, RegisterActivity.this, errorTextView);
                } else {
                    Toast.makeText(RegisterActivity.this, "فشل التحقق: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent:" + verificationId);
                hideLoading();

                mVerificationId = verificationId;
                mResendToken = token;

                navigateToVerifyCodeActivity(verificationId, token);
            }
        };
    }

    private void navigateToVerifyCodeActivity(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
        Intent intent = new Intent(RegisterActivity.this, VerifyCodeActivity.class);
        intent.putExtra("verificationId", verificationId);
        intent.putExtra("resendToken", token);
        intent.putExtra("phoneNumber", phoneNumber);
        intent.putExtra("userId", idInput.getText().toString().trim());
        intent.putExtra("birthdate", birthdateInput.getText().toString().trim());
        int selectedRoleId = roleSelection.getCheckedRadioButtonId();
        String role = (selectedRoleId == R.id.radio_waqif) ? "واقف" : "ناظر";
        intent.putExtra("role", role);

        startActivity(intent);
        finish();
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        showLoading();

        DirectPhoneAuthSolution.startVerification(
                this,
                phoneNumber,
                mCallbacks,
                progressBar,
                errorTextView
        );

        Log.d(TAG, "Verification started for phone: " + phoneNumber);
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            Log.w(TAG, "showLoading: progressBar is null");
        }
        continueButton.setEnabled(false);
    }

    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        } else {
            Log.w(TAG, "hideLoading: progressBar is null");
        }
        continueButton.setEnabled(true);
    }
}