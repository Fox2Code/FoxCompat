package com.fox2code.foxcompat.example;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.fox2code.foxcompat.FoxActivity;
import com.fox2code.foxcompat.FoxLineage;

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
            preference.setSummary("Rom: " + foxLineage.getRomType().name);
            preference.setOnPreferenceClickListener(preference1 -> {
                super.startActivity(new Intent(foxLineage.getDataUsageIntent()));
                return true;
            });
        }
    }
}