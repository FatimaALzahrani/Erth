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

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LoginVerifyCodeActivity extends AppCompatActivity {

    private static final String TAG = "LoginVerifyCodeActivity";
    private static final long RESEND_TIMER = 60000;

    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private EditText[] otpFields;

    private AppCompatButton verifyButton;
    private TextView resendText;
    private TextView timerText, phoneText;

    private String verificationId, phoneNumber;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private FirebaseAuth mAuth;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private boolean isLoading = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code0);

        Log.d(TAG, "onCreate: Initializing LoginVerifyCodeActivity with new layout");

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);
        otpFields = new EditText[]{otp1, otp2, otp3, otp4, otp5, otp6};

        verifyButton = findViewById(R.id.verify_button);
        resendText = findViewById(R.id.resend_text);
        timerText = findViewById(R.id.timer_text);
        phoneText = findViewById(R.id.phone_text);
        // progressBar = findViewById(R.id.progressBar);
        // errorTextView = findViewById(R.id.errorTextView);

        mAuth = FirebaseAuth.getInstance();

        Intent intent = getIntent();
        verificationId = intent.getStringExtra("verificationId");
        resendToken = intent.getParcelableExtra("resendToken");
        phoneNumber = intent.getStringExtra("phoneNumber");

        if (phoneText != null && phoneNumber != null) {
            phoneText.setText(phoneNumber);
        }

        setupOtpInputListeners();

        verifyButton.setOnClickListener(v -> {
            if (isLoading) return;
            String code = getOtpCode();
            if (code.length() != 6) {
                Toast.makeText(this, "الرجاء إدخال رمز التحقق المكون من 6 أرقام", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyCode(code);
        });

        resendText.setOnClickListener(v -> {
            if (!isTimerRunning && !isLoading) {
                resendCode();
            }
        });

        startResendTimer();

        Log.d(TAG, "LoginVerifyCodeActivity initialized successfully with verificationId: " + verificationId);
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
                        // Handled by onKeyListener
                    }
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
        resendText.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary));
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
                resendText.setTextColor(ContextCompat.getColor(LoginVerifyCodeActivity.this, R.color.primary_dark));
                resendText.setText("إرسال مرة أخرى");
                resendText.setOnClickListener(v -> {
                    if (!isLoading) {
                        resendCode();
                    }
                });
                isTimerRunning = false;
            }
        }.start();
    }

    private void verifyCode(String code) {
        showLoading();
        try {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            signInWithCredential(credential);
        } catch (IllegalArgumentException e) {
            hideLoading();
            Log.e(TAG, "Error creating credential (invalid verification ID or code format): ", e);
            Toast.makeText(this, "خطأ في التحقق: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            hideLoading();
            Log.e(TAG, "Error creating credential: ", e);
            Toast.makeText(this, "خطأ غير متوقع في التحقق. حاول مرة أخرى.", Toast.LENGTH_LONG).show();
            // FirebaseErrorHandler.handleFirebaseException(e, this, errorTextView);
        }
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                Log.d(TAG, "signInWithCredential:success");
                String uid = mAuth.getCurrentUser().getUid();
                fetchUserDataAndNavigate(uid);
            } else {
                hideLoading();
                Log.w(TAG, "signInWithCredential:failure", task.getException());
                FirebaseErrorHandler.handleFirebaseException(task.getException(), this, null);
            }
        });
    }

    private void fetchUserDataAndNavigate(String uid) {
        if (!isLoading) showLoading();

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.get().addOnCompleteListener(task1 -> {
            hideLoading();
            if (task1.isSuccessful() && task1.getResult() != null && task1.getResult().exists()) {
                String role = String.valueOf(task1.getResult().child("role").getValue());
                Log.d(TAG, "User role fetched: " + role);

                Intent intent;
                if ("ناظر".equals(role)) {
                    intent = new Intent(LoginVerifyCodeActivity.this, NaderActivity.class);
                } else if ("واقف".equals(role)) {
                    intent = new Intent(LoginVerifyCodeActivity.this, WaqefActivity.class);
                } else {
                    Log.w(TAG, "Unexpected user role: " + role + ". Navigating to MainActivity.");
                    Toast.makeText(LoginVerifyCodeActivity.this, "دور المستخدم غير معروف.", Toast.LENGTH_SHORT).show();
                    intent = new Intent(LoginVerifyCodeActivity.this, MainActivity.class);
                }

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Log.e(TAG, "Failed to fetch user data or user does not exist.", task1.getException());
                Toast.makeText(LoginVerifyCodeActivity.this, "تعذر تحميل بيانات المستخدم أو المستخدم غير موجود.", Toast.LENGTH_LONG).show();
                // mAuth.signOut();
            }
        });
    }

    private void resendCode() {
        showLoading();
        DirectPhoneAuthSolution.startVerification(this, phoneNumber, new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "Resend: onVerificationCompleted");
                signInWithCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.w(TAG, "Resend: onVerificationFailed", e);
                hideLoading();
                FirebaseErrorHandler.handleFirebaseException(e, LoginVerifyCodeActivity.this, null);
            }

            @Override
            public void onCodeSent(@NonNull String newVerificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "Resend: onCodeSent:" + newVerificationId);
                hideLoading();
                verificationId = newVerificationId;
                resendToken = token;
                Toast.makeText(LoginVerifyCodeActivity.this, "تم إعادة إرسال رمز التحقق", Toast.LENGTH_SHORT).show();
                clearOtpFields();
                startResendTimer();
            }
        }, null, null);
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
    }

    private void hideLoading() {
        isLoading = false;
        verifyButton.setEnabled(true);
        verifyButton.setText("تأكيد");
        if (!isTimerRunning) {
            resendText.setEnabled(true);
            resendText.setTextColor(ContextCompat.getColor(this, R.color.primary_dark));
            resendText.setOnClickListener(v -> {
                if (!isLoading) resendCode();
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


