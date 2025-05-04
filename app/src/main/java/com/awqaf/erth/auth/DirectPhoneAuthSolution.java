package com.awqaf.erth.auth;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;


public class DirectPhoneAuthSolution {
    private static final String TAG = "DirectPhoneAuth";

    public static void startVerification(Activity activity,
                                         String phoneNumber,
                                         PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks,
                                         ProgressBar progressBar,
                                         TextView errorTextView) {

        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+966" + phoneNumber;
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        if (errorTextView != null) {
            errorTextView.setVisibility(View.GONE);
        }

        Log.d(TAG, "Starting phone verification for: " + phoneNumber);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        String finalPhoneNumber = phoneNumber;
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
//                .setTimeout(120L, TimeUnit.SECONDS)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        Log.d(TAG, "onVerificationCompleted");
                        hideLoading(progressBar);
                        callbacks.onVerificationCompleted(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Log.e(TAG, "onVerificationFailed", e);
                        hideLoading(progressBar);

                        // التحقق من خطأ 17093 المحدد
                        if (e.getMessage() != null &&
                                (e.getMessage().contains("missing a valid app identifier") ||
                                        e.getMessage().contains("17093") ||
                                        e.getMessage().contains("reCAPTCHA checks were unsuccessful"))) {

                            Log.d(TAG, "Detected 17093 error or reCAPTCHA failure, trying secondary approach");

                            retryWithSecondaryApproach(activity, finalPhoneNumber, callbacks, progressBar, errorTextView);
                        } else {
                            callbacks.onVerificationFailed(e);
                        }
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        Log.d(TAG, "onCodeSent: " + verificationId);
                        hideLoading(progressBar);
                        callbacks.onCodeSent(verificationId, token);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private static void retryWithSecondaryApproach(Activity activity,
                                                   String phoneNumber,
                                                   PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks,
                                                   ProgressBar progressBar,
                                                   TextView errorTextView) {

        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+966" + phoneNumber;
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(180L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build();

        Toast.makeText(activity,
                "جاري إعادة محاولة التحقق...",
                Toast.LENGTH_LONG).show();

        Log.d(TAG, "Retrying with secondary approach for: " + phoneNumber);

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public static void verifyManualCode(Activity activity,
                                        String verificationId,
                                        String code,
                                        OnCompleteListener<Void> onSuccess,
                                        OnCompleteListener<Exception> onFailure) {

        try {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);

            FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (onSuccess != null) {
                                onSuccess.onComplete(null);
                            }
                        } else {
                            Exception error = task.getException();
                            if (onFailure != null) {
                                onFailure.onComplete(null);
                            }
                            Toast.makeText(activity,
                                    "فشل التحقق: " + (error != null ? error.getMessage() : "رمز غير صحيح"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (Exception e) {
            if (onFailure != null) {
                onFailure.onComplete(null);
            }
            Toast.makeText(activity, "خطأ في التحقق: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void hideLoading(ProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }
}