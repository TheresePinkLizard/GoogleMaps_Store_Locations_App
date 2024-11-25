package com.s333329.mappe3;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;
import com.s333329.mappe3.Location;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Handler handler;
    private ExecutorService executor;
    private List<Location> locations = new ArrayList<>();
    TextView addresse;
    TextView latitude;
    TextView longitude;
    TextView description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        handler = new Handler();
        executor = Executors.newSingleThreadExecutor();

        addresse = findViewById(R.id.addressTextview);
        latitude = findViewById(R.id.latitudeTextview);
        longitude = findViewById(R.id.longitudeTextview);
        description = findViewById(R.id.editTextDescription);


        // code for map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        makeHttpRequest();

        Button save = findViewById(R.id.button);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                String theaddress = addresse.getText().toString();
                double thelatitude = Double.parseDouble(latitude.getText().toString());
                double thelongitude = Double.parseDouble(longitude.getText().toString());
                String thedescription = description.getText().toString();

                makeWebServiceRequest(theaddress, thelatitude, thelongitude, thedescription);
            }
        });

    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        /* Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in SydneyTest"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

         */

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng point) {
                mMap.clear();
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());

                try {
                    List<Address> addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1);
                    Address obj = addresses.get(0);
                    String add = obj.getAddressLine(0);

                    addresse.setText(add);
                    latitude.setText(String.valueOf(point.latitude));
                    longitude.setText(String.valueOf(point.longitude));

                    // Get the current zoom level
                    float currentZoomLevel = mMap.getCameraPosition().zoom;

                    String locationDescription = ""; // Default description
                    for (Location location : locations){
                        if (Math.abs(location.latitude - point.latitude) < 0.000001 && Math.abs(location.longitude - point.longitude) < 0.000001) {
                            locationDescription = location.description; // Update the description if a matching location is found
                        }
                    }
                    description.setText(locationDescription); // Update the description TextView

                    // Move the camera to the clicked point and add a new marker at the clicked point
                    mMap.addMarker(new MarkerOptions().position(point).title("Clicked Point"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, currentZoomLevel));

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("setOnMapClick", "could not set textfield");
                }

                // Add this line to display the stored locations again
                displayLocationsOnMap();
            }
        });
        // code to update the textfields if one selects a place on server
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                LatLng position = marker.getPosition();
                for (Location location : locations) {
                    if (Math.abs(location.latitude - position.latitude) < 0.000001 && Math.abs(location.longitude - position.longitude) < 0.000001) {
                        addresse.setText(location.address);
                        latitude.setText(String.valueOf(location.latitude));
                        longitude.setText(String.valueOf(location.longitude));
                        description.setText(location.description);
                        break;
                    }
                }
                return false;
            }
        });
    }

    private void displayLocationsOnMap() {
        // Display all pins from 'locations' on the map
        if (mMap != null && locations != null) {
            for (Location location : locations) {
                LatLng position = location.position;
                mMap.addMarker(new MarkerOptions().position(position).title(location.description).snippet(location.address));
            }
        }
    }
    // retrieve data
    private void makeHttpRequest() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://dave3600.cs.oslomet.no/~s333329/locationsJsonout.php");
                    HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
                    httpUrlConnection.setRequestMethod("GET");
                    httpUrlConnection.connect();
                    int responseCode = httpUrlConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = httpUrlConnection.getInputStream();
                        locations = readInputStreamToString(inputStream);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                float zoomLevel = 5.0f;
                                if(!locations.isEmpty()) {
                                    for (Location location : locations) {
                                        Log.i("","");
                                        LatLng position = location.position;
                                        mMap.addMarker(new MarkerOptions().position(position).title(location.description).snippet(location.address));
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoomLevel));
                                        addresse.setText(location.address);
                                        latitude.setText(String.valueOf(location.latitude));
                                        longitude.setText(String.valueOf(location.longitude));
                                        description.setText(location.description);
                                    }
                                    displayLocationsOnMap();
                                }
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Error: " + responseCode,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }



// send data
private void makeWebServiceRequest(String theaddress, double thelatitude, double thelongitude, String thedescription) {
    executor.execute(new Runnable() {
        @Override
        public void run() {
            try {
                URL url = new URL("https://dave3600.cs.oslomet.no/~s333329/locationsJsonin.php");
                HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
                httpUrlConnection.setRequestMethod("POST");
                httpUrlConnection.setDoOutput(true);

                // Prepare the data to be sent
                String data = "address=" + URLEncoder.encode(theaddress, "UTF-8")
                        + "&latitude=" + thelatitude
                        + "&longitude=" + thelongitude
                        + "&description=" + URLEncoder.encode(thedescription, "UTF-8");

                // Write the data to the output stream
                OutputStream out = httpUrlConnection.getOutputStream();
                out.write(data.getBytes());
                out.flush();
                out.close();

                TextView status = findViewById(R.id.savedStatus);

                int responseCode = httpUrlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // The request was successful
                    status.setText("Your location is stored on server");
                    status.setTextColor(Color.parseColor("#009000"));
                    makeHttpRequest();
                    // Handle the server's response here if needed
                } else {
                    // The request failed
                    status.setText("Failed storing location to server");
                    status.setTextColor(Color.RED);
                    throw new Exception("Server responded with: " + responseCode);
                }
                //code to hide keyboard when storing location
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                View view = getCurrentFocus();
                if (view == null) {
                    view = new View(MainActivity.this);
                }
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            } catch (Exception e) {
                // Handle the exception
                e.printStackTrace();
            }
        }
    });
}
    // post to database
    private List<Location> readInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new
                InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();

        String lest = stringBuilder.toString();
        try {

            JSONArray place = new JSONArray(lest);
            for (int i = 0; i < place.length(); i++) {
                JSONObject jsonobject = place.getJSONObject(i);
                String address = jsonobject.getString("address");
                double latitude = jsonobject.getDouble("latitude");
                double longitude = jsonobject.getDouble("longitude");
                String description = jsonobject.getString("description");

                LatLng position = new LatLng(latitude, longitude);
                Log.i("","Output: "+ address + latitude + longitude + description);
                locations.add(new Location(position, description, address, latitude, longitude));
            }
            return locations;
        } catch(JSONException e){
            Log.e("DataRetrieval", "Error parsing JSON", e);
            e.printStackTrace();
            return locations;
        }catch(Exception e){
            Log.e("MainActivity, readInputStreamToString","Something went wrong",e);
            return locations;
        }
    }

}


