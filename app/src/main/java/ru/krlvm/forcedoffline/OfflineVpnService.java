package ru.krlvm.forcedoffline;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.HashSet;

public class OfflineVpnService extends VpnService {

    static boolean isRunning = false;

    private static final int FOREGROUND_ID   = 1;
    private static final String CHANNEL_ID = "ForcedOffline";
    static final String APPS_LIST_PREFERENCE = "apps";
    static final String ACTION_START = "ru.krlvm.forcedoffline.action.START";
    static final String ACTION_STOP  = "ru.krlvm.forcedoffline.action.STOP";

    private ParcelFileDescriptor mVpn;

    @Override
    public void onCreate() {
        super.onCreate();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            if(notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stop();
        } else {
            start();

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_text))
                    .addAction(R.drawable.ic_stop, getString(R.string.action_stop),
                            PendingIntent.getBroadcast(this, 0,
                                   new Intent(this, ActionReceiver.class)
                                           .setAction(ACTION_STOP), 0))
                    .setContentIntent(PendingIntent.getActivity(this, 0,
                            new Intent(this, MainActivity.class), 0))
                    .setShowWhen(false)
                    .build();

            startForeground(FOREGROUND_ID, notification);
        }
        return START_STICKY;
    }

    @Override
    public void onRevoke() {
        stop();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    private void start() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Builder builder = new Builder();
        builder.setSession(getString(R.string.app_name));
        builder.setConfigureIntent(PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0));

        /* ----- */
        builder.addAddress("10.1.10.1", 32);
        builder.addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128);

        builder.addRoute("0.0.0.0", 0);
        builder.addRoute("0:0:0:0:0:0:0:0", 0);
        /* ----- */

        for (String pkg : prefs.getStringSet(APPS_LIST_PREFERENCE, new HashSet<>())) {
            try {
                builder.addAllowedApplication(pkg);
            } catch (PackageManager.NameNotFoundException ignore) {
                // this package was deleted by user
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            builder.setMetered(false);
        }

        try {
            mVpn = builder.establish();
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(this, R.string.failed_to_initialize_vpn, Toast.LENGTH_LONG).show();
            stop();
            return;
        }

        prefs.edit().putBoolean("running", true).apply();
        isRunning = true;
        sendBroadcast(new Intent(ACTION_START));
    }

    private void stop() {
        stopForeground(true);
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean("running", false).apply();

        if(mVpn == null) return;
        try {
            mVpn.close();
            mVpn = null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        isRunning = false;
        sendBroadcast(new Intent(ACTION_STOP));
    }


    /* ----- */

    static boolean connect(Context context) {
        if(PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet(APPS_LIST_PREFERENCE, new HashSet<>()).size() == 0) {
            Toast.makeText(context, R.string.no_app_selected_to_run, Toast.LENGTH_LONG).show();
            return false;
        }
        context.startService(getServiceIntent(context).setAction(ACTION_START));
        return true;
    }

    static void disconnect(Context context) {
        context.startService(getServiceIntent(context).setAction(ACTION_STOP));
    }

    private static Intent getServiceIntent(Context context) {
        return new Intent(context, OfflineVpnService.class);
    }
}
