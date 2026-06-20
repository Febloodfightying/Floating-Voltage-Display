package com.batteryv.windows;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private Switch switchLock, switchTheme;
    private SharedPreferences preferences;
    private LinearLayout mainLayout;
    private TextView tvLockLabel, tvThemeLabel;
    private Button btnStart, btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // 初始化SharedPreferences
        preferences = getSharedPreferences("FloatWindowPrefs", MODE_PRIVATE);

        // 初始化视图
        mainLayout = findViewById(R.id.mainLayout);
        tvLockLabel = findViewById(R.id.tvLockLabel);
        tvThemeLabel = findViewById(R.id.tvThemeLabel);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        switchLock = findViewById(R.id.switchLock);
        switchTheme = findViewById(R.id.switchTheme);

        // 读取保存的主题状态
        boolean isDarkTheme = preferences.getBoolean("dark_theme", true);
        switchTheme.setChecked(isDarkTheme);
        applyTheme(isDarkTheme);

        // 读取保存的锁定状态
        boolean isLocked = preferences.getBoolean("lock_position", false);
        switchLock.setChecked(isLocked);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndStartService();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this, FloatService.class));
                // 更新服务状态
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("service_running", false);
                editor.apply();
                Toast.makeText(MainActivity.this, "悬浮窗已关闭", Toast.LENGTH_SHORT).show();
            }
        });

        // 主题切换开关状态变化监听（只保存状态和更新UI，不改变文本）
        switchTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = switchTheme.isChecked();
                applyTheme(isChecked);

                // 保存主题状态
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("dark_theme", isChecked);
                editor.apply();
            }
        });

        // 锁定位置开关状态变化监听
        switchLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = switchLock.isChecked();
                // 保存锁定状态
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("lock_position", isChecked);
                editor.apply();

                // 发送广播通知服务更新锁定状态
                Intent lockIntent = new Intent("LOCK_POSITION_CHANGED");
                lockIntent.putExtra("locked", isChecked);
                sendBroadcast(lockIntent);
            }
        });
    }

    // 应用主题：只改变背景和文字颜色，标签文本固定（已在布局中设置）
    private void applyTheme(boolean isDark) {
        if (isDark) {
            // 深色主题
            mainLayout.setBackgroundColor(Color.parseColor("#FF121212"));
            tvLockLabel.setTextColor(Color.WHITE);
            tvThemeLabel.setTextColor(Color.WHITE);
        } else {
            // 浅色主题
            mainLayout.setBackgroundColor(Color.WHITE);
            tvLockLabel.setTextColor(Color.BLACK);
            tvThemeLabel.setTextColor(Color.BLACK);
        }
    }

    private void checkAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // 启动服务
        Intent serviceIntent = new Intent(this, FloatService.class);
        startService(serviceIntent);
        // 保存服务状态
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("service_running", true);
        editor.apply();
        Toast.makeText(this, "悬浮窗已启动", Toast.LENGTH_SHORT).show();
    }
}