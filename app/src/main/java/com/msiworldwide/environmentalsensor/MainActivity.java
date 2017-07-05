package com.msiworldwide.environmentalsensor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.msiworldwide.environmentalsensor.Data.CurrentSelections;
import com.msiworldwide.environmentalsensor.Data.DatabaseHelper;
import com.msiworldwide.environmentalsensor.Data.FieldData;
import com.msiworldwide.environmentalsensor.Data.WaterSourceData;
import com.msiworldwide.environmentalsensor.ble.BleManager;
import com.msiworldwide.environmentalsensor.ble.BleDevicesScanner;
import com.msiworldwide.environmentalsensor.ble.BleUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener,
        BleManager.BleManagerListener, BleUtils.ResetBluetoothAdapterListener {

    // Constants
    private final static String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    // Activity request codes (used for onActivityResult)
    private static final int kActivityRequestCode_EnableBluetooth = 1;
    private static final int kActivityRequestCode_Settings = 2;
    private static final int kActivityRequestCode_ConnectedActivity = 3;
    private static final int NewFieldRequestCode = 4;
    private static final int NewWaterSourceRequestCode = 5;
    private static final int CropSelectRequestCode = 6;

    // Data
    private BleManager mBleManager;
    private boolean mIsScanPaused = true;
    private BleDevicesScanner mScanner;

    private ArrayList<BluetoothDeviceData> mScannedDevices;
    private BluetoothDeviceData mSelectedDeviceData;
    private Class<?> mComponentToStartWhenConnected;
    private boolean mShouldEnableWifiOnQuit = false;
    private String mLatestCheckedDeviceAddress;

    private DataFragment mRetainedDataFragment;


    private Spinner field_spinner;
    private ArrayList<String> fieldString = new ArrayList<>(Arrays.asList("Field Boundary","New Field"));
    private Spinner water_source_spinner;
    private ArrayList<String> waterString = new ArrayList<>(Arrays.asList("Water Source","New Source"));
    private Spinner crop_spinner;
    private ArrayList<String> cropString = new ArrayList<>(Arrays.asList("Crop","Database"));
    private Spinner devices_spinner;
    private ArrayList<String> devString = new ArrayList<>(Arrays.asList("Select Device","Refresh"));

    DatabaseHelper db;

    List<FieldData> Fields;
    List<WaterSourceData> Water;

    CurrentSelections currentSelections = new CurrentSelections();
    TextView connectionStatus;
    boolean connected = false;
    IntentFilter filter = new IntentFilter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Home");
        mToolbar.setTitleTextColor(Color.WHITE);

        // Init variables
        mBleManager = BleManager.getInstance(this);
        restoreRetainedDataFragment();

        db = new DatabaseHelper(getApplicationContext());

        db.deleteCurrentSelections();

        // Setup when activity is created for the first time
        if (savedInstanceState == null) {
            // Read preferences
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean autoResetBluetoothOnStart = sharedPreferences.getBoolean("pref_resetble", false);
            boolean disableWifi = sharedPreferences.getBoolean("pref_disableWifi", false);

            // Turn off wifi
            if (disableWifi) {
                final boolean isWifiEnabled = BleUtils.isWifiEnabled(this);
                if (isWifiEnabled) {
                    BleUtils.enableWifi(false, this);
                    mShouldEnableWifiOnQuit = true;
                }
            }

            // Check if bluetooth adapter is available
            final boolean wasBluetoothEnabled = manageBluetoothAvailability();
            final boolean areLocationServicesReadyForScanning = manageLocationServiceAvailabilityForScanning();

            // Reset bluetooth
            if (autoResetBluetoothOnStart && wasBluetoothEnabled && areLocationServicesReadyForScanning) {
                BleUtils.resetBluetoothAdapter(this, this);
            }
        }

        // Request Bluetooth scanning permissions
        requestLocationPermissionIfNeeded();

        // Spinner Drop Down for Field Selection
        field_spinner = (Spinner)findViewById(R.id.Field_Select);

        Fields = db.getAllFieldData();
        for (int i = 0; i < Fields.size(); i++) {
            FieldData field = Fields.get(i);
            fieldString.add(field.getFieldId());
        }
        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_item,fieldString);

        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        field_spinner.setAdapter(fieldAdapter);
        field_spinner.setOnItemSelectedListener(this);

        // Spinner Drop Down for Water Source Selection
        water_source_spinner = (Spinner)findViewById(R.id.Water_Source_Select);

        Water = db.getAllWaterSourceData();
        for (int i = 0; i < Water.size(); i++) {
            WaterSourceData water = Water.get(i);
            waterString.add(water.getWaterSourceId());
        }
        ArrayAdapter<String> waterAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_item,waterString);

        waterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        water_source_spinner.setAdapter(waterAdapter);
        water_source_spinner.setOnItemSelectedListener(this);

        // Spinner Drop Down for Crop Selection
        crop_spinner = (Spinner)findViewById(R.id.Crop_Select);
        ArrayAdapter<String> cropAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_item,cropString);

        cropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        crop_spinner.setAdapter(cropAdapter);
        crop_spinner.setOnItemSelectedListener(this);


        // Spinner Drop Down for Device Selection
        devices_spinner = (Spinner)findViewById(R.id.Device_Select);
        ArrayAdapter<String> devadapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_item,devString);

        devadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        devices_spinner.setAdapter(devadapter);
        devices_spinner.setOnItemSelectedListener(this);

        connectionStatus = (TextView) findViewById(R.id.connection);

        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);
        }

    public void openMeasurementLocations(View view){

        if (currentSelections.getField_id() !=0) {
            db.createCurrentSelections(currentSelections);
            //Intent intent = new Intent(this, MeasurementLocations.class);
            Intent intent = new Intent(this, Measurement.class);
            startActivity(intent);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No Field Selected")
                    .setMessage("Please Select a Field")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    public void onItemSelected(AdapterView<?> parent, View v, int position, long id){

        switch (parent.getId()) {
            case R.id.Field_Select:
                if (position == 1) {
                    Intent intent = new Intent(this, NewFieldBoundary.class);
                    startActivityForResult(intent, NewFieldRequestCode);
                } else if (position > 1) {
                    String f_id = String.valueOf(position - 1);
                    long field_id = Long.parseLong(f_id);
                    currentSelections.setField_id(field_id);
                }
                break;
            case R.id.Water_Source_Select:
                if (position == 1) {
                    Intent intent = new Intent(this, NewWaterSource.class);
                    startActivityForResult(intent, NewWaterSourceRequestCode);
                } else if (position > 1) {
                    String w_id = String.valueOf(position - 1);
                    long water_id = Long.parseLong(w_id);
                    currentSelections.setWater_id(water_id);
                }
                break;
            case R.id.Crop_Select:
                if (position == 1) {
                    Intent intent = new Intent(this, CropDatabase.class);
                    startActivityForResult(intent, CropSelectRequestCode);
                } else if(position > 1) {

                }
                break;
            case R.id.Device_Select:
                if (position == 1) {
                    //mScannedDevices.clear();

                    startScan(null);
                    Toast scanning = Toast.makeText(getApplicationContext(), "Scanning...", Toast.LENGTH_SHORT);
                    scanning.show();
                    // devices list
                    devString.clear();
                    devString.add("Select Device");
                    devString.add("Refresh");

                    if (mScannedDevices.size() > 0) {
                        for (int i = 0; i < mScannedDevices.size(); i++) {
                            mSelectedDeviceData = mScannedDevices.get(i);
                            devString.add(mSelectedDeviceData.getNiceName());
                        }
                    }

                    ArrayAdapter<String> devadapter = new ArrayAdapter<>(MainActivity.this,
                            android.R.layout.simple_spinner_item, devString);

                    devadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    devices_spinner.setAdapter(devadapter);
                    devices_spinner.setOnItemSelectedListener(this);
                } else if (position > 1) {
                    stopScanning();
                    int index = position - 2;
                    if (mScannedDevices.size() > 0) {
                        mSelectedDeviceData = mScannedDevices.get(index);
                        BluetoothDevice device = mSelectedDeviceData.device;
                        mBleManager.setBleListener(MainActivity.this);
                        connect(device);

                    }

                }
                break;

        }

     }

     public void onNothingSelected(AdapterView<?> parent){

     }

    @Override
    public void onResume() {
        super.onResume();

        // Set listener
        mBleManager.setBleListener(this);

        // Autostart scan
        autostartScan();
        registerReceiver(mReceiver,filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onDestroy() {
        db.deleteCurrentSelections();
        db.close();
        super.onDestroy();
    }

    private void autostartScan() {
        if (BleUtils.getBleStatus(this) == BleUtils.STATUS_BLE_ENABLED) {
            // If was connected, disconnect
            mBleManager.disconnect();

            // Force restart scanning
            if (mScannedDevices != null) {      // Fixed a weird bug when resuming the app (this was null on very rare occasions even if it should not be)
                mScannedDevices.clear();
            }
            startScan(null);
        }
    }

    // region BleManagerListener
    @Override
    public void onConnected() {
    }

    @Override
    public void onConnecting() {
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "MainActivity onDisconnected");
        //showConnectionStatus(false);
        /*connectionStatus.setText("No Bluetooth Connection");
        connected = false;*/
    }

    private void connect(BluetoothDevice device) {
        boolean isConnecting = mBleManager.connect(this, device.getAddress());
        /*if (isConnecting) {
            connectionStatus.setText("Connected");
            connected = true;
        }*/
    }

    @Override
    public void onServicesDiscovered(){

    }


    private boolean manageBluetoothAvailability() {
        boolean isEnabled = true;

        // Check Bluetooth HW status
        //int errorMessageId = 0;
        final int bleStatus = BleUtils.getBleStatus(getBaseContext());
        switch (bleStatus) {
            case BleUtils.STATUS_BLE_NOT_AVAILABLE:
                //errorMessageId = R.string.dialog_error_no_ble;
                isEnabled = false;
                break;
            case BleUtils.STATUS_BLUETOOTH_NOT_AVAILABLE: {
                //errorMessageId = R.string.dialog_error_no_bluetooth;
                isEnabled = false;      // it was already off
                break;
            }
            case BleUtils.STATUS_BLUETOOTH_DISABLED: {
                isEnabled = false;      // it was already off
                // if no enabled, launch settings dialog to enable it (user should always be prompted before automatically enabling bluetooth)
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, kActivityRequestCode_EnableBluetooth);
                // execution will continue at onActivityResult()
                break;
            }
        }
        return isEnabled;
    }

    private boolean manageLocationServiceAvailabilityForScanning() {

        boolean areLocationServiceReady = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {        // Location services are only needed to be enabled from Android 6.0
            int locationMode = Settings.Secure.LOCATION_MODE_OFF;
            try {
                locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            areLocationServiceReady = locationMode != Settings.Secure.LOCATION_MODE_OFF;

            if (!areLocationServiceReady) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                AlertDialog dialog = builder.setMessage(R.string.dialog_error_nolocationservices_requiredforscan_marshmallow)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                DialogUtils.keepDialogOnOrientationChanges(dialog);
            }
        }

        return areLocationServiceReady;
    }

    // region Permissions
    @TargetApi(Build.VERSION_CODES.M)
    private void requestLocationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can use the GPS");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    // region ResetBluetoothAdapterListener
    @Override
    public void resetBluetoothCompleted() {
        Log.d(TAG, "Reset completed -> Resume scanning");
        resumeScanning();
    }
    // endregion

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == kActivityRequestCode_ConnectedActivity) {
            if (resultCode < 0) {
                Toast.makeText(this, R.string.scan_unexpecteddisconnect, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == kActivityRequestCode_EnableBluetooth) {
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth was enabled, resume scanning
                resumeScanning();
            } else if (resultCode == Activity.RESULT_CANCELED) {
 /*               AlertDialog.Builder builder = new AlertDialog.Builder(this);
                AlertDialog dialog = builder.setMessage(R.string.dialog_error_no_bluetooth)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                DialogUtils.keepDialogOnOrientationChanges(dialog);*/

            }
        } else if (requestCode == kActivityRequestCode_Settings) {
        } else if (requestCode == NewFieldRequestCode) {
            if(resultCode == Activity.RESULT_OK) {
                String result = data.getStringExtra("result");
                long field_id = Long.parseLong(result);
                currentSelections.setField_id(field_id);
                int count = Integer.parseInt(result);
                Fields = db.getAllFieldData();
                FieldData field = Fields.get(count - 1);
                String selectedname = field.getFieldId();
                if (!fieldString.contains(selectedname)) {
                    fieldString.add(selectedname);
                }
                ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_item, fieldString);

                fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                field_spinner.setAdapter(fieldAdapter);
                field_spinner.setOnItemSelectedListener(this);
                field_spinner.setSelection(fieldAdapter.getPosition(selectedname));
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_item,fieldString);

                fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                field_spinner.setAdapter(fieldAdapter);
                field_spinner.setOnItemSelectedListener(this);
            }
        } else if (requestCode == NewWaterSourceRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                String result = data.getStringExtra("result");
                long water_id = Long.parseLong(result);
                currentSelections.setWater_id(water_id);
                int count = Integer.parseInt(result);
                Water = db.getAllWaterSourceData();
                WaterSourceData water = Water.get(count - 1);
                String selectedname = water.getWaterSourceId();
                if(!waterString.contains(selectedname)) {
                    waterString.add(selectedname);
                }
                ArrayAdapter<String> waterAdapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_item,waterString);

                waterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                water_source_spinner.setAdapter(waterAdapter);
                water_source_spinner.setOnItemSelectedListener(this);
                water_source_spinner.setSelection(waterAdapter.getPosition(selectedname));
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                ArrayAdapter<String> waterAdapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_item,waterString);

                waterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                water_source_spinner.setAdapter(waterAdapter);
                water_source_spinner.setOnItemSelectedListener(this);
            }
        } else if (requestCode == CropSelectRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                String selectedCrop = data.getStringExtra("result");
                currentSelections.setCrop_id(selectedCrop);
                if(!cropString.contains(selectedCrop)) {
                    cropString.add(selectedCrop);
                }
                ArrayAdapter<String> cropAdapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_item,cropString);

                cropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                crop_spinner.setAdapter(cropAdapter);
                crop_spinner.setOnItemSelectedListener(this);
                crop_spinner.setSelection(cropAdapter.getPosition(selectedCrop));
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                ArrayAdapter<String> cropAdapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_item,cropString);

                cropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                crop_spinner.setAdapter(cropAdapter);
                crop_spinner.setOnItemSelectedListener(this);
            }
        }
    }

    private void resumeScanning() {
        if (mIsScanPaused) {
            startScan(null);
            mIsScanPaused = mScanner == null;
        }
    }

    // region Scan
    private void startScan(final UUID[] servicesToScan) {
        Log.d(TAG, "startScan");

        // Stop current scanning (if needed)
        stopScanning();

        // Configure scanning
        BluetoothAdapter bluetoothAdapter = BleUtils.getBluetoothAdapter(getApplicationContext());
        if (BleUtils.getBleStatus(this) != BleUtils.STATUS_BLE_ENABLED) {
            Log.w(TAG, "startScan: BluetoothAdapter not initialized or unspecified address.");
        } else {
            mScanner = new BleDevicesScanner(bluetoothAdapter, servicesToScan, new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    //final String deviceName = device.getName();
                    //Log.d(TAG, "Discovered device: " + (deviceName != null ? deviceName : "<unknown>"));

                    BluetoothDeviceData previouslyScannedDeviceData = null;
                    if (mScannedDevices == null)
                        mScannedDevices = new ArrayList<>();       // Safeguard

                    // Check that the device was not previously found
                    for (BluetoothDeviceData deviceData : mScannedDevices) {
                        if (deviceData.device.getAddress().equals(device.getAddress())) {
                            previouslyScannedDeviceData = deviceData;
                            break;
                        }
                    }

                    BluetoothDeviceData deviceData;
                    if (previouslyScannedDeviceData == null) {
                        // Add it to the mScannedDevice list
                        deviceData = new BluetoothDeviceData();
                        mScannedDevices.add(deviceData);
                    } else {
                        deviceData = previouslyScannedDeviceData;
                    }

                    deviceData.device = device;
                    deviceData.rssi = rssi;
                    deviceData.scanRecord = scanRecord;
                    decodeScanRecords(deviceData);

                }
            });

            // Start scanning
            mScanner.start();
        }
    }

    private void stopScanning() {
        // Stop scanning
        if (mScanner != null) {
            mScanner.stop();
            mScanner = null;
        }
    }

    private void decodeScanRecords(BluetoothDeviceData deviceData) {
        // based on http://stackoverflow.com/questions/24003777/read-advertisement-packet-in-android
        final byte[] scanRecord = deviceData.scanRecord;

        ArrayList<UUID> uuids = new ArrayList<>();
        byte[] advertisedData = Arrays.copyOf(scanRecord, scanRecord.length);
        int offset = 0;
        deviceData.type = BluetoothDeviceData.kType_Unknown;

        // Check if is an iBeacon ( 0x02, 0x0x1, a flag byte, 0x1A, 0xFF, manufacturer (2bytes), 0x02, 0x15)
        final boolean isBeacon = advertisedData[0] == 0x02 && advertisedData[1] == 0x01 && advertisedData[3] == 0x1A && advertisedData[4] == (byte) 0xFF && advertisedData[7] == 0x02 && advertisedData[8] == 0x15;

        // Check if is an URIBeacon
        final byte[] kUriBeaconPrefix = {0x03, 0x03, (byte) 0xD8, (byte) 0xFE};
        final boolean isUriBeacon = Arrays.equals(Arrays.copyOf(scanRecord, kUriBeaconPrefix.length), kUriBeaconPrefix) && advertisedData[5] == 0x16 && advertisedData[6] == kUriBeaconPrefix[2] && advertisedData[7] == kUriBeaconPrefix[3];

        if (isBeacon) {
            deviceData.type = BluetoothDeviceData.kType_Beacon;

            // Read uuid
            offset = 9;
            UUID uuid = BleUtils.getUuidFromByteArrayBigEndian(Arrays.copyOfRange(scanRecord, offset, offset + 16));
            uuids.add(uuid);
            offset += 16;

            // Skip major minor
            offset += 2 * 2;   // major, minor

            // Read txpower
            final int txPower = advertisedData[offset++];
            deviceData.txPower = txPower;
        } else if (isUriBeacon) {
            deviceData.type = BluetoothDeviceData.kType_UriBeacon;

            // Read txpower
            final int txPower = advertisedData[9];
            deviceData.txPower = txPower;
        } else {
            // Read standard advertising packet
            while (offset < advertisedData.length - 2) {
                // Length
                int len = advertisedData[offset++];
                if (len == 0) break;

                // Type
                int type = advertisedData[offset++];
                if (type == 0) break;

                // Data
//            Log.d(TAG, "record -> lenght: " + length + " type:" + type + " data" + data);

                switch (type) {
                    case 0x02:          // Partial list of 16-bit UUIDs
                    case 0x03: {        // Complete list of 16-bit UUIDs
                        while (len > 1) {
                            int uuid16 = advertisedData[offset++] & 0xFF;
                            uuid16 |= (advertisedData[offset++] << 8);
                            len -= 2;
                            uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                        }
                        break;
                    }

                    case 0x06:          // Partial list of 128-bit UUIDs
                    case 0x07: {        // Complete list of 128-bit UUIDs
                        while (len >= 16) {
                            try {
                                // Wrap the advertised bits and order them.
                                UUID uuid = BleUtils.getUuidFromByteArraLittleEndian(Arrays.copyOfRange(advertisedData, offset, offset + 16));
                                uuids.add(uuid);

                            } catch (IndexOutOfBoundsException e) {
                                Log.e(TAG, "BlueToothDeviceFilter.parseUUID: " + e.toString());
                            } finally {
                                // Move the offset to read the next uuid.
                                offset += 16;
                                len -= 16;
                            }
                        }
                        break;
                    }

                    case 0x09: {
                        byte[] nameBytes = new byte[len - 1];
                        for (int i = 0; i < len - 1; i++) {
                            nameBytes[i] = advertisedData[offset++];
                        }

                        String name = null;
                        try {
                            name = new String(nameBytes, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        deviceData.advertisedName = name;
                        break;
                    }

                    case 0x0A: {        // TX Power
                        final int txPower = advertisedData[offset++];
                        deviceData.txPower = txPower;
                        break;
                    }

                    default: {
                        offset += (len - 1);
                        break;
                    }
                }
            }

            // Check if Uart is contained in the uuids
            boolean isUart = false;
            for (UUID uuid : uuids) {
                if (uuid.toString().equalsIgnoreCase(Measurement.UUID_SERVICE)) {
                    isUart = true;
                    break;
                }
            }
            if (isUart) {
                deviceData.type = BluetoothDeviceData.kType_Uart;
            }
        }

        deviceData.uuids = uuids;
    }

    @Override
    public void onDataAvailable(BluetoothGattCharacteristic characteristic) {
    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor) {
    }

    @Override
    public void onReadRemoteRssi(int rssi) {

    }

    // region Helpers
    private class BluetoothDeviceData {
        BluetoothDevice device;
        private int rssi;
        byte[] scanRecord;
        private String advertisedName;           // Advertised name
        private String cachedNiceName;
        private String cachedName;

        // Decoded scan record (update R.array.scan_devicetypes if this list is modified)
        static final int kType_Unknown = 0;
        static final int kType_Uart = 1;
        static final int kType_Beacon = 2;
        static final int kType_UriBeacon = 3;

        private int type;
        int txPower;
        ArrayList<UUID> uuids;

        String getName() {
            if (cachedName == null) {
                cachedName = device.getName();
                if (cachedName == null) {
                    cachedName = advertisedName;      // Try to get a name (but it seems that if device.getName() is null, this is also null)
                }
            }

            return cachedName;
        }

        String getNiceName() {
            if (cachedNiceName == null) {
                cachedNiceName = getName();
                if (cachedNiceName == null) {
                    cachedNiceName = device.getAddress();
                }
            }

            return cachedNiceName;
        }
    }
    //endregion



    public static class DataFragment extends Fragment {
        private ArrayList<BluetoothDeviceData> mScannedDevices;
        private Class<?> mComponentToStartWhenConnected;
        private boolean mShouldEnableWifiOnQuit;
        private String mLatestCheckedDeviceAddress;
        private BluetoothDeviceData mSelectedDeviceData;
        //private PeripheralList mPeripheralList;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

    }

    private void restoreRetainedDataFragment() {
        // find the retained fragment
        FragmentManager fm = getFragmentManager();
        mRetainedDataFragment = (DataFragment) fm.findFragmentByTag(TAG);

        if (mRetainedDataFragment == null) {
            // Create
            mRetainedDataFragment = new DataFragment();
            fm.beginTransaction().add(mRetainedDataFragment, TAG).commitAllowingStateLoss();        // http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-h

            mScannedDevices = new ArrayList<>();
            // mPeripheralList = new PeripheralList();

        } else {
            // Restore status
            mScannedDevices = mRetainedDataFragment.mScannedDevices;
            mComponentToStartWhenConnected = mRetainedDataFragment.mComponentToStartWhenConnected;
            mShouldEnableWifiOnQuit = mRetainedDataFragment.mShouldEnableWifiOnQuit;
            mLatestCheckedDeviceAddress = mRetainedDataFragment.mLatestCheckedDeviceAddress;
            mSelectedDeviceData = mRetainedDataFragment.mSelectedDeviceData;
            //mPeripheralList = mRetainedDataFragment.mPeripheralList;

        }
    }

    private void saveRetainedDataFragment() {
        mRetainedDataFragment.mScannedDevices = mScannedDevices;
        mRetainedDataFragment.mComponentToStartWhenConnected = mComponentToStartWhenConnected;
        mRetainedDataFragment.mShouldEnableWifiOnQuit = mShouldEnableWifiOnQuit;
        mRetainedDataFragment.mLatestCheckedDeviceAddress = mLatestCheckedDeviceAddress;
        mRetainedDataFragment.mSelectedDeviceData = mSelectedDeviceData;
        //mRetainedDataFragment.mPeripheralList = mPeripheralList;
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Device found
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
                connectionStatus.setText("Connected");
                connected = true;
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Done searching
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected
                connectionStatus.setText("No Bluetooth Connection");
                connected = false;
            }
        }
    };

}



