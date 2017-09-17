package com.msiworldwide.environmentalsensor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.common.api.PendingResult;
//import com.google.android.gms.common.api.Status;
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

    private static final long INTERVAL = 1000*2;
    private static final long FASTEST_INTERVAL = 1000;
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
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle("New Field Boundary");
        }
        mToolbar.setTitleTextColor(Color.WHITE);

        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        loc = (TextView) findViewById(R.id.location);
        fieldname = (EditText) findViewById(R.id.FieldName);

        db = new DatabaseHelper(getApplicationContext());
    }

    public void start_loc(View view) {
        if (mCurrentLocation == null) {
            AlertDialog.Builder location = new AlertDialog.Builder(this);
            location.setTitle("No Location Data")
                    .setMessage("Please Check if GPS is enabled")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            AlertDialog alert = location.create();
            alert.show();
        }
        else {
            if (!started) {
                Toast begin = Toast.makeText(getApplicationContext(), "Begin Walking", Toast.LENGTH_SHORT);
                begin.show();
                double lat = mCurrentLocation.getLatitude();
                double lng = mCurrentLocation.getLongitude();
                String coords = String.valueOf(lat) + "," + String.valueOf(lng);
                Log.i("Data", coords+"\n");
                boundary.add(coords);
                loc.setText("Accuracy: " + String.valueOf(mCurrentLocation.getAccuracy()));
                start();
            }
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
        Toast end = Toast.makeText(getApplicationContext(), "Measurement Finished", Toast.LENGTH_SHORT);
        end.show();
        //loc.setText(boundaryCoordsList);
        //loc.setText("Stopped");
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (mCurrentLocation.getAccuracy() < 20) {
                double lat = mCurrentLocation.getLatitude();
                double lng = mCurrentLocation.getLongitude();
                String coords = String.valueOf(lat) + "," + String.valueOf(lng);
                boundary.add(coords);
                loc.setText("Accuracy: " + String.valueOf(mCurrentLocation.getAccuracy()));
                //loc.setText("Running");
                if (started) {
                    start();
                }
            } else {
                handler.postDelayed(secondcheck,2000);
            }
        }
    };

    private Runnable secondcheck = new Runnable() {
        @Override
        public void run() {
            double lat = mCurrentLocation.getLatitude();
            double lng = mCurrentLocation.getLongitude();
            String coords = String.valueOf(lat) + "," + String.valueOf(lng);
            if (mCurrentLocation.getAccuracy() < 20) {
                boundary.add(coords);
            }
            loc.setText("Accuracy: " + String.valueOf(mCurrentLocation.getAccuracy()));
            //loc.setText("Running");
            if (started) {
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
        try {
            /*PendingResult<Status> pendingResult = LocationServices.FusedLocationApi
                    .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);*/
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch(SecurityException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
                requestLocationPermissions();
            }
        }
    }

    // region Permissions
    @TargetApi(Build.VERSION_CODES.M)
    private void requestLocationPermissions() {
        // Android M Permission check
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("This app needs location access");
        builder.setMessage("Please grant location access so this app can use the GPS");
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        });
        builder.show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        loc.setText("Accuracy: " + String.valueOf(mCurrentLocation.getAccuracy()));
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

/*    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }*/

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
