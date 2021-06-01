package ru.krlvm.forcedoffline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.preference.PreferenceManager;

public class ActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if(action == null) return;

        switch (action) {
            case OfflineVpnService.ACTION_STOP: {
                OfflineVpnService.disconnect(context);
                break;
            }
            case "android.intent.action.QUICKBOOT_POWERON":
            case Intent.ACTION_REBOOT:
            case Intent.ACTION_BOOT_COMPLETED: {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if(prefs.getBoolean("boot", false)
                        && prefs.getBoolean("running", false)) {
                    if(VpnService.prepare(context) == null) {
                        OfflineVpnService.connect(context);
                    }
                }
                break;
            }
        }
    }
}
