package com.msiworldwide.environmentalsensor;

import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telecom.Connection;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.vision.text.Text;
import com.msiworldwide.environmentalsensor.ble.BleManager;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
//import java.util.logging.Handler;

public class Measurement extends AppCompatActivity implements OnMapReadyCallback,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        BleManager.BleManagerListener {

    private GoogleMap mMap;

    private static final long INTERVAL = 5000*10;
    private static final long FASTEST_INTERVAL = 500*5;
    Button BtnMeasurement;
    //TextView tvLocation;
    TextView received_data;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation;
    String mLastUpdateTime;

    // Service Constants
    private final static String TAG = Measurement.class.getSimpleName();
    public static final String UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_RX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_TX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_DFU = "00001530-1212-EFDE-1523-785FEABCD123";
    public static final int kTxMaxCharacters = 20;
    private boolean isRxNotificationEnabled = false;


    // Data
    protected BleManager mBleManager;
    protected BluetoothGattService mUartService;


    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Measurement");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager() .findFragmentById(R.id.map);
        if (mapFragment!=null){
            mapFragment.getMapAsync(this);
        }

        if (!isGooglePlayServicesAvailable()) {
            finish();
        }
        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();

        mBleManager = BleManager.getInstance(this);
        mBleManager.setBleListener(this);
       // mUartService = mBleManager.getGattService(UUID_SERVICE);
        // Continue
        mUartService = mBleManager.getGattService(UUID_SERVICE);
        enableRxNotifications();
        //tvLocation = (TextView) findViewById(R.id.tvLocation);
        received_data = (TextView) findViewById(R.id.received_data);
        BtnMeasurement = (Button) findViewById(R.id.take_measurement);
        BtnMeasurement.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                String text = "M";
                sendData(text);
                updateUI();
            }
        });


    }

    public void openVisualizeResults(View view){
        Intent intent = new Intent(this, VisualizeResults.class);
        startActivity(intent);
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

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else{
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
        mMap.setMyLocationEnabled(true);
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

    private void updateUI() {
        if (null != mCurrentLocation) {
/*            String lat = String.valueOf(mCurrentLocation.getLatitude());
            String lng = String.valueOf(mCurrentLocation.getLongitude());
            tvLocation.setText("At Time: " + mLastUpdateTime + "\n" +
                    "Latitude: " + lat + "\n" +
                    "Longitude: " + lng + "\n" +
                    "Accuracy: " + mCurrentLocation.getAccuracy() + "\n" +
                    "Provider: " + mCurrentLocation.getProvider());*/

            LatLng loc = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(loc).title("Current Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(18));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
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
        mBleManager = BleManager.getInstance(this);
        mBleManager.setBleListener(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        // Add a marker in Blacksburg and move the camera
 /*       LatLng blacksburg = new LatLng(37.229572,  -80.413940);
        mMap.addMarker(new MarkerOptions().position(blacksburg).title("Marker in Blacksburg"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(blacksburg));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(10));*/
    }

    // region Send Data to UART
    protected void sendData(String text) {
        final byte[] value = text.getBytes(Charset.forName("UTF-8"));
        sendData(value);
    }

    protected void sendData(byte[] data) {
        if (mUartService != null) {
            // Split the value into chunks (UART service has a maximum number of characters that can be written )
            for (int i = 0; i < data.length; i += kTxMaxCharacters) {
                final byte[] chunk = Arrays.copyOfRange(data, i, Math.min(i + kTxMaxCharacters, data.length));
                mBleManager.writeService(mUartService, UUID_TX, chunk);
            }
            received_data.setText("Measuring...");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BluetoothGattCharacteristic characteristic = mUartService.getCharacteristic(UUID.fromString(UUID_RX));
                    byte[] dataread = characteristic.getValue();
                    String data = new String(dataread, Charset.forName("UTF-8"));
                    received_data.setText("Data: " + data);
                }
            }, 5000);
        } else {
            Log.w(TAG, "Uart Service not discovered. Unable to send data");
        }
    }

// BleManager Listener
    @Override
    public void onConnected() {

    }

    @Override
    public void onConnecting() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onServicesDiscovered() {
        mUartService = mBleManager.getGattService(UUID_SERVICE);
        enableRxNotifications();
    }

    @Override
    public void onDataAvailable(BluetoothGattCharacteristic characteristic) {
        // UART RX
/*        received_data.setText("Received");
*//*        final byte[] bytes = characteristic.getValue();
        final String data = new String(bytes, Charset.forName("UTF-8"));
        received_data.setText(data);*//*
        // Check if there is a pending sendDataRunnable
        if (sendDataRunnable != null) {
            if (characteristic.getService().getUuid().toString().equalsIgnoreCase(UUID_SERVICE)) {
                if (characteristic.getUuid().toString().equalsIgnoreCase(UUID_RX)) {

                    Log.d(TAG, "sendData received data");
                    sendDataTimeoutHandler.removeCallbacks(sendDataRunnable);
                    sendDataRunnable = null;

                    if (sendDataCompletionHandler != null) {
                        final byte[] bytes = characteristic.getValue();
                        final String data = new String(bytes, Charset.forName("UTF-8"));
                        received_data.setText(data);

                        final SendDataCompletionHandler dataCompletionHandler =  sendDataCompletionHandler;
                        sendDataCompletionHandler = null;
                        dataCompletionHandler.sendDataResponse(data);
                    }
                }
            }
        }*/
    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor){

    }

    @Override
    public void onReadRemoteRssi(int rssi) {

    }

    protected void enableRxNotifications() {
        isRxNotificationEnabled = true;
        mBleManager.enableNotification(mUartService, UUID_RX, true);
    }

    // region SendDataWithCompletionHandler
    protected interface SendDataCompletionHandler {
        void sendDataResponse(String data);
    }

    final private Handler sendDataTimeoutHandler = new Handler();
    private Runnable sendDataRunnable = null;
    private SendDataCompletionHandler sendDataCompletionHandler = null;
    protected void sendData(byte[] data, SendDataCompletionHandler completionHandler) {

        if (completionHandler == null) {
            sendData(data);
            return;
        }

        if (!isRxNotificationEnabled) {
            Log.w(TAG, "sendData warning: RX notification not enabled. completionHandler will not be executed");
        }

        if (sendDataRunnable != null || sendDataCompletionHandler != null) {
            Log.d(TAG, "sendData error: waiting for a previous response");
            return;
        }

        Log.d(TAG, "sendData");
        sendDataCompletionHandler = completionHandler;
        sendDataRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "sendData timeout");
                final SendDataCompletionHandler dataCompletionHandler =  sendDataCompletionHandler;

                Measurement.this.sendDataRunnable = null;
                Measurement.this.sendDataCompletionHandler = null;

                dataCompletionHandler.sendDataResponse(null);
            }
        };

        sendDataTimeoutHandler.postDelayed(sendDataRunnable, 2*1000);
        sendData(data);

    }

    protected boolean isWaitingForSendDataResponse() {
        return sendDataRunnable != null;
    }

    // endregion
}
