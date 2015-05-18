package com.gaiagps.iburn.gj.message.internal;

import com.gaiagps.iburn.gj.message.GjMessage;

public class GjMessageFtdi extends GjMessage {

    public GjMessageFtdi(boolean attached) {
        super(Type.FTDI);
        data = new byte[] { attached ? (byte)1 : (byte)0 };
    }

    public boolean getStatus() {
        return data[0] != 0;
    }

    public String getStatusString() {
        return getStatus() ? "Open" : "Closed";
    }

    @Override
    public String toString() {
        return Type.FTDI + ":" + getStatusString() ;
    }
}
