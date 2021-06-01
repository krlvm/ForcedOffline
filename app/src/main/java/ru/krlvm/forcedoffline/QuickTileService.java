package ru.krlvm.forcedoffline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickTileService extends TileService {

    private boolean mPendingAction = false;
    private BroadcastReceiver mBroadcastReceiver = null;

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateState();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateState();

        IntentFilter filter = new IntentFilter();
        filter.addAction(OfflineVpnService.ACTION_START);
        filter.addAction(OfflineVpnService.ACTION_STOP);
        registerReceiver(mBroadcastReceiver = new StatusReceiver(), filter);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        if(mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        if(mPendingAction) return;

        setState(Tile.STATE_UNAVAILABLE);
        mPendingAction = true;

        try {
            if(VpnService.prepare(this) != null) throw new NotReadyException();
            if (OfflineVpnService.isRunning) {
                OfflineVpnService.disconnect(this);
            } else {
                if(!OfflineVpnService.connect(this)) throw new NotReadyException();
            }
        } catch (Exception ex) {
            mPendingAction = false;
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            if(!(ex instanceof NotReadyException)) ex.printStackTrace();
        }
    }

    private void updateState() {
        updateState(OfflineVpnService.isRunning);
    }

    private void updateState(boolean isRunning) {
        setState(isRunning ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
    }

    private void setState(int state) {
        Tile tile = getQsTile();
        tile.setState(state);
        tile.updateTile();
    }

    class StatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() == null) return;
            updateState(intent.getAction().equals(OfflineVpnService.ACTION_START));
            mPendingAction = false;
        }
    }

    static class NotReadyException extends IllegalStateException {}
}