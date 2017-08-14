package com.msiworldwide.environmentalsensor;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class VisualizeResults extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualize_results);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Visualize Results");
        }
        mToolbar.setTitleTextColor(Color.WHITE);
    }

    public void osm(View view){
        Toast.makeText(VisualizeResults.this, "OSM", Toast.LENGTH_LONG).show();
    }


    public void openSoilMoisture(View view){
        Log.i("home", "btn openSoilMoisture");
        Intent intent = new Intent(this, SoilMoisture.class);
        startActivity(intent);

        /*Intent intent = new Intent(VisualizeResults.this, SoilMoisture.class);
        startActivity(intent);*/
    }

    public void openSoilNutrition(View view) {
        Intent intent = new Intent(this, SoilNutrition.class);
        startActivity(intent);
    }

    public void openTemperature(View view) {
        Intent intent = new Intent(this, Temperature.class);
        startActivity(intent);
    }

    public void openHumidity(View view) {
        Intent intent = new Intent(this, Humidity.class);
        startActivity(intent);
    }

    public void openSunlight(View view) {
        Intent intent = new Intent(this, Sunlight.class);
        startActivity(intent);
    }
}
