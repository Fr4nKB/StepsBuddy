package com.frank.stepsbuddy;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static com.frank.stepsbuddy.MyUtil.*;

public class MainService extends Service implements SensorEventListener {

    private Notification n;
    private SensorManager sm;
    private Sensor s;
    private int oldSteps = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        s = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if(s == null) Toast.makeText(getApplicationContext(), "No sensor", Toast.LENGTH_SHORT).show();
        else {
            sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
        }

        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

        n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentIntent(pi).setContentTitle(APP_NAME).setSmallIcon(R.drawable.ic_home).build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sm.unregisterListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(SERVICE_NOTIF_ID, n);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if(oldSteps == 0) {
                //first time capturing data
               oldSteps = (int) sensorEvent.values[0];
            }

            Intent i = new Intent(GOALUPDATE);
            i.putExtra(STEPS, (int) sensorEvent.values[0] - oldSteps); //send new steps amount
            oldSteps = (int) sensorEvent.values[0];     //save to substract next time
            sendBroadcast(i);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { ; }
}
