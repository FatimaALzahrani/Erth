package com.awqaf.erth.start;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.awqaf.erth.R;
import com.awqaf.erth.auth.DirectPhoneAuthSolution;
import com.awqaf.erth.utils.FirebaseErrorHandler;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText phoneInput;
    private AppCompatButton loginButton;
    private ProgressBar progressBar;
    private TextView errorTextView;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        phoneInput = findViewById(R.id.phone_input);
        loginButton = findViewById(R.id.login_button);
        progressBar = findViewById(R.id.progressBar);
        errorTextView = findViewById(R.id.errorTextView);
        Button registerButton = findViewById(R.id.register_button);

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        setupVerificationCallbacks();

        loginButton.setOnClickListener(v -> {
            String phone = phoneInput.getText().toString().trim();

            if (TextUtils.isEmpty(phone)) {
                phoneInput.setError("رقم الجوال مطلوب");
                return;
            }

            phone = formatPhoneNumber(phone);

            startVerification(phone);
        });
    }

    private void setupVerificationCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                hideLoading();
                Log.w(TAG, "onVerificationFailed", e);
                FirebaseErrorHandler.handleFirebaseException(e, LoginActivity.this, errorTextView);
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                hideLoading();
                mVerificationId = verificationId;
                mResendToken = token;

                Intent intent = new Intent(LoginActivity.this, LoginVerifyCodeActivity.class);
                intent.putExtra("verificationId", verificationId);
                intent.putExtra("resendToken", token);
                intent.putExtra("phoneNumber", phoneInput.getText().toString().trim());
                startActivity(intent);
                finish();
            }
        };
    }

    private void startVerification(String phoneNumber) {
        showLoading();

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.orderByChild("phone").equalTo(phoneNumber).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    DirectPhoneAuthSolution.startVerification(
                            LoginActivity.this,
                            phoneNumber,
                            mCallbacks,
                            progressBar,
                            errorTextView
                    );
                } else {
                    hideLoading();
                    Toast.makeText(LoginActivity.this, "هذا الرقم غير مسجل، الرجاء إنشاء حساب أولاً.", Toast.LENGTH_LONG).show();
                }
            } else {
                hideLoading();
                Log.e(TAG, "Database Error: ", task.getException());
                Toast.makeText(LoginActivity.this, "خطأ في الاتصال بقاعدة البيانات: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        loginButton.setEnabled(true);
    }

    private String formatPhoneNumber(String phone) {
        if (phone.startsWith("0")) {
            phone = phone.substring(1);
        }

        if (!phone.startsWith("+966")) {
            phone = "+966" + phone;
        }

        return phone;
    }
}