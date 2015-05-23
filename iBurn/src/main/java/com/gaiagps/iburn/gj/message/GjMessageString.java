package com.gaiagps.iburn.gj.message;

public class GjMessageString extends GjMessage {

    public static final int MAX_LENGTH = 140;

    public GjMessageString(Type type, String dataString) {
        super(type);
        // force 140
        int length = Math.min(dataString.length(), MAX_LENGTH);
        data = dataString.substring(0, length).getBytes();
    }

    public String getString() {
        return new String(data);
    }

    @Override
    public String toString() {
        return super.toString() + ":" + new String(data);
    }
}
