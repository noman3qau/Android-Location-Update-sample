package com.noman.locations;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    public static final int REQUEST_CHECK_SETTINGS = 0x1;

    private AppCompatTextView locationView;
    private AppCompatButton justCurrentLocation, locationUpdate, stopLocationUpdate;

    private LocationProvider locationProvider;
    private boolean justCurrentLcoaiton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationView = (AppCompatTextView) findViewById(R.id.locationView);
        justCurrentLocation = (AppCompatButton) findViewById(R.id.justCurrentLocation);
        locationUpdate = (AppCompatButton) findViewById(R.id.locationUpdate);
        stopLocationUpdate = (AppCompatButton) findViewById(R.id.stopLocationUpdate);

        justCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                justCurrentLcoaiton = true;
                checkLocation(justCurrentLcoaiton);
            }
        });
        locationUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                justCurrentLcoaiton = false;
                checkLocation(justCurrentLcoaiton);
            }
        });
        stopLocationUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationProvider != null) {
                    locationProvider.stopLocationUpdate();
                }
            }
        });

    }

    private void checkLocation(boolean getCurrentLocationOnly) {

        locationView.setText("");

        locationProvider = new LocationProvider(this, REQUEST_PERMISSIONS_REQUEST_CODE, REQUEST_CHECK_SETTINGS, getCurrentLocationOnly, new LocationProvider.LocationUpdateListner() {
            @Override
            public void onLocationUpdate(Location location) {

                updateUI(location);
            }
        });

    }

    private void updateUI(Location location) {

        String locationString = locationView.getText().toString();

        locationString = locationString + "\n" + location.getLatitude() + " | " + location.getLongitude();

        locationView.setText(locationString);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:

                checkLocation(justCurrentLcoaiton);

                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_PERMISSIONS_REQUEST_CODE:

                checkLocation(justCurrentLcoaiton);

                break;

        }

    }


}