package net.lgiki.floatingstatusbar.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.graphics.TypefaceCompat;
import androidx.preference.PreferenceManager;

import net.lgiki.floatingstatusbar.Constants;
import net.lgiki.floatingstatusbar.MainActivity;
import net.lgiki.floatingstatusbar.R;
import net.lgiki.floatingstatusbar.databinding.FloatingStatusBarBinding;
import net.lgiki.floatingstatusbar.utils.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FloatingStatusBarService extends Service {
    private static final String TAG = "FloatingStatusBarService";
    private WindowManager windowManager;
    private View floatingWindowView;
    private Handler handler;
    private FloatingStatusBarBinding binding;
    private PendingIntent startMainActivityPendingIntent;
    private boolean showSeconds;
    private boolean showBattery;
    private boolean showBatteryPercentageSign;
    private String windowPosition;
    private int windowMargin;
    private int windowBackgroundColor;
    private boolean openSettingsOnPress;
    private boolean closeWindowOnLongPress;
    private int fontSize;
    private int fontWeight;
    private String fontFamily;
    private int fontColor;
    private int refreshInterval;

    public static void start(Context context) {
        Intent intent = new Intent(context, FloatingStatusBarService.class);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, FloatingStatusBarService.class);
        context.stopService(intent);
    }

    public static void restart(Context context) {
        Intent intent = new Intent(context, FloatingStatusBarService.class);
        context.stopService(intent);
        context.startService(intent);
    }

    private void sendStatusBroadcast(boolean isRunning) {
        Intent intent = new Intent(Constants.FloatingStatusBarStatus);
        intent.putExtra("isRunning", isRunning);
        sendBroadcast(intent);
    }

    private void getPreferenceValues() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            refreshInterval = Integer.parseInt(prefs.getString(
                    getString(R.string.key_floating_status_bar_refresh_interval),
                    getString(R.string.default_value_floating_status_bar_refresh_interval)
            ));
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.toast_get_preference_values_failed, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "failed to parse refresh interval: ", e);
        }
        showSeconds = prefs.getBoolean(
                getString(R.string.key_floating_status_bar_show_seconds),
                getResources().getBoolean(R.bool.default_value_floating_status_bar_show_seconds)
        );
        showBattery = prefs.getBoolean(
                getString(R.string.key_floating_status_bar_show_battery),
                getResources().getBoolean(R.bool.default_value_floating_status_bar_show_battery)
        );
        showBatteryPercentageSign = prefs.getBoolean(
                getString(R.string.key_floating_status_bar_show_battery_percentage_sign),
                getResources().getBoolean(R.bool.default_value_floating_status_bar_show_battery_percentage_sign)
        );
        windowPosition = prefs.getString(
                getString(R.string.key_window_position),
                getString(R.string.default_value_window_position)
        );
        windowMargin = prefs.getInt(
                getString(R.string.key_window_margin),
                getResources().getInteger(R.integer.default_value_window_margin)
        );
        windowBackgroundColor = prefs.getInt(
                getString(R.string.key_window_background_color),
                getResources().getInteger(R.integer.default_value_window_background_color)
        );
        openSettingsOnPress = prefs.getBoolean(
                getString(R.string.key_window_open_settings_on_press),
                getResources().getBoolean(R.bool.default_value_window_open_settings_on_press)
        );
        closeWindowOnLongPress = prefs.getBoolean(
                getString(R.string.key_window_close_window_on_long_press),
                getResources().getBoolean(R.bool.default_value_window_close_window_on_long_press)
        );
        fontSize = prefs.getInt(
                getString(R.string.key_font_size),
                getResources().getInteger(R.integer.default_value_font_size)
        );
        try {
            fontWeight = Integer.parseInt(prefs.getString(
                    getString(R.string.key_font_weight),
                    getString(R.string.default_value_font_weight)
            ));
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.toast_get_preference_values_failed, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "failed to parse font weight: ", e);
        }
        fontFamily = prefs.getString(
                getString(R.string.key_font_family),
                getString(R.string.default_value_font_family)
        );
        fontColor = prefs.getInt(
                getString(R.string.key_font_color),
                getResources().getInteger(R.integer.default_value_font_color)
        );
    }

    private int getLayoutGravity(String windowPosition) {
        switch (windowPosition) {
            case "top_center":
                return Gravity.TOP | Gravity.CENTER;
            case "top_right":
                return Gravity.TOP | Gravity.END;
            case "bottom_center":
                return Gravity.BOTTOM | Gravity.CENTER;
            case "bottom_right":
                return Gravity.BOTTOM | Gravity.END;
            case "top_left":
                return Gravity.TOP | Gravity.START;
            case "bottom_left":
            default:
                return Gravity.BOTTOM | Gravity.START;
        }
    }

    private Typeface getTypeFace(String fontFamily) {
        switch (fontFamily) {
            case "cursive":
                return Typeface.create("cursive", Typeface.NORMAL);
            case "serif":
                return Typeface.SERIF;
            case "monospaced":
                return Typeface.MONOSPACE;
            case "sans_serif":
            default:
                return Typeface.SANS_SERIF;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sendStatusBroadcast(true);
        getPreferenceValues();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler = new Handler();

        Intent startMainActivityIntent = new Intent(this, MainActivity.class);
        startMainActivityPendingIntent = PendingIntent.getActivity(
                this,
                0,
                startMainActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        createFloatingWindow();
        startTimeUpdate(refreshInterval);
    }

    private void createFloatingWindow() {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        binding = FloatingStatusBarBinding.inflate(layoutInflater);

        floatingWindowView = binding.getRoot();
        floatingWindowView.setBackgroundColor(windowBackgroundColor);

        if (openSettingsOnPress) {
            floatingWindowView.setOnClickListener(new FloatingWindowOnClickListener());
        }

        if (closeWindowOnLongPress) {
            floatingWindowView.setOnLongClickListener(new FloatingWindowOnLongClickListener());
        }

        binding.batteryText.setTextColor(fontColor);
        binding.timeText.setTextColor(fontColor);

        binding.timeText.setTextSize(fontSize);
        binding.batteryText.setTextSize(fontSize);

        if (fontSize < 20) {
            int spaceBetweenTimeAndBatteryWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
            ViewGroup.LayoutParams spaceBetweenTimeAndBatteryLayoutParams = binding.spaceBetweenTimeAndBattery.getLayoutParams();
            spaceBetweenTimeAndBatteryLayoutParams.width = spaceBetweenTimeAndBatteryWidth;
            binding.spaceBetweenTimeAndBattery.setLayoutParams(spaceBetweenTimeAndBatteryLayoutParams);
        }

        Typeface typeface = getTypeFace(fontFamily);

        binding.timeText.setTypeface(TypefaceCompat.create(this, typeface, fontWeight, false));
        binding.batteryText.setTypeface(TypefaceCompat.create(this, typeface, fontWeight, false));

        if (showBattery) {
            binding.batteryText.setVisibility(View.VISIBLE);
            binding.spaceBetweenTimeAndBattery.setVisibility(View.VISIBLE);
        } else {
            binding.batteryText.setVisibility(View.GONE);
            binding.spaceBetweenTimeAndBattery.setVisibility(View.GONE);
        }

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        layoutParams.gravity = getLayoutGravity(windowPosition);
        layoutParams.x = windowMargin;
        layoutParams.y = windowMargin;

        windowManager.addView(floatingWindowView, layoutParams);
    }

    private void startTimeUpdate(int refreshInterval) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                updateStatusBar();
                handler.postDelayed(this, refreshInterval);
            }
        });
    }

    private void openSettings() {
        try {
            startMainActivityPendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            Toast.makeText(this, R.string.toast_open_settings_failed, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "openSettings: ", e);
        }
    }

    private void updateStatusBar() {
        updateTime(showSeconds);
        if (showBattery) {
            updateBattery(showBatteryPercentageSign);
        }
    }

    private void updateTime(boolean showSeconds) {
        boolean is24HourFormat = DateFormat.is24HourFormat(this);
        SimpleDateFormat sdf = new SimpleDateFormat(
                TimeUtils.getTimePattern(is24HourFormat, showSeconds),
                Locale.getDefault()
        );
        binding.timeText.setText(sdf.format(new Date()));
    }

    private void updateBattery(boolean showBatteryPercentageSign) {
        BatteryManager batteryManager = (BatteryManager) this.getSystemService(BATTERY_SERVICE);
        int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        binding.batteryText.setText(showBatteryPercentageSign ? String.format(Locale.getDefault(), "%d%%", batteryLevel) : Integer.toString(batteryLevel));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendStatusBroadcast(false);
        if (floatingWindowView != null) {
            windowManager.removeView(floatingWindowView);
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class FloatingWindowOnLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            stopSelf();
            return true;
        }
    }

    private class FloatingWindowOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            openSettings();
        }
    }
}