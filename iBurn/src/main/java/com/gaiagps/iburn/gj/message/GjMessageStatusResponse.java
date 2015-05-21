package com.gaiagps.iburn.gj.message;

public class GjMessageStatusResponse extends GjMessage {

    private static final byte BITMASK_RADIO = 0x01;
    private static final byte BITMASK_VOLTAGE = 0x02;
    private static final byte BITMASK_TEMP = 0x04;
    private static final byte BITMASK_COMPASS = 0x08;
    private static final byte BITMASK_GPS = 0x10;

    public GjMessageStatusResponse(byte status) {
        super(Type.StatusResponse);
        data = new byte[] {status};
    }

    public boolean getErrorRadio() { return (getByte() & BITMASK_RADIO) != 0; }
    public boolean getErrorVoltage() { return (getByte() & BITMASK_VOLTAGE) != 0; }
    public boolean getErrorTemp() { return (getByte() & BITMASK_TEMP) != 0; }
    public boolean getErrorCompass() { return (getByte() & BITMASK_COMPASS) != 0; }
    public boolean getErrorGps() { return (getByte() & BITMASK_GPS) != 0; }
}
