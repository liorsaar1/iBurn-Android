package com.gaiagps.iburn.gj.message;

import com.gaiagps.iburn.gj.message.internal.GjMessageConsole;
import com.gaiagps.iburn.gj.message.internal.GjMessageUsb;

import java.io.EOFException;
import java.nio.ByteBuffer;

/**
 * Created by liorsaar on 2015-04-21
 */

/*
    Preamble:
    All message begin with 0xFF 0x55 0xAA

    Message Type:
    0x01 StatusRequest request
    0x02 Mode
    0x03 Report own GPS location
    0x04 Request buffered GPS locations
    0x05 Lighting cues
    0x06 Text message

    Data length:
    Can be any number between 0 and 140 (0x00 - 0x8C)
    For [StatusRequest request] use length of 0
    For [Mode] use length 1

    Data:
    The length of this data must match the [Data length]
    The GPS, Lighting cue and Text message formats are somewhat arbitrary, whatever format you choose will be the format returned to your app.
    For [StatusRequest request] send no data bytes
    For [Mode]:
    Send a byte 0x00 for standard buffered mode. This will buffer GPS updates until they are requested.
    Send a byte 0x01 for non-buffered mode.  This will report GPS updates as they are received.

    Checksum:
    This will be a single byte sum (truncated to the LSB) that includes ALL bytes in the packet including the preamble. If an incorrect checksum is received an error packet will be returned

    Sample packet:
    0xFF 0x55 0xAA 0x03 0x02 0x48 0x49 0x94
    This is a text message sending "HI".
 */

public class GjMessage {
    public static final byte[] preamble = {(byte) 0xFF, (byte) 0x55, (byte) 0xAA};
    protected final static String TAG = GjMessage.class.getSimpleName();
    protected byte type;
    protected byte[] data = new byte[0];

    public GjMessage(Type type) {
        this.type = type.getValue();
    }

    public static GjMessage create(ByteBuffer bb) throws ChecksumException, EOFException, PreambleNotFoundException, ParserException {
        // find first preamble
        if (!findFirst(bb, preamble)) {
            throw new PreambleNotFoundException();
        }
        // read body
        byte typeByte = read(bb);
        byte dataLength = read(bb);
        byte[] data = read(bb, dataLength);
        byte expectedChecksum = read(bb);

        // verify checksum
        int messageLength = preamble.length + 1 + 1 + data.length;
        ByteBuffer tmp = ByteBuffer.allocate(messageLength);
        tmp.put(preamble).put(typeByte).put(dataLength).put(data);
        byte actualChecksum = checksum(tmp);
        if (actualChecksum != expectedChecksum) {
            throw new ChecksumException(expectedChecksum, actualChecksum);
        }

        // phew
        try {
            Type type = Type.valueOf(typeByte);
            switch (type) {
                case StatusRequest:
                    return new GjMessageStatusRequest();
                case Mode:
                    return new GjMessageMode(Mode.valueOf(data[0]));
                case ReportGps:
                    return new GjMessageReportGps(new String(data));
                case RequestGps:
                    return new GjMessageRequestGps();
                case Lighting:
                    return new GjMessageLighting(new String(data));
                case Text:
                    return new GjMessageText(new String(data));
                case Console:
                    return new GjMessageConsole(new String(data));
                case USB:
                    return new GjMessageUsb( data[0] != 0 ? true : false);
            }
        } catch (RuntimeException e) {
            throw new ParserException(e.getMessage());
        }
        return null;
    }

    protected static byte checksum(ByteBuffer byteBuffer) {
        int checksum = 0;

        for (int i : byteBuffer.array()) {
            checksum += i;
        }
        return (byte) checksum;
    }

    private static boolean findFirst(ByteBuffer bb, byte[] bytes) {
        while (bb.remaining() > 0) {
            if (compare(bb, bytes)) {
                // skip the preamble
                bb.position(bb.position() + preamble.length);
                return true;
            }
            bb.position(bb.position() + 1);
        }
        return false;
    }

    private static boolean compare(ByteBuffer bb, byte[] bytes) {
        if (bb.remaining() < bytes.length) {
            return false;
        }
        for (int i = 0; i < bytes.length; i++) {
            byte expected = bytes[i];
            byte actual = bb.get(bb.position() + i);
            if (actual != expected) {
                return false;
            }
        }
        return true;
    }

    protected static byte read(ByteBuffer bb) throws EOFException {
        if (bb.remaining() < 1) {
            throw new EOFException("Reading byte");
        }
        return bb.get();
    }

    protected static byte[] read(ByteBuffer bb, byte dataLength) throws EOFException {
        if (bb.remaining() < dataLength) {
            throw new EOFException("Reading bytes " + dataLength);
        }
        byte[] data = new byte[dataLength];
        bb.get(data, 0, dataLength);
        return data;
    }

    public byte[] toByteArray() {
        // preamble + type + length + data + checksum
        int length = preamble.length + 1 + 1 + data.length + 1;

        ByteBuffer buffer = ByteBuffer.allocate(length);

        buffer.put(preamble);
        buffer.put(type);
        buffer.put((byte) data.length);
        if (data.length > 0) {
            buffer.put(data);
        }
        buffer.put(checksum(buffer));

        return buffer.array();
    }

    @Override
    public String toString() {
        return Type.valueOf(type).toString();
    }

    public String toHexString() {
        StringBuilder sb = new StringBuilder();
        for (byte b : toByteArray()) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString();
    }

    public enum Type {
        StatusRequest((byte) 0x01),
        Mode((byte) 0x02),
        ReportGps((byte) 0x03),
        RequestGps((byte) 0x04),
        Lighting((byte) 0x05),
        Text((byte) 0x06),
        Console((byte) 0x10),
        USB((byte) 0x11);

        private final byte value;

        Type(byte value) {
            this.value = value;
        }

        public static Type valueOf(byte b) {
            switch (b) {
                case 0x01: return StatusRequest;
                case 0x02: return Mode;
                case 0x03: return ReportGps;
                case 0x04: return RequestGps;
                case 0x05: return Lighting;
                case 0x06: return Text;
                case 0x10: return Console;
                case 0x11: return USB;
            }
            throw new RuntimeException("Type: Illegal Value: " + b);
        }

        public byte getValue() {
            return value;
        }
    }

    public enum Mode {
        // standard buffered mode. This will buffer GPS updates until they are requested.
        Buffered((byte) 0x00),
        // non-buffered mode.  This will report GPS updates as they are received.
        NonBuffered((byte) 0x01);

        private final byte value;

        Mode(byte value) {
            this.value = value;
        }

        public static Mode valueOf(byte b) {
            if (b >= values().length)
                throw new RuntimeException("Mode: Illegal Value: " + b);
            return values()[b];
        }

        public byte getValue() {
            return value;
        }
    }

    public static class PreambleNotFoundException extends Exception {
        public PreambleNotFoundException() {
            super("Message preamble not found");
        }
    }

    public static class ParserException extends Exception {
        public ParserException(String whatBroke) {
            super("Parser Error: " + whatBroke);
        }
    }

    public static class ChecksumException extends Exception {
        public ChecksumException(byte expected, byte actual) {
            super(String.format("Checksum Error: expected:%02x actual:%02x ", expected, actual));
        }
    }

}

