package es.esy.CosyDVR;

import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.Preference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import es.esy.CosyDVR.StorageUtils;
import java.util.Map;
import java.util.Map.Entry;

public class CosyDVRPreferenceActivity extends PreferenceActivity
{
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
        implements OnSharedPreferenceChangeListener
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            ListPreference LP = (ListPreference) findPreference("sd_card_path");
            CharSequence[] entries = new CharSequence[StorageUtils.getStorageList().size()];
            CharSequence[] entryValues = new CharSequence[StorageUtils.getStorageList().size()];
            for (int i = 0; i < StorageUtils.getStorageList().size(); i++) {
            	entries[i] = StorageUtils.getStorageList().get(i).getDisplayName();
            	entryValues[i] = StorageUtils.getStorageList().get(i).path;
            }
            LP.setEntries(entries);
            LP.setEntryValues(entryValues);
            SharedPreferences sharedPref = getPreferenceManager().getSharedPreferences();
            sharedPref.registerOnSharedPreferenceChangeListener(this);
            Map<String,?> keys = sharedPref.getAll();
            for(Map.Entry<String,?> entry : keys.entrySet()){
                onSharedPreferenceChanged(sharedPref,entry.getKey());
            } 
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
            Preference pref = findPreference(key);
            if (pref instanceof EditTextPreference 
                || pref instanceof ListPreference ) {
                String prefixStr = sharedPreferences.getString(key, "");
                pref.setSummary(prefixStr);
            }
        }
    }
}
