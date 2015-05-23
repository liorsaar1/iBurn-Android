package com.gaiagps.iburn.gj.message.internal;

import com.gaiagps.iburn.gj.message.GjMessage;

public class GjMessageUsb extends GjMessage {

    public GjMessageUsb(boolean attached) {
        super(Type.USB);
        setByte(attached);
    }

    public boolean getStatus() {
        return getBoolean();
    }

    public String getStatusString() {
        return getStatus() ? "Attached" : "Detached";
    }

    @Override
    public String toString() {
        return super.toString() + ":" + getStatusString() ;
    }
}
