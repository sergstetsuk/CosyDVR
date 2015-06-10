package es.esy.CosyDVR;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import es.esy.CosyDVR.StorageUtils;

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
            ListPreference LP = (ListPreference) findPreference("sd_card_path");
            CharSequence[] entries = new CharSequence[StorageUtils.getStorageList().size()];
            CharSequence[] entryValues = new CharSequence[StorageUtils.getStorageList().size()];
            for (int i = 0; i < StorageUtils.getStorageList().size(); i++) {
            	entries[i] = StorageUtils.getStorageList().get(i).getDisplayName();
            	entryValues[i] = StorageUtils.getStorageList().get(i).path;
            }
            LP.setEntries(entries);
            LP.setEntryValues(entryValues);
            

            
        }
    }
}
