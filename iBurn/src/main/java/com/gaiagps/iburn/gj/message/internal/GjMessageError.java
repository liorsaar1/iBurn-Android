package com.gaiagps.iburn.gj.message.internal;

import com.gaiagps.iburn.gj.message.GjMessageString;

public class GjMessageError extends GjMessageString {

    public GjMessageError(String dataString) {
        super(Type.Error, dataString);
    }

    public GjMessageError(byte packetNumber, byte vehicle, byte[] data) {
        super(Type.Error, packetNumber, vehicle, data);
    }
}
