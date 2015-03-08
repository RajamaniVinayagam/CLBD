package com.dreamerindia.clbd;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

public class TrainSchedule extends FragmentActivity {

    private SupportMapFragment supportMapFragment;
    private int width, height;
    private static final float DEFAULT_ZOOM = 3;
    private static final float UPDATE_ZOOM = 10;
    ArrayList<LatLng> markerPoints;
    Marker marker;

    GoogleMap googleMap;
    TextView trainStart, trainLocation, expectTime, ll, departTime, arrivalTime, traintemp, tempArrivalTime, tempExpectTime;
    EditText myLocation, destination;
    Handler handler = null;
    Runnable runnable = null;
    LocationAddress locationAddress;
    LocationLocality locationLocality;
    String myLocality;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isGooglePlayServicesAvailable()) {
            showSettingsAlert();
        }
        setContentView(R.layout.activity_main);
        getScreenDimensions();
        locationAddress = new LocationAddress();
        locationLocality = new LocationLocality();
        trainStart = (TextView) findViewById(R.id.trainStart);
        destination = (EditText) findViewById(R.id.destination);
        trainLocation = (TextView) findViewById(R.id.trainLocation);
        expectTime = (TextView) findViewById(R.id.expectTime);
        ll = (TextView) findViewById(R.id.ll);
        arrivalTime = (TextView) findViewById(R.id.arrivalTime);
        departTime = (TextView) findViewById(R.id.departTime);
        traintemp = (TextView) findViewById(R.id.traintemp);
        tempExpectTime = (TextView) findViewById(R.id.tempExpectTime);
        tempArrivalTime = (TextView) findViewById(R.id.tempArrivalTime);
        myLocation = (EditText) findViewById(R.id.myLocation);
        this.markerPoints = new ArrayList<LatLng>();
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.googleMap);
        googleMap = supportMapFragment.getMap();

        LatLng ll = new LatLng(21.0000, 78.0000);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, DEFAULT_ZOOM);
        googleMap.moveCamera(update);


        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng ll) {
                Geocoder gc = new Geocoder(TrainSchedule.this);
                List<Address> list;
                try {
                    list = gc.getFromLocation(ll.latitude, ll.longitude, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Address add = list.get(0);
                myLocality = add.getLocality();
                myLocation.setText(myLocality);
                double toLat = add.getLatitude();
                double toLng = add.getLongitude();
                setMyLocationMarker(myLocality, toLat, toLng);
            }
        });

        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                update();
                handler.postDelayed(runnable, 15000);
            }
        };

        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, 15000);

    }

    public void navigate(View v) {
        hideSoftKeyboard(v);
        String dLocation = destination.getText().toString();
        String location = myLocation.getText().toString();

        if (dLocation.length() == 0 && location.length() == 0) {
            Toast.makeText(getApplicationContext(), "Enter the Boarding and Destination point", Toast.LENGTH_SHORT).show();
        } else {
            // Checks, whether start and end locations are captured
            this.googleMap.clear();
            String temp = ll.getText().toString();
            StringTokenizer tokens = new StringTokenizer(temp, ",");
            String first = tokens.nextToken();
            String second = tokens.nextToken();
            double fromLat = Double.parseDouble(first);
            double fromLng = Double.parseDouble(second);
            LatLng origin = new LatLng(fromLat, fromLng);
            LatLng dest = null;
            LatLng myl = null;
            try {
                Geocoder gc = new Geocoder(this);
                List<Address> list = gc.getFromLocationName(location, 1);
                Address add = list.get(0);
                String myLoc = add.getLocality();
                double toLat = add.getLatitude();
                double toLng = add.getLongitude();
                myl = new LatLng(toLat, toLng);
                setMyLocationMarker(myLoc, toLat, toLng);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("Error", "check destination Marker");
            }
            try {
                Geocoder gc = new Geocoder(this);
                List<Address> list = gc.getFromLocationName(dLocation, 1);
                Address add = list.get(0);
                myLocality = add.getLocality();
                double toLat = add.getLatitude();
                double toLng = add.getLongitude();
                dest = new LatLng(toLat, toLng);
                setDestinationMarker(myLocality, toLat, toLng);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("Error", "check destination Marker");
            }
            String urlTwo = TrainSchedule.this.getDirectionsUrl(myl, dest);

            DownloadTask downloadTaskTwo = new DownloadTask();
            downloadTaskTwo.execute(urlTwo);
            // Getting URL to the Google Directions API
            String url = TrainSchedule.this.getDirectionsUrl(origin, myl);

            DownloadTask downloadTask = new DownloadTask();

            // Start downloading json data from Google Directions API
            downloadTask.execute(url);
            String urlThree = TrainSchedule.this.getDirectionsUrlTwo(origin, dest);
            DownloadTaskTWO downloadTaskThree = new DownloadTaskTWO();
            downloadTaskThree.execute(urlThree);
            time();
        }
    }

    private void update() {
        new Thread(new Runnable() {
            public void run() {
                {

                    new GetLocationFromUrl().execute("http://www.embeddedcollege.org/rewebservices/location.txt");
                   // trace();
//                    String ti = tempExpectTime.getText().toString();
//                    if (ti.length() != 0) {
//                        try {
//                            int mi = Integer.parseInt(ti);
//                            if (mi < 600 && mi > 470) {
//                                Toast.makeText(getApplicationContext(), "Train Number : 12345 will reach the station in another 10 minutes", Toast.LENGTH_SHORT).show();
//                            } else if (mi < 119 && mi > 114) {
//                                Toast.makeText(getApplicationContext(), "Train Number : 12345 will reach the station in another few minutes", Toast.LENGTH_SHORT).show();
//                            } else if (mi < 5 && mi > -1) {
//                                Toast.makeText(getApplicationContext(), "You have boarded in Train Number : 12345... \n Enjoy your journey", Toast.LENGTH_SHORT).show();
//                            }
//                        } catch (NumberFormatException nfe) {
//                            System.out.println("Could not parse " + nfe);
//                        }
//                    }
                }
            }
        }).start();

    }
    public void trace() {
        String et = arrivalTime.getText().toString();
        String location = myLocation.getText().toString();

        if (et.length() == 0) {

        } else {
            String temp = ll.getText().toString();
            StringTokenizer tokens = new StringTokenizer(temp, ",");
            String first = tokens.nextToken();
            String second = tokens.nextToken();
            double fromLat = Double.parseDouble(first);
            double fromLng = Double.parseDouble(second);
            LatLng origin = new LatLng(fromLat, fromLng);
            LatLng myl = null;
            try {
                Geocoder gc = new Geocoder(this);
                List<Address> list = gc.getFromLocationName(location, 1);
                Address add = list.get(0);
                double toLat = add.getLatitude();
                double toLng = add.getLongitude();
                myl = new LatLng(toLat, toLng);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("Error", "check timer");
            }
            String url = TrainSchedule.this.getDirectionsUrl(origin, myl);
            DownloadTask downloadTask = new DownloadTask();
            downloadTask.execute(url);
            time();
        }
    }

    public void time() {
        int mi = 0;
        Calendar c = Calendar.getInstance();
        System.out.println("Current time => " + c.getTime());
        SimpleDateFormat df = new SimpleDateFormat("hh:mm a");
        String formattedDate = df.format(c.getTime());
        if (departTime.getText().toString().length() < 14) {
            departTime.append(" " + formattedDate);
        }
        String att = arrivalTime.getText().toString();
        String timeConvert = tempArrivalTime.getText().toString();
        if (att.length() == 0) {
            try {
                mi = Integer.parseInt(timeConvert);
                Toast.makeText(getApplicationContext(), timeConvert, Toast.LENGTH_SHORT).show();
                c.add(Calendar.SECOND, mi);
                String fTime = df.format(c.getTime());
                TrainSchedule.this.arrivalTime.setText(fTime);
            } catch (NumberFormatException nfe) {
                System.out.println("Could not parse " + nfe);
            }

        }
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @TargetApi(14)
    private void getScreenDimensions() {
        Display display = getWindowManager().getDefaultDisplay();
        width = display.getWidth();
        height = display.getHeight();
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        String url = null;
        try {        // Origin of route
            String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
            //waypoints
            //    String str_waypoints = "waypoints=" + way.latitude + "," + way.longitude;
            // Destination of route
            String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

            // Sensor enabled
            String sensor = "sensor=false";

            // Building the parameters to the web service
            String parameters = str_origin + "&" + str_dest + "&" + sensor;

            // Output format
            String output = "json";

            // Building the url to the web service
            url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
            return url;
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Please check your Internet connection and try again", Toast.LENGTH_LONG).show();
        }
        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception while downloading url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {
        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = TrainSchedule.this.downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();
            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);


        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            String distance = "";
            String duration = "";

            if (result.size() < 1) {
                Toast.makeText(TrainSchedule.this.getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
                return;
            }

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    if (j == 0) { // Get distance from the list
                        distance = point.get("duration");
                        continue;
                    } else if (j == 1) { // Get duration from the list
                        duration = point.get("duration");
                        continue;
                    }
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(3);
                lineOptions.color(Color.RED);
            }

            TrainSchedule.this.expectTime.setText(duration);
            TrainSchedule.this.tempExpectTime.setText(distance);

            // Drawing polyline in the Google Map for the i-th route
            TrainSchedule.this.googleMap.addPolyline(lineOptions);
        }
    }

    private String getDirectionsUrlTwo(LatLng origin, LatLng dest) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        //waypoints
        //    String str_waypoints = "waypoints=" + way.latitude + "," + way.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrlTWO(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception while downloading url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class DownloadTaskTWO extends AsyncTask<String, Void, String> {
        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = TrainSchedule.this.downloadUrlTWO(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTaskTwo parserTask = new ParserTaskTwo();
            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);


        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */

    private class ParserTaskTwo extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            String durationValue = "";
            String duration = "";

            if (result.size() < 1) {
                Toast.makeText(TrainSchedule.this.getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
                return;
            }

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    if (j == 0) { // Get distance from the list
                        durationValue = point.get("duration");
                        continue;
                    } else if (j == 1) { // Get duration from the list
                        duration = point.get("duration");
                        continue;
                    }
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(3);
                lineOptions.color(Color.RED);
            }
            TrainSchedule.this.tempArrivalTime.setText(durationValue);
//            Toast.makeText(getApplicationContext(), durationValue, Toast.LENGTH_LONG).show();
            // Drawing polyline in the Google Map for the i-th route
            TrainSchedule.this.googleMap.addPolyline(lineOptions);
        }
    }

    private class GetLocationFromUrl extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            try {
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(params[0]);
                HttpResponse response = httpClient.execute(httpGet);
                HttpEntity entity = response.getEntity();

                BufferedHttpEntity buf = new BufferedHttpEntity(entity);

                InputStream is = buf.getContent();

                BufferedReader r = new BufferedReader(new InputStreamReader(is));

                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line + "\n");
                }
                String result = total.toString();
                Log.i("Get URL", "Downloaded string: " + result);
                return result;
            } catch (Exception e) {
                Log.e("Get Url", "Error in downloading: " + e.toString());
                Toast.makeText(getApplicationContext(), "Please check your internet connection", Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ll.setText(result);
            if (result == null) {
                trainLocation.setText("");
//                trainStart.setText("");
                Toast.makeText(getApplicationContext(), "Train schedule not available", Toast.LENGTH_LONG).show();
            } else {
                StringTokenizer tokens = new StringTokenizer(result, ",");
                String first = tokens.nextToken();
                String second = tokens.nextToken();
                double fromLat = Double.parseDouble(first);
                double fromLng = Double.parseDouble(second);
                String ttemp = traintemp.getText().toString();
                locationAddress.getAddressFromLocation(fromLat, fromLng,
                        getApplicationContext(), new GeocoderHandler());
                String temp = trainStart.getText().toString();
                trainStartingMarker(ttemp, fromLat, fromLng);
                String dLocation = destination.getText().toString();
                if (dLocation.length() == 0) {
                } else {
                    try {
                        Geocoder gc = new Geocoder(TrainSchedule.this);
                        List<Address> list = gc.getFromLocationName(dLocation, 1);
                        Address add = list.get(0);
                        myLocality = add.getLocality();
                        double toLat = add.getLatitude();
                        double toLng = add.getLongitude();
                        setDestinationMarker(myLocality, toLat, toLng);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i("Error", "check destination Marker");
                    }
                }
                if (temp.length() < 15) {
                    locationLocality.getLocalityFromLocation(fromLat, fromLng,
                            getApplicationContext(), new GeocoderHand());
                    LatLng ll = new LatLng(fromLat, fromLng);
                    CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, UPDATE_ZOOM);
                    googleMap.moveCamera(update);
                } else {
                }
            }
        }
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new
                AlertDialog.Builder(
                com.dreamerindia.clbd.TrainSchedule.this);
        alertDialog.setTitle("SETTINGS");
        alertDialog.setMessage("Enable Location Provider! Go to settings menu?");
        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        com.dreamerindia.clbd.TrainSchedule.this.startActivity(intent);
                    }
                });
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialog.show();
    }


    public void hideSoftKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            String locationAddress;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    locationAddress = bundle.getString("address");
                    break;
                default:
                    locationAddress = null;
            }

            trainLocation.setText(locationAddress);
//            Toast.makeText(getApplicationContext(), locationAddress, Toast.LENGTH_SHORT).show();
        }
    }

    private class GeocoderHand extends Handler {
        @Override
        public void handleMessage(Message message) {
            String locationAddress;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    locationAddress = bundle.getString("address");
                    break;
                default:
                    locationAddress = null;
            }
            trainStart.append(" " + locationAddress);
            traintemp.setText(locationAddress);
        }
    }

    public void trainStartingMarker(String myLocality, double lat, double lng) {
        if (marker != null) {
            marker.remove();
        }
        MarkerOptions options = new MarkerOptions()
                .title("Train : " + myLocality)
                .position(new LatLng(lat, lng))
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_VIOLET
                ));
        marker = googleMap.addMarker(options);
    }

    public void setMyLocationMarker(String myLocality, double lat, double lng) {
        MarkerOptions options = new MarkerOptions()
                .title("My Location : " + myLocality)
                .position(new LatLng(lat, lng))
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_VIOLET
                ));
        marker = googleMap.addMarker(options);

    }

    public void setDestinationMarker(String myLocality, double lat, double lng) {
        MarkerOptions options = new MarkerOptions()
                .title("Destination: " + myLocality)
                .position(new LatLng(lat, lng))
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_VIOLET
                ));
        marker = googleMap.addMarker(options);

    }

}