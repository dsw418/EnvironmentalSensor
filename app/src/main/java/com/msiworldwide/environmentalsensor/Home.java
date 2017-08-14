package com.msiworldwide.environmentalsensor;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.msiworldwide.environmentalsensor.Data.DatabaseHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Home extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //CardView cv = (CardView) findViewById(R.id.btn);
        CardView btnMeasures = (CardView) findViewById(R.id.btnMeasures);
        CardView btnVisualization = (CardView) findViewById(R.id.btnVisualization);
        CardView btnExport = (CardView) findViewById(R.id.btnExport);

        TextView bluetoothConnectivityIndicator = (TextView) findViewById(R.id.bluetoothConnectivityIndicator);
        bluetoothConnectivityIndicator.setTextColor(Color.RED);
       // Button btnExpData = (Button) findViewById(R.id.btnExpData);


        btnMeasures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intnt = new Intent(Home.this, MainActivity.class);
                startActivity(intnt);
            }
        });

        btnVisualization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent innt = new Intent(Home.this, VisualizeResults.class);
                startActivity(innt);
            }
        });

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent innt = new Intent(Home.this, SendData.class);
                startActivity(innt);
            }
        });



        Spinner spinner = (Spinner) findViewById(R.id.spinner_blue);


        // Spinner click listener

        // Spinner Drop down elements
        List<String> categories = new ArrayList<String>();
        categories.add("Select Device");
        categories.add("Refresh");

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);

    }




}
