package com.msiworldwide.environmentalsensor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
//import android.widget.TextView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.common.api.PendingResult;
//import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

//import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.msiworldwide.environmentalsensor.Data.CurrentSelections;
import com.msiworldwide.environmentalsensor.Data.DatabaseHelper;
import com.msiworldwide.environmentalsensor.Data.FieldData;
import com.msiworldwide.environmentalsensor.Data.MeasurementIdentifiers;
import com.msiworldwide.environmentalsensor.Data.SensorData;
import com.msiworldwide.environmentalsensor.ble.BleManager;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Measurement extends AppCompatActivity implements OnMapReadyCallback,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        BleManager.BleManagerListener {

    private GoogleMap mMap;

    private static final long INTERVAL = 1000*5;
    private static final long FASTEST_INTERVAL = 1000;
    Button BtnMeasurement;
    Button BtnVisualize;
    //TextView tvLocation;
    //TextView received_data;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation;
    String mLastUpdateTime;
    SensorData MeasuredData = new SensorData();
    MeasurementIdentifiers measurementIdentifiers = new MeasurementIdentifiers();

    CurrentSelections currentSelections = new CurrentSelections();
    long Field_id;
    String FieldName;
    FieldData fieldData = new FieldData();
    String field_coords_str;
    String[] field_coords_array;
    ArrayList<LatLng> boundary = new ArrayList<>();
    ArrayList<Double> lats = new ArrayList<>();
    ArrayList<Double> lngs = new ArrayList<>();
    LatLng mNortheast;
    LatLng mSouthwest;

    int measurement_id;
    IntentFilter filter = new IntentFilter();
    boolean connected = true;
    BluetoothDevice device;
    String deviceAddress;
    AlertDialog lostcon;
    BluetoothAdapter adapter;
    boolean loaded = false;

    DatabaseHelper db;

    // Service Constants
    private final static String TAG = Measurement.class.getSimpleName();
    public static final String UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_RX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_TX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final int kTxMaxCharacters = 20;

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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Measurement");
        }
        mToolbar.setTitleTextColor(Color.WHITE);

        db = new DatabaseHelper(getApplicationContext());
        vizSensorIncomingData();

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
        //received_data = (TextView) findViewById(R.id.received_data);
        BtnVisualize = (Button) findViewById(R.id.visualize);
        BtnMeasurement = (Button) findViewById(R.id.take_measurement);
        BtnMeasurement.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (mCurrentLocation == null) {
                    AlertDialog.Builder location = new AlertDialog.Builder(Measurement.this);
                    location.setTitle("No Location Data")
                            .setMessage("Please Check if GPS is enabled")
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    AlertDialog alert = location.create();
                    alert.show();
                } else {
                    double lat = mCurrentLocation.getLatitude();
                    double lng = mCurrentLocation.getLongitude();
                    String time = DateFormat.getTimeInstance().format(new Date());
                    String date = DateFormat.getDateInstance().format(new Date());
                    measurementIdentifiers.setDate(date);
                    MeasuredData.setlat(lat);
                    MeasuredData.setlng(lng);
                    MeasuredData.setTime(time);
                    MeasuredData.setDate(date);
                    String text = "M";
                    sendData(text);

                    Log.v("test", lat +","+lng);
                   /* DatabaseHelper db = new DatabaseHelper(getApplicationContext());
                    db.getSensorData(1);*/
                }
            }
        });

        currentSelections = db.getCurrentSelections();
        Field_id = currentSelections.getField_id();
        fieldData = db.getFieldData(Field_id);
        FieldName = fieldData.getFieldId();
        measurementIdentifiers.setFieldId(FieldName);
        MeasuredData.setFieldId(fieldData.getFieldId());
        field_coords_str = fieldData.getCoordinates();
        field_coords_array = field_coords_str.split(",");
        for (int i=0;i<field_coords_array.length/2;i++) {
            double lat = Double.valueOf(field_coords_array[2*i]);
            lats.add(lat);
            double lng = Double.valueOf(field_coords_array[2*i+1]);
            lngs.add(lng);
            LatLng point = new LatLng(lat,lng);
            boundary.add(point);
        }

        adapter = BluetoothAdapter.getDefaultAdapter();
        device = mBleManager.getConnectedDevice();
        if (device == null) {
            AlertDialog.Builder location = new AlertDialog.Builder(Measurement.this);
            location.setTitle("Device Connection Issue")
                    .setMessage("Please try again")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            onBackPressed();
                        }
                    });
            AlertDialog alert = location.create();
            alert.show();
        } else {
            deviceAddress = device.getAddress();

            //received_data.setText(String.valueOf(measurement_id));
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            this.registerReceiver(mReceiver, filter);
        }

    }



    public void openVisualizeResults(View view){
        Intent intent = new Intent(this, VisualizeResults.class);
        startActivity(intent);
    }

    public void save_measurement(View view) {
        Toast save = Toast.makeText(getApplicationContext(), "Measurements Saved", Toast.LENGTH_SHORT);
        save.show();
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
    public void onBackPressed() {
        super.onBackPressed();
        db.deleteCurrentSelections();
        try {
            this.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unregister: " + e.toString());
        }
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
        try {
            mMap.setMyLocationEnabled(true);
        }
        catch(SecurityException e){
            Log.e(TAG, "Location Exception");
        }
        if (!boundary.isEmpty()) {
            PolygonOptions opts = new PolygonOptions();
            for (LatLng location : boundary) {
                opts.add(location);
            }
            //Polygon polygon = mMap.addPolygon(opts.strokeColor(Color.RED));
            mMap.addPolygon(opts.strokeColor(Color.RED));
            Collections.sort(lats);
            Collections.sort(lngs);
            mNortheast = new LatLng(lats.get(lats.size() - 1), lngs.get(lngs.size() - 1));
            mSouthwest = new LatLng(lats.get(0), lngs.get(0));
            LatLngBounds field_bound = new LatLngBounds(mSouthwest,mNortheast);
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(field_bound,0));
        }
        if (!loaded) {
            final String date = DateFormat.getDateInstance().format(new Date());
            if (db.checkSensorData(FieldName, date)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Previous Data Exists")
                        .setMessage("Would you like to continue previous measurements?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                List<SensorData> sensorDatas = db.getAllSensorDatabyDate(FieldName, date);
                                measurement_id = sensorDatas.get(sensorDatas.size() - 1).getMeasurement_number_id();
                                measurementIdentifiers.setMeasurementNumberId(measurement_id);
                                MeasuredData.setMeasurementNumberId(measurement_id);

                                for (int i = 0; i < sensorDatas.size(); i++) {
                                    SensorData loadedData = sensorDatas.get(i);
                                    if (loadedData.getMeasurement_number_id() == measurement_id) {
                                        LatLng loc = new LatLng(loadedData.getLat(), loadedData.getLng());
                                        mMap.addMarker(new MarkerOptions().position(loc).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                                .title("M:" + loadedData.getmoisture() + ", S:" + loadedData.getsunlight()
                                                        + ", T:" + loadedData.gettemperature() + ", H:" + loadedData.gethumidity()));
                                    }
                                }
                                loaded = true;
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                measurement_id = db.getNewIdentifier();
                                measurementIdentifiers.setMeasurementNumberId(measurement_id);
                                MeasuredData.setMeasurementNumberId(measurement_id);
                                loaded = true;
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();

            } else {
                measurement_id = db.getNewIdentifier();
                measurementIdentifiers.setMeasurementNumberId(measurement_id);
                MeasuredData.setMeasurementNumberId(measurement_id);
                loaded = true;
            }
        }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
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
        registerReceiver(mReceiver,filter);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
       // mMap.setMaxZoomPreference(18.0f);
       // mMap.moveCamera(CameraUpdateFactory.zoomTo(18.0f));
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    }

    // region Send Data to UART
    protected void sendData(String text) {
       // Log.i(TAG, ".... Text .... : "+ text);
        final byte[] value = text.getBytes(Charset.forName("UTF-8"));
        Log.i(TAG, ".... Value .... : "+ value);
        sendData(value);
    }


    protected void sendData(byte[] data) {
        if (mUartService != null) {
            // Split the value into chunks (UART service has a maximum number of characters that can be written )
            for (int i = 0; i < data.length; i += kTxMaxCharacters) {
                final byte[] chunk = Arrays.copyOfRange(data, i, Math.min(i + kTxMaxCharacters, data.length));
                mBleManager.writeService(mUartService, UUID_TX, chunk);
            }
            if (connected) {
                //received_data.setText("Measuring...");
                Toast measuring = Toast.makeText(getApplicationContext(), "Measuring...", Toast.LENGTH_LONG);
                measuring.show();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BluetoothGattCharacteristic characteristic = mUartService.getCharacteristic(UUID.fromString(UUID_RX));
                        byte[] dataread = characteristic.getValue();
                        if (dataread != null) {
                            String data = new String(dataread, Charset.forName("UTF-8"));
                            String[] values = data.split(",");
                            try {
                                int moisture = Integer.parseInt(values[0]);
                                int sunlight = Integer.parseInt(values[1]);
                                double temp = Double.parseDouble(values[2]);
                                double humid = Double.parseDouble(values[3]);
                                MeasuredData.setmoisture(moisture);
                                MeasuredData.setsunlight(sunlight);
                                MeasuredData.settemperature(temp);
                                MeasuredData.sethumidity(humid);
                                db.createMeasurementId(measurementIdentifiers);
                                db.createSensorData(MeasuredData);
                                Toast complete = Toast.makeText(getApplicationContext(), "Measurement Complete", Toast.LENGTH_SHORT);
                                complete.show();
                                //received_data.setText("M:" + values[0] + "S:" + values[1] + "T:" + values[2] + "H:" + values[3]);
                                //received_data.setText("Measurement Complete");
                                LatLng loc = new LatLng(MeasuredData.getLat(), MeasuredData.getLng());
                                mMap.addMarker(new MarkerOptions().position(loc).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                        .title("M:" + values[0] + ", S:" + values[1] + ", T:" + values[2] + ", H:" + values[3]));
                            } catch (IndexOutOfBoundsException | NumberFormatException ex) {
                                //received_data.setText("No Data");
                                AlertDialog.Builder location = new AlertDialog.Builder(Measurement.this);
                                location.setTitle("Data Measurement Failed")
                                        .setMessage("Please try measurement again")
                                        .setCancelable(false)
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                            }
                                        });
                                AlertDialog alert = location.create();
                                alert.show();
                            }
                        } else {
                            AlertDialog.Builder location = new AlertDialog.Builder(Measurement.this);
                            location.setTitle("Data Read Failed")
                                    .setMessage("Please try measurement again")
                                    .setCancelable(false)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    });
                            AlertDialog alert = location.create();
                            alert.show();
                        }
                    }
                }, 5000);
            }else {
                AlertDialog.Builder location = new AlertDialog.Builder(Measurement.this);
                location.setTitle("Device Disconnected")
                        .setMessage("Please connect to a sensor")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                AlertDialog alert = location.create();
                alert.show();
            }
        } else {
            Log.w(TAG, "Uart Service not discovered. Unable to send data");
            AlertDialog.Builder location = new AlertDialog.Builder(this);
            location.setTitle("No Bluetooth Connection")
                    .setMessage("Please connect to a sensor")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            AlertDialog alert = location.create();
            alert.show();
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
    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor){
    }

    @Override
    public void onReadRemoteRssi(int rssi) {

    }

    protected void enableRxNotifications() {
        mBleManager.enableNotification(mUartService, UUID_RX, true);
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (!connected) {
                    BluetoothDevice newdevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (newdevice != null && deviceAddress != null) {
                        // Check if the found device is one we had comm with
                        if (newdevice.getAddress().equals(deviceAddress))
                            connect(deviceAddress);
                    }
                }
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
                lostcon.cancel();
                adapter.cancelDiscovery();
                connected = true;
                AlertDialog.Builder connection = new AlertDialog.Builder(Measurement.this);
                connection.setTitle("Bluetooth Connection Recovered")
                        .setMessage("Continue with Measurements")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                AlertDialog alert = connection.create();
                alert.show();
            }
            /*else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Done searching
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect
            }*/
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected
                connected = false;
                AlertDialog.Builder lostconnection = new AlertDialog.Builder(Measurement.this);
                lostconnection.setTitle("Bluetooth Connection Lost")
                        .setMessage("Attempting to reconnect")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                lostcon = lostconnection.create();
                lostcon.show();
                adapter.startDiscovery();

                //connect(device);`
            }
        }
    };

    private void connect(String deviceAddress) {
        //boolean isConnecting = mBleManager.connect(this, deviceAddress);
        mBleManager.connect(this, deviceAddress);
    }


    private Handler handler = new Handler();
    TextView tvLtLng, tvTime, tvSoilMoisture, tvHumidity, tvSunLight, tvTemp;

/*protected void vizSensorIncomingData(String text) {
    Log.i(TAG, ".... Text .... : "+ text);
    final byte[] value = text.getBytes(Charset.forName("UTF-8"));
    Log.i(TAG, ".... Value .... : "+ value);
    vizSensorIncomingData(value);
}*/

protected String vizSensorIncomingData(String text) {
    String data1="";
    //String text = "M";
    final byte[] data = text.getBytes(Charset.forName("UTF-8"));

    if (mUartService != null) {
        // Split the value into chunks (UART service has a maximum number of characters that can be written )
        for (int i = 0; i < data.length; i += kTxMaxCharacters) {
            final byte[] chunk = Arrays.copyOfRange(data, i, Math.min(i + kTxMaxCharacters, data.length));
            mBleManager.writeService(mUartService, UUID_TX, chunk);
        }

        BluetoothGattCharacteristic characteristic = mUartService.getCharacteristic(UUID.fromString(UUID_RX));
        byte[] dataread = characteristic.getValue();

        if (dataread != null) {
            data1 = new String(dataread, Charset.forName("UTF-8"));
            String[] values = data1.split(",");
            try {
                int moisture = Integer.parseInt(values[0]);
                int sunlight = Integer.parseInt(values[1]);
                double temp = Double.parseDouble(values[2]);
                double humid = Double.parseDouble(values[3]);

                Log.i(TAG, "moisture : " + moisture + " sunlight : " + sunlight + " temp : " + temp + " humid : " + humid);

                double lat = mCurrentLocation.getLatitude();
                double lng = mCurrentLocation.getLongitude();
                String time = DateFormat.getTimeInstance().format(new Date());
                String date = DateFormat.getDateInstance().format(new Date());

                tvLtLng.setText(lat +", "+lng);
                tvTime.setText(time +" "+date);
                tvSoilMoisture.setText(moisture+"");
                tvHumidity.setText(humid+"");
                tvSunLight.setText(sunlight+"");
                tvTemp.setText(temp+"");

                Log.i(TAG, "Lat : " + lat + " Lng : " + lng + " Time : " + time + " Date : " + date);


            } catch (Exception e) {

                Log.i(TAG, "Error vizSensorIncomingData : " + e);

            }


        }

    }
    return  data1;
}

public void vizSensorIncomingData(){

            tvLtLng = (TextView) findViewById(R.id.tvLatLng);
            tvTime = (TextView) findViewById(R.id.tvTime);
            tvSoilMoisture = (TextView) findViewById(R.id.tvSoilMoisture);
            tvHumidity = (TextView) findViewById(R.id.tvHumidity);
            tvSunLight = (TextView) findViewById(R.id.tvSunlight);
            tvTemp = (TextView) findViewById(R.id.tvTemp);

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    //Log.i(TAG, "handler...");
                    String st = vizSensorIncomingData("M");
                    Log.i(TAG, "St : "+ st);

                    vizSensorIncomingData();
                }
            }, 500);
}



}