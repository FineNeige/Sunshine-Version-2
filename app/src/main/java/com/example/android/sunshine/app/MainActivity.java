package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        //TODO emorand is this really necessary ?
//        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ForecastFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private final String GEO_BASE_URL = "geo:0,0";
    private final String GEO_PARAM = "q";

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Start the setting activity
            startActivity(new Intent(this,SettingsActivity.class));
            return true;
        } else if (id == R.id.action_viewonmap) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            String preferredLocation = PreferenceManager.getDefaultSharedPreferences(this)
                                            .getString(getString(R.string.pref_location_key),getString(R.string.pref_location_default));
            Uri builtUri = Uri.parse(GEO_BASE_URL).buildUpon()
                    .appendQueryParameter(GEO_PARAM, preferredLocation)
                    .build();
            i.setData(builtUri);
            if (i.resolveActivity(getPackageManager()) != null) {
                startActivity(i);
            } else {
                Toast.makeText(this, "No map app found!", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

}
