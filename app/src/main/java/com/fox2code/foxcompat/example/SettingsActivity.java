package com.fox2code.foxcompat.example;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.fox2code.foxcompat.app.FoxActivity;
import com.fox2code.foxcompat.os.FoxLineage;
import com.fox2code.foxcompat.os.FoxNavigationMode;

import java.util.Objects;

public class SettingsActivity extends FoxActivity implements FoxActivity.Embeddable {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        this.setDisplayHomeAsUpEnabled(true);
        if (this.getSharedPreferences("example", MODE_PRIVATE)
                .getBoolean("allow_back", false)) {
            this.enableBackButton();
        } else {
            this.disableBackButton();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName("example");
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            FoxLineage foxLineage = FoxLineage.getFoxLineage(this.requireContext());
            Preference preference = Objects.requireNonNull(findPreference("data"));
            if (foxLineage.getDataUsageIntent() == null)
                preference.setEnabled(false);
            FoxNavigationMode foxNavigationMode = FoxNavigationMode.queryForActivity(
                    FoxActivity.getFoxActivity(this));
            preference.setSummary("Rom: " + foxLineage.getRomType().name +
                    " (" + foxNavigationMode + ")");
            preference.setOnPreferenceClickListener(preference1 -> {
                super.startActivity(new Intent(foxLineage.getDataUsageIntent()));
                return true;
            });
            findPreference("transparent").setOnPreferenceChangeListener((preference12, newValue) -> {
                FoxActivity foxActivity = FoxActivity.getFoxActivity(SettingsFragment.this);
                foxActivity.postOnUiThread(foxActivity::refreshUI);
                return true;
            });
            findPreference("allow_back").setOnPreferenceChangeListener((preference1, newValue) -> {
                if (newValue == Boolean.TRUE) {
                    FoxActivity.getFoxActivity(this).enableBackButton();
                } else {
                    FoxActivity.getFoxActivity(this).disableBackButton();
                }
                return true;
            });
        }
    }
}