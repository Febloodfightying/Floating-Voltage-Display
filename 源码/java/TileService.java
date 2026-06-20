package com.batteryv.windows;

import android.content.Intent;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

public class TileService extends TileService {

    private SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences("FloatWindowPrefs", MODE_PRIVATE);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        
        if (isTileActive()) {
            stopService(new Intent(this, FloatService.class));
            setServiceRunning(false);
            setTileInactive();
        } else {
            if (checkOverlayPermission()) {
                startService(new Intent(this, FloatService.class));
                setServiceRunning(true);
                setTileActive();
            } else {
                Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityAndCollapse(intent);
            }
        }
        
        updateTileState();
    }

    private void updateTileState() {
        boolean isServiceRunning = isServiceRunning();
        Tile tile = getQsTile();
        
        if (tile != null) {
            if (isServiceRunning) {
                setTileActive();
            } else {
                setTileInactive();
            }
            tile.updateTile();
        }
    }

    private void setTileActive() {
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel("关闭浮窗");
        }
    }

    private void setTileInactive() {
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel("电压浮窗");
        }
    }

    private boolean isTileActive() {
        Tile tile = getQsTile();
        return tile != null && tile.getState() == Tile.STATE_ACTIVE;
    }

    private boolean isServiceRunning() {
        return preferences.getBoolean("service_running", false);
    }

    private void setServiceRunning(boolean running) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("service_running", running);
        editor.apply();
    }

    private boolean checkOverlayPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return android.provider.Settings.canDrawOverlays(this);
        }
        return true;
    }
}