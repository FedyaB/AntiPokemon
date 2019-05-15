package edu.amd.spbstu.antipokemon.target;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import edu.amd.spbstu.antipokemon.R;

public class TargetBox {
    private static final String BUNDLE_KEY_FOUND_TARGETS = "FOUND_TARGETS";

    private static final int FOUND_R = 30;
    private static final int INF_DIST = 10000;
    private static final int HIDDEN_COLOR_FAR = 0x22FF0000;
    private static final int HIDDEN_COLOR_CURRENT = 0x22FFFF00;

    private static final LatLng[] s_targets;
    static {
        s_targets = new LatLng[8];
        s_targets[0] = new LatLng(60.008933, 30.372243);
        s_targets[1] = new LatLng(60.006502, 30.372195);
        s_targets[2] = new LatLng(60.008466, 30.375319);
        s_targets[3] = new LatLng(60.007020, 30.381580);
        s_targets[4] = new LatLng(60.005797, 30.374247);
        s_targets[5] = new LatLng(60.011095, 30.372662);
        s_targets[6] = new LatLng(60.018957, 30.350996);
        s_targets[7] = new LatLng(59.999824, 30.364192);
        //s_targets[8] = new LatLng(60.033915, 30.378831);
    }

    private LatLng getRandomLocation(LatLng point, int radius) {
        List<LatLng> randomPoints = new ArrayList<>();
        List<Float> randomDistances = new ArrayList<>();
        Location myLocation = new Location("");
        myLocation.setLatitude(point.latitude);
        myLocation.setLongitude(point.longitude);

        //This is to generate 10 random points
        for(int i = 0; i<10; i++) {
            double x0 = point.latitude;
            double y0 = point.longitude;

            Random random = new Random();

            // Convert radius from meters to degrees
            double radiusInDegrees = radius / 111000f;

            double u = random.nextDouble();
            double v = random.nextDouble();
            double w = radiusInDegrees * Math.sqrt(u);
            double t = 2 * Math.PI * v;
            double x = w * Math.cos(t);
            double y = w * Math.sin(t);

            // Adjust the x-coordinate for the shrinking of the east-west distances
            double new_x = x / Math.cos(y0);

            double foundLatitude = new_x + x0;
            double foundLongitude = y + y0;
            LatLng randomLatLng = new LatLng(foundLatitude, foundLongitude);
            randomPoints.add(randomLatLng);
            Location l1 = new Location("");
            l1.setLatitude(randomLatLng.latitude);
            l1.setLongitude(randomLatLng.longitude);
            randomDistances.add(l1.distanceTo(myLocation));
        }
        //Get nearest point to the centre
        int indexOfNearestPointToCentre = randomDistances.indexOf(Collections.min(randomDistances));
        return randomPoints.get(indexOfNearestPointToCentre);
    }

    private class Target implements MapTarget {
        private static final int FOUND_R_1 = 10;
        private static final int FOUND_R_2 = 20;
        private static final int FOUND_R_3 = 30;
        private static final int FOUND_COLOR_1 = 0x5500FF00;
        private static final int FOUND_COLOR_2 = 0x55FFFF00;
        private static final int FOUND_COLOR_3 = 0x55FF0000;

        LatLng realCenter;
        LatLng hiddenCenter;
        int hiddenRadius;
        int hiddenColor;
        boolean isFound;

        @StringRes int nameID;
        @StringRes int infoID;
        @DrawableRes int imageID;

        private CircleOptions figureHidden;
        private CircleOptions figureFound;
        private float[] distanceResults;

        Target() {
            isFound = false;
            hiddenColor = HIDDEN_COLOR_FAR;

            nameID = -1;
            infoID = -1;

            figureHidden = new CircleOptions();
            figureFound = new CircleOptions();
            distanceResults = new float[1];
        }

        @Override
        public int getInfo() {
            return infoID;
        }

        @Override
        public int getName() {
            return nameID;
        }

        @Override
        public int getImage() {
            return imageID;
        }

        @Override
        public LatLng getCenter() {
            return isFound ? realCenter : hiddenCenter;
        }

        void draw() {
            if (isFound)
                drawFound();
            else
                drawHidden();
        }

        private float distanceToReal(Location location) {
            Location.distanceBetween(realCenter.latitude, realCenter.longitude, location.getLatitude(), location.getLongitude(), distanceResults);
            return distanceResults[0];
        }

        private float distanceToHidden(Location location) {
            Location.distanceBetween(hiddenCenter.latitude, hiddenCenter.longitude, location.getLatitude(), location.getLongitude(), distanceResults);
            return distanceResults[0];
        }

        private void drawHidden() {
            figureHidden.center(hiddenCenter).fillColor(hiddenColor).radius(hiddenRadius).clickable(true);
            mMap.addCircle(figureHidden);
        }

        private void drawFound() {
            figureFound.center(realCenter).strokeColor(FOUND_COLOR_1).radius(FOUND_R_1);
            mMap.addCircle(figureFound);
            figureFound.center(realCenter).strokeColor(FOUND_COLOR_2).radius(FOUND_R_2);
            mMap.addCircle(figureFound);
            figureFound.center(realCenter).strokeColor(FOUND_COLOR_3).radius(FOUND_R_3);
            mMap.addCircle(figureFound);
        }
    }

    private GoogleMap mMap;
    private MapTargetUpdater mapTargetUpdater;
    private Target[] targets;
    private Target currentTarget;
    private boolean initialized;

    public TargetBox(MapTargetUpdater updater, @Nullable Bundle savedInstanceState) {
        initialized = false;
        currentTarget = null;
        mapTargetUpdater = updater;

        targets = new Target[s_targets.length];
        initializeTargets();

        restore(savedInstanceState);
    }

    private void restoreMembers(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;
        if (savedInstanceState.keySet().contains(BUNDLE_KEY_FOUND_TARGETS)) {
            boolean[] foundFlags =  savedInstanceState.getBooleanArray(BUNDLE_KEY_FOUND_TARGETS);
            if (foundFlags == null)
                return;
            for (int i = 0; i < foundFlags.length; ++i)
                targets[i].isFound = foundFlags[i];
        }
    }

    public void initialize(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle circle) {
                for (Target target : targets)
                    if (target.hiddenCenter.equals(circle.getCenter())) {
                        mapTargetUpdater.onRequestTargetInfo(target);
                        return;
                    }
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });
        initialized = true;
    }

    public void draw() {
        if (!initialized)
            return;
        for (Target target: targets)
            target.draw();
    }

    public boolean areAllTargetsFound() {
        for (Target target: targets)
            if (!target.isFound)
                return false;
        return true;
    }

    public void update(Location user) {
        processCurrentTarget(user);
        setAppropriateCurrentTarget(user);
    }

    private void processCurrentTarget(Location user) {
        if (currentTarget == null)
            return;
        if (currentTarget.distanceToHidden(user) > currentTarget.hiddenRadius) {
            currentTarget.hiddenColor = HIDDEN_COLOR_FAR;
            mapTargetUpdater.onLeavingTargetArea(currentTarget);
            currentTarget = null;
        } else if (currentTarget.distanceToReal(user) < FOUND_R) {
            currentTarget.isFound = true;
            mapTargetUpdater.onTargetFound(currentTarget);
            currentTarget = null;
        }
    }

    public LatLng processFindClosestTarget(Location user) {
        float min = INF_DIST, distance;
        Target chosen = targets[0];
        for (Target target: targets) {
            if (!target.isFound && (distance = target.distanceToHidden(user)) < min) {
                min = distance;
                chosen = target;
            }
        }
        return chosen.hiddenCenter;
    }

    private void setAppropriateCurrentTarget(Location user) {
        Target chosen = null;
        float min = INF_DIST, realDistance;
        for (Target target: targets) {
            if (!target.isFound && target.distanceToHidden(user) < target.hiddenRadius && (realDistance = target.distanceToReal(user)) < min) {
                min = realDistance;
                chosen = target;
            }
        }

        if (chosen != null && currentTarget != chosen) {
            if (currentTarget != null)
                currentTarget.hiddenColor = HIDDEN_COLOR_FAR;

            currentTarget = chosen;
            currentTarget.hiddenColor = HIDDEN_COLOR_CURRENT;
            mapTargetUpdater.onEnterTargetArea(currentTarget);
        }
    }

    private void initializeTargets() {
        targets[0] = new Target();
        targets[0].realCenter = s_targets[0];
        targets[0].hiddenRadius = 50;
        targets[0].hiddenCenter = s_targets[0];
        targets[0].nameID = R.string.app_target_1_caption;
        targets[0].infoID = R.string.app_target_1_info;
        targets[0].imageID = R.raw.target1;

        targets[1] = new Target();
        targets[1].realCenter = s_targets[1];
        targets[1].hiddenRadius = 60;
        targets[1].hiddenCenter = getRandomLocation(targets[1].realCenter, targets[1].hiddenRadius);
        targets[1].nameID = R.string.app_target_2_caption;
        targets[1].infoID = R.string.app_target_2_info;
        targets[1].imageID = R.raw.target2;

        targets[2] = new Target();
        targets[2].realCenter = s_targets[2];
        targets[2].hiddenRadius = 80;
        targets[2].hiddenCenter = getRandomLocation(targets[2].realCenter, targets[2].hiddenRadius);
        targets[2].nameID = R.string.app_target_3_caption;
        targets[2].infoID = R.string.app_target_3_info;
        targets[2].imageID = R.raw.target3;

        targets[3] = new Target();
        targets[3].realCenter = s_targets[3];
        targets[3].hiddenRadius = 60;
        targets[3].hiddenCenter = getRandomLocation(targets[3].realCenter, targets[3].hiddenRadius);
        targets[3].nameID = R.string.app_target_4_caption;
        targets[3].infoID = R.string.app_target_4_info;
        targets[3].imageID = R.raw.target4;

        targets[4] = new Target();
        targets[4].realCenter = s_targets[4];
        targets[4].hiddenRadius = 70;
        targets[4].hiddenCenter = getRandomLocation(targets[4].realCenter, targets[4].hiddenRadius);
        targets[4].nameID = R.string.app_target_5_caption;
        targets[4].infoID = R.string.app_target_5_info;
        targets[4].imageID = R.raw.target5;

        targets[5] = new Target();
        targets[5].realCenter = s_targets[5];
        targets[5].hiddenRadius = 90;
        targets[5].hiddenCenter = getRandomLocation(targets[5].realCenter, targets[5].hiddenRadius);
        targets[5].nameID = R.string.app_target_6_caption;
        targets[5].infoID = R.string.app_target_6_info;
        targets[5].imageID = R.raw.target6;

        targets[6] = new Target();
        targets[6].realCenter = s_targets[6];
        targets[6].hiddenRadius = 160;
        targets[6].hiddenCenter =  getRandomLocation(targets[6].realCenter, targets[6].hiddenRadius);
        targets[6].nameID = R.string.app_target_7_caption;
        targets[6].infoID = R.string.app_target_7_info;
        targets[6].imageID = R.raw.target7;

        targets[7] = new Target();
        targets[7].realCenter = s_targets[7];
        targets[7].hiddenRadius = 130;
        targets[7].hiddenCenter =  getRandomLocation(targets[7].realCenter, targets[7].hiddenRadius);
        targets[7].nameID = R.string.app_target_8_caption;
        targets[7].infoID = R.string.app_target_8_info;
        targets[7].imageID = R.raw.target8;

        /*
        targets[8] = new Target();
        targets[8].realCenter = s_targets[8];
        targets[8].hiddenRadius = 60;
        targets[8].hiddenCenter = getRandomLocation(targets[8].realCenter, targets[8].hiddenRadius);
        targets[8].nameID = R.string.app_target_1_caption;
        targets[8].infoID = R.string.app_target_1_info;
        targets[8].imageID = R.raw.target5;*/
    }

    public void saveInstance(Bundle outState) {
        boolean[] foundFlags = new boolean[targets.length];
        for (int i = 0; i < targets.length; ++i) {
            foundFlags[i] = targets[i].isFound;
        }
        outState.putBooleanArray(BUNDLE_KEY_FOUND_TARGETS, foundFlags);
    }

    public void restore(Bundle savedInstanceState) {
        restoreMembers(savedInstanceState);
    }
}
