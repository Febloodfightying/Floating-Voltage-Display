package com.batteryv.windows;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatService extends Service {

    private WindowManager windowManager;
    private View floatView;
    private WindowManager.LayoutParams params;
    private boolean isPositionLocked = false;
    private SharedPreferences preferences;
    private BroadcastReceiver lockReceiver;
    private BroadcastReceiver batteryReceiver;
    
    private NotificationManager notificationManager;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "battery_lock_screen";

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化SharedPreferences
        preferences = getSharedPreferences("FloatWindowPrefs", MODE_PRIVATE);
        isPositionLocked = preferences.getBoolean("lock_position", false);
        
        // 初始化通知管理器
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        
        createFloatingWindow();
        registerBatteryReceiver();
        setupLockReceiver();
        
        // 标记服务正在运行
        setServiceRunning(true);
        
        // 启动锁屏通知
        startLockScreenNotification();
    }

    private void setupLockReceiver() {
        // 注册锁定状态变化接收器
        lockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("LOCK_POSITION_CHANGED".equals(intent.getAction())) {
                    isPositionLocked = intent.getBooleanExtra("locked", false);
                }
            }
        };
        registerReceiver(lockReceiver, new IntentFilter("LOCK_POSITION_CHANGED"));
    }

    private void createFloatingWindow() {
        // 获取窗口管理器
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // 获取布局填充器
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        
        // 加载悬浮窗布局
        floatView = inflater.inflate(R.layout.float_window, null);

        // 设置窗口参数
        params = new WindowManager.LayoutParams();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        
        params.format = PixelFormat.TRANSLUCENT;
        
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                  WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                  WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
    
        params.gravity = Gravity.TOP | Gravity.START;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        
        // 加载保存的位置
        loadWindowPosition();

        // 添加悬浮窗到窗口
        windowManager.addView(floatView, params);

        // 设置拖拽功能
        setupDragListener();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "电池电压锁屏显示",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("在锁屏界面显示电池电压信息");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startLockScreenNotification() {
        // 创建在锁屏显示的通知
        Notification notification = createBatteryNotification("----mV --%");
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, notification);
    }

    private Notification createBatteryNotification(String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("电压浮窗")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setShowWhen(false)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
        } else {
            return new Notification.Builder(this)
                .setContentTitle("电压浮窗")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setShowWhen(false)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
        }
    }

    private void updateLockScreenNotification(int voltage, int percent) {
        String content = voltage + "mV " + percent + "%";
        Notification notification = createBatteryNotification(content);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void loadWindowPosition() {
        int savedX = preferences.getInt("window_x", 50);
        int savedY = preferences.getInt("window_y", 100);
        params.x = savedX;
        params.y = savedY;
    }

    private void saveWindowPosition(int x, int y) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("window_x", x);
        editor.putInt("window_y", y);
        editor.apply();
    }

    private void setServiceRunning(boolean running) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("service_running", running);
        editor.apply();
    }

    private void registerBatteryReceiver() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // 获取电池数据
                final int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                final int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                final int percent = (level * 100) / scale;

                // 更新悬浮窗（确保在主线程）
                if (floatView != null) {
                    floatView.post(new Runnable() {
                        @Override
                        public void run() {
                            TextView tvVoltage = floatView.findViewById(R.id.tvVoltage);
                            TextView tvPercentage = floatView.findViewById(R.id.tvPercentage);
                            
                            tvVoltage.setText(voltage + "mV");
                            tvPercentage.setText(" " + percent + "%");
                        }
                    });
                }

                // 更新锁屏通知
                updateLockScreenNotification(voltage, percent);
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    }

    private void setupDragListener() {
        floatView.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float startTouchX, startTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isPositionLocked) {
                    return false;
                }
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = params.x;
                        startY = params.y;
                        startTouchX = event.getRawX();
                        startTouchY = event.getRawY();
                        break;
                        
                    case MotionEvent.ACTION_MOVE:
                        params.x = startX + (int) (event.getRawX() - startTouchX);
                        params.y = startY + (int) (event.getRawY() - startTouchY);
                        windowManager.updateViewLayout(floatView, params);
                        break;
                        
                    case MotionEvent.ACTION_UP:
                        saveWindowPosition(params.x, params.y);
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 注销广播接收器
        if (batteryReceiver != null) {
            unregisterReceiver(batteryReceiver);
        }
        if (lockReceiver != null) {
            unregisterReceiver(lockReceiver);
        }
        
        if (windowManager != null && floatView != null) {
            windowManager.removeView(floatView);
        }
        notificationManager.cancel(NOTIFICATION_ID);
        stopForeground(true);
        
        setServiceRunning(false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}