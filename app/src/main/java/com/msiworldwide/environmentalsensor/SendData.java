package com.msiworldwide.environmentalsensor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.msiworldwide.environmentalsensor.Data.DatabaseHelper;
import com.msiworldwide.environmentalsensor.Data.SensorData;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class SendData extends AppCompatActivity {

    private RadioGroup radioGroupFileFormat;
    private RadioButton radioSexButton;
    private int STORAGE_PERMISSION_CODE=23;
    Button btnExportData;
    EditText txtEmail, txtTo;

    public static final String TAG = SendData.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_data);

        btnExportData = (Button) findViewById(R.id.btnExpData);
        txtEmail = (EditText) findViewById(R.id.txtEmail);
        txtTo = (EditText) findViewById(R.id.txt_to);



        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, STORAGE_PERMISSION_CODE);

        }

//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////
        btnExportData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                DatabaseHelper db;
                db = new DatabaseHelper(getApplicationContext());

                String dataFromDatabase =  "";
                dataFromDatabase =  db.getAllSensor_Data();

               // Log.i("SensorData : ", ""+dataFromDatabase.toString());

                radioGroupFileFormat =(RadioGroup)findViewById(R.id.radioGroupFileFormat);
                int selectedId = radioGroupFileFormat.getCheckedRadioButtonId();
                radioSexButton = (RadioButton)findViewById(selectedId);
                //  Toast.makeText(SendData.this, radioSexButton.getText(), Toast.LENGTH_SHORT).show();


                String to =  txtEmail.getText().toString(); //"qkahmadzai2016@gmail.com"; //
                String subject =  txtTo.getText().toString(); //"eSensor"; //
                String message = "eSensor Data";


                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("plain/text");
                File data = null;

                //  int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

                if (ContextCompat.checkSelfPermission(SendData.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(SendData.this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, STORAGE_PERMISSION_CODE);

                }else {




                    if (radioSexButton.getText().toString().contains("Both")) {
                        Log.i("SensorData : ", "radioSexButton Both");
                        try {
                            Date dateVal = new Date();
                            String filename = dateVal.toString();
                            data = File.createTempFile(filename, ".csv", Environment.getExternalStorageDirectory());
                            GenerateCsv.generateCsvFile(data, "MeasurementId,FieldId,Date,Time,Latitude,Longitude,Moisture,Sunlight,Temperature,Humidity" + dataFromDatabase);

                            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(data));
                            i.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
                            i.putExtra(Intent.EXTRA_SUBJECT, subject+"");
                            i.putExtra(Intent.EXTRA_TEXT, "" + dataFromDatabase);
                            startActivity(Intent.createChooser(i, "E-mail"));
                            //Toast.makeText(SendData.this, "Data Sent", Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                        }

                    } else if (radioSexButton.getText().toString().contains(".CSV File")) {
                        Log.i("SensorData : ", "radioSexButton CSV");
                        try {
                            Date dateVal = new Date();
                            String filename = dateVal.toString();
                            data = File.createTempFile(filename, ".csv", Environment.getExternalStorageDirectory());
                            GenerateCsv.generateCsvFile(data, "MeasurementId,FieldName,Date,Time,Latitude,Longitude,Moisture % ,Sunlight(watts/m2),Temperature (C),Humidity %" + dataFromDatabase);

                            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(data));
                            i.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
                            i.putExtra(Intent.EXTRA_SUBJECT, subject+"");
                            i.putExtra(Intent.EXTRA_TEXT, "");
                            startActivity(Intent.createChooser(i, "E-mail"));
                            //Toast.makeText(SendData.this, "Data Sent", Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                        }


                    } else {
                        Log.i("SensorData : ", "radioSexButton text");
                        try {

                            i.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
                            i.putExtra(Intent.EXTRA_SUBJECT, subject+"");
                            i.putExtra(Intent.EXTRA_TEXT, "" + dataFromDatabase);
                            startActivity(Intent.createChooser(i, "E-mail"));
                            //Toast.makeText(SendData.this, "Data Sent", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                }


            }
        });

//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////


    }



}
