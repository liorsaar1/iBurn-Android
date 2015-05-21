package com.gaiagps.iburn.gj.message;

import com.gaiagps.iburn.gj.message.internal.GjMessageConsole;
import com.gaiagps.iburn.gj.message.internal.GjMessageError;
import com.gaiagps.iburn.gj.message.internal.GjMessageFtdi;
import com.gaiagps.iburn.gj.message.internal.GjMessageUsb;

import java.io.EOFException;
import java.nio.ByteBuffer;

/**
 * Created by liorsaar on 2015-04-21
 */

/*

https://docs.google.com/document/d/1COgntcMCB9yj2noEOQpfmL1X5a2sasidZgQr8DDS_bY/edit

(ROUGH DRAFT)

Protocol definition
* Fully asynchronous. No tablet->controller requests. Packet number used to determine sequence and identification
* Tablet refreshes every 2 seconds (subject to experimentation)
* Vehicle number set on the controller’s dip switch, reported (controller->tablet) in status response
* Lighting Cues sync:
   * The originator of a cue is the master, until a cue is received from another vehicle
   * The originator of a cue is responsible to refresh that cue for the pack, every 5 seconds (subject to experimentation)
   * The originator stops refreshing once a cue is received from another vehicle
   * (Suggestion from Zach) Lighting cue should contain a timestamp (synchronized with the last GPS time received)
* Checksum
   * Tablet->controller: reported as an error with packet ID - retransmitted
   * Controller->tablet: ignored  (Zach say yes)

================================================================================================

Packet sent to the controller (wireless module)


Packet layout:
[Preamble][Packet number][Vehicle number**] [Message Type] [Data length] [Data] [Checksum]


**(place holder to make structure on Tablet cleaner, ignored by radio controller)


Timing:
A packet must take less than 500mSec to write otherwise an error will be returned


Preamble:
All message begin with 0xFF 0x55 0xAA


Packet number:
Single byte incremented with each packet sent.  For checksum error responses, the packet number will match the failed packet number.


Message Type:
0x01 Status request (Will stream status once every 5 seconds)
0x02 Mode (not needed since GPS is read by PCB)
0x03 Report own GPS location  (not needed since GPS is read by PCB)
0x04 Request buffered GPS locations  (Not needed as GPS data will be streamed)
0x05 Lighting cues
0x06 Text message


Data length:
Can be any number between 0 and 140 (0x00 - 0x8C)
For [Status request] use length of 0
For [Mode] use length 1


Data:
The length of this data must match the [Data length]
The Lighting cue and Text message formats are somewhat arbitrary, whatever format you choose will be the format returned to your app.
For [Status request] send no data bytes


Checksum:
This will be a single byte sum (truncated to the LSB) that includes ALL bytes in the packet including the preamble. For checksum error responses, the packet number will match the failed packet number.


Sample packet:
0xFF 0x55 0xAA 0x06 0x02 0x48 0x49 0x97
This is a text message sending "HI".


============================================================================================

Packet received from the wireless module


Packet layout:
[Preamble][Packet number] [Vehicle number] [Message Type] [Data length] [Data] [Checksum]


Preamble:
All message begin with
0xFF 0x55 0xAA


Packet number:
Single byte incremented with each packet sent.  For checksum error responses, the packet number will match the failed packet number.


Vehicle number:
For [Response] and [Status request] responses this will be your own vehicle number
All other messages will have the originating sender's vehicle number (lighting cues, text messages, GPS updates)


Message Type:
0x00 Response  (local)
0x01 Status response (local)
0x04 Buffered GPS locations (from all vehicles, including self)
0x05 Lighting cues (from remote vehicles)
0x06 Text message (from remote vehicles)


Data length:
Will be some number between 0 and 140 (0x00 - 0x8C)


Data:
[Response]
0x00 Checksum Error
0x01 No error


[Status request]
This is a single byte
Bit 0 (LSB) -  0= Radio module is up and operational, 1 = Radio module is not ready
Bit 1 -  0 = Voltage to the system is within range 1 = Voltage to system is out of range
Bit 2 -  0 = System temperature is within range  1 = System temperature is out of range
Bit 3 -  0 = Received good data from compass 1 = Problem with compass data
Bit 4 -  0 = Received good data from GPS 1 = Problem with GPS data
Bit 5 -  TBD
Bit 6 -  TBD
Bit 7 (MSB) -  TBD


[Buffered GPS locations]
Multiple packets will be returned, one for each vehicle that has reported its location.
The GPS data string will be be formated like:
Byte 0          Vehicle number
Bytes 1-4     Number of milliseconds from the beginning of the week (Unsigned long)
Bytes 5-8     Longitude  (Signed Long)
Bytes 9-12   Latitude  (Signed Long)
Bytes 13-16 Heading (0.0 to 360.0 deg, four byte float)


A packet with a type 0x04 and a data length of 0 will notify the app that the entire list has been transmitted.


[Lighting cues] and [Text messages]
The format will be determined by the app.  These messages will be reported to the app as soon as they are received.  If a message is received during communications with the host, the new message will be buffered just long enough for the current communication to be finished.


Notes:
The update rate for data is TBD depending on system bandwidth (i.e. not sure how often we can send GPS updates, maybe 1 per second, maybe less)
System latency is TBD

 */

public class GjMessage {
    public static final byte[] preamble = {(byte) 0xFF, (byte) 0x55, (byte) 0xAA};
    protected final static String TAG = GjMessage.class.getSimpleName();
    private static final int TYPE_LENGTH = 1;
    private static final int NUMBER_LENGTH = 1;
    private static final int VEHICLE_LENGTH = 1;
    private static final int DATA_LENGTH = 1;
    private static final int CHECKSUM_LENGTH = 1;
    protected byte type;
    protected byte number;
    protected byte vehicle;
    protected byte[] data = new byte[0];

    public GjMessage(Type type) {
        this.type = type.getValue();
        this.number = 23;
        this.vehicle = 34;
    }

    public byte getByte() {
        return data[0];
    }

    public static GjMessage create(ByteBuffer bb) throws ChecksumException, EOFException, PreambleNotFoundException, ParserException {
        // find first preamble
        if (!findFirst(bb, preamble)) {
            throw new PreambleNotFoundException();
        }
        // read body
        byte typeByte = read(bb);
        byte number = read(bb);
        byte vehicle = read(bb);
        byte dataLength = read(bb);
        byte[] data = read(bb, dataLength);
        byte expectedChecksum = read(bb);

        // verify checksum
        int messageLength = preamble.length + TYPE_LENGTH + NUMBER_LENGTH + VEHICLE_LENGTH + DATA_LENGTH + data.length;
        ByteBuffer tmp = ByteBuffer.allocate(messageLength);
        tmp.put(preamble).put(typeByte).put(number).put(vehicle).put(dataLength).put(data);
        byte actualChecksum = checksum(tmp);
        if (actualChecksum != expectedChecksum) {
            throw new ChecksumException(expectedChecksum, actualChecksum);
        }

        // phew
        try {
            Type type = Type.valueOf(typeByte);
            switch (type) {
                case Response:
                    return new GjMessageResponse();
                case StatusResponse:
                    return new GjMessageStatusResponse(data[0]);
                case Gps:
                    return new GjMessageGps(new String(data));
                case Lighting:
                    return new GjMessageLighting(new String(data));
                case Text:
                    return new GjMessageText(new String(data));
                case Console:
                    return new GjMessageConsole(new String(data));
                case Error:
                    return new GjMessageError(new String(data));
                case USB:
                    return new GjMessageUsb( data[0] != 0 ? true : false);
                case FTDI:
                    return new GjMessageFtdi( data[0] != 0 ? true : false);
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
        int messageLength = preamble.length + TYPE_LENGTH + NUMBER_LENGTH + VEHICLE_LENGTH + DATA_LENGTH + data.length + CHECKSUM_LENGTH;

        ByteBuffer buffer = ByteBuffer.allocate(messageLength);

        buffer.put(preamble);
        buffer.put(type);
        buffer.put(number);
        buffer.put(vehicle);
        buffer.put((byte) data.length);
        if (data.length > 0) {
            buffer.put(data);
        }
        // TODO make sure last byte is 0
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
        Response((byte) 0x00),
        StatusResponse((byte) 0x01),
        Gps((byte) 0x04),
        Lighting((byte) 0x05),
        Text((byte) 0x06),
        Console((byte) 0x10),
        Error((byte) 0x11),
        USB((byte) 0x12),
        FTDI((byte) 0x13);

        private final byte value;

        Type(byte value) {
            this.value = value;
        }

        public static Type valueOf(byte b) {
            switch (b) {
                case 0x00: return Response;
                case 0x01: return StatusResponse;
                case 0x04: return Gps;
                case 0x05: return Lighting;
                case 0x06: return Text;
                case 0x10: return Console;
                case 0x11: return Error;
                case 0x12: return USB;
                case 0x13: return FTDI;
            }
            throw new RuntimeException("Type: Illegal Value: " + b);
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

