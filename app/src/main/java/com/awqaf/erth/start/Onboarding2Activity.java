package com.awqaf.erth.start;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.awqaf.erth.R;

public class Onboarding2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding2);

    }

    public void login(View view) {
        Intent intent=new Intent(Onboarding2Activity.this,LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void next(View view) {
        Intent intent=new Intent(Onboarding2Activity.this,Onboarding3Activity.class);
        startActivity(intent);
        finish();
    }
}