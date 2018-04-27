package com.noman.locations;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


import static android.content.ContentValues.TAG;

public class LocationProvider {

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Represents a geographical location.
     */
    private Location mCurrentLocation;

    /**
     * Context of Calling Activity
     */
    private Context mContext;

    /**
     * Lisnter of location update
     */
    private LocationUpdateListner locationUpdateListner;

    /**
     * Permission code to map after permission allowed
     */
    private int permissionsRequestCode;

    /**
     * Settings request will map after settings changed
     */
    private int checkSettingsRequestCode;

    /**
     * Boolean on based of which locaiton update for one time or multiple time
     */
    private boolean getJustCurrentLocation;

    /**
     * Contructor of Location provider cass
     *
     * @param context                  Context of calling Activity
     * @param permissionsRequestCode   Code that will map on onRequestPermissionsResult function
     * @param checkSettingsRequestCode Request code that will map in onActivityResult
     * @param getJustCurrentLocation   Boolean that will tell get location for one time or continuesly
     * @param locationUpdateListner    Listener for location update
     */
    LocationProvider(Context context, int permissionsRequestCode, int checkSettingsRequestCode, boolean getJustCurrentLocation, LocationUpdateListner locationUpdateListner) {

        this.mContext = context;

        this.permissionsRequestCode = permissionsRequestCode;
        this.checkSettingsRequestCode = checkSettingsRequestCode;

        this.getJustCurrentLocation = getJustCurrentLocation;

        this.locationUpdateListner = locationUpdateListner;

        iniLocationProviders();

    }

    /**
     * Initialize location providers
     */
    private void iniLocationProviders() {

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        mSettingsClient = LocationServices.getSettingsClient(mContext);

        createLocationCallback();

        createLocationRequest();

        buildLocationSettingsRequest();

        startLocationUpdates();

    }


    /**
     * Create location callback listener that will on location update.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mCurrentLocation = locationResult.getLastLocation();
                locationUpdateListner.onLocationUpdate(mCurrentLocation);
            }
        };
    }

    /**
     * Create location request and assign value to params when to update location and how to update location
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Build a request to check locaiton settings
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * This functions is used for to update continuesly device current location with respect to setInterval value
     */
    private void startLocationUpdates() {
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                        getDeviceLocation();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult((Activity) mContext, checkSettingsRequestCode);
                                } catch (IntentSender.SendIntentException sie) {
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                Log.d(TAG, "No location settings change available in this device.");
                                break;
                        }
                    }
                });

    }

    /**
     * This function is used to to get Device location either one time or continuesly
     */
    private void getDeviceLocation() {
        if (checkIfLocationPermissionsGranted()) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (getJustCurrentLocation) {// Get Device location for one time only

                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            mCurrentLocation = task.getResult();
                            locationUpdateListner.onLocationUpdate(mCurrentLocation);
                        } else {
                            mLocationRequest.setNumUpdates(1);
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        }
                    }
                });

            } else {//  Get device location continuesly
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            }
        }
    }

    /**
     * This function is used for to check if requred permissions are granted or NOT
     * First check if android OS version is greater or equal to Marshmellow
     * Second Check if ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission are granted or not
     *
     * @return Boolean
     */
    public boolean checkIfLocationPermissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, permissionsRequestCode);
                return false;
            }
        }
        return true;
    }

    /**
     * This function is used to remove location updates calling
     */
    public void stopLocationUpdate() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Log.d(TAG, "Location update stopped!");
                }
            });
        }
    }


    /**
     * Interface class for getting update locations onLocaitonUpdate(Location) will return updated location
     */
    public interface LocationUpdateListner {

        void onLocationUpdate(Location location);

    }

}
