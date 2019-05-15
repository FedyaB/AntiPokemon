package edu.amd.spbstu.antipokemon.location;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.content.res.AppCompatResources;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Delayed;

import edu.amd.spbstu.antipokemon.R;

public class FakeLocationBox implements LocationBox {
    private static final int TIME_INTERVAL = 1000;
    private static final float ICON_MODIFIER = 0.1f;
    private static final String BUNDLE_KEY_CURRENT_INDEX = "CURRENT_FAKE_INDEX";

    private List<LatLng> coordinatesList;

    private LocationUpdater updater;
    private int currentIndex;
    private Location location;
    private GoogleMap googleMap;
    private Timer timer;
    private volatile boolean startTask;
    private boolean isFirstRun;

    private Activity activity;
    private MarkerOptions markerOptions;
    private int iconSize;

    public FakeLocationBox(LocationUpdater locationUpdater, @Nullable Bundle savedInstanceState) {
        updater = locationUpdater;
        location = new Location("");
        timer = new Timer();
        startTask = false;
        isFirstRun = true;
        coordinatesList = new ArrayList<>();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (startTask) {
                    if (currentIndex != coordinatesList.size() - 1) {
                        currentIndex++;
                    } else
                        if (isFirstRun) {
                            isFirstRun = false;
                        } else
                            return;
                    LatLng coordinates = coordinatesList.get(currentIndex);
                    location.setLatitude(coordinates.latitude);
                    location.setLongitude(coordinates.longitude);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updater.onLocationUpdate(location);
                        }
                    });
                }
            }
        },1000, TIME_INTERVAL);

        restore(savedInstanceState);
    }

    private void readCoordinatesFile() {
        InputStream inputStream = activity.getResources().openRawResource(R.raw.coordinates);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String line;
            while (( line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                coordinatesList.add(new LatLng(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
            }
        } catch (IOException e) {
            coordinatesList.add(new LatLng(60.009251, 30.372385));
            coordinatesList.add(new LatLng(60.009165, 30.372342));
            coordinatesList.add(new LatLng(60.009079, 30.372680));
            coordinatesList.add(new LatLng(60.008953, 30.372610));
            coordinatesList.add(new LatLng(60.008881, 30.372433));
            coordinatesList.add(new LatLng(60.008693, 30.371988));
            coordinatesList.add(new LatLng(60.008489, 30.371403));
            coordinatesList.add(new LatLng(60.008178, 30.371194));
            coordinatesList.add(new LatLng(60.007722, 30.370888));
            coordinatesList.add(new LatLng(60.007330, 30.370620));
            coordinatesList.add(new LatLng(60.007196, 30.370534));
            coordinatesList.add(new LatLng(60.006910, 30.370931));
            coordinatesList.add(new LatLng(60.006685, 30.371382));
            coordinatesList.add(new LatLng(60.006608, 30.371816));
            coordinatesList.add(new LatLng(60.006559, 30.372412));
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                //Do nothing
            }
        }
    }

    @Override
    public boolean checkTrackingPermission(@NonNull Context context) {
        return true;
    }

    @Override
    public void initialize(@NonNull Activity activity) {
        this.activity = activity;
        markerOptions = new MarkerOptions();

        markerOptions.draggable(false);
        BitmapDrawable bitmapDrawable = (BitmapDrawable)AppCompatResources.getDrawable(activity, activity.getResources().
                getIdentifier("player_icon", "drawable", activity.getPackageName()));
        Bitmap bmp = bitmapDrawable.getBitmap();
        processIconSize();
        Bitmap smallMarker = Bitmap.createScaledBitmap(bmp, iconSize, iconSize, false);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));

        readCoordinatesFile();
    }

    private void processIconSize() {
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int minDim = size.x < size.y ? size.x : size.y;
        iconSize = (int)(minDim * ICON_MODIFIER);
    }

    @Override
    public Location getDeviceLocation() {
        return location;
    }

    @Override
    public void startTracking(@NonNull Context context) {
        LatLng coordinates = coordinatesList.get(currentIndex);
        location.setLatitude(coordinates.latitude);
        location.setLongitude(coordinates.longitude);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updater.onLocationUpdate(location);
            }
        });
    }

    @Override
    public void stopTracking() {
        timer.cancel();
        timer.purge();
    }

    @Override
    public void setTracking(boolean isTracking) {
        //Do nothing
    }

    @Override
    public void saveInstance(Bundle outState) {
        if (outState != null)
            outState.putInt(BUNDLE_KEY_CURRENT_INDEX, currentIndex);
    }

    @Override
    public void enableTrackingOptions(GoogleMap map) {
        googleMap = map;
    }

    @Override
    public void drawCurrentPosition(GoogleMap map) {
        markerOptions.position(new LatLng(location.getLatitude(), location.getLongitude()));
        googleMap.addMarker(markerOptions);
    }

    @Override
    public void suspendTimer() { startTask = false; }

    @Override
    public void resumeTimer() {
        startTask = true;
    }

    @Override
    public void restore(Bundle savedInstanceState) {
        currentIndex = 0;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(BUNDLE_KEY_CURRENT_INDEX))
                currentIndex = savedInstanceState.getInt(BUNDLE_KEY_CURRENT_INDEX);

            if (savedInstanceState.containsKey("IS_STOP_MODE"))
                startTask = !savedInstanceState.getBoolean("IS_STOP_MODE");
            else
                startTask = false;
        }
    }
}
