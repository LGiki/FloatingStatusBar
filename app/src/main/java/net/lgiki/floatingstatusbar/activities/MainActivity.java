package net.lgiki.floatingstatusbar.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import net.lgiki.floatingstatusbar.BuildConfig;
import net.lgiki.floatingstatusbar.Constants;
import net.lgiki.floatingstatusbar.R;
import net.lgiki.floatingstatusbar.services.FloatingStatusBarService;
import net.lgiki.floatingstatusbar.utils.ServiceUtils;

public class MainActivity extends AppCompatActivity {
    private Menu optionsMenu;

    private SettingsFragment settingsFragment;

    private BroadcastReceiver floatingStatusBarStatusReceiver;

    private final ActivityResultLauncher<Intent> requestSystemWindowAlertPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    activityResult -> {
                        if (!Settings.canDrawOverlays(this)) {
                            Toast.makeText(this, R.string.toast_permission_not_granted, Toast.LENGTH_SHORT).show();
                            exitApp();
                        }
                    });

    private void startFloatingStatusBarService() {
        FloatingStatusBarService.start(this);
        updateServiceToggleMenuIcon();
    }

    private void stopFloatingStatusBarService() {
        FloatingStatusBarService.stop(this);
        updateServiceToggleMenuIcon();
    }

    private void restartFloatingStatusBarService() {
        FloatingStatusBarService.restart(this);
        updateServiceToggleMenuIcon();
    }

    private boolean isFloatingStatusBarServiceRunning() {
        return ServiceUtils.isServiceRunning(this, FloatingStatusBarService.class);
    }

    private void updateServiceToggleMenuIcon() {
        if (this.optionsMenu != null) {
            MenuItem toggleItem = optionsMenu.findItem(R.id.floating_status_bar_toggle);
            if (toggleItem != null) {
                toggleItem.setIcon(isFloatingStatusBarServiceRunning() ?
                        R.drawable.baseline_stop :
                        R.drawable.baseline_play_arrow);
            }
        }
    }

    private void resetPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        settingsFragment.reloadPreferences();
    }

    private void exitApp() {
        stopFloatingStatusBarService();
        finishAffinity();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.exit) {
            exitApp();
        } else if (id == R.id.about) {
            AlertDialog aboutDialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.about_dialog_title)
                    .setMessage(Html.fromHtml(getString(R.string.about_dialog_html, BuildConfig.VERSION_NAME)))
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            aboutDialog.show();
            TextView messageText = aboutDialog.findViewById(android.R.id.message);
            if (messageText != null) {
                messageText.setMovementMethod(LinkMovementMethod.getInstance());
            }
        } else if (id == R.id.floating_status_bar_toggle) {
            if (isFloatingStatusBarServiceRunning()) {
                stopFloatingStatusBarService();
            } else {
                startFloatingStatusBarService();
            }
        } else if (id == R.id.reset_settings) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.reset_settings_dialog_title)
                    .setMessage(R.string.reset_settings_dialog_content)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        resetPreferences();
                        if (isFloatingStatusBarServiceRunning()) {
                            restartFloatingStatusBarService();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar, menu);
        updateServiceToggleMenuIcon();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingsFragment = new SettingsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsFragment)
                .commit();

        if (!Settings.canDrawOverlays(this)) {
            final Intent requestPermissionIntent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            requestSystemWindowAlertPermissionLauncher.launch(requestPermissionIntent);
        }

        floatingStatusBarStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateServiceToggleMenuIcon();
                Log.d("TAG", "onReceive: "+"Run here");
            }
        };
        ContextCompat.registerReceiver(this, floatingStatusBarStatusReceiver, new IntentFilter(Constants.FloatingStatusBarStatusChanged), ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceToggleMenuIcon();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (floatingStatusBarStatusReceiver != null) {
            unregisterReceiver(floatingStatusBarStatusReceiver);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }

        @Override
        public void onPause() {
            SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
            if (sharedPreferences != null) {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
            }
            super.onPause();
        }

        @Override
        public void onResume() {
            SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
            if (sharedPreferences != null) {
                sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            }
            super.onResume();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                MainActivity mainActivity = (MainActivity) activity;
                if (mainActivity.isFloatingStatusBarServiceRunning()) {
                    mainActivity.restartFloatingStatusBarService();
                }
            }
        }

        private void reloadPreferences() {
            setPreferenceScreen(null);
            addPreferencesFromResource(R.xml.preferences);
        }
    }
}