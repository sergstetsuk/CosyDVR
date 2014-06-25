package com.example.CosyDVR;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import com.example.CosyDVR.SDList;

public class CosyDVRPreferenceActivity extends PreferenceActivity
{
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            SDList sd = new SDList();
            ListPreference LP = (ListPreference) findPreference("sd_card_path");
            sd.determineStorageOptions();
            CharSequence[] entries = new CharSequence[sd.sVold.size()];
            for (int i = 0; i < sd.sVold.size(); i++) {
            	entries[i] = sd.sVold.get(i);
            }
            CharSequence[] entryValues = entries;
            LP.setEntries(entries);
            LP.setEntryValues(entryValues);
            

            
        }
    }
}
