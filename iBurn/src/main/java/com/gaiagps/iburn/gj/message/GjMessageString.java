package com.gaiagps.iburn.gj.message;

public class GjMessageString extends GjMessage {

    public GjMessageString(Type type, String dataString) {
        super(type);
        // force 140
        int length = Math.min(dataString.length(), 140);
        data = dataString.substring(0, length).getBytes();
    }

    public String getString() {
        return new String(data);
    }

    @Override
    public String toString() {
        return Type.valueOf(type) + ":" + new String(data) ;
    }
}
