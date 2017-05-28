package com.msiworldwide.environmentalsensor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.msiworldwide.environmentalsensor.Data.DatabaseHelper;
import com.msiworldwide.environmentalsensor.Data.WaterSourceData;
import com.msiworldwide.environmentalsensor.ble.BleManager;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class NewWaterSource extends AppCompatActivity implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_EXTERNAL_STORAGE_RESULT = 1;
    private static final int REQUEST_CAMERA_RESULT = 1;
    String mCurrentPhotoPath;
    TextView sourceCount;
    int count = 0;
    ArrayList<String> WaterSources = new ArrayList<>();
    EditText watersourcename;
    WaterSourceData waterSourceData = new WaterSourceData();
    String waterSourceList;
    long water_id;

    DatabaseHelper db;

    int cameraAllowed = 0;
    int writeDataAllowed = 0;

    private static final long INTERVAL = 5000*10;
    private static final long FASTEST_INTERVAL = 500*10;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation;
    String mLastUpdateTime;

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_water_source);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("New Water Source");

        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();

        db = new DatabaseHelper(getApplicationContext());

        sourceCount = (TextView) findViewById(R.id.source_count);
        String number = String.valueOf(count+1);
        sourceCount.setText("Water Source #" + number);

        watersourcename = (EditText) findViewById(R.id.waterName);
    }

    public void MeasureWaterLocation(View view) {
        double lat = mCurrentLocation.getLatitude();
        double lng = mCurrentLocation.getLongitude();
        String coords = String.valueOf(lat) + "," + String.valueOf(lng);
        WaterSources.add(coords);
        sourceCount.setText(coords);
        count++;
    }

    public void addSource(View view) {
        String number = String.valueOf(count+1);
        sourceCount.setText("Water Source #" + number);
    }

    public void saveWater(View view) {
        String name = watersourcename.getText().toString();
        waterSourceList = TextUtils.join(",", WaterSources);
        if (TextUtils.isEmpty(name)) {
            watersourcename.setError("Please Enter a Name");
        } else if(TextUtils.isEmpty(waterSourceList)) {
            AlertDialog alertDialog = new AlertDialog.Builder(
                    NewWaterSource.this).create();

            // Setting Dialog Title
            alertDialog.setTitle("No Coordinates");

            // Setting Dialog Message
            alertDialog.setMessage("Please Measure the Water Source Location");

            // Setting OK Button
            alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            // Showing Alert Message
            alertDialog.show();
        }
        else {
            waterSourceData.setWaterSourceId(name);
            waterSourceData.setCoordinates(waterSourceList);
            water_id = db.createWaterSourceData(waterSourceData);
            Intent returnIntent = new Intent();
            returnIntent.putExtra("result", String.valueOf(water_id));
            setResult(Activity.RESULT_OK,returnIntent);
            finish();
        }
    }

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
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    public void takePicture(View view) {
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >22) {
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    Toast.makeText(this, "External storage permission required to save images",
                            Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_EXTERNAL_STORAGE_RESULT);
            }
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >22) {
                if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                    Toast.makeText(this, "Camera permission is required to take images",
                            Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_RESULT);
            }
        } else {
            if (Build.VERSION.SDK_INT >22) {
                /*if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                    Toast.makeText(this, "Camera permission is required to take images",
                            Toast.LENGTH_SHORT).show();
                }*/
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CAMERA_RESULT);
                /*if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    Toast.makeText(this, "External storage permission required to save images",
                            Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_EXTERNAL_STORAGE_RESULT);*/
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == 1) {
            for(int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED && permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    writeDataAllowed = 1;
                } else if (grantResults[i] == PackageManager.PERMISSION_GRANTED && permissions[i].equals(Manifest.permission.CAMERA)) {
                    cameraAllowed = 1;
                }
            }
        }
        if (cameraAllowed == 1 && writeDataAllowed == 1) {
            dispatchTakePictureIntent();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        // Ensure that there's a camera activity to handle the intent
/*        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the file where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }*/
    }
/*
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp+ "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(mCurrentPhotoPath);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
        }
    }*/
}
