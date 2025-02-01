package net.lgiki.floatingstatusbar.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import net.lgiki.floatingstatusbar.services.FloatingStatusBarService;

public class TransparentActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FloatingStatusBarService.start(this);
        finish();
    }
}