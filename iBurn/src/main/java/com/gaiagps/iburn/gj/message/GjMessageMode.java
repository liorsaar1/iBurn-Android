package com.gaiagps.iburn.gj.message;

public class GjMessageMode extends GjMessage {

    public GjMessageMode(Mode mode) {
        super(Type.Mode);
        data = new byte[] { mode.getValue() };
    }

    @Override
    public String toString() {
        return Type.Mode + ":" + Mode.valueOf(data[0]) ;
    }
}
