package com.msiworldwide.environmentalsensor.AzureServicesEs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.OkHttpClientFactory;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.msiworldwide.environmentalsensor.Data.DatabaseHelper;
import com.msiworldwide.environmentalsensor.R;
import com.squareup.okhttp.OkHttpClient;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SynDataToAzure extends AppCompatActivity {

    DatabaseHelper db;

    private String mMobileBackendUrl = "https://environmentalsensor.azurewebsites.net";
    private MobileServiceClient mClient;
    private int counter = 0;

    Spinner spinnerField, spinnerDate;
    EditText txtCanalName;
    String SelectedField, SelectedDate;

    ArrayAdapter<String> adapterFeild, adapterDate;
    List<String> listField;
    List<String> listDate;

    public static SharedPreferences pref;
    public static String KEY_CN = "canalname";

    private MobileServiceTable<EnvSensorData> mEnvSensorDataTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_syn_data_to_azure);


        db = new DatabaseHelper(getApplicationContext());

        txtCanalName = (EditText) findViewById(R.id.txtCanalName);

        pref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        String st1 = pref.getString(KEY_CN, null);
        if(st1 != null ){ txtCanalName.setText(st1); }

        ///////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////

        txtCanalName = (EditText) findViewById(R.id.txtCanalName);
        spinnerField = (Spinner) findViewById(R.id.spinnerField);
        spinnerDate = (Spinner) findViewById(R.id.spinnerDate);

        //listField = new ArrayList<String>();
        listField =  db.getFieldNames();

        adapterFeild = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, listField);
        adapterFeild.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerField.setAdapter(adapterFeild);

        db.getAllSensor_Data();

        ////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        try {

            mClient = new MobileServiceClient( mMobileBackendUrl, this );

            // Extend timeout from default of 10s to 20s
            mClient.setAndroidHttpClientFactory(new OkHttpClientFactory() {
                @Override
                public OkHttpClient createOkHttpClient() {
                    OkHttpClient client = new OkHttpClient();
                    client.setReadTimeout(20, TimeUnit.SECONDS);
                    client.setWriteTimeout(20, TimeUnit.SECONDS);
                    return client;
                }
            });

            // Get the Mobile Service Table instance to use
            mEnvSensorDataTable = mClient.getTable(EnvSensorData.class);

            // Offline Sync
            //mToDoTable = mClient.getSyncTable("ToDoItem", ToDoItem.class);


        } catch (MalformedURLException e) {
            createAndShowDialog(new Exception("There was an error creating the Mobile Service. Verify the URL"), "Error");
        } catch (Exception e){
            createAndShowDialog(e, "Error");
        }

        spinnerField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.i("LOGG", spinnerField.getItemAtPosition(position).toString());
                SelectedField = spinnerField.getItemAtPosition(position).toString();


                listDate = db.getDates(SelectedField);
                adapterDate = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, listDate);
                adapterDate.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDate.setAdapter(adapterDate);


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerDate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.i("LOGG", spinnerDate.getItemAtPosition(position).toString());
                SelectedDate =  spinnerDate.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Button btnAddd = (Button) findViewById(R.id.btnadd);
        btnAddd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i("LOGG", SelectedDate +" - "+ SelectedField);
                addItemEsd(SelectedField, SelectedDate);
            }
        });


    }


    @Override
    protected void onStop() {
        super.onStop();

        final SharedPreferences.Editor editer = pref.edit();
        editer.putString( KEY_CN, txtCanalName.getText().toString() );
        editer.commit();

    }

    @Override
    protected void onPause() {
        super.onPause();

        final SharedPreferences.Editor editer = pref.edit();
        editer.putString( KEY_CN, txtCanalName.getText().toString() );
        editer.commit();
    }

    public void addItemEsd(String field, String date){

        if (mClient == null) {
            return;
        }

        final DatabaseHelper db;
        db = new DatabaseHelper(getApplicationContext());

        List<EnvSensorData> sensorDatas = new ArrayList<>();
        sensorDatas = db.getAllSensorDataForAzureSync(field, date);

        // Insert the new item
        final List<EnvSensorData> finalSensorDatas = sensorDatas;
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                         EnvSensorData entity;

                        for (int i=0; i<finalSensorDatas.size() ; i++){
                             entity = addEsdInTable(finalSensorDatas.get(i));

                             db.updateStatus( finalSensorDatas.get(i).getFieldname(), finalSensorDatas.get(i).getDate(), finalSensorDatas.get(i).getTime() );

                            counter++;
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SynDataToAzure.this, "( "+ counter +" ) Added Successfuly! ", Toast.LENGTH_LONG).show();
                            }
                        });

                } catch (final Exception e) {
                    createAndShowDialogFromTask(e, "Error");
                }
                return null;
            }


            ProgressDialog pDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                pDialog = new ProgressDialog(SynDataToAzure.this);

                pDialog.setTitle("Syncing Data!");
                pDialog.setMessage("Syncing Data Please wait...");
                pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                pDialog.setCancelable(false);
                pDialog.show();

            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                counter=0;
                pDialog.cancel();
                finish();
                startActivity(getIntent());
            }
        };

        runAsyncTask(task);

    }



    /**
     * Add an item to the Mobile Service Table
     *
     * @param item
     *            The item to Add
     */

    public EnvSensorData addEsdInTable(EnvSensorData item) throws ExecutionException, InterruptedException {
        EnvSensorData entity = mEnvSensorDataTable.insert(item).get();
        return entity;
    }


    /**
     * Creates a dialog and shows it
     *
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialogFromTask(final Exception exception, String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createAndShowDialog(exception, "Error");
            }
        });
    }

    /**
     * Creates a dialog and shows it
     *
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(Exception exception, String title) {
        Throwable ex = exception;
        if(exception.getCause() != null){
            ex = exception.getCause();
        }
        createAndShowDialog(exception, title);
    }


    /**
     * Run an ASync task on the corresponding executor
     * @param task
     * @return
     */
    private AsyncTask<Void, Void, Void> runAsyncTask(AsyncTask<Void, Void, Void> task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            return task.execute();
        }
    }



}
