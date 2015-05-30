package com.gaiagps.iburn.gj.message;

public class GjMessageText extends GjMessageString {

    public GjMessageText(String dataString) {
        super(Type.Text, dataString);
    }

    public GjMessageText(String dataString, byte vehicle) {
        super(Type.Text, dataString);
        this.vehicle = vehicle;
    }

    public GjMessageText(byte packetNumber, byte vehicle, byte[] data) {
        super(Type.Text, packetNumber, vehicle, data);
    }

    @Override
    public String toString() {
        return super.toString() + ":" + vehicle + ":" +new String(data);
    }
}
