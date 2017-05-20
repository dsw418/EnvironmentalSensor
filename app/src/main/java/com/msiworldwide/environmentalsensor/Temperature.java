package com.msiworldwide.environmentalsensor;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class Temperature extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Temperature");
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
                Intent mIntent = new Intent(this, SoilMoisture.class);
                startActivity(mIntent);
                this.finish();
                break;
            case R.id.nutrition_id:
                Intent nIntent = new Intent(this, SoilNutrition.class);
                startActivity(nIntent);
                this.finish();
                break;
            case R.id.temperature_id:
                //Intent tIntent = new Intent(this, Temperature.class);
                //startActivity(tIntent);
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
}
