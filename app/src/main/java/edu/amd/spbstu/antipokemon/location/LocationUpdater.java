package edu.amd.spbstu.antipokemon.location;

import android.location.Location;

public interface LocationUpdater {
    void onLocationUpdate(Location location);
}
