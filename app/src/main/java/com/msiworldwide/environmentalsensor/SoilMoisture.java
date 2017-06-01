package com.msiworldwide.environmentalsensor;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.msiworldwide.environmentalsensor.Data.DatabaseHelper;

public class SoilMoisture extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soil_moisture);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Soil Moisture");
        mToolbar.setTitleTextColor(Color.WHITE);

        db = new DatabaseHelper(getApplicationContext());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager() .findFragmentById(R.id.soil_map);
        if (mapFragment!=null){
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.result_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.moisture_id:
                //Intent mIntent = new Intent(this, SoilMoisture.class);
                //startActivity(mIntent);
                break;
            case R.id.nutrition_id:
                Intent nIntent = new Intent(this, SoilNutrition.class);
                startActivity(nIntent);
                this.finish();
                break;
            case R.id.temperature_id:
                Intent tIntent = new Intent(this, Temperature.class);
                startActivity(tIntent);
                this.finish();
                break;
            case R.id.humidity_id:
                Intent hIntent = new Intent(this, Humidity.class);
                startActivity(hIntent);
                this.finish();
                break;
            case R.id.sunlight_id:
                Intent sIntent = new Intent(this, Sunlight.class);
                startActivity(sIntent);
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        // Add a marker in Blacksburg and move the camera
        LatLng blacksburg = new LatLng(37.229572,  -80.413940);
        mMap.addMarker(new MarkerOptions().position(blacksburg).title("Marker in Blacksburg"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(blacksburg));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(10));
    }
}
