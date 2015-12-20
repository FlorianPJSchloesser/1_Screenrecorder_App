package floschlo.screencast.activity;

import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import floschlo.screencast.R;
import floschlo.screencast.fragment.SettingsFragment;

/**
 * Created by Florian on 19.12.2015.
 */
public class SettingsActivity extends AppCompatActivity {

    public final static String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setSupportActionBar(((Toolbar) findViewById(R.id.toolbar)));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentManager lFragmentManager = getSupportFragmentManager();
        FragmentTransaction lFragmentTransaction = lFragmentManager.beginTransaction();
        lFragmentTransaction.replace(R.id.settings_frame, new SettingsFragment()).commit();
    }

    @Override
    public boolean onNavigateUp() {
        return super.onNavigateUp();
    }
}
