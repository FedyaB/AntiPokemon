package edu.amd.spbstu.antipokemon.target;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import com.google.android.gms.maps.model.LatLng;

public interface MapTarget {
    @StringRes int getName();
    @StringRes int getInfo();
    @DrawableRes int getImage();
    LatLng getCenter();
}
