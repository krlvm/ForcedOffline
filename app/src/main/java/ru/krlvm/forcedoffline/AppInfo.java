package ru.krlvm.forcedoffline;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppInfo {
    final String label;
    final String packageName;
    final Drawable icon;
    boolean checked;

    AppInfo(String label, String packageName, Drawable icon) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
    }

    static List<AppInfo> getInstalledApps(Context context) {
        final Set<String> checked = PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet(OfflineVpnService.APPS_LIST_PREFERENCE, new HashSet<>());

        final PackageManager packageManager = context.getPackageManager();

        List<AppInfo> apps = new ArrayList<>();
        List<PackageInfo> packages = packageManager.getInstalledPackages(0);
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(context.getPackageName())) continue;
            final AppInfo appInfo = new AppInfo(
                    packageInfo.applicationInfo.loadLabel(packageManager).toString(),
                    packageInfo.packageName,
                    packageInfo.applicationInfo.loadIcon(packageManager)
            );
            appInfo.checked = checked.contains(appInfo.packageName);
            apps.add(appInfo);
        }

        Collections.sort(apps, (a, b) -> a.label.compareTo(b.label));

        return apps;
    }

    interface FilterCallback {
        boolean filtrate(AppInfo app);
    }
}