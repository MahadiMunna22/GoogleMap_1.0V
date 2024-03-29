package com.example.googlemap;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    private static final int REQUEST_LOCATION = 1;

    private Sensor mysensor, mAccelerometer;
    private SensorManager sm, mSensorManager;
    private ShakeDetector mShakeDetector;

    Button getlocationBtn;
    EditText sourceLocation;
    EditText destinationLocation;

    LocationManager locationManager;
    String latitude, longitude = "";

    private MarkerOptions source, destination;
    private Polyline polyline;

    private double a, b, c;
    private float x, y, z;

    private LatLng l1, l2;

    private Double lat1, lat2, lon1, lon2;

    static String []Lat = new String [100];
    static String []Lon = new String [100];
    static int coordinatesLength;

    private String location1, location2 = "";
    private URL url;


    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sm = (SensorManager) getSystemService((SENSOR_SERVICE));
        mysensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake(int count) {
                z = z + (float) 0.1;
                map.moveCamera(CameraUpdateFactory.zoomIn());
                Toast.makeText(MainActivity.this, "Shaked!!!", Toast.LENGTH_SHORT).show();
            }
        });

        ActivityCompat.requestPermissions(this, new String[]
                {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);

        sourceLocation = findViewById(R.id.source);
        destinationLocation = findViewById(R.id.destination);
        getlocationBtn = findViewById(R.id.getLocation);

        getlocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    OnGPS();
                } else if (sourceLocation.getText().toString().isEmpty() == true && destinationLocation.getText().toString().isEmpty() == false) {
                    getLocation();
                    lat1 = Double.parseDouble(latitude);
                    lon1 = Double.parseDouble(longitude);
                    source = new MarkerOptions().position(new LatLng(lat1, lon1)).title("Source Location");

                    location2 = destinationLocation.getText().toString();
                    l2 = getLocationFromAddress(MainActivity.this, location2);

                    lat2 = l2.latitude;

                    lon2 = l2.longitude;
                    destination = new MarkerOptions().position(new LatLng(l2.latitude, l2.longitude)).title("Destination Location");

                } else if (sourceLocation.getText().toString().isEmpty() == true && destinationLocation.getText().toString().isEmpty() == true) {
                    getLocation();
                    lat1 = Double.parseDouble(latitude);
                    lon1 = Double.parseDouble(longitude);
                    source = new MarkerOptions().position(new LatLng(lat1, lon1)).title("Current Location");
                    destination = new MarkerOptions().position(new LatLng(lat1, lon1)).title("Current Location");

                } else {
                    location1 = sourceLocation.getText().toString();
                    l1 = getLocationFromAddress(MainActivity.this, location1);
                    lat1 = l1.latitude;
                    lon1 = l1.longitude;
                    source = new MarkerOptions().position(new LatLng(lat1, lon1)).title("Source Location");

                    location2 = destinationLocation.getText().toString();
                    l2 = getLocationFromAddress(MainActivity.this, location2);

                    lat2 = l2.latitude;

                    lon2 = l2.longitude;

                    destination = new MarkerOptions().position(new LatLng(l2.latitude, l2.longitude)).title("Destination Location");


                }
                //polylineFunc();
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                mapFragment.getMapAsync(MainActivity.this);

            }
        });


    }

    private void polylineFunc() {

        List <LatLng> path = new ArrayList();
        double[] lat = new double[100];
        double[] lon = new double[100];

        for(int i = 0; i<coordinatesLength;i++)
        {
            lat[i] = Double.parseDouble(Lon[i]);
            lon[i] = Double.parseDouble(Lat[i]);

            path.add(new LatLng(lat[i],lon[i]));
        }

        //path.add(new LatLng(lat1,lon1));
        //path.add(new LatLng(lat2,lon2));

        //Toast.makeText(getApplicationContext(),Double.parseDouble(Lon[0])+" "+Double.parseDouble(Lat[0]),Toast.LENGTH_SHORT).show();

        //Log.v("Messege ", Lat[1]);
        if(path.size()>0)
        {
            polyline = map.addPolyline(new PolylineOptions().addAll(path).color(Color.BLUE).width(10));
            //Toast.makeText(getApplicationContext(),"Path found",Toast.LENGTH_SHORT).show();
        }

        //Toast.makeText(getApplicationContext(),path.size()+"",Toast.LENGTH_SHORT).show();

    }

    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this,

                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location LocationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location LocationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location LocationPassive = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

            /*if (LocationGps !=null)
            {
                double lat=LocationGps.getLatitude();
                double longi=LocationGps.getLongitude();

                latitude=String.valueOf(lat);
                longitude=String.valueOf(longi);

                String LocationAddress = getAddress(lat,longi);

                sourceLocation.setText(LocationAddress);
            }
            else */
            if (LocationNetwork != null) {
                double lat = LocationNetwork.getLatitude();
                double longi = LocationNetwork.getLongitude();

                latitude = String.valueOf(lat);
                longitude = String.valueOf(longi);

                String LocationAddress = getAddress(lat, longi);

                sourceLocation.setText(LocationAddress);
            } else if (LocationPassive != null) {
                double lat = LocationPassive.getLatitude();
                double longi = LocationPassive.getLongitude();

                latitude = String.valueOf(lat);
                longitude = String.valueOf(longi);

                String LocationAddress = getAddress(lat, longi);

                sourceLocation.setText(LocationAddress);
            } else {
                Toast.makeText(this, "Can't Get Your Location", Toast.LENGTH_SHORT).show();
            }

            //Thats All Run Your App
        }
    }

    private String getAddress(double lat, double longi) {

        String myAddress = "";

        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, longi, 2);
            String address = addresses.get(1).getAddressLine(0);
            myAddress = addresses.get(1).getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return myAddress;
    }


    public LatLng getLocationFromAddress(Context context, String place) {

        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;

        try {
            address = coder.getFromLocationName(place, 5);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            p1 = new LatLng(location.getLatitude(), location.getLongitude());

        } catch (Exception ex) {

            ex.printStackTrace();
        }

        return p1;
    }


    private void getJson(double srcLat, double srcLon, double desLat, double desLon) {
        try {
            url = new URL("http://www.yournavigation.org/api/1.0/gosmore.php?flat="+srcLat+"&flon="+srcLon+"&tlat="+desLat+"&tlon="+desLon+"&format=geojson");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        GetJSON getJSON = new GetJSON();
        getJSON.execute();
    }

    private class GetJSON extends AsyncTask<Void,Void,String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
            try {
                loadIntoListView(s);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                //URL url = new URL("http://ec2-34-207-172-7.compute-1.amazonaws.com/getData.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                StringBuilder sb = new StringBuilder();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String json;
                while ((json = bufferedReader.readLine()) != null) {
                    sb.append(json + "\n");
                }
                return sb.toString();
            } catch (Exception e) {
                return null;
            }
        }
    }

    private void loadIntoListView(String json) throws JSONException {
        JSONObject myResponse = new JSONObject(json.toString());

        JSONArray coordinates = myResponse.getJSONArray("coordinates");

        coordinatesLength = coordinates.length();

        for(int i = 0; i<coordinatesLength; i++)
        {
            JSONArray s = (JSONArray) coordinates.get(i);

            Lat[i] = s.get(0).toString();
            Lon[i] = s.get(1).toString();
            //Toast.makeText(this, coordinatesLength+"", Toast.LENGTH_SHORT).show();
        }

        polylineFunc();

        //Toast.makeText(getApplicationContext(),(Lon[0])+" "+(Lat[0]),Toast.LENGTH_SHORT).show();
    }


    private void OnGPS() {

        final AlertDialog.Builder builder= new AlertDialog.Builder(this);

        builder.setMessage("Enable GPS").setCancelable(false).setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog alertDialog=builder.create();
        alertDialog.show();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.clear();
        map.addMarker(source);
        map.addMarker(destination);
        LatLng Source = new LatLng(Double.parseDouble(latitude),Double.parseDouble(longitude));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(Source,15f));

        try {
            getJson(lat1,lon1,lat2,lon2);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        a=event.values[0];
        b=event.values[1];
        c=event.values[2];
        x = 0;
        y = 0;
        z = 0;
        if(a>=8.0)
        {
            x = x + 5;
            y = y + 0;
            map.moveCamera(CameraUpdateFactory.scrollBy(x,y));
            //Toast.makeText(getApplicationContext(),"Left",Toast.LENGTH_SHORT).show();
        }
        else if(a<=-8.0)
        {
            x = x - 5;
            y = y - 0;
            map.moveCamera(CameraUpdateFactory.scrollBy(x,y));
            //Toast.makeText(getApplicationContext(),"Right",Toast.LENGTH_SHORT).show();
        }
        else if(b>=8.0)
        {
            x = x + 0;
            y = y - 5;
            //map.moveCamera(CameraUpdateFactory.scrollBy(x,y));
            //Toast.makeText(getApplicationContext(),"Up",Toast.LENGTH_SHORT).show();
        }
        else if(b<=-8.0)
        {
            x = x + 0;
            y = y + 5;
            map.moveCamera(CameraUpdateFactory.scrollBy(x,y));
            //Toast.makeText(getApplicationContext(),"Down",Toast.LENGTH_SHORT).show();
        }
        else if(c>=8.0)
        {
            //map.moveCamera(CameraUpdateFactory.zoomOut());
            //Toast.makeText(getApplicationContext(),"Plane Up",Toast.LENGTH_SHORT).show();
        }
        else if(c<=-8.0)
        {
            //Toast.makeText(getApplicationContext(),"Plane Down",Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onResume()
    {
        super.onResume();
        sm.registerListener(this,mysensor,SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);
    }
    public void onPause()
    {
        super.onPause();
        sm.unregisterListener(this);
        mSensorManager.unregisterListener(mShakeDetector);
    }
}
