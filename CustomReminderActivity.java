package com.example.petreminder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;

import com.example.petreminder.databinding.ActivityCustomReminderBinding;
import com.example.petreminder.databinding.ActivityMainBinding;


public class CustomReminderActivity extends AppCompatActivity {

    private int id;
    private String name;
    //CalendarView calendarView;

    private ActivityCustomReminderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_custom_reminder);

        super.onCreate(savedInstanceState);
        binding = ActivityCustomReminderBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);


        Bundle extras = getIntent().getExtras();
        id = extras.getInt("id");
        name = extras.getString("name");

        //calendarView = findViewById(R.id.calendar_view);

        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Intent dailyPlanIntent = new Intent(this, DailyPlanActivity.class);
            dailyPlanIntent.putExtra("year", year);
            dailyPlanIntent.putExtra("month", month);
            dailyPlanIntent.putExtra("day", dayOfMonth);
            startActivity(dailyPlanIntent);
        });
    }

}