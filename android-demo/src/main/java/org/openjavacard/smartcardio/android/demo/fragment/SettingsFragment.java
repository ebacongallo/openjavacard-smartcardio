package org.openjavacard.smartcardio.android.demo.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import org.openjavacard.smartcardio.android.demo.R;

public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = SettingsFragment.class.getName();

    @Override
    public void onCreatePreferences(Bundle savedState, String rootKey) {
        Log.d(TAG, "onCreatePreferences()");
        setPreferencesFromResource(R.xml.settings, rootKey);
        /*
        PreferenceManager pm = getPreferenceManager();
        SharedPreferences prefs = pm.getSharedPreferences();
        Map<String, ?> map = prefs.getAll();
        if(map != null) {
            for (String key : map.keySet()) {
                Preference pref = pm.findPreference(key);
                bindPreferenceSummaryToValue(pref);
            }
        }
        */
    }

    /*
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(" + preference.getKey() + ")");
        String stringValue = newValue.toString();
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            preference.setSummary(
                    index >= 0 ? listPreference.getEntries()[index] : "");
        } else {
            preference.setSummary(stringValue);
        }
        return true;
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        Log.v(TAG, "bindPreferenceSummaryToValue(" + preference.getKey() + ")");
        if(preference instanceof ListPreference) {
            preference.setOnPreferenceChangeListener(this);
            onPreferenceChange(preference, ((ListPreference) preference).getValue());
        }
    }
    */

}
