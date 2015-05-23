package com.gaiagps.iburn.gj.message;

import android.util.Log;

import com.gaiagps.iburn.gj.message.internal.GjMessageConsole;
import com.gaiagps.iburn.gj.message.internal.GjMessageError;
import com.google.android.gms.maps.model.LatLng;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by liorsaar on 2015-04-21
 */

public class GjMessageFactory {
    protected final static String TAG = GjMessageFactory.class.getSimpleName();

    public static void test() {
        byte[] buf;
        String hex;
        String s;

        GjMessageText textMessage = new GjMessageText("HI");
        buf = textMessage.toByteArray();
        hex = textMessage.toHexString();
        s = textMessage.toString();

        LatLng latLng = new LatLng(40.7888, -119.20315);
        GjMessageGps reportGpsMessage = new GjMessageGps(latLng);
        buf = reportGpsMessage.toByteArray();
        hex = reportGpsMessage.toHexString();
        s = reportGpsMessage.toString();
    }

    public static ByteBuffer create1() {
        ByteBuffer bb = ByteBuffer.allocate(4096);
        bb.put((byte) 0x01);
        bb.put((byte) 0x02);
        bb.put((byte) 0xFF);
        bb.put((byte) 0x04);
        bb.put((byte) 0xFF);
        bb.put((byte) 0x55);
        bb.put(new GjMessageText("1234567890").toByteArray());
        bb.put(new GjMessageText("abcdefghijklmnopqrstuvwxysABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxysABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789").toByteArray());
        bb.put(new GjMessageStatusResponse((byte)0x1F).toByteArray());
        bb.put(new GjMessageGps(new LatLng(40.7888, -119.20315)).toByteArray());

        bb.flip(); // IMPORTANT !!!
        return bb;
    }

    public static ByteBuffer create2() {
        ByteBuffer bb = ByteBuffer.allocate(4096);
        for (int i = 0; bb.remaining() > 100; i++) {
            bb.put(new GjMessageText(i + "-abcdefghijklmnopqrstuvwxysABCDEFGHIJKLMNOPQRSTUVWXYZ").toByteArray());
        }

        bb.flip(); // IMPORTANT !!!
        return bb;
    }

    public static ByteBuffer create3() {
        ByteBuffer bb = ByteBuffer.allocate(50);
        bb.put(new GjMessageText("123456").toByteArray());

        bb.flip(); // IMPORTANT !!!
        return bb;
    }

    public static ByteBuffer create4() {
        StringBuffer sb = new StringBuffer();
        sb.append("bd ff 55 aa 35 00 01 01 00 35 ");
        sb.append("ff 55 aa 36 00 04 10 80 30 78 1d 6a 3d 3c b7 15 06 a6 16 91 23 00 00 b2 ");
        sb.append("");
        sb.append("");
        sb.append("");
        return fromString(sb.toString());
    }

    public static void testStream() {

        ByteBuffer bb = create1();

        ByteBuffer cc = ByteBuffer.allocate(400);
        cc.rewind();
        while (bb.remaining() > 0) {
            for (int i = 0; i < 7 && bb.remaining() > 0; i++) {
                byte b = bb.get();
                cc.put(b);
            }
            cc.limit(cc.position());
            cc.rewind();
            List<GjMessage> list = parseAll(cc);
            cc.compact();
            for (GjMessage message : list) {
                Log.e(TAG, message.toString());
            }
        }
    }

    public static void testStream2() {

        ByteBuffer bb = ByteBuffer.allocate(4096);
        bb.put((byte) 0x01);
        bb.put((byte) 0x02);
        bb.put((byte) 0xFF);
        bb.put((byte) 0x04);
        bb.put((byte) 0xFF);
        bb.put((byte) 0x55);
        bb.put(new GjMessageText("123456").toByteArray());
        bb.put(new GjMessageText("abcdefghijklmnopqrstuvwxysABCDEFGHIJKLMNOPQRSTUVWXYZ").toByteArray());
        bb.put(new GjMessageStatusResponse((byte)0x1F).toByteArray());
        bb.put(new GjMessageGps(new LatLng(40.7888, -119.20315)).toByteArray());

        bb.limit(bb.position() - 5);  // IMPORTANT !!!
        bb.rewind(); // IMPORTANT !!!

        List<GjMessage> list = parseAll(bb);
        for (GjMessage message : list) {
            Log.e(TAG, message.toString());
        }

        bb.limit(bb.limit() + 5);
        list = parseAll(bb);
        for (GjMessage m : list) {
            Log.e(TAG, m.toString());
        }
    }

    public static List<GjMessage> parseAll(ByteBuffer bb) {
        List<GjMessage> list = new ArrayList<>();
        while (bb.remaining() > 0) {
            int savePosition = bb.position();
            try {
                GjMessage message = GjMessage.create(bb);
                list.add(message);
            } catch (EOFException e) {
                // rewind to pre-eof position
                bb.position(savePosition);
                list.add(new GjMessageConsole("EOF reached. Remaining " + bb.remaining()));
                break;
            } catch (GjMessage.ChecksumException e) {
                // too bad, but continue
                list.add(new GjMessageError(e.getMessage()));
                continue;
            } catch (GjMessage.PreambleNotFoundException e) {
                // hmmm
                bb.position(savePosition);
                list.add(new GjMessageError(e.getMessage()));
                break;
            } catch (GjMessage.ParserException e) {
                // some parameter value is off, continue
                list.add(new GjMessageError(e.getMessage()));
                continue;
            }
        }
        return list;
    }

    public static List<GjMessage> parseAll(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes);
        bb.rewind();
        return parseAll(bb);
    }

    public static ByteBuffer fromString(String string) {
        String[] byteStrings = string.split(" ");
        ByteBuffer bb = ByteBuffer.allocate(byteStrings.length);
        for (String byteString : byteStrings) {
            byte b = Integer.decode("0x"+byteString).byteValue();
            bb.put(b);
        }
        bb.rewind();
        return bb;
    }
}

