package com.gaiagps.iburn.gj.message;

import com.google.android.gms.maps.model.LatLng;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GjMessageGps extends GjMessage {

    private long time;
    private double lat;
    private double lng;
    private double head;

    public GjMessageGps(LatLng latLng) {
        super(Type.Gps);
    }

//    sb.append("ff 55 aa 36 00 04 12 80 30 78 1d 6a 3d 3c b7 15 06 a6 16 91 23 00 00 b2 00 66 ");

    public GjMessageGps(byte[] data) {
        super(Type.Gps);

        time = ByteBuffer.wrap(data,  0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        lat  = ByteBuffer.wrap(data,  4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt()*0.0000001;
        lng  = ByteBuffer.wrap(data,  8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt()*0.0000001;
        head = ByteBuffer.wrap(data, 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt()*0.01;
    }

    public double getHead() {
        return head;
    }

    public long getTime() {
        return time;
    }

    public double getLat() {
        return lat;
    }

    public double getLong() {
        return lng;
    }

    @Override
    public String toString() {
        return super.toString()+": time:" + getTime() + " lat:" + getLat() + " long:"+getLong() + " head:" + getHead();
    }
}
