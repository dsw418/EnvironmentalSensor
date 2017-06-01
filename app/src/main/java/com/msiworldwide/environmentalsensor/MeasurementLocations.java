package com.msiworldwide.environmentalsensor;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.msiworldwide.environmentalsensor.Data.DatabaseHelper;

public class MeasurementLocations extends AppCompatActivity {

    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement_locations);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Measurement Locations");
        mToolbar.setTitleTextColor(Color.WHITE);

        db = new DatabaseHelper(getApplicationContext());
    }

    public void openMeasurement(View view){
        Intent intent = new Intent(this, Measurement.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        db.deleteCurrentSelections();
    }
}
