package com.gaiagps.iburn.gj.message;

import com.google.android.gms.maps.model.LatLng;

public class GjMessageReportGps extends GjMessageString {

    public GjMessageReportGps(int id, LatLng latLng) {
        super(Type.ReportGps, report(id, latLng));
    }

    public GjMessageReportGps(String payload) {
        super(Type.ReportGps, payload);
    }

    private static String report(int id, LatLng latLng) {
        return id+"," + latLng.latitude + "," + latLng.longitude;
    }
}
