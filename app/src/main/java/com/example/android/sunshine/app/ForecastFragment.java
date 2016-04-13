package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by ericm_000 on 19/03/2016.
 * Fragment containing a simple view
 */
public class ForecastFragment extends Fragment {

    private final static String LOG_TAG = ForecastFragment.class.getSimpleName();
    private ArrayAdapter<String> mForecastAdapter;
    private final String DEFAULT_COUNTRY = "fr";

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // In order to get the onCreateOptionsMenu called
        setHasOptionsMenu(true);
    }

    // Add the temporary refresh item   to the menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item click here
        // Home and up automatically handled if there is a parent activity defined
        // in AndroidManifest.xml
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Create an adapter that takes data from a source and use it to populate the listview
        ArrayList<String> weekForecast = new ArrayList<String>();
        mForecastAdapter = new ArrayAdapter<String>(getActivity()
                , R.layout.list_item_forecast
                , R.id.list_item_forecast_textview
                , weekForecast);

        // Set the adapter to the listview
        ListView listView_Forecast = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView_Forecast.setAdapter(mForecastAdapter);

        // Do something when we click on an item
        listView_Forecast.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                Toast t = Toast.makeText(getActivity()
//                                , mForecastAdapter.getItem(i)
//                                , Toast.LENGTH_SHORT);
//                t.show();

                // Replaced with intent to launch DetailActivity
                Intent detailIntent = new Intent(getActivity(),DetailActivity.class);
                detailIntent.putExtra(Intent.EXTRA_TEXT, mForecastAdapter.getItem(i));
                startActivity(detailIntent);
            }
        });

        return rootView;
    }

    private void updateWeather() {
        // use the shared preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = sp.getString(getString(R.string.pref_location_key)
                , getString(R.string.pref_location_default));
        location = location  + "," + DEFAULT_COUNTRY;

        new FetchWeatherTask().execute(location);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    // Returns the JSON string fetched from openweatherforecast website
    private class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        // To query the openweathermap api
        private final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
        private final String QUERY_PARAM = "q";
        private final String FORMAT_PARAM = "mode";
        private final String UNIT_PARAM = "units";
        private final String DAYS_PARAM = "cnt";


        // Do something with the weather data
        protected void onPostExecute(String[] weatherData) {
            mForecastAdapter.clear();
//            ArrayList<String> weekForecast = new ArrayList<String>();
//            weekForecast.addAll(Arrays.asList(weatherData));
            mForecastAdapter.addAll(Arrays.asList(weatherData));
        }

        protected String[] doInBackground(String... strings) {

            // check the argument
            if (strings.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                String format = "json";
                String units = "metric";
                int days = 7;

                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                //                String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?id=2990189&mode=json&units=metric&cnt=7";
                //                String apiKey = "&APPID=" + BuildConfig.OPEN_WEATHER_MAP_API_KEY;
                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, strings[0])
                        .appendQueryParameter(FORMAT_PARAM,format)
                        .appendQueryParameter(UNIT_PARAM,units)
                        .appendQueryParameter(DAYS_PARAM,Integer.toString(days))
                        .appendQueryParameter("APPID",BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .build();

                String urlString = builtUri.toString();
                URL url = new URL(urlString);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                String[] resultStrs;
                try {
                    resultStrs = getWeatherDataFromJson(buffer.toString(), days);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Could not parse JSON: " + e.getMessage());
                    return null;
                }
                return resultStrs;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
        }

        /**
         * Test data
         */
        private static final String testJsonStr = "{\"city\":{\"id\":6452041,\"name\":\"Villiers-sur-Marne\",\"coord\":{\"lon\":2.55,\"lat\":48.833328},\"country\":\"FR\",\"population\":0},\"cod\":\"200\",\"message\":0.0087,\"cnt\":7,\"list\":[{\"dt\":1458471600,\"temp\":{\"day\":279.48,\"min\":277.1,\"max\":279.48,\"night\":277.36,\"eve\":279.48,\"morn\":279.48},\"pressure\":1019.26,\"humidity\":71,\"weather\":[{\"id\":500,\"main\":\"Rain\",\"description\":\"light rain\",\"icon\":\"10d\"}],\"speed\":4.91,\"deg\":28,\"clouds\":20},{\"dt\":1458558000,\"temp\":{\"day\":280.78,\"min\":276.91,\"max\":282.45,\"night\":278.03,\"eve\":281.23,\"morn\":276.91},\"pressure\":1019.7,\"humidity\":89,\"weather\":[{\"id\":500,\"main\":\"Rain\",\"description\":\"light rain\",\"icon\":\"10d\"}],\"speed\":4.06,\"deg\":9,\"clouds\":56,\"rain\":0.34},{\"dt\":1458644400,\"temp\":{\"day\":282.04,\"min\":277.36,\"max\":283.01,\"night\":277.86,\"eve\":280.97,\"morn\":277.36},\"pressure\":1014.45,\"humidity\":85,\"weather\":[{\"id\":801,\"main\":\"Clouds\",\"description\":\"few clouds\",\"icon\":\"02d\"}],\"speed\":3.87,\"deg\":18,\"clouds\":12},{\"dt\":1458730800,\"temp\":{\"day\":281.68,\"min\":274.97,\"max\":283.09,\"night\":279.63,\"eve\":282.2,\"morn\":274.97},\"pressure\":1014.68,\"humidity\":90,\"weather\":[{\"id\":500,\"main\":\"Rain\",\"description\":\"light rain\",\"icon\":\"10d\"}],\"speed\":2.37,\"deg\":326,\"clouds\":64},{\"dt\":1458817200,\"temp\":{\"day\":282.24,\"min\":277.97,\"max\":283.07,\"night\":280.53,\"eve\":282.23,\"morn\":277.97},\"pressure\":1016.34,\"humidity\":84,\"weather\":[{\"id\":500,\"main\":\"Rain\",\"description\":\"light rain\",\"icon\":\"10d\"}],\"speed\":4.21,\"deg\":259,\"clouds\":88,\"rain\":0.98},{\"dt\":1458903600,\"temp\":{\"day\":284.14,\"min\":282.77,\"max\":285.31,\"night\":284.97,\"eve\":285.31,\"morn\":282.77},\"pressure\":1011.5,\"humidity\":0,\"weather\":[{\"id\":501,\"main\":\"Rain\",\"description\":\"moderate rain\",\"icon\":\"10d\"}],\"speed\":2.84,\"deg\":235,\"clouds\":100,\"rain\":10.67},{\"dt\":1458990000,\"temp\":{\"day\":287.5,\"min\":282.91,\"max\":287.5,\"night\":282.91,\"eve\":287.08,\"morn\":284.45},\"pressure\":1006.53,\"humidity\":0,\"weather\":[{\"id\":501,\"main\":\"Rain\",\"description\":\"moderate rain\",\"icon\":\"10d\"}],\"speed\":7.62,\"deg\":202,\"clouds\":40,\"rain\":11.61}]}";

        /* The date/time conversion code is going to be moved outside the asynctask later,
        * so for convenience we're breaking it out into its own method now.
        */
        public String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        public double ctof(double c) {
            return (1.8*c+32);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        public String formatHighLows(double high, double low) {
            // Check if the data should be displayed in metric or imperial
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unit = sp.getString(getString(R.string.pref_units_key)
                    , getString(R.string.pref_units_metric));

            if (unit.equals(getString(R.string.pref_units_imperial))) {
                high = ctof(high);
                low = ctof(low);
            }
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            return roundedHigh + "/" + roundedLow;
        }

        /**
         * Given a string of the form returned by the api call:
         * http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7
         * retrieve the maximum temperature for the day indicated by dayIndex
         * (Note: 0-indexed, so 0 would refer to the first day).
         */
        public double getMaxTemperatureForDay(String weatherJsonStr, int dayIndex) throws JSONException {

            final String DAYS_DATA_NAME = "list";
            final String DAY_TEMP_NAME = "temp";
            final String TEMP_MAX_NAME = "max";

            // Parse incoming data
            JSONObject root = new JSONObject(weatherJsonStr);

            // Retrieve the correct day
            JSONArray daysData = root.getJSONArray(DAYS_DATA_NAME);
            JSONObject day = daysData.getJSONObject(dayIndex);
            JSONObject dayTemp = day.getJSONObject(DAY_TEMP_NAME);
            return dayTemp.getDouble(TEMP_MAX_NAME);
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        public String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            return resultStrs;
        }
    }
}
