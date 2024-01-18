package com.example.petreminder;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.example.petreminder.authentication.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (FirebaseAuth.getInstance().getCurrentUser()==null){
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
            else {
                startActivity(new Intent(SplashActivity.this, ProfilesActivity.class));
                finish();
            }
        }, 2000);

    }
}