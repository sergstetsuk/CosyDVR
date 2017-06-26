package es.esy.CosyDVR;

import android.os.Bundle;
import android.content.Context;
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

    public class MyPreferenceFragment extends PreferenceFragment
        implements OnSharedPreferenceChangeListener
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            ListPreference LP = (ListPreference) findPreference("sd_card_path");
            Context context = getActivity();
            StorageUtils stutils = new StorageUtils();
            stutils.getStorageList(context);
            CharSequence[] entries = new CharSequence[stutils.getStorageList(context).size()];
            CharSequence[] entryValues = new CharSequence[stutils.getStorageList(context).size()];
            for (int i = 0; i < stutils.getStorageList(context).size(); i++) {
            	entries[i] = stutils.getStorageList(context).get(i).getDisplayName();
            	entryValues[i] = stutils.getStorageList(context).get(i).path;
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
