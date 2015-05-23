package com.gaiagps.iburn.gj.message;

public class GjMessageResponse extends GjMessage {

    public GjMessageResponse(boolean checksumOk) {
        super(Type.Response);
        setByte(checksumOk);
    }

    public boolean getChecksumOk() {
        return getBoolean();
    }

    @Override
    public String toString() {
        return super.toString()+": OK:" + getChecksumOk();
    }
}
