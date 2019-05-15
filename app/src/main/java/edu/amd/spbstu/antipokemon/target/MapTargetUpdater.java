package edu.amd.spbstu.antipokemon.target;

public interface MapTargetUpdater {
    void onEnterTargetArea(MapTarget target);
    void onLeavingTargetArea(MapTarget target);
    void onTargetFound(MapTarget target);
    void onRequestTargetInfo(MapTarget target);
}
