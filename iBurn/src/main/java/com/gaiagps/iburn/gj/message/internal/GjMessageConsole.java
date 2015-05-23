package com.gaiagps.iburn.gj.message.internal;

import com.gaiagps.iburn.gj.message.GjMessageString;

public class GjMessageConsole extends GjMessageString {

    public GjMessageConsole(String dataString) {
        super(Type.Console, dataString);
    }

    public GjMessageConsole(byte packetNumber, byte vehicle, byte[] data) {
        super(Type.Console, packetNumber, vehicle, data);
    }
}
