package ru.krlvm.forcedoffline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_VPN = 0;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateStatus();
        }
    };

    private ProgressBar mProgressBar;
    private RecyclerView mAppList;
    private TextView mWarningText;

    private AppAdapter mAppAdapter;

    private MenuItem mToggleItem;
    private MenuItem mShowCheckedItem;

    private AppInfo.FilterCallback mAppFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applyTheme(PreferenceManager.getDefaultSharedPreferences(this)
                .getString("theme", "-1"));

        mProgressBar = findViewById(R.id.progress_circular);
        mWarningText = findViewById(R.id.stop_to_configure);

        mAppList = findViewById(R.id.app_list);
        mAppList.setLayoutManager(new LinearLayoutManager(this));
        mAppList.addItemDecoration(new DividerItemDecoration(
                mAppList.getContext(),
                RecyclerView.VERTICAL
        ));
        resetAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateStatus();

        IntentFilter filter = new IntentFilter();
        filter.addAction(OfflineVpnService.ACTION_START);
        filter.addAction(OfflineVpnService.ACTION_STOP);
        registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mToggleItem = menu.findItem(R.id.action_toggle);
        mToggleItem.setOnMenuItemClickListener(item -> {
            if (OfflineVpnService.isRunning) {
                OfflineVpnService.disconnect(this);
            } else {
                Intent intent = VpnService.prepare(this);
                if (intent != null) {
                    startActivityForResult(intent, REQUEST_VPN);
                } else {
                    onActivityResult(REQUEST_VPN, RESULT_OK, null);
                }
            }
            return false;
        });

        mShowCheckedItem = menu.findItem(R.id.action_show_checked);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                final String _query = query.toLowerCase();
                setFilter(app -> app.label.toLowerCase().contains(_query)
                        || app.packageName.toLowerCase().contains(_query));
                return false;
            }
            @Override
            public boolean onQueryTextChange(String query) { return false; }
        });
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) { return true; }
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mShowCheckedItem.setChecked(false);
                setFilter(null);
                return true;
            }
        });

        updateStatus();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_toggle) {
            return true;
        } else if (id == R.id.action_show_checked) {
            item.setChecked(!item.isChecked());
            if (item.isChecked()) {
                setFilter(app -> app.checked);
            } else {
                setFilter(null);
            }
            return true;
        } else if (id == R.id.action_refresh) {
            resetAdapter();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(getString(R.string.about_text) + "\n\n(c) krlvm, 2021")
                    .setPositiveButton(R.string.visit_github, (dialog, which) -> {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/krlvm/ForcedOffline")));
                    }).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VPN) {
            if (resultCode == RESULT_OK) {
                OfflineVpnService.connect(this);
            }
        }
    }

    private void updateStatus() {
        if (mToggleItem == null) return;
        if (OfflineVpnService.isRunning) {
            mAppList.setVisibility(View.GONE);
            mWarningText.setVisibility(View.VISIBLE);

            mToggleItem.setTitle(R.string.action_stop);
            mToggleItem.setIcon(R.drawable.ic_stop);
        } else {
            mAppList.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.GONE);

            mToggleItem.setTitle(R.string.action_start);
            mToggleItem.setIcon(R.drawable.ic_start);
        }
    }

    private void setFilter(AppInfo.FilterCallback filter) {
        mAppFilter = filter;
        mAppAdapter.filtrate(mAppFilter);
    }

    private void resetAdapter() {
        mAppList.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            final List<AppInfo> apps = AppInfo.getInstalledApps(this);
            runOnUiThread(() -> {
                mAppAdapter = new AppAdapter(this, apps);
                mAppList.setAdapter(mAppAdapter);
                mAppAdapter.filtrate(mAppFilter);

                mProgressBar.setVisibility(View.GONE);
                mAppList.setVisibility(View.VISIBLE);
            });
        }).start();
    }

    static void applyTheme(String id) {
        AppCompatDelegate.setDefaultNightMode(Integer.parseInt(id));
    }
}