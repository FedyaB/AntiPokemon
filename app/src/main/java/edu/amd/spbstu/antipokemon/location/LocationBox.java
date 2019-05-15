package edu.amd.spbstu.antipokemon.location;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;

public interface LocationBox {
    boolean checkTrackingPermission(@NonNull Context context);
    void initialize(@NonNull Activity activity);
    Location getDeviceLocation();
    void startTracking(@NonNull Context context);
    void stopTracking();
    void setTracking(boolean isTracking);
    void saveInstance(Bundle outState);
    void enableTrackingOptions(GoogleMap map);
    void drawCurrentPosition(GoogleMap map);
    void suspendTimer();
    void resumeTimer();
    void restore(Bundle savedInstanceState);
}
