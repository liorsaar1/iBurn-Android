package com.gaiagps.iburn.gj.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GjMessageGps extends GjMessage {

    private long time;
    private double lat;
    private double lng;
    private double head;

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
