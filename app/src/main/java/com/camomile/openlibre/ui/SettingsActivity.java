package com.camomile.openlibre.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.camomile.openlibre.model.GlucoseData;

import static com.camomile.openlibre.OpenLibre.GLUCOSE_TARGET_MAX;
import static com.camomile.openlibre.OpenLibre.GLUCOSE_TARGET_MIN;
import static com.camomile.openlibre.OpenLibre.GLUCOSE_UNIT_IS_MMOL;
import static com.camomile.openlibre.OpenLibre.refreshApplicationSettings;

public class SettingsActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
        // when username or server changes update the key for the sync progress
        if (key.equals("pref_glucose_unit_is_mmol")) {
            GLUCOSE_UNIT_IS_MMOL = settings.getBoolean("pref_glucose_unit_is_mmol", GLUCOSE_UNIT_IS_MMOL);
            SharedPreferences.Editor editor = settings.edit();
            if (GLUCOSE_UNIT_IS_MMOL) {
                editor.putString("pref_glucose_target_min", Float.toString(GlucoseData.convertGlucoseMGDLToMMOL(GLUCOSE_TARGET_MIN)));
                editor.putString("pref_glucose_target_max", Float.toString(GlucoseData.convertGlucoseMGDLToMMOL(GLUCOSE_TARGET_MAX)));
            } else {
                editor.putString("pref_glucose_target_min", Float.toString(GlucoseData.convertGlucoseMMOLToMGDL(GLUCOSE_TARGET_MIN)));
                editor.putString("pref_glucose_target_max", Float.toString(GlucoseData.convertGlucoseMMOLToMGDL(GLUCOSE_TARGET_MAX)));
            }
            editor.apply();
            refreshApplicationSettings(settings);
        }
        if (key.equals("pref_glucose_target_min") || key.equals("pref_glucose_target_max")) {
            refreshApplicationSettings(settings);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
