package com.example.petreminder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.petreminder.authentication.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {

    RelativeLayout rootLayout;

    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        TextView userName = findViewById(R.id.user_profile_name);
        userName.setText(mAuth.getCurrentUser().getDisplayName());

        CircleImageView userImage = findViewById(R.id.user_profile_image);
        Glide.with(UserProfileActivity.this).load(mAuth.getCurrentUser().getPhotoUrl()).into(userImage);

        TextView userEmail = findViewById(R.id.user_profile_email);
        userEmail.setText(mAuth.getCurrentUser().getEmail());


        Button logoutBtn = findViewById(R.id.logout);
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        rootLayout = findViewById(R.id.profiles_activity_rl);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnNavigationItemSelectedListener
                (new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                        switch (item.getItemId()) {
                            case R.id.nav_home:
                                UserProfileActivity.this.startActivity(new Intent(UserProfileActivity.this,
                                        ProfilesActivity.class));
                                UserProfileActivity.this.finish();
                                UserProfileActivity.this.overridePendingTransition(0, 0);
                                return true;
                            case R.id.nav_profile:
                                return true;
                        }
                        return false;
                    }
                });
        
    }
}