package com.gaiagps.iburn.gj.message.internal;

import com.gaiagps.iburn.gj.message.GjMessage;

public class GjMessageFtdi extends GjMessage {

    public GjMessageFtdi(boolean attached) {
        super(Type.FTDI);
        setByte(attached);
    }

    public GjMessageFtdi(byte packetNumber, byte vehicle, byte[] data) {
        super(Type.FTDI, packetNumber, vehicle);
        setByte(data[0]);
    }

    public boolean getStatus() {
        return getBoolean();
    }

    public String getStatusString() {
        return getStatus() ? "Open" : "Closed";
    }

    @Override
    public String toString() {
        return super.toString() + ":" + getStatusString() ;
    }
}
