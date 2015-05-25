package com.gaiagps.iburn.gj.message;

public class GjMessageResponse extends GjMessage {

    public GjMessageResponse(byte packetNumber, byte vehicle, byte[] data) {
        super(Type.Response, packetNumber, vehicle);
        setByte(data[0]); // true == checksum OK
    }

    public boolean getChecksumOk() {
        return getBoolean();
    }

    private String checksumStatus() {
        return getChecksumOk() ? "No error" : "Checksum Error. packet " + getPacketNumber();
    }

    @Override
    public String toString() {
        return super.toString()+": " + checksumStatus();
    }
}
