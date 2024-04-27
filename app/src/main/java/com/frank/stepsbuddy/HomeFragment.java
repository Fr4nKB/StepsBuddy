package com.frank.stepsbuddy;

import static android.content.Context.ALARM_SERVICE;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;

import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;

import static com.frank.stepsbuddy.MyUtil.*;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private boolean started = false;
    private ProgressBar circleBar;
    private RelativeLayout path;
    private TextView subgoalL, subgoalD, goalc;
    private EditText pathName;
    private Button startbtn, savepathbtn, cancelpathbtn;
    private SharedPreferences settings;
    private int goalNum = 1, currentNumSavedPaths = 0;
    private String goalType = STEPS;
    private MapView mapView;
    private static GoogleMap map;
    private Location lastLoc = null;
    private LocationRequest lr;
    private LocationCallback locationCB;
    private FusedLocationProviderClient fusedLocClient;
    private static Database db;
    private double height = 0, weight = 0;
    private boolean sex = true, tracking = false;    //true for male, false for female

    public MainActivity activity;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (MainActivity) getActivity();

        //setting up receiver for midnight reset/update of steps count
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        setAlarm(activity, calendar);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MIDNIGHT_RESET);
        filter.addAction(GOALUPDATE);
        activity.registerReceiver(br, filter);

        db = new Database(activity, 1);
        settings = activity.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);

        fusedLocClient = LocationServices.getFusedLocationProviderClient(activity);
        lr = LocationRequest.create().setInterval(5000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationCB = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location currentLoc = locationResult.getLastLocation();

                loadLatestLoc();
                if(lastLoc == null) {
                    lastLoc = currentLoc;
                    return;
                }

                float d = currentLoc.distanceTo(lastLoc);
                if(paths != null && started && d > 0) {
                    drawPath(currentLoc);
                }
                lastLoc = currentLoc;
            }
        };

        tracking = settings.getInt(MODE, 0) == R.id.radio_geo;
        if(tracking) fusedLocClient.requestLocationUpdates(lr, locationCB, Looper.getMainLooper());

    }

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup parent, Bundle state) {
        View v = inf.inflate(R.layout.home_fragment, parent, false);

        activity.findViewById(R.id.bottomNavBar).setVisibility(View.VISIBLE);

        circleBar = v.findViewById(R.id.progress_bar);
        path = v.findViewById(R.id.savePathDisplay);
        path.setVisibility(View.GONE);
        goalc = v.findViewById(R.id.goal_counter);
        subgoalL = v.findViewById(R.id.subgoal_label);
        subgoalD = v.findViewById(R.id.subgoal_display);
        pathName = v.findViewById(R.id.pathName);

        startbtn = v.findViewById(R.id.startbtn);
        startbtn.setOnClickListener(serviceHandler);
        savepathbtn = v.findViewById(R.id.savebtnPath);
        savepathbtn.setOnClickListener(serviceHandler);
        cancelpathbtn = v.findViewById(R.id.cancelbtnPath);
        cancelpathbtn.setOnClickListener(serviceHandler);

        currentNumSavedPaths = settings.getInt(NPATHSAVED, 0);

        String tmp = settings.getString(CURRENTDATE, ""); //retrieving date of last usage
        if (!MyUtil.getCurrentDate().equals(tmp)) {  //if the date is different reset the counter and update the date
            settings.edit().putInt(STEPS, 0).putInt(CAL, 0)
                    .putString(CURRENTDATE, MyUtil.getCurrentDate()).apply();
            circleBar.setProgress(0);
            goalc.setText("0");
            subgoalD.setText("0");
        }

        loadData();

        mapView = v.findViewById(R.id.map);
        mapView.onCreate(state);
        if(!tracking) mapView.setVisibility(View.GONE);
        else {
            mapView.setVisibility(View.VISIBLE);
            mapView.getMapAsync(this);
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        String tmp = settings.getString(STARTBTN, "");
        if (tmp.equals(getString(R.string.startbtn))) startbtn.setText(R.string.startbtn);
        else if (tmp.equals(getString(R.string.stopbtn))) startbtn.setText(R.string.stopbtn);

        int steps = settings.getInt(STEPS, 0);
        int cal = settings.getInt(CAL, 0);

        if (goalType.equals(STEPS)) updateGoalData(steps, cal);
        else updateGoalData(cal, steps);

        loadLatestLoc();

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        settings.edit().putString(STARTBTN, startbtn.getText().toString()).apply();
        saveGoalData();
    }

    @Override
    public void onStop() {
        super.onStop();
        db.addProgress(settings.getInt(STEPS, 0), settings.getInt(CAL, 0));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        activity.unregisterReceiver(br);
        if(tracking) fusedLocClient.removeLocationUpdates(locationCB);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void loadLatestLoc(){
        if(lastLoc != null) {
            LatLng locll = new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude());
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(locll).zoom(18).build();
            map.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition));
        }
    }

    private void drawPath(Location loc) {
        LatLng locll = new LatLng(loc.getLatitude(), loc.getLongitude());
        map.moveCamera(CameraUpdateFactory.newLatLng(locll));
        map.animateCamera(CameraUpdateFactory.zoomTo(18.0f));

        int dim = paths.size();
        if(dim == 0) {
            paths.add(new ArrayList<LatLng>());
            dim++;
        }
        List<LatLng> tmp = paths.get(dim - 1);
        tmp.add(locll);
        drawPolyLineOnMap(tmp, map, Color.BLUE);
    }

    private static class SavePathTask extends AsyncTask<Integer, Void , Void> {

        @Override
        protected Void doInBackground(Integer... integers) {
            int max = paths.size();
            LatLng tmp;
            for(int i = 0; i < max; i++) {
                ArrayList<LatLng> list = paths.get(i);
                int maxcoord = list.size();
                for(int j = 0; j < maxcoord; j++) {
                    tmp = list.get(j);
                    db.addCoordinates(integers[0], i, tmp.latitude, tmp.longitude);
                }
            }

            clearList();
            return null;
        }
    }

    private void setAlarm(Context context, Calendar calendar) {
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Intent i = new Intent(MIDNIGHT_RESET);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
    }

    private BroadcastReceiver br = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            switch (action) {
                case MIDNIGHT_RESET: {

                    SharedPreferences settings = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
                    db.addProgress(settings.getInt(STEPS, 0), settings.getInt(CAL, 0));
                    settings.edit().putInt(STEPS, 0).putInt(CAL, 0).putString(CURRENTDATE, getCurrentDate()).apply();
                    circleBar.setProgress(0);
                    goalc.setText("0");
                    subgoalD.setText("0");

                    //set alarm 24 hours from now
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    calendar.set(Calendar.HOUR_OF_DAY, 24);

                    setAlarm(context, calendar);
                    break;
                }

                case GOALUPDATE:
                    int tmp = intent.getExtras().getInt(STEPS);
                    tmp += settings.getInt(STEPS, 0);

                    loadGoalData(tmp);

                    saveGoalData();
                    break;
            }
        }
    };

    private final View.OnClickListener serviceHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            if(view == startbtn) {

                String tmp = startbtn.getText().toString();
                Intent i = new Intent(activity, MainService.class);

                if (tmp.equals(getString(R.string.startbtn))) {      //button says "start"

                    started = true;

                    boolean loc = checkLocation();
                    if(tracking && !loc) return;
                    else if(tracking) {
                        path.setVisibility(View.GONE);
                        mapView.setVisibility(View.VISIBLE);
                        loadLatestLoc();
                        loadPaths(loadMode);
                        paths.add(new ArrayList<LatLng>());
                    }

                    activity.startForegroundService(i);
                    startbtn.setText(R.string.stopbtn);

                } else {      //button says "stop"
                    started = false;
                    db.addProgress(settings.getInt(STEPS, 0), settings.getInt(CAL, 0));
                    saveGoalData();
                    activity.stopService(i);
                    startbtn.setText(R.string.startbtn);

                    if(tracking && currentNumSavedPaths < MAXNUMSAVEDPATH) {
                        mapView.setVisibility(View.GONE);
                        path.setVisibility(View.VISIBLE);
                    }

                }

            }

            else if(view == savepathbtn) {

                String name = pathName.getText().toString();
                path.setVisibility(View.GONE);
                pathName.setText("");
                mapView.setVisibility(View.VISIBLE);

                boolean tmp = db.addPath(currentNumSavedPaths, name, 2000, 100);
                Log.w("test", Boolean.toString(tmp));
                new SavePathTask().execute(currentNumSavedPaths);
                currentNumSavedPaths += 1;
                settings.edit().putInt(NPATHSAVED, currentNumSavedPaths).apply();
                map.clear();

            }

            else if(view == cancelpathbtn) {
                path.setVisibility(View.GONE);
                pathName.setText("");
                mapView.setVisibility(View.VISIBLE);
            }

        }
    };

    @SuppressLint("SetTextI18n")
    private void loadData() {

        if (settings.getInt(GOAL, -1) == R.id.radio_steps) { //main goal is steps
            subgoalL.setTextColor(Color.rgb(133, 52, 0));
            subgoalL.setText(R.string.cal);
            goalType = STEPS;
        } else {  //main goal is calories
            subgoalL.setTextColor(Color.rgb(0, 110, 230));
            subgoalL.setText(R.string.steps);
            goalType = CAL;
        }

        //default value set to 1 to avoid division by zero
        goalNum = Integer.parseInt(settings.getString(GOALD, "1"));

        height = Double.parseDouble(settings.getString(HEIGHT, "0"));
        weight = Double.parseDouble(settings.getString(WEIGHT, "0"));

        int id = settings.getInt(SEX, -1);
        if (id == R.id.radio_male) sex = true;
        else if (id == R.id.radio_female) sex = false;


    }

    @SuppressLint("SetTextI18n")
    private void updateGoalData(int maingoal, int secgoal) {
        goalc.setText(Integer.toString(maingoal));
        circleBar.setProgress(100 * maingoal / goalNum);
        subgoalD.setText(Integer.toString(secgoal));
    }

    private void loadGoalData(int newSteps) {
        double distance = newSteps * height;
        if (sex) distance *= STEPMALE;
        else distance *= STEPFEMALE;
        int newCal = (int) (distance * weight / 2);
        settings.edit().putInt(STEPS, newSteps).putInt(CAL, newCal).apply();

        if (goalType.equals(STEPS)) updateGoalData(newSteps, newCal);
        else updateGoalData(newCal, newSteps);

    }

    private void saveGoalData() {
        SharedPreferences.Editor e = settings.edit();
        e.putInt(goalType, Integer.parseInt(goalc.getText().toString()));

        String tmp;
        if (goalType.equals(STEPS)) tmp = CAL;
        else tmp = STEPS;
        e.putInt(tmp, Integer.parseInt(subgoalD.getText().toString())).apply();

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMyLocationEnabled(true);
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                loadPaths(loadMode);
            }
        });
    }

    private void loadPaths(boolean choice) {
        if(paths == null) return;
        int dim = paths.size();
        if (dim < 1) return;

        int color = (choice) ? Color.RED : Color.BLUE;
        for(int i = 0; i < dim; i++) {
            drawPolyLineOnMap(paths.get(i), map, color);
        }

        if(choice) {

            ArrayList<LatLng> last = paths.get(dim-1);
            if(!last.isEmpty()) map.addMarker(new MarkerOptions().position(last.get(last.size()-1)).title(getString(R.string.end)));

            ArrayList<LatLng> first = paths.get(0);
            if(!first.isEmpty()) map.addMarker(new MarkerOptions().position(first.get(0)).title(getString(R.string.start)));

            clearList();

        }

        loadMode = false;
    }

    private final DialogInterface.OnClickListener dialogHandler = (dialogInterface, i) -> {
        if(i == Dialog.BUTTON_POSITIVE) startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    };

    private boolean checkLocation() {
        LocationManager lm = (LocationManager)
                activity.getSystemService(Context.LOCATION_SERVICE) ;
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ;
        }
        catch (Exception ignored) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ;
        }
        catch (Exception ignored) {}

        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(activity)
                    .setMessage(getString(R.string.abilGPS))
                    .setPositiveButton( getString(R.string.settings), dialogHandler)
                    .setNegativeButton( getString(R.string.abort), null)
                    .show();
            return false;
        }

        return true;
    }

}
