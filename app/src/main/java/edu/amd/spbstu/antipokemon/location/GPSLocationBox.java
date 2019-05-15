package edu.amd.spbstu.antipokemon.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.view.Display;
import android.view.WindowManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class GPSLocationBox implements LocationBox {
    private static final String BUNDLE_KEY_REQUESTING_LOCATION_UPDATES = "REQUESTING_LOCATION_UPDATES";

    private static final int TRACKING_INTERVAL = 2000;
    private static final int TRACKING_FASTEST_INTERVAL = 1000;
    private static final float ICON_MODIFIER = 0.1f;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private boolean isRequestingLocationUpdates;
    private LocationUpdater locationUpdater;

    private Location deviceLocation;
    private boolean initialized;

    private MarkerOptions markerOptions;
    private int iconSize;
    private Activity activity;

    public GPSLocationBox(LocationUpdater updater, @Nullable Bundle savedInstanceState) {
        mLocationCallback = null;
        mLocationRequest = null;
        mFusedLocationClient = null;
        deviceLocation = null;
        locationUpdater = updater;
        initialized = false;

        restore(savedInstanceState);
    }

    public boolean checkTrackingPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void restoreMembers(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            isRequestingLocationUpdates = !savedInstanceState.keySet().contains(BUNDLE_KEY_REQUESTING_LOCATION_UPDATES) || savedInstanceState.getBoolean(BUNDLE_KEY_REQUESTING_LOCATION_UPDATES);
        } else
            isRequestingLocationUpdates = true;
    }

    public void initialize(@NonNull Activity activity) {
        initializeLocationProvider(activity);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(TRACKING_INTERVAL);
        mLocationRequest.setFastestInterval(TRACKING_FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null)
                    return;
                for (Location location : locationResult.getLocations()) {
                    deviceLocation = location;
                    locationUpdater.onLocationUpdate(deviceLocation);
                }
            }
        };

        markerOptions = new MarkerOptions();

        markerOptions.draggable(false);
        BitmapDrawable bitmapDrawable = (BitmapDrawable) AppCompatResources.getDrawable(activity, activity.getResources().
                getIdentifier("player_icon", "drawable", activity.getPackageName()));
        Bitmap bmp = bitmapDrawable.getBitmap();
        processIconSize();
        Bitmap smallMarker = Bitmap.createScaledBitmap(bmp, iconSize, iconSize, false);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));
    }

    private void processIconSize() {
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int minDim = size.x < size.y ? size.x : size.y;
        iconSize = (int)(minDim * ICON_MODIFIER);
    }

    private void initializeLocationProvider(Activity activity) {
        if (initialized || !checkTrackingPermission(activity))
            return;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(activity, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                deviceLocation = location;
                if (deviceLocation == null)
                    return;
                locationUpdater.onLocationUpdate(deviceLocation);
            }
        });
        this.activity = activity;
        initialized = true;
    }

    public Location getDeviceLocation() {
        return deviceLocation;
    }

    public void startTracking(@NonNull Context context) {
        if (initialized && isRequestingLocationUpdates && checkTrackingPermission(context))
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    public void stopTracking() {
        if (initialized)
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    public void setTracking(boolean isTracking) {
        isRequestingLocationUpdates = isTracking;
    }

    public void saveInstance(Bundle outState) {
        outState.putBoolean(BUNDLE_KEY_REQUESTING_LOCATION_UPDATES, isRequestingLocationUpdates);
    }

    public void enableTrackingOptions(GoogleMap map) {
        /*try {
            map.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            //Do nothing
        }
        map.getUiSettings().setMyLocationButtonEnabled(true);*/

        //Do nothing
    }

    @Override
    public void drawCurrentPosition(GoogleMap map) {
        if (deviceLocation != null) {
            markerOptions.position(new LatLng(deviceLocation.getLatitude(), deviceLocation.getLongitude()));
            map.addMarker(markerOptions);
        }
    }

    @Override
    public void suspendTimer() {
        //Do nothing
    }

    @Override
    public void resumeTimer() {
        //Do nothing
    }

    @Override
    public void restore(Bundle savedInstanceState) {
        restoreMembers(savedInstanceState);
    }
}

