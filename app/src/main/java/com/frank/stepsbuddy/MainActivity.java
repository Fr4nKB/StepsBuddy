package com.frank.stepsbuddy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import static com.frank.stepsbuddy.MyUtil.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    public BottomNavigationView navbar;
    private int prev = -1;
    private SharedPreferences settings;
    public static LocalBroadcastManager lbm;
    private String bl = Manifest.permission.ACCESS_BACKGROUND_LOCATION;
    private int sdkV = Build.VERSION.SDK_INT;
    private List<String> listPermissionsNeeded = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askPermission();

        navbar = findViewById(R.id.bottomNavBar);
        navbar.setOnItemSelectedListener(navbarlistener);
        lbm = LocalBroadcastManager.getInstance(this);

        settings = getSharedPreferences(SETTINGS, MODE_PRIVATE);
        settings.edit().putString(STARTBTN, getString(R.string.startbtn)).apply();

        if(settings.getBoolean(FIRSTBOOT, true)) {
            settings.edit().putInt(NPATHSAVED, 0).apply();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentHolder, new Setup()).commit();
        }
        else if(savedInstanceState != null) restoreFragment(savedInstanceState);
        else  navbar.setSelectedItemId(R.id.homeTab);

        createNotifChannel();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        settings.edit().putString(CURRENTDATE, MyUtil.getCurrentDate()).apply();
    }

    private void restoreFragment(Bundle state) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment f = fm.getFragment(state, "fragState");
        if(f == null)  {
            ft.add(R.id.fragmentHolder, new HomeFragment());
            navbar.setSelectedItemId(R.id.homeTab);
        }
        else {
            ft.replace(R.id.fragmentHolder, f);
            navbar.setSelectedItemId(f.getId());
        }
        ft.commit();
    }

    protected void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        getSupportFragmentManager().putFragment(state, "fragState", getSupportFragmentManager().findFragmentById(R.id.fragmentHolder));
    }

    private final NavigationBarView.OnItemSelectedListener navbarlistener =
            item -> {
                int check = item.getItemId();
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();

                if(prev != R.id.homeTab && check == R.id.homeTab) {
                    if(prev == R.id.settingsTab) ft.setCustomAnimations(R.anim.slide_r2l_in, R.anim.slide_l2r_out);
                    else ft.setCustomAnimations(R.anim.slide_l2r_in, R.anim.slide_r2l_out);

                    ft.replace(R.id.fragmentHolder, new HomeFragment());
                }
                else if(prev != R.id.settingsTab && check == R.id.settingsTab) {
                    ft.setCustomAnimations(R.anim.slide_l2r_in, R.anim.slide_r2l_out)
                            .replace(R.id.fragmentHolder, new SettingsFragment());
                }
                else if(prev != R.id.statsTab && check == R.id.statsTab) {
                    ft.setCustomAnimations(R.anim.slide_r2l_in, R.anim.slide_l2r_out)
                            .replace(R.id.fragmentHolder, new StatsFragment());
                }

                ft.commit();
                prev = check;   //prev set equal to the id of current selected tab
                return true;
            };

    private void createNotifChannel() {
        NotificationChannel serviceCH =
                new NotificationChannel(CHANNEL_ID, SERVICE_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(serviceCH);

    }

    private void askPermission() {
        String ar = Manifest.permission.ACTIVITY_RECOGNITION;
        String fl = Manifest.permission.ACCESS_FINE_LOCATION;

        int sdkV = Build.VERSION.SDK_INT;

        int arperm = ContextCompat.checkSelfPermission(this, ar);
        int flperm = ContextCompat.checkSelfPermission(this, fl);

        if (arperm != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listPermissionsNeeded.add(ar);
        }
        if (flperm != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(fl);
            if (sdkV == Build.VERSION_CODES.Q) listPermissionsNeeded.add(bl);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            displayAlert(getString(R.string.permtitle), getString(R.string.permmsg),
                    getString(R.string.blbtn), null, mainHandler);
        }

    }

    private boolean askBL() {
        if(sdkV == Build.VERSION_CODES.R) {
            requestPermissions(new String[] {bl}, BL_CODE);
            return true;
        }

        return false;
    }

    private void displayAlert(String title, String msg, String pos, String neg, DialogInterface.OnClickListener handler) {
        new AlertDialog.Builder(this).setCancelable(false)
                .setTitle(title).setMessage(msg)
                .setPositiveButton(pos, handler).setNegativeButton(neg, handler).show();
    }

    private final DialogInterface.OnClickListener mainHandler = (dialogInterface, i) -> {
        if(i == Dialog.BUTTON_POSITIVE) requestPermissions(listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), PERM_CODE);
        else finish();
    };

    private final DialogInterface.OnClickListener dialogHandler = (dialogInterface, i) -> {
        if(i == Dialog.BUTTON_POSITIVE) askPermission();
        else finish();
    };

    private final DialogInterface.OnClickListener blHandler = (dialogInterface, i) -> {
        if(i == Dialog.BUTTON_POSITIVE) askBL();
        else finish();
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERM_CODE) {
            if(grantResults[0] == PackageManager.PERMISSION_DENIED ||
                    grantResults[1] == PackageManager.PERMISSION_DENIED) {
                displayAlert(getString(R.string.permtitle), getString(R.string.permmsg),
                        getString(R.string.posbtn), getString(R.string.negbtn), dialogHandler);

            }

            else displayAlert(getString(R.string.permtitle), getString(R.string.blmsg),
                        getString(R.string.blbtn), null, blHandler);

            return;
        }

        if(requestCode == BL_CODE && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            displayAlert(getString(R.string.permtitle), getString(R.string.blmsg),
                    getString(R.string.blbtn), null, blHandler);
        }

    }
}