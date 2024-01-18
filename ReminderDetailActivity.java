package com.example.petreminder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.petreminder.databinding.ActivityReminderDetailBinding;
import com.example.petreminder.models.Event;
import com.example.petreminder.utils.Constants;
import com.example.petreminder.utils.NotificationUtil;
import com.example.petreminder.utils.SharedPreferencesUtil;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ReminderDetailActivity extends AppCompatActivity {
    private static final String TAG = ReminderDetailActivity.class.getSimpleName();

    private ActivityReminderDetailBinding binding;
    private Date mDate;
    private Date mOriginalDate;

    private FirebaseFirestore db;
    private String userId;
    private String eventName;
    private Event mEvent;

    @SuppressWarnings("deprecation")
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
        binding = ActivityReminderDetailBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);

        Intent i = getIntent();

        if (i == null) return;

        long timeLong = i.getLongExtra("reminder", -1);
        eventName = i.getStringExtra("name");
        userId = i.getStringExtra("userId");
        String eventJsonString = i.getStringExtra("event");

        final Locale locale = new Locale(Constants.LOCALE_LANGUAGE, Constants.LOCALE_COUNTRY);

        if (eventName == null || userId == null || eventJsonString == null) return;

        mEvent = new Gson().fromJson(eventJsonString, Event.class);

        if (timeLong != -1) {
            mDate = new Date(timeLong);
            mOriginalDate = new Date(timeLong);

            String dateText = getString(R.string.date) + " "
                    + DateFormat.getDateInstance(DateFormat.FULL, locale).format(mDate);

            binding.reminderDetailStatusText.setText(dateText);
        } else {
            mDate = new Date();
            mDate.setSeconds(0);
        }

        db = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.reminderDetailDatePicker.setOnDateChangedListener(
                    (view, year, monthOfYear, dayOfMonth) -> {
                        mDate.setYear(year - Constants.DATE_YEAR_DIFF);
                        mDate.setMonth(monthOfYear);
                        mDate.setDate(dayOfMonth);

                        String dateText = getString(R.string.date) + " "
                                + DateFormat.getDateInstance(DateFormat.FULL, locale).format(mDate);

                        binding.reminderDetailStatusText.setText(dateText);
                    });
        }

        binding.reminderDetailTimePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            mDate.setHours(hourOfDay);
            mDate.setMinutes(minute);
            mDate.setSeconds(0);

            String dateText = getString(R.string.date) + " "
                    + DateFormat.getDateInstance(DateFormat.FULL, locale).format(mDate);

            binding.reminderDetailStatusText.setText(dateText);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reminder_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reminder_menu_save: {
                if (mDate != null &&
                        (mDate.before(mEvent.getStartDate()) || mDate.after(mEvent.getEndDate()))) {
                    Toast.makeText(this,
                            R.string.reminder_date_err_message, Toast.LENGTH_SHORT).show();
                    break;
                }

                if (mOriginalDate == null) {
                    save();
                } else {
                    update();
                }

                Intent reminderListIntent = new Intent(this,
                        ReminderListActivity.class);

                reminderListIntent.putExtra("name", eventName);
                reminderListIntent.putExtra("event", new Gson().toJson(mEvent));
                reminderListIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(reminderListIntent);
                finish();

                break;
            }

            case R.id.reminder_menu_delete: {
                if (null == mOriginalDate) break;

                delete();
                Intent reminderListIntent = new Intent(this,
                        ReminderListActivity.class);

                reminderListIntent.putExtra("name", eventName);
                reminderListIntent.putExtra("event", new Gson().toJson(mEvent));
                reminderListIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(reminderListIntent);
                finish();

                break;
            }

            default:
                break;
        }

        return true;
    }

    private void save() {
        if (db == null) return;

        Task<QuerySnapshot> result = db.collection(Constants.Collections.USERS)
                .document(userId)
                .collection(Constants.Collections.USER_EVENTS)
                .get();

        result.addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot s : queryDocumentSnapshots) {
                Event e = s.toObject(Event.class);
                if (e.getName() == null) continue;

                if (!e.getName().equals(eventName)) continue;

                ArrayList<Date> reminders = new ArrayList<>();
                if (e.getReminders() != null) {
                    reminders.addAll(e.getReminders());
                }

                reminders.add(mDate);

                s.getReference().update(Constants.EventFields.REMINDERS, reminders);

                final int requestCode = s.getId().hashCode();

                NotificationUtil.startRepeatingNotification(
                        this,
                        mDate,
                        requestCode,
                        e.getName(),
                        e.getDetail(),
                        e.getReminderFreq()
                );

                if (e.getReminderType().equals(Constants.ReminderTypes.VIBRATION)) {
                    NotificationUtil.startVibration(this, mDate, requestCode);
                } else if (e.getReminderType().equals(Constants.ReminderTypes.SOUND)) {
                    NotificationUtil.startSound(this, mDate, requestCode);
                } else {
                    Log.e(TAG, "Unknown reminder type");
                }

                break;
            }

            Toast.makeText(this,
                    R.string.reminder_add_ok_message, Toast.LENGTH_SHORT).show();
        });

        result.addOnFailureListener(e -> {
            Log.e(TAG, "Reminder save failed");
            Toast.makeText(this,
                    R.string.reminder_add_error_message, Toast.LENGTH_SHORT).show();
        });
    }

    private void update() {
        Task<QuerySnapshot> result = db.collection(Constants.Collections.USERS).document(userId)
                .collection(Constants.Collections.USER_EVENTS)
                .get();

        result.addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot s : queryDocumentSnapshots) {
                Event e = s.toObject(Event.class);

                if (e.getName() == null) continue;
                if (!e.getName().equals(eventName)) continue;

                ArrayList<Date> reminders = new ArrayList<>();

                if (e.getReminders() != null) {
                    reminders.addAll(e.getReminders());
                }


                for (Date d : reminders) {
                    if (d.equals(mOriginalDate)) {
                        reminders.remove(d);
                        break;
                    }
                }

                reminders.add(mDate);
                Task<Void> updateResult = s.getReference()
                        .update(Constants.EventFields.REMINDERS, reminders);

                updateResult.addOnSuccessListener(o -> {
                    NotificationUtil.cancelRepeatingNotification(this, s.getId().hashCode());
                    NotificationUtil.startRepeatingNotification(
                            this,
                            mDate,
                            s.getId().hashCode(),
                            e.getName(),
                            e.getDetail(),
                            e.getReminderFreq()
                    );

                    Toast.makeText(this,
                            R.string.reminder_update_ok_message, Toast.LENGTH_SHORT).show();
                });

                updateResult.addOnFailureListener(err -> {
                    Log.e(TAG, "Reminder update failed");
                    Toast.makeText(this,
                            R.string.reminder_update_error_message, Toast.LENGTH_SHORT).show();
                });
            }
        });

        result.addOnFailureListener(e -> {
            Log.e(TAG, "Reminder update failed");
            Toast.makeText(this,
                    R.string.reminder_update_error_message, Toast.LENGTH_SHORT).show();
        });
    }

    private void delete() {
        Task<QuerySnapshot> result = db.collection(Constants.Collections.USERS)
                .document(userId)
                .collection(Constants.Collections.USER_EVENTS)
                .whereEqualTo("name", eventName)
                .get();

        result.addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot s : queryDocumentSnapshots) {
                Event e = s.toObject(Event.class);
                ArrayList<Date> reminders = new ArrayList<>(e.getReminders());

                for (Date d : reminders) {
                    if (d.equals(mDate)) {
                        reminders.remove(d);
                        break;
                    }
                }

                s.getReference().update(Constants.EventFields.REMINDERS, reminders);
                final String reminderType = e.getReminderType();
                final int requestCode = s.getId().hashCode();

                NotificationUtil.cancelRepeatingNotification(this, requestCode);

                if (reminderType == null) {
                    Log.e(TAG, "Reminder is null");
                    return;
                }

                if (reminderType.equals(Constants.ReminderTypes.SOUND)) {
                    NotificationUtil.cancelSound(this, requestCode);
                } else if (reminderType.equals(Constants.ReminderTypes.VIBRATION)) {
                    NotificationUtil.cancelVibration(this, requestCode);
                } else {
                    Log.e(TAG, "Unknown reminder type");
                }

                break;
            }

            Toast.makeText(this,
                    R.string.reminder_delete_ok_message, Toast.LENGTH_SHORT).show();
        });

        result.addOnFailureListener(e -> {
            Log.e(TAG, "Reminder delete failed");
            Toast.makeText(this,
                    R.string.reminder_delete_error_message, Toast.LENGTH_SHORT).show();
        });
    }
}