package edu.amd.spbstu.antipokemon;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;

import edu.amd.spbstu.antipokemon.dialog.InfoDialogFragment;
import edu.amd.spbstu.antipokemon.dialog.ListenerOK;
import edu.amd.spbstu.antipokemon.intro.BaseActivity;
import edu.amd.spbstu.antipokemon.location.FakeLocationBox;
import edu.amd.spbstu.antipokemon.location.GPSLocationBox;
import edu.amd.spbstu.antipokemon.location.LocationBox;
import edu.amd.spbstu.antipokemon.location.LocationUpdater;
import edu.amd.spbstu.antipokemon.target.MapTarget;
import edu.amd.spbstu.antipokemon.target.MapTargetUpdater;
import edu.amd.spbstu.antipokemon.target.TargetBox;

public class MainActivity extends BaseActivity implements OnMapReadyCallback, LocationUpdater, MapTargetUpdater {
    private static final String BUNDLE_KEY_IS_FIRST_LOCATION = "IS_FIRST_LOCATION";
    private static final String BUNDLE_KEY_IS_WELCOME_SHOWN = "IS_WELCOME_SHOWN";
    private static final String BUNDLE_KEY_IS_STOP_MODE = "IS_STOP_MODE";

    private static final int PERMISSION_TRACKING_CODE = 1000;
    private static final float ZOOM_CONST = 16.3f;

    private enum PermissionStatus {
        GRANTED,
        DECLINED,
        REQUESTED,
        WARNED_USER,
        REQUESTED_TWICE
    }

    private View applicationView;

    private GoogleMap mMap;
    private LocationBox locationBox;
    private PermissionStatus permissionStatus;
    private TargetBox targetBox;

    private boolean isFirstLocation;
    private boolean isWelcomeShown;
    private boolean isButtonRegistered;
    private boolean isStopMode;

    private Bundle savedInstance;

    //Flags need to be initialized before onCreate
    private boolean fieldsCreated;
    private boolean demandedView;
    private boolean demandRedraw;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fieldsCreated = false;
        demandedView  = false;
        demandRedraw  = false;
        targetBox = new TargetBox(this, savedInstanceState);
        permissionStatus = null;
        savedInstance = savedInstanceState;
        mMap = null;
        isButtonRegistered = false;
        applicationView = getLayoutInflater().inflate(R.layout.activity_maps, null);
        restoreMembers(savedInstanceState);
        fieldsCreated = true;

        setAppViewOnDemand();
    }

    private LocationBox createLocationBox(@Nullable Bundle savedInstanceState) {
         return cheatMode ? new FakeLocationBox(this, savedInstanceState) : new GPSLocationBox(this, savedInstanceState);
    }

    private boolean isGooglePlayServicesAvailable(Context context){
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
    }

    private void restoreMembers(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            isFirstLocation = true;
            isWelcomeShown = false;
            isStopMode = true;
        } else {
            isFirstLocation = !savedInstanceState.keySet().contains(BUNDLE_KEY_IS_FIRST_LOCATION) || savedInstanceState.getBoolean(BUNDLE_KEY_IS_FIRST_LOCATION);
            isWelcomeShown = savedInstanceState.keySet().contains(BUNDLE_KEY_IS_WELCOME_SHOWN) && savedInstanceState.getBoolean(BUNDLE_KEY_IS_WELCOME_SHOWN);
            isStopMode = !savedInstanceState.keySet().contains(BUNDLE_KEY_IS_STOP_MODE) || savedInstanceState.getBoolean(BUNDLE_KEY_IS_STOP_MODE);
        }
    }

    private void setAppViewOnDemand() {
        if (demandedView) {
            demandedView = false;
            setView(ViewMode.APPLICATION);
        }
    }

    public void initialize() {
        setContentView(applicationView);

        Button button = (Button)findViewById(R.id.closest_button);
        button.setVisibility(View.INVISIBLE);
        button = (Button)findViewById(R.id.my_location_button);
        button.setVisibility(View.INVISIBLE);

        locationBox = createLocationBox(savedInstance);
        initializeTracking();
        initializeMaps();
    }

    private void initializeTracking() {
        locationBox.setTracking(true);
        getPermissionWhileInitialization();
    }

    private void getPermissionWhileInitialization() {
        permissionStatus = processPermission();
        if (permissionStatus == PermissionStatus.GRANTED)
            onApplicationStartInteracting();
    }

    private PermissionStatus processPermission() {
        setLockScreenOrientation(true);
        if (Build.VERSION.SDK_INT >= 23 && !locationBox.checkTrackingPermission(this)) { //On SDK < 23 we don't need runtime permission
            if (permissionStatus == PermissionStatus.REQUESTED_TWICE) {
                setLockScreenOrientation(false);
                return PermissionStatus.DECLINED;
            }
            if (permissionStatus == PermissionStatus.WARNED_USER) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_TRACKING_CODE);
                return PermissionStatus.REQUESTED_TWICE;
            }
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showPermissionRationaleMessage();
                return PermissionStatus.WARNED_USER;
            }
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_TRACKING_CODE);
            return PermissionStatus.REQUESTED;
        }
        setLockScreenOrientation(false);
        return PermissionStatus.GRANTED;
    }

    private void setLockScreenOrientation(boolean lock) {
        if (lock)
            lockActivityOrientation(this);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @SuppressWarnings("deprecation")
    private static void lockActivityOrientation(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();
        int height;
        int width;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
            height = display.getHeight();
            width = display.getWidth();
        } else {
            Point size = new Point();
            display.getSize(size);
            height = size.y;
            width = size.x;
        }
        switch (rotation) {
            case Surface.ROTATION_90:
                if (width > height)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_180:
                if (height > width)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case Surface.ROTATION_270:
                if (width > height)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            default :
                if (height > width)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void onApplicationStartInteracting() {
        if (!isWelcomeShown) {
            showWelcomeMessage();
            isWelcomeShown = true;
        } else
            startTracking();
    }

    private void initializeMapTracking() {
        if (mMap != null && locationBox.checkTrackingPermission(this)) {
            locationBox.enableTrackingOptions(mMap);
        }
    }

    private void initializeMaps() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null)
            mapFragment.getMapAsync(this);
    }

    private void showPermissionRationaleMessage() {
        InfoDialogFragment.showDialog(this, R.string.app_tracking_permission_caption, R.string.app_tracking_permission, 0, R.string.app_ok, new ListenerOK() {
            @Override
            public void onOKButtonPressed() {
                permissionStatus = processPermission();
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel parcel, int i) {
                //Do nothing
            }
        });
    }

    private void showPSDeclinedMessage() {
        InfoDialogFragment.showDialog(this, R.string.app_ps_decline_caption, R.string.app_ps_decline, 0, R.string.app_ok, new ListenerOK() {
            @Override
            public void onOKButtonPressed() {
                //Do nothing
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel parcel, int i) {
                //Do nothing
            }
        });
    }

    private void showDeclinedMessage() {
        InfoDialogFragment.showDialog(this, R.string.app_tracking_decline_caption, R.string.app_tracking_decline, 0, R.string.app_ok, new ListenerOK() {
            @Override
            public void onOKButtonPressed() {
                //Do nothing
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel parcel, int i) {
                //Do nothing
            }
        });
    }

    private void showWelcomeMessage() {
        InfoDialogFragment.showDialog(this, R.string.app_welcome_caption, R.string.app_welcome, 0, R.string.app_welcome_ok, new ListenerOK() {
            @Override
            public void onOKButtonPressed() {
                startTracking();
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel parcel, int i) {
                //Do nothing
            }
        });

        /*ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.DialogTheme);
        AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
        builder.setCancelable(false);
        builder.setTitle(R.string.app_welcome_caption);
        builder.setMessage(R.string.app_welcome);
        builder.setPositiveButton(R.string.app_welcome_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startTracking();
            }
        });
        builder.show();*/
    }

    private void startTracking() {
        initializeMapTracking();
        locationBox.initialize(this);
        locationBox.startTracking(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_TRACKING_CODE && permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION))
            getPermissionWhileRequest(grantResults[0]);
        setLockScreenOrientation(false);
    }

    private void getPermissionWhileRequest(int grantedID) {
        if (grantedID == PackageManager.PERMISSION_GRANTED) {
            permissionStatus = PermissionStatus.GRANTED;
            onApplicationStartInteracting();
        } else {
            permissionStatus = processPermission();
            if (permissionStatus == PermissionStatus.DECLINED)
                onTrackingDeclined();
        }
    }

    private void onTrackingDeclined() {
        setView(ViewMode.INTRO);
        showDeclinedMessage();
    }

    @Override
    protected void setInitialMode() {
        setView(ViewMode.INTRO);
    }

    @Override
    public void setView(ViewMode viewID) {
        if (viewID == ViewMode.INTRO)
            super.setView(viewID);
        else {
            if (mViewMode == viewID)
                return;
            if (!isGooglePlayServicesAvailable(this)) {
                showPSDeclinedMessage();
            }
            if (!fieldsCreated) {
                demandedView = true;
                return;
            }
            mViewMode = ViewMode.APPLICATION;
            initialize();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mViewMode == ViewMode.INTRO)
            return;
        if (locationBox != null)
            locationBox.startTracking(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mViewMode == ViewMode.INTRO)
            return;
        if (locationBox != null)
            locationBox.stopTracking();
    }

    @Override
    public void onBackPressed() {
        if (mViewMode == ViewMode.APPLICATION) {
            if (locationBox != null)
                locationBox.stopTracking();
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        } else if (mViewMode == ViewMode.INTRO) {
            super.onBackPressed();
        }
    }

    @Override
    public void onLocationUpdate(final Location location) {
        if (isFirstLocation) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), ZOOM_CONST));
            isFirstLocation = false;
        }

        if (!isButtonRegistered) {
            Button button = (Button)findViewById(R.id.closest_button);
            if (!targetBox.areAllTargetsFound()) {
                button.setVisibility(View.VISIBLE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Location location = locationBox.getDeviceLocation();
                        if (location != null) {
                            LatLng coordinates = targetBox.processFindClosestTarget(locationBox.getDeviceLocation());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(coordinates.latitude, coordinates.longitude), ZOOM_CONST));
                        }
                    }
                });
            } else {
                button.setVisibility(View.GONE);
            }

            button = (Button)findViewById(R.id.my_location_button);
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Location location = locationBox.getDeviceLocation();
                    if (location != null)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), ZOOM_CONST));
                }
            });

            if (cheatMode) {
                button = (Button)findViewById(R.id.stop_button);
                button.setVisibility(View.VISIBLE);
                button.setText(isStopMode ? R.string.button_resume : R.string.button_stop);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Button b = (Button)view;
                        b.setText(isStopMode ? R.string.button_stop : R.string.button_resume);
                        if (isStopMode)
                            locationBox.resumeTimer();
                        else
                            locationBox.suspendTimer();
                        isStopMode = !isStopMode;
                    }
                });
            }

            isButtonRegistered = true;
        }

        targetBox.update(location);
        redraw();
    }

    private void redraw() {
        if (mMap != null) {
            mMap.clear();
            targetBox.draw();
            locationBox.drawCurrentPosition(mMap);
        } else
            demandRedraw = true;
    }

    @Override
    public void onEnterTargetArea(MapTarget target) {
        showEnterMessage(target, true);
    }

    @Override
    public void onRequestTargetInfo(MapTarget target) {
        showEnterMessage(target, false);
    }

    @Override
    public void onLeavingTargetArea(MapTarget target) {
        //Do nothing
    }

    @Override
    public void onTargetFound(MapTarget target) {
        if (targetBox.areAllTargetsFound()) {
            showAllFoundMessage();

            Button button = (Button)findViewById(R.id.closest_button);
            button.setVisibility(View.GONE);

            if (cheatMode) {
                button = (Button)findViewById(R.id.stop_button);
                button.setVisibility(View.GONE);
                isStopMode = true;
            }
        } else
            showFoundMessage();
    }

    private void showEnterMessage(final MapTarget target, final boolean move) {
        locationBox.suspendTimer();
        InfoDialogFragment.showDialog(this, target.getName(),
                target.getInfo(), target.getImage(), R.string.app_ok, new ListenerOK() {
                    @Override
                    public void onOKButtonPressed() {
                        if (move)
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target.getCenter(), ZOOM_CONST));
                        if (!isStopMode)
                            locationBox.resumeTimer();
                    }

                    @Override
                    public int describeContents() {
                        return 0;
                    }

                    @Override
                    public void writeToParcel(Parcel parcel, int i) {
                        //Do nothing
                    }
                });
    }

    private void showFoundMessage() {
        locationBox.suspendTimer();
        InfoDialogFragment.showDialog(this, R.string.app_found_caption, R.string.app_found_info, 0, R.string.app_ok, new ListenerOK() {
            @Override
            public void onOKButtonPressed() {
                if (!isStopMode)
                    locationBox.resumeTimer();
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel parcel, int i) {
                //Do nothing
            }
        });
    }

    private void showAllFoundMessage() {
        InfoDialogFragment.showDialog(this, R.string.app_all_found_caption, R.string.app_all_found_info, 0, R.string.app_ok, new ListenerOK() {
            @Override
            public void onOKButtonPressed() {
                //Do nothing
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel parcel, int i) {
                //Do nothing
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.map_style));
        initializeMapTracking();
        targetBox.initialize(mMap);
        if (demandRedraw)
            redraw();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreMembers(savedInstanceState);
        if (locationBox != null)
            locationBox.restore(savedInstanceState);
        if (targetBox != null)
            targetBox.restore(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.putBoolean(BUNDLE_KEY_IS_WELCOME_SHOWN, isWelcomeShown);
            outState.putBoolean(BUNDLE_KEY_IS_FIRST_LOCATION, isFirstLocation);
            outState.putBoolean(BUNDLE_KEY_IS_STOP_MODE, isStopMode);
            if (locationBox != null)
                locationBox.saveInstance(outState);
            if (targetBox != null)
                targetBox.saveInstance(outState);
        }
    }
}
