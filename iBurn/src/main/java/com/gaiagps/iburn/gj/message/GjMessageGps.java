package com.gaiagps.iburn.gj.message;

import com.google.android.gms.maps.model.LatLng;

public class GjMessageGps extends GjMessageString {

    public GjMessageGps(int id, LatLng latLng) {
        super(Type.Gps, report(id, latLng));
    }

    public GjMessageGps(String payload) {
        super(Type.Gps, payload);
    }

    private static String report(int id, LatLng latLng) {
        return id+"," + latLng.latitude + "," + latLng.longitude;
    }
}
