package com.msiworldwide.environmentalsensor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.msiworldwide.environmentalsensor.Data.Crops;

import java.util.ArrayList;
import java.util.List;

public class CropDatabase extends AppCompatActivity {

    private String SelectedCrop = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_database);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Crop");
        mToolbar.setTitleTextColor(Color.WHITE);

        List<Crops> list_data = getListData();
        final ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(new CustomListAdapter(this, list_data));

        // When the user clicks on the ListItem
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Object o = listView.getItemAtPosition(position);
                Crops crops = (Crops) o;
                SelectedCrop = crops.getCropName();
            }
        });
    }

    private List<Crops> getListData() {
        List<Crops> list = new ArrayList<>();
        Crops corn = new Crops("Corn", "corn", "Type of Corn");
        Crops cotton = new Crops("Cotton", "cotton_plant", "Type of Cotton");
        Crops rice = new Crops("Rice", "rice_plant", "Type of Rice");
        Crops wheat = new Crops("Wheat", "wheat", "Type of Wheat");

        list.add(corn);
        list.add(cotton);
        list.add(rice);
        list.add(wheat);

        return list;
    }

    public void saveCrop(View view) {

        if (SelectedCrop == null) {
            AlertDialog.Builder no_crop = new AlertDialog.Builder(this);
            no_crop.setTitle("No Crop Selected")
                    .setMessage("Please Select the Crop Type")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            AlertDialog alert = no_crop.create();
            alert.show();
        } else {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("result", SelectedCrop);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        }
    }
}
