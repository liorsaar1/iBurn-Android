package com.gaiagps.iburn.gj.message.internal;

import com.gaiagps.iburn.gj.message.GjMessage;

public class GjMessageUsb extends GjMessage {

    public GjMessageUsb(boolean attached) {
        super(Type.USB);
        data = new byte[] { attached ? (byte)1 : (byte)0 };
    }

    public boolean getStatus() {
        return data[0] != 0;
    }

    public String getStatusString() {
        return getStatus() ? "Attached" : "Detached";
    }

    @Override
    public String toString() {
        return Type.USB + ":" + getStatusString() ;
    }
}
