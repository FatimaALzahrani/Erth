package com.awqaf.erth.start;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.awqaf.erth.MainActivity;
import com.awqaf.erth.Nader.NaderActivity;
import com.awqaf.erth.R;
import com.awqaf.erth.Waqef.WaqefActivity;
import com.awqaf.erth.auth.DirectPhoneAuthSolution;
import com.awqaf.erth.utils.FirebaseErrorHandler;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class VerifyCodeActivity extends AppCompatActivity {

    private static final String TAG = "VerifyCodeActivity";
    private static final long RESEND_TIMER = 60000;

    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private EditText[] otpFields;

    private AppCompatButton verifyButton;
    private TextView resendText;
    private TextView timerText, phoneText;
    // private ProgressBar progressBar;
    // private TextView errorTextView;

    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private String phoneNumber, userId, birthdate, role;
    private FirebaseAuth mAuth;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private boolean isLoading = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code0);

        Log.d(TAG, "onCreate: Initializing VerifyCodeActivity with new layout");

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);
//        otpFields = new EditText[]{otp1, otp2, otp3, otp4, otp5, otp6};
        otpFields = new EditText[]{otp6, otp5, otp4, otp3, otp2, otp1};

        verifyButton = findViewById(R.id.verify_button);
        resendText = findViewById(R.id.resend_text);
        timerText = findViewById(R.id.timer_text);
        phoneText = findViewById(R.id.phone_text);
        // progressBar = findViewById(R.id.progressBar);
        // errorTextView = findViewById(R.id.errorTextView);

        // if (progressBar != null) progressBar.setVisibility(View.GONE);
        // if (errorTextView != null) errorTextView.setVisibility(View.GONE);

        Intent intent = getIntent();
        verificationId = intent.getStringExtra("verificationId");
        resendToken = intent.getParcelableExtra("resendToken");
        phoneNumber = intent.getStringExtra("phoneNumber");
        userId = intent.getStringExtra("userId");
        birthdate = intent.getStringExtra("birthdate");
        role = intent.getStringExtra("role");

        mAuth = FirebaseAuth.getInstance();

        if (phoneText != null && phoneNumber != null) {
            // String maskedPhone = phoneNumber.length() > 4 ? phoneNumber.substring(0, phoneNumber.length() - 4) + "****" : phoneNumber;
            phoneText.setText(phoneNumber);
        }

        setupOtpInputListeners();

        verifyButton.setOnClickListener(v -> {
            if (isLoading) return;
            String code = getOtpCode();
            if (code.length() != 6) {
                Toast.makeText(VerifyCodeActivity.this, "الرجاء إدخال رمز التحقق المكون من 6 أرقام", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyCode(code);
        });

        resendText.setOnClickListener(v -> {
            if (!isTimerRunning && !isLoading) {
                resendVerificationCode();
            }
        });

        startResendTimer();

        Log.d(TAG, "VerifyCodeActivity initialized successfully with verificationId: " + verificationId);
    }

    private void setupOtpInputListeners() {
        for (int i = 0; i < otpFields.length; i++) {
            final int currentIndex = i;
            otpFields[currentIndex].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && currentIndex < otpFields.length - 1) {
                        otpFields[currentIndex + 1].requestFocus();
                    } else if (s.length() == 0 && before == 1 && currentIndex > 0) {
                        // otpFields[currentIndex - 1].requestFocus();
                    }
                    // Optionally trigger verification when the last field is filled
                    if (currentIndex == otpFields.length - 1 && s.length() == 1) {
                        String finalCode = getOtpCode();
                        if (finalCode.length() == 6) {
                             verifyCode(finalCode);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            otpFields[currentIndex].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (otpFields[currentIndex].getText().toString().isEmpty() && currentIndex > 0) {
                        otpFields[currentIndex - 1].requestFocus();
                        // otpFields[currentIndex - 1].setText("");
                    }
                }
                return false;
            });
        }
        if (otpFields.length > 0) {
            otpFields[0].requestFocus();
        }
    }

    private String getOtpCode() {
        StringBuilder otp = new StringBuilder();
        for (EditText editText : otpFields) {
            otp.append(editText.getText().toString().trim());
        }
        return otp.toString();
    }

    private void startResendTimer() {
        resendText.setEnabled(false);
        resendText.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary)); // Use a grey color for disabled state
        resendText.setOnClickListener(null);
        isTimerRunning = true;

        countDownTimer = new CountDownTimer(RESEND_TIMER, 1000) {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                String timeLeft = String.format(Locale.getDefault(), "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)));
                timerText.setText("الرمز صالح لمدة " + timeLeft);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFinish() {
                timerText.setText("لم تستلم الرمز؟");
                resendText.setEnabled(true);
                resendText.setTextColor(ContextCompat.getColor(VerifyCodeActivity.this, R.color.primary_dark)); // Use a link color for enabled state
                resendText.setText("إرسال مرة أخرى");
                resendText.setOnClickListener(v -> {
                    if (!isLoading) {
                        resendVerificationCode();
                    }
                });
                isTimerRunning = false;
            }
        }.start();
    }

    private void verifyCode(String code) {
        showLoading();

        if (verificationId != null && verificationId.startsWith("manual-")) {
            DirectPhoneAuthSolution.verifyManualCode(
                    this,
                    verificationId,
                    code,
                    task -> {
                        registerUser();
                    },
                    task -> {
                        hideLoading();
                        Toast.makeText(this, "رمز التحقق غير صحيح. يرجى المحاولة مرة أخرى.", Toast.LENGTH_LONG).show();
                    }
            );
        } else {
            try {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
                signInWithPhoneAuthCredential(credential);
            } catch (IllegalArgumentException e) {
                hideLoading();
                Log.e(TAG, "Error creating credential (likely invalid verification ID or code format): ", e);
                Toast.makeText(this, "خطأ في التحقق: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                hideLoading();
                Log.e(TAG, "Error creating credential: ", e);
                Toast.makeText(this, "خطأ غير متوقع في التحقق. حاول مرة أخرى.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        registerUser();
                    } else {
                        hideLoading();
                        Log.w(TAG, "signInWithCredential:failure", task.getException());

                        if (task.getException() != null) {
                            FirebaseErrorHandler.handleFirebaseException(task.getException(), this, null); // Pass null for TextView
                            // Toast.makeText(this, "فشل التحقق: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void registerUser() {
        if (!isLoading) showLoading();

        HashMap<String, Object> userData = new HashMap<>();
        userData.put("phone", phoneNumber);
        userData.put("id", userId);
        userData.put("birthdate", birthdate);
        userData.put("role", role);
        userData.put("createdAt", System.currentTimeMillis());

        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (uid == null) {
            Log.e(TAG, "User is not authenticated, cannot save data.");
            hideLoading();
            Toast.makeText(VerifyCodeActivity.this, "خطأ في المصادقة، لا يمكن حفظ البيانات.", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        usersRef.setValue(userData).addOnCompleteListener(task -> {
            hideLoading();

            if (task.isSuccessful()) {
                Log.d(TAG, "User data saved successfully");
                Toast.makeText(VerifyCodeActivity.this, "تم التحقق والتسجيل بنجاح!", Toast.LENGTH_SHORT).show();

                Intent intent;
                if (Objects.equals(role, "واقف")) {
                    intent = new Intent(VerifyCodeActivity.this, WaqefActivity.class);
                } else if (Objects.equals(role, "ناظر")) {
                    intent = new Intent(VerifyCodeActivity.this, NaderActivity.class);
                } else {
                    intent = new Intent(VerifyCodeActivity.this, MainActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Log.e(TAG, "Failed to save user data", task.getException());
                Toast.makeText(VerifyCodeActivity.this, "فشل في حفظ بيانات المستخدم. يرجى المحاولة مرة أخرى.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resendVerificationCode() {
        showLoading();

        PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);
                hideLoading();
                FirebaseErrorHandler.handleFirebaseException(e, VerifyCodeActivity.this, null); // Pass null for TextView
                // Toast.makeText(VerifyCodeActivity.this, "فشل إعادة الإرسال: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(@NonNull String newVerificationId, @NonNull PhoneAuthProvider.ForceResendingToken newToken) {
                Log.d(TAG, "onCodeSent:" + newVerificationId);
                hideLoading();

                verificationId = newVerificationId;
                resendToken = newToken;

                Toast.makeText(VerifyCodeActivity.this, "تم إعادة إرسال رمز التحقق", Toast.LENGTH_SHORT).show();
                clearOtpFields();
                startResendTimer();
            }
        };

        DirectPhoneAuthSolution.startVerification(
                this,
                phoneNumber,
                callbacks,
                null,
                null
        );
    }

    private void showLoading() {
        isLoading = true;
        verifyButton.setEnabled(false);
        verifyButton.setText("جار التحقق...");
        if (!isTimerRunning) {
            resendText.setEnabled(false);
            resendText.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary));
            resendText.setOnClickListener(null);
        }
        for (EditText editText : otpFields) {
            editText.setEnabled(false);
        }
        //  progressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        isLoading = false;
        verifyButton.setEnabled(true);
        verifyButton.setText("تأكيد");
        if (!isTimerRunning) {
            resendText.setEnabled(true);
            resendText.setTextColor(ContextCompat.getColor(this, R.color.primary_dark));
            resendText.setOnClickListener(v -> {
                if (!isLoading) resendVerificationCode();
            });
        }
        for (EditText editText : otpFields) {
            editText.setEnabled(true);
        }
        //  progressBar.setVisibility(View.GONE);
    }

    private void clearOtpFields() {
        for (EditText editText : otpFields) {
            editText.setText("");
        }
        if (otpFields.length > 0) {
            otpFields[0].requestFocus();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}

