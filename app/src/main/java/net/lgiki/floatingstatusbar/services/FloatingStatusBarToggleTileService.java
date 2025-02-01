package net.lgiki.floatingstatusbar.services;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.service.quicksettings.PendingIntentActivityWrapper;
import androidx.core.service.quicksettings.TileServiceCompat;

import net.lgiki.floatingstatusbar.Constants;
import net.lgiki.floatingstatusbar.R;
import net.lgiki.floatingstatusbar.activities.TransparentActivity;
import net.lgiki.floatingstatusbar.utils.ServiceUtils;

@RequiresApi(api = Build.VERSION_CODES.N)
public class FloatingStatusBarToggleTileService extends TileService {
    private BroadcastReceiver floatingStatusBarStatusReceiver;

    private boolean isFloatingStatusBarServiceRunning() {
        return ServiceUtils.isServiceRunning(this, FloatingStatusBarService.class);
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile != null) {
            boolean isRunning = isFloatingStatusBarServiceRunning();

            if (isRunning) {
                tile.setState(Tile.STATE_ACTIVE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.setSubtitle(getString(R.string.tile_toggle_running));
                }
            } else {
                tile.setState(Tile.STATE_INACTIVE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.setSubtitle(getString(R.string.tile_toggle_stopped));
                }
            }

            tile.updateTile();
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingStatusBarStatusReceiver != null) {
            unregisterReceiver(floatingStatusBarStatusReceiver);
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        if (isFloatingStatusBarServiceRunning()) {
            FloatingStatusBarService.stop(this);
        } else {
            Intent activityIntent = new Intent(this, TransparentActivity.class);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            TileServiceCompat.startActivityAndCollapse(this, new PendingIntentActivityWrapper(
                    this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT, false
            ));
        }
        updateTile();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        floatingStatusBarStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateTile();
            }
        };
        ContextCompat.registerReceiver(this, floatingStatusBarStatusReceiver, new IntentFilter(Constants.FloatingStatusBarStatusChanged), ContextCompat.RECEIVER_EXPORTED);
    }
}
