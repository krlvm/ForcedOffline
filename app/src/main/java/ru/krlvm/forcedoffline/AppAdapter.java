package ru.krlvm.forcedoffline;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder>{

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final List<AppInfo> mSource;
    private List<AppInfo> mList;

    AppAdapter(Context context, List<AppInfo> source) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mSource = source;

        filtrate(null);
    }

    public void filtrate(AppInfo.FilterCallback callback) {
        mList = new ArrayList<>();
        if(callback == null) {
            mList.addAll(mSource);
        } else {
            for (AppInfo app : mSource) {
                if (callback.filtrate(app)) {
                    mList.add(app);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public AppAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.app_item, parent, false));
    }

    @Override
    public void onBindViewHolder(AppAdapter.ViewHolder holder, int position) {
        AppInfo app = mList.get(position);
        holder.labelView.setText(app.label);
        holder.packageView.setText(app.packageName);
        holder.iconView.setImageDrawable(app.icon);
        holder.checkBoxView.setChecked(app.checked);

        holder.root.setOnClickListener(v -> {
            boolean checked = !holder.checkBoxView.isChecked();
            holder.checkBoxView.setChecked(checked);
            app.checked = checked;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            Set<String> apps = new HashSet<>(prefs.getStringSet(
                    OfflineVpnService.APPS_LIST_PREFERENCE, new HashSet<>()));
            if (apps.contains(app.packageName)) {
                apps.remove(app.packageName);
            } else {
                apps.add(app.packageName);
            }
            prefs.edit().putStringSet(OfflineVpnService.APPS_LIST_PREFERENCE, apps).apply();
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View root;
        final ImageView iconView;
        final TextView labelView;
        final TextView packageView;
        final CheckBox checkBoxView;

        ViewHolder(View view) {
            super(view);

            root = view;
            iconView = (ImageView) view.findViewById(R.id.icon);
            labelView = (TextView) view.findViewById(R.id.app_label);
            packageView = (TextView) view.findViewById(R.id.app_package);
            checkBoxView = (CheckBox) view.findViewById(R.id.checkBox);
        }
    }
}