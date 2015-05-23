package com.gaiagps.iburn.gj.message;

public class GjMessageText extends GjMessageString {

    public GjMessageText(String dataString) {
        super(Type.Text, dataString);
    }

    public GjMessageText(byte packetNumber, byte vehicle, byte[] data) {
        super(Type.Text, packetNumber, vehicle, data);
    }
}
