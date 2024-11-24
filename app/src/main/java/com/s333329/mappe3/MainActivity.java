package com.s333329.mappe3;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Handler handler;
    private ExecutorService executor;


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
        Button button = findViewById(R.id.knapp);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeHttpRequest();
            }
        });

        // code for map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        makeWebServiceRequest();
    }

    // code for map
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in SydneyTest"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
    // send data to server
    private void makeHttpRequest() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://maps.googleapis.com/maps/api/geocode/json?address=Bergen"
                            + "&key=AIzaSyDo1iNp71Yynw1L_0diIi8QylFhkc-ke7w");
                    HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
                    httpUrlConnection.setRequestMethod("GET");
                    httpUrlConnection.connect();
                    int responseCode = httpUrlConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = httpUrlConnection.getInputStream();
                        String response = readInputStreamToString(inputStream);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                TextView textView = findViewById(R.id.textView);
                                textView.setText(response);
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

// Retrieves from database
    private void makeWebServiceRequest() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://dave3600.cs.oslomet.no/~s333329/jsonout.php");

                    Log.i("DataRetrieval", "Connecting to URL: " + url.toString());

                    HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
                    httpUrlConnection.setRequestMethod("GET");
                    httpUrlConnection.connect();
                    int responseCode = httpUrlConnection.getResponseCode();
                    Log.i("DataRetrieval", "Response code: " + responseCode);

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = httpUrlConnection.getInputStream();
                        // calls method to covert to string
                        String response = readInputStreamToString(inputStream);

                        Log.i("DataRetrieval", "Response from server: " + response);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                TextView textView = findViewById(R.id.textView);
                                textView.setText(response);
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Error: " + responseCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Throwable e) {
                    Log.e("DataRetrieval", "Error: " + e.getMessage(), e);
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
    // post to database
    private String readInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new
                InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();

        String lest = stringBuilder.toString();
        String retur = "";
        try {

            JSONArray mat = new JSONArray(lest);
            for (int i = 0; i < mat.length(); i++) {
                JSONObject jsonobject = mat.getJSONObject(i);
                String name = jsonobject.getString("name");
                retur = retur + name + "\n";
            }
            return retur;
        } catch(JSONException e){
            Log.e("DataRetrieval", "Error parsing JSON", e);
            e.printStackTrace();
            return retur;
        }catch(Exception e){
            return "Noe gikk feil" + e;
        }
    }

}


