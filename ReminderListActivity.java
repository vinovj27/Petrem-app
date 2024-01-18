package com.example.petreminder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.petreminder.adapters.ReminderListActivityViewModel;
import com.example.petreminder.adapters.ReminderListAdapter;
import com.example.petreminder.adapters.ReminderSwipeToDeleteCallback;
import com.example.petreminder.databinding.ActivityReminderListBinding;
import com.example.petreminder.utils.Constants;
import com.example.petreminder.utils.SharedPreferencesUtil;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Date;
import java.util.List;

public class ReminderListActivity extends AppCompatActivity {

    private ActivityReminderListBinding binding;
    @SuppressWarnings("FieldCanBeLocal")
    private ReminderListActivityViewModel viewModel;
    private String mEventName;
    private String mUserId;
    private String mData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String t = SharedPreferencesUtil.getTheme();

        if (t == null) {
            setTheme(R.style.AppTheme);
        } else if (t.equals(Constants.AppThemes.DARK)) {
            setTheme(R.style.AppTheme);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
        binding = ActivityReminderListBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);

        Intent i = getIntent();

        if (i == null) return;

        mEventName = i.getStringExtra("name");
        mData = i.getStringExtra("event");

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            mUserId = auth.getCurrentUser().getUid();
        }

        if (mEventName == null) return;

        viewModel = new ViewModelProvider(this).get(ReminderListActivityViewModel.class);
        viewModel.getReminders(mEventName).observe(this, this::initRecyclerView);

        binding.reminderListBackFab.setOnClickListener(v -> {
            Intent eventDetailIntent = new Intent(this, EventDetailActivity.class);
            eventDetailIntent.putExtra("event", mData);
            eventDetailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(eventDetailIntent);
            finish();
        });

        binding.reminderListAddFab.setOnClickListener(v -> {
            Intent reminderDetailIntent = new Intent(ReminderListActivity.this,
                    ReminderDetailActivity.class);

            reminderDetailIntent.putExtra("name", mEventName);
            reminderDetailIntent.putExtra("userId", mUserId);
            reminderDetailIntent.putExtra("event", mData);

            startActivity(reminderDetailIntent);
            finish();
        });
    }

    private void initRecyclerView(List<Date> reminders) {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);

        RecyclerViewClickListener listener = (view, position) -> {
            Intent reminderDetailIntent = new Intent(this, ReminderDetailActivity.class);

            reminderDetailIntent.putExtra("reminder", reminders.get(position).getTime());
            reminderDetailIntent.putExtra("name", mEventName);
            reminderDetailIntent.putExtra("userId", mUserId);
            reminderDetailIntent.putExtra("event", mData);

            startActivity(reminderDetailIntent);
        };

        ReminderListAdapter adapter = new ReminderListAdapter(this, reminders, listener);
        binding.recyclerView.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ReminderSwipeToDeleteCallback(adapter, mEventName, mUserId)
        );

        itemTouchHelper.attachToRecyclerView(binding.recyclerView);

        if (reminders.isEmpty()) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.emptyView.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.emptyView.setVisibility(View.GONE);
        }
    }
}