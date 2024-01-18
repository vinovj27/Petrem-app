package com.example.petreminder;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petreminder.authentication.LoginActivity;
import com.example.petreminder.reminders.RemindersActivity;
import com.example.petreminder.schedules.ScheduleActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static android.view.View.GONE;

public class ProfilesActivity extends AppCompatActivity implements ProfileInterface,ActionMode.Callback {

    private List<Profile> profileList = new ArrayList<>();
    private RecyclerView profilesRecyclerView;
    private ProfileAdapter profileAdapter;
    private boolean pAIsMultiSelect = false;
    private List<Integer> pASelectedPositions = new ArrayList<>();
    private ActionMode actionMode;
    private FloatingActionButton nPFloatingActionButton;
    private CardView setReminderBtn, setScheduleBtn;
    private int PICK_IMAGE_REQUEST = 1;
    private ProfilesDatabaseAdapter profilesDatabaseAdapter;
    private LinearLayout profilesActivityESLL;
    private boolean isEditingPPU = false;
    private int selectedPosition = 0;
    RelativeLayout rootLayout;

    protected Button updateAppBtn;
    protected LinearLayout updateAppLinearLayout;
    protected Double latestAppVersion=1.0;
    protected Double currentAppVersion;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);


        Float elevation=0.0f;
        getSupportActionBar().setElevation(elevation);

        rootLayout = findViewById(R.id.profiles_activity_rl);
        profilesActivityESLL = findViewById(R.id.pa_empty_state_linear_layout);

        profilesDatabaseAdapter = new ProfilesDatabaseAdapter(this);
        profilesDatabaseAdapter.open();

        nPFloatingActionButton = findViewById(R.id.new_profile_fab);
        nPFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startImagePickingProcedure();
            }
        });

        initializeProfileList();

        profilesRecyclerView = findViewById(R.id.profiles_recycler_view);

        profileAdapter = new ProfileAdapter(this, profileList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true);
        profilesRecyclerView.setLayoutManager(mLayoutManager);
        profilesRecyclerView.setItemAnimator(new DefaultItemAnimator());

        profilesRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this, profilesRecyclerView, new RecyclerTouchListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (pAIsMultiSelect){
                    multiSelect(position);
                }
                else {

                    Profile clickedProfile = profileList.get(position);

                    int id = clickedProfile.getId();
                    String name = clickedProfile.getName();
                    String profilePictureUri = clickedProfile.getProfilePictureUri();
                    int weight = clickedProfile.getWeight();
                    int gender = clickedProfile.getGender();
                    String breed = clickedProfile.getBreed();
                    String dob = clickedProfile.getDateOfBirth();
                    int age = clickedProfile.getAge();


                    openDetailActivity(id, name ,profilePictureUri ,weight ,gender ,breed , dob, age);

                }

            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (!pAIsMultiSelect){
                    pASelectedPositions = new ArrayList<>();
                    pAIsMultiSelect = true;

                    if (actionMode == null){
                        nPFloatingActionButton.hide();
                        actionMode = startActionMode(ProfilesActivity.this);
                    }
                }

                multiSelect(position);
            }
        }));

        profilesRecyclerView.setAdapter(profileAdapter);
        setProfilesActivityEmptyState();

        String currentAppVersionString="1.0";
        try {
            currentAppVersionString = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        currentAppVersion=Double.valueOf(currentAppVersionString);


        setReminderBtn = findViewById(R.id.setReminder);
        setReminderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ProfilesActivity.this, RemindersActivity.class));
            }
        });

//        setScheduleBtn = findViewById(R.id.setSchedule);
//        setScheduleBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                startActivity(new Intent(ProfilesActivity.this, ScheduleActivity.class));
//            }
//        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnNavigationItemSelectedListener
                (new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                        switch (item.getItemId()) {
                            case R.id.nav_home:
                                return true;
                            case R.id.nav_profile:
                                ProfilesActivity.this.startActivity(new Intent(ProfilesActivity.this,
                                        UserProfileActivity.class));
                                ProfilesActivity.this.finish();
                                ProfilesActivity.this.overridePendingTransition(0, 0);
                                return true;
                        }
                        return false;
                    }
                });
        
    }


    public void showEditCSnackBar(){
        Snackbar.make(rootLayout, "Profile Updated", Snackbar.LENGTH_SHORT).show();
    }

    public void updateProfile(final int profileId, final String nProfileName, final int nProfileWeight, final int nProfileGender, final String nProfileBreed, final String nProfileDob, final int profilePosition) {

        Runnable updateProfileRunnable = new Runnable() {
            @Override
            public void run() {
                profilesDatabaseAdapter.updateProfile(profileId, nProfileName, nProfileWeight, nProfileGender, nProfileBreed, nProfileDob);
            }
        };
        Thread updateProfileThread = new Thread(updateProfileRunnable);
        updateProfileThread.start();
        try {
            updateProfileThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        updateProfileListItem(nProfileName, nProfileWeight, nProfileGender, nProfileBreed, nProfileDob, profilePosition);
    }

    protected void updateProfileListItem(String nProfileName, int nProfileWeight, int nProfileGender, String nProfileBreed, String nProfileDob, int profilePosition) {

        int age = getAgeFromDateOfBirth(nProfileDob);

        profileList.get(profilePosition).setName(nProfileName);
        profileList.get(profilePosition).setWeight(nProfileWeight);
        profileList.get(profilePosition).setGender(nProfileGender);
        profileList.get(profilePosition).setBreed(nProfileBreed);
        profileList.get(profilePosition).setDateOfBirth(nProfileDob);
        profileList.get(profilePosition).setAge(age);

        ProfileComparator profileComparator = new ProfileComparator();
        Collections.sort(profileList, profileComparator);

        profileAdapter.notifyDataSetChanged();
    }

    protected void setProfilesActivityEmptyState() {

        if (profileList.size() == 0) {

            if (profilesRecyclerView.getVisibility() == View.VISIBLE) {
                profilesRecyclerView.setVisibility(GONE);
            }

            if (profilesActivityESLL.getVisibility() == GONE) {
                profilesActivityESLL.setVisibility(View.VISIBLE);
            }


        } else {

            if (profilesActivityESLL.getVisibility() == View.VISIBLE) {
                profilesActivityESLL.setVisibility(GONE);
            }

            if (profilesRecyclerView.getVisibility() == GONE) {
                profilesRecyclerView.setVisibility(View.VISIBLE);
            }

        }

    }

    public void openNewProfileFSD(String profilePictureUri){
        FragmentManager oNPFSD = getSupportFragmentManager();
        ProfileFSD newProfileFSD = ProfileFSD.newInstance(profilePictureUri,0,"",0, 0, "", "", 0, false);
        FragmentTransaction oNPFSDT = oNPFSD.beginTransaction();
        oNPFSDT.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        oNPFSDT.add(android.R.id.content, newProfileFSD).addToBackStack(null).commit();
    }

    public void openEditProfileFSD(int id, String name, int weight, int gender, String breed, String dateOfBirth, int profilePosition){
        FragmentManager oEPFSD = getSupportFragmentManager();
        ProfileFSD editProfileFSD = ProfileFSD.newInstance("",id,name,weight, gender, breed, dateOfBirth, profilePosition, true);
        FragmentTransaction oEPFSDT = oEPFSD.beginTransaction();
        oEPFSDT.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        oEPFSDT.add(android.R.id.content, editProfileFSD).addToBackStack(null).commit();

    }

    public void startImagePickingProcedure(){
        Toast.makeText(getApplicationContext(),"Select a profile picture",Toast.LENGTH_SHORT).show();
        Intent imagePickingIntent = new Intent();
        imagePickingIntent.setType("image/*");
        if (Build.VERSION.SDK_INT <19){
            imagePickingIntent.setAction(Intent.ACTION_GET_CONTENT);
        } else {
            imagePickingIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        }
        startActivityForResult(Intent.createChooser(imagePickingIntent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    private void updateProfilePicture(int selectedPosition1, final String imagePickerUriS){
        final int sProfileId = profileList.get(selectedPosition1).getId();

        Runnable updateProfilePRunnable = new Runnable() {
            @Override
            public void run() {
                profilesDatabaseAdapter.updateProfilePicture(sProfileId, imagePickerUriS);
            }
        };
        Thread updateProfilePThread = new Thread(updateProfilePRunnable);
        updateProfilePThread.start();
        try {
            updateProfilePThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        profileList.get(selectedPosition1).setProfilePictureUri(imagePickerUriS);
        profileAdapter.notifyItemChanged(selectedPosition1);
        isEditingPPU = false;
        selectedPosition=0;

        Snackbar.make(rootLayout, "Profile Picture Updated", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Toast.makeText(getApplicationContext(),"Profile picture selected",Toast.LENGTH_SHORT).show();

            Uri imagePickerUri = data.getData();

            if (Build.VERSION.SDK_INT >= 19) {
                final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                try {
                    getContentResolver().takePersistableUriPermission(imagePickerUri, takeFlags);
                }
                catch (SecurityException e){
                    e.printStackTrace();
                }
            }


            if(isEditingPPU){
                updateProfilePicture(selectedPosition, String.valueOf(imagePickerUri));
            }

            else {
                openNewProfileFSD(String.valueOf(imagePickerUri));
            }

        }

    }

    protected void initializeProfileList() {

        Runnable initializeProfileListRunnable = new Runnable() {
            @Override
            public void run() {
                profileList = profilesDatabaseAdapter.fetchProfiles();
            }
        };
        Thread initializeProfileListThread = new Thread(initializeProfileListRunnable);
        initializeProfileListThread.start();
        try {
            initializeProfileListThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    public long getTIMFromDS(String pDTS){
        DateFormat dateFormat=new SimpleDateFormat("dd/MM/yyyy");
        Date date = null;
        try {
            date=dateFormat.parse(pDTS);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        long reminderTIM = 0;

        if (date != null) {
            reminderTIM = date.getTime();
        }

        return reminderTIM;
    }

    protected long getNowTIM(){
        Date currentDateTime =new Date();
        long currentDateTimeInMills = currentDateTime.getTime();
        return currentDateTimeInMills;
    }

    protected int getAgeFromDateOfBirth(String dob){

        long dobMills = getTIMFromDS(dob);

        long nowTIM = getNowTIM();

        long ageInMills = nowTIM - dobMills;

        int ageInDays = (int) (ageInMills/ (1000*60*60*24));

        int ageInYears = (int) (ageInDays/ (365));

        return ageInYears;
    }

    public void addProfile(final String name, final String profilePictureUri, final int weight, final int gender, final String breed, final String dateOfBirth) {
        final int[] newProfileId = new int[1];

        Runnable addProfileRunnable = new Runnable() {
            @Override
            public void run() {
                newProfileId[0] = profilesDatabaseAdapter.createProfile(name, profilePictureUri, weight, gender, breed, dateOfBirth);

            }
        };
        Thread addProfileThread = new Thread(addProfileRunnable);
        addProfileThread.start();
        try {
            addProfileThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int age = getAgeFromDateOfBirth(dateOfBirth);

        Profile newProfile = new Profile(newProfileId[0], name, profilePictureUri, weight, gender, breed, dateOfBirth, age);
        addProfileToList(newProfile);
        setProfilesActivityEmptyState();
    }

    protected void addProfileToList(Profile newProfile) {
        profileList.add(newProfile);

        ProfileComparator profileComparator = new ProfileComparator();
        Collections.sort(profileList, profileComparator);

        profileAdapter.notifyDataSetChanged();

        openDetailActivity(newProfile.getId(),newProfile.getName(), newProfile.getProfilePictureUri(),newProfile.getWeight(),newProfile.getGender(),newProfile.getBreed(), newProfile.getDateOfBirth(), newProfile.getAge());
    }

    private void multiSelect(int position) {
        Profile selectedProfile = profileAdapter.getItem(position);
        if (selectedProfile != null){
            if (actionMode != null) {
                int previousPosition=-1;
                if(pASelectedPositions.size()>0){
                    previousPosition= pASelectedPositions.get(0);
                }
                pASelectedPositions.clear();
                pASelectedPositions.add(position);

                profileAdapter.setSelectedPositions(previousPosition, pASelectedPositions);
            }
        }
    }

    public void openDetailActivity(int profileId, String name, String profilePictureUri, int weight, int gender, String breed, String dob, int age){

        Intent mainToFlexibleActivityIntent = new Intent(ProfilesActivity.this,DetailActivity.class);
        mainToFlexibleActivityIntent.putExtra("id", profileId);
        mainToFlexibleActivityIntent.putExtra("name",name);
        mainToFlexibleActivityIntent.putExtra("profilePictureUri",profilePictureUri);
        mainToFlexibleActivityIntent.putExtra("weight",weight);
        mainToFlexibleActivityIntent.putExtra("gender",gender);
        mainToFlexibleActivityIntent.putExtra("breed",breed);
        mainToFlexibleActivityIntent.putExtra("dob",dob);
        mainToFlexibleActivityIntent.putExtra("age",age);

        startActivity(mainToFlexibleActivityIntent);
    }

    public void hideActionBar(){
        getSupportActionBar().hide();
    }

    public void showActionBar(){
        getSupportActionBar().show();
    }

    protected void deleteProfileListItem(int profilePosition) {
        profileList.remove(profilePosition);
        profileAdapter.notifyItemRemoved(profilePosition);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profiles_activity_options_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.profiles_activity_help:
                openHelp();
                return true;

            case R.id.profiles_activity_sa:
                shareApp();
                return true;

            case R.id.profiles_activity_lo:
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return true;
            default:
                return false;
        }
    }


    protected void shareApp(){
        Intent sAIntent = new Intent();
        sAIntent.setAction(Intent.ACTION_SEND);
        sAIntent.putExtra(Intent.EXTRA_TEXT,"Pet Manager is a fast and simple app that enables me to store basic information about my pets for free. Get it at: https://play.google.com/store/apps/details?id="+getPackageName());
        sAIntent.setType("text/plain");
        Intent.createChooser(sAIntent,"Share via");
        startActivity(sAIntent);
    }

    protected void openHelp(){
        Intent homeToHelpActivityIntent = new Intent(ProfilesActivity.this,HelpActivity.class);
        startActivity(homeToHelpActivityIntent);
    }



    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.profiles_avw, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {

            case R.id.delete_profile:
                AlertDialog.Builder deleteProfileDialogBuilder = new AlertDialog.Builder(this);
                deleteProfileDialogBuilder.setTitle(getResources().getString(R.string.delete_profile_dialog_title));
                deleteProfileDialogBuilder.setMessage(getResources().getString(R.string.delete_profile_dialog_message));
                deleteProfileDialogBuilder.setNegativeButton(getResources().getString(R.string.delete_profile_dialog_negative_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        actionMode.finish();

                    }
                });

                deleteProfileDialogBuilder.setPositiveButton(getResources().getString(R.string.delete_profile_dialog_positive_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        if (pASelectedPositions.size() > 0) {
                            int selectedPosition = pASelectedPositions.get(0);
                            final int profileId = profileList.get(selectedPosition).getId();

                            Runnable deleteProfileRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    profilesDatabaseAdapter.deleteProfile(profileId);
                                }
                            };
                            Thread deleteProfileThread = new Thread(deleteProfileRunnable);
                            deleteProfileThread.start();
                            try {
                                deleteProfileThread.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            deleteProfileListItem(selectedPosition);
                            setProfilesActivityEmptyState();
                        }

                        dialogInterface.dismiss();
                        actionMode.finish();
                    }
                });
                deleteProfileDialogBuilder.create().show();

                return true;

            case R.id.edit_pdetails:

                if (pASelectedPositions.size() > 0) {
                    int selectedPosition = pASelectedPositions.get(0);
                    Profile selectedProfile = profileList.get(selectedPosition);
                    openEditProfileFSD(selectedProfile.getId(), selectedProfile.getName(),selectedProfile.getWeight(),selectedProfile.getGender(),selectedProfile.getBreed(),selectedProfile.getDateOfBirth(),selectedPosition);
                }
                actionMode.finish();
                return true;

            case R.id.edit_ppicture:

                if (pASelectedPositions.size() > 0) {
                    int selectedPosition1 = pASelectedPositions.get(0);
                    isEditingPPU = true;
                    selectedPosition = selectedPosition1;
                    startImagePickingProcedure();
                }

                actionMode.finish();
                return true;
            default:

        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

        nPFloatingActionButton.show();
        actionMode = null;
        pAIsMultiSelect = false;

        int previousPosition = -1;
        if (pASelectedPositions.size() > 0) {
            previousPosition = pASelectedPositions.get(0);
        }
        pASelectedPositions = new ArrayList<>();

        profileAdapter.setSelectedPositions(previousPosition, new ArrayList<Integer>());
    }

    protected static class ProfileComparator implements Comparator<Profile> {

        public int compare(Profile profileOne, Profile profileTwo) {
            String profileOName = profileOne.getName();
            String profileTName = profileTwo.getName();

            int comparisonResult = profileOName.compareToIgnoreCase(profileTName);

            return comparisonResult;
        }


    }

}
