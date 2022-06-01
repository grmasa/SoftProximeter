package com.android.softproximeter;

import static java.lang.Math.round;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class DetectService extends Service implements SensorEventListener {

    private DevicePolicyManager deviceManger;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer, light;
    private long lastUpdate = 0;
    private float[] g;
    private float light1;
    private boolean isSensorRunning = false;
    private int lastAction = -1, pocket = 0, inclination = -1;

    private static final int EVENT_UNLOCK = 2;
    private static final int EVENT_TURN_ON_SCREEN = 1;
    private static final int EVENT_TURN_OFF_SCREEN = 0;

    public DetectService() {
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                lastUpdate = curTime;

                g = new float[3];
                g = event.values.clone();

                double norm_Of_g = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);

                g[0] = (float) (g[0] / norm_Of_g);
                g[1] = (float) (g[1] / norm_Of_g);
                g[2] = (float) (g[2] / norm_Of_g);

                inclination = (int) round(Math.toDegrees(Math.acos(g[2])));
                Log.d("", "XYZ: " + round(g[0]) + ",  " + round(g[1]) + ",  " + round(g[2]) + "  inc: " + inclination + " LIGHT: " + light1);
            }
        }
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            light1 = event.values[0];
        }
        if ((light1 != -1) && (inclination != -1)) {
            detect(light1, g, inclination);
        }
    }

    public void detect(float light, float[] g, int inc) {
        if ((light < 2) && (g[1] < -0.6) && (inc > 75) || (inc < 100)) {
            pocket = 1;
            KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (!myKM.isKeyguardLocked()) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(VibrationEffect.createOneShot(2500, VibrationEffect.DEFAULT_AMPLITUDE));
                deviceManger.lockNow();
                Log.d("", "Locked");
            }
        }
        if ((light >= 2) && (g[1] >= -0.7)) {
            if (pocket == 1) {
                pocket = 0;
            }
            //OUT OF POCKET
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel channel1 = new NotificationChannel(
                "666",
                "Channel 1",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel1.setDescription("This is channel 1");
        NotificationManager manager = this.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel1);

        Notification notification = new NotificationCompat.Builder(this, "666")
                .setContentTitle("SoftProximeter")
                .setContentText("SoftProximeter Service")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();

        startForeground(666, notification);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MyService", "onStartCommand");
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenStateFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mScreenStateReceiver, screenStateFilter);
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        deviceManger = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        light = senSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (light == null) {
            Toast.makeText(this, "No light sensor found in device.", Toast.LENGTH_SHORT).show();
        } else {
            senSensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (senAccelerometer == null) {
            Toast.makeText(this, "No accelerometer sensor found in device.", Toast.LENGTH_SHORT).show();
        } else {
            senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void disableSensor() {
        if (!isSensorRunning) return;
        Log.d("TAG", "Disable proximity sensor");
        senSensorManager.unregisterListener(this, senAccelerometer);
        senSensorManager.unregisterListener(this, light);
        isSensorRunning = false;
    }

    private void enableSensor() {
        Log.d("TAG", "Enable proximity sensor");
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        isSensorRunning = true;
    }

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                if (lastAction != EVENT_UNLOCK) enableSensor();
                lastAction = EVENT_TURN_ON_SCREEN;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                disableSensor();

                lastAction = EVENT_TURN_OFF_SCREEN;
            }
        }
    };
}
