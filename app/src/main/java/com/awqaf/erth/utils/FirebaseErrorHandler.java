package com.awqaf.erth.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

/**
 * معالج أخطاء Firebase للتعامل مع مشاكل المصادقة
 */
public class FirebaseErrorHandler {
    private static final String TAG = "FirebaseErrorHandler";

    // أكواد الأخطاء
    public static final int ERROR_NETWORK = 1;
    public static final int ERROR_INVALID_PHONE = 2;
    public static final int ERROR_TOO_MANY_REQUESTS = 3;
    public static final int ERROR_VERIFICATION_FAILED = 4;
    public static final int ERROR_INVALID_CODE = 5;
    public static final int ERROR_SESSION_EXPIRED = 6;
    public static final int ERROR_RECAPTCHA_CHECK_FAILED = 7;
    public static final int ERROR_GENERAL = 100;

    /**
     * معالجة استثناءات Firebase وإرجاع رسائل مناسبة باللغة العربية
     * @param exception الاستثناء المراد معالجته
     * @param context سياق التطبيق لعرض رسائل Toast
     * @param errorTextView TextView اختياري لعرض الخطأ (يمكن أن يكون null)
     * @return رمز الخطأ
     */
    public static int handleFirebaseException(Exception exception, Context context, TextView errorTextView) {
        String errorMessage;
        int errorCode = ERROR_GENERAL;

        Log.e(TAG, "Firebase error: " + exception.getMessage());

        if (exception instanceof FirebaseNetworkException) {
            errorMessage = "خطأ في الاتصال بالإنترنت. يرجى التحقق من اتصالك والمحاولة مرة أخرى.";
            errorCode = ERROR_NETWORK;
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            if (exception.getMessage().contains("The format of the phone number provided is incorrect")) {
                errorMessage = "تنسيق رقم الهاتف غير صحيح. يرجى إدخال رقم صالح.";
                errorCode = ERROR_INVALID_PHONE;
            } else if (exception.getMessage().contains("The verification code is invalid")) {
                errorMessage = "رمز التحقق غير صحيح. يرجى التحقق والمحاولة مرة أخرى.";
                errorCode = ERROR_INVALID_CODE;
            } else {
                errorMessage = "بيانات الاعتماد غير صالحة. يرجى المحاولة مرة أخرى.";
                errorCode = ERROR_VERIFICATION_FAILED;
            }
        } else if (exception instanceof FirebaseTooManyRequestsException) {
            errorMessage = "تم إرسال الكثير من الطلبات. يرجى المحاولة مرة أخرى لاحقًا.";
            errorCode = ERROR_TOO_MANY_REQUESTS;
        } else if (exception.getMessage() != null && exception.getMessage().contains("missing a valid app identifier")
                || exception.getMessage() != null && exception.getMessage().contains("reCAPTCHA checks were unsuccessful")) {
            errorMessage = "فشل التحقق من صحة الجهاز. تأكد من تحديث خدمات Google Play وحاول مرة أخرى.";
            errorCode = ERROR_RECAPTCHA_CHECK_FAILED;
        } else if (exception.getMessage() != null && exception.getMessage().contains("verification code has expired")) {
            errorMessage = "انتهت صلاحية رمز التحقق. يرجى طلب رمز جديد.";
            errorCode = ERROR_SESSION_EXPIRED;
        } else {
            errorMessage = "حدث خطأ غير متوقع. يرجى المحاولة مرة أخرى لاحقًا.";
            errorCode = ERROR_GENERAL;
        }

        // عرض الخطأ في TextView إذا كان متوفرًا
        if (errorTextView != null) {
            errorTextView.setVisibility(View.VISIBLE);
            errorTextView.setText(errorMessage);
        } else {
            // وإلا استخدم Toast
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
        }

        return errorCode;
    }

    /**
     * إنشاء دوال استدعاء التحقق المحسّنة مع معالجة أفضل للأخطاء
     * @param activity نشاط التطبيق
     * @param progressBar شريط التقدم للتحكم في الظهور
     * @param errorTextView TextView لعرض الأخطاء
     * @param successCallback واجهة للتعامل مع التحقق الناجح
     * @return دوال الاستدعاء المكونة
     */
    public static PhoneAuthProvider.OnVerificationStateChangedCallbacks createVerificationCallbacks(
            Activity activity, ProgressBar progressBar, TextView errorTextView,
            VerificationSuccessCallback successCallback) {

        return new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted: Auto-verification completed");
                hideLoading(progressBar);

                // دالة الاستدعاء لنجاح التحقق التلقائي
                if (successCallback != null) {
                    successCallback.onVerificationSuccess(credential, null, null);
                }
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);
                hideLoading(progressBar);

                // معالجة الخطأ المحدد
                handleFirebaseException(e, activity, errorTextView);
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent: " + verificationId);
                hideLoading(progressBar);

                // دالة الاستدعاء لإرسال الرمز بنجاح
                if (successCallback != null) {
                    successCallback.onVerificationSuccess(null, verificationId, token);
                }
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(String verificationId) {
                Log.d(TAG, "onCodeAutoRetrievalTimeOut: " + verificationId);
                hideLoading(progressBar);

                if (errorTextView != null) {
                    errorTextView.setVisibility(View.VISIBLE);
                    errorTextView.setText("انتهت مهلة استرداد الرمز تلقائيًا. يرجى إدخال الرمز يدويًا.");
                }
            }
        };
    }

    /**
     * واجهة للتعامل مع نجاح التحقق
     */
    public interface VerificationSuccessCallback {
        void onVerificationSuccess(PhoneAuthCredential credential, String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token);
    }

    /**
     * إخفاء مؤشرات التحميل
     */
    private static void hideLoading(ProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }
}