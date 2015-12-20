package floschlo.screencast.fragment;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import floschlo.screencast.R;

/**
 * Created by Florian on 19.12.2015.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    public final static String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle pBundle, String s) {

        addPreferencesFromResource(R.xml.settings);

    }
}
