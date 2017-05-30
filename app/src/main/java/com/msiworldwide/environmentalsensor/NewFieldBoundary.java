package com.msiworldwide.environmentalsensor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
//import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.msiworldwide.environmentalsensor.Data.DatabaseHelper;
import com.msiworldwide.environmentalsensor.Data.FieldData;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class NewFieldBoundary extends AppCompatActivity implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final long INTERVAL = 1000*5;
    private static final long FASTEST_INTERVAL = 1000*1;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation;
    String mLastUpdateTime;
    private boolean started = false;
    private Handler handler = new Handler();
    ArrayList<String> boundary = new ArrayList<>();
    TextView loc;
    String boundaryCoordsList;
    EditText fieldname;

    FieldData fieldData = new FieldData();
    long field_id;

    DatabaseHelper db;

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_field_boundary);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("New Field Boundary");

        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        loc = (TextView) findViewById(R.id.location);
        fieldname = (EditText) findViewById(R.id.FieldName);

        db = new DatabaseHelper(getApplicationContext());
    }

    public void start_loc(View view) {
        if (!started) {
            double lat = mCurrentLocation.getLatitude();
            double lng = mCurrentLocation.getLongitude();
            String coords = String.valueOf(lat) + "," + String.valueOf(lng);
            boundary.add(coords);
            loc.setText(coords);
            start();
        }
    }

    public void start() {
        started = true;
        handler.postDelayed(runnable,5000);
    }

    public void stop_loc(View view) {
        stop();
    }

    public void stop() {
        started = false;
        handler.removeCallbacks(runnable);
        boundaryCoordsList = TextUtils.join(",", boundary);
        fieldData.setCoordinates(boundaryCoordsList);
        //loc.setText(boundaryCoordsList);
        //loc.setText("Stopped");
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            double lat = mCurrentLocation.getLatitude();
            double lng = mCurrentLocation.getLongitude();
            String coords = String.valueOf(lat) + "," + String.valueOf(lng);
            boundary.add(coords);
            loc.setText(coords);
            //loc.setText("Running");
            if(started) {
                start();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop(){
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stopLocationUpdates();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    public void save(View view) {
        String name = fieldname.getText().toString();
        //String namecheck = "'" + name + "'";
        fieldData.setFieldId(name);
        if (TextUtils.isEmpty(name)) {
            fieldname.setError("Please Enter a Name");
        } else if(TextUtils.isEmpty(boundaryCoordsList)) {
            AlertDialog alertDialog = new AlertDialog.Builder(
                    NewFieldBoundary.this).create();

            // Setting Dialog Title
            alertDialog.setTitle("No Coordinates");

            // Setting Dialog Message
            alertDialog.setMessage("Please Measure the Field Boundary");

            // Setting OK Button
            alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            // Showing Alert Message
            alertDialog.show();
        } else if(db.CheckIsDataAlreadyInDBorNot("FieldData","FieldId",name)){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Field Name Already Exists")
                    .setMessage("Would you like to overwrite this field?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            field_id = db.updateFieldData(fieldData);
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("result", String.valueOf(field_id));
                            setResult(Activity.RESULT_OK,returnIntent);
                            finish();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            AlertDialog.Builder builder = new AlertDialog.Builder(NewFieldBoundary.this);
                            builder.setMessage("Please Select a New Field Name")
                                    .setCancelable(false)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
        else {
            field_id = db.createFieldData(fieldData);
            //loc.setText(name + "," + boundaryCoordsList);
            Intent returnIntent = new Intent();
            returnIntent.putExtra("result", String.valueOf(field_id));
            setResult(Activity.RESULT_OK,returnIntent);
            finish();
        }
    }
}
