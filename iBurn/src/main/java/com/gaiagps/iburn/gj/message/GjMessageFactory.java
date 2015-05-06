package com.gaiagps.iburn.gj.message;

import android.util.Log;

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

        GjMessageStatusRequest statusRequestMessage = new GjMessageStatusRequest();
        buf = statusRequestMessage.toByteArray();
        hex = statusRequestMessage.toHexString();
        s = statusRequestMessage.toString();

        GjMessageMode modeMessage = new GjMessageMode(GjMessage.Mode.Buffered);
        buf = modeMessage.toByteArray();
        hex = modeMessage.toHexString();
        s = modeMessage.toString();

        modeMessage = new GjMessageMode(GjMessage.Mode.NonBuffered);
        buf = modeMessage.toByteArray();
        hex = modeMessage.toHexString();
        s = modeMessage.toString();

        GjMessageText textMessage = new GjMessageText("HI");
        buf = textMessage.toByteArray();
        hex = textMessage.toHexString();
        s = textMessage.toString();

        LatLng latLng = new LatLng(40.7888, -119.20315);
        int id = 5;
        GjMessageReportGps reportGpsMessage = new GjMessageReportGps(id, latLng);
        buf = reportGpsMessage.toByteArray();
        hex = reportGpsMessage.toHexString();
        s = reportGpsMessage.toString();
    }

    public static void testStream() {

        ByteBuffer bb = ByteBuffer.allocate(4096);
        bb.put((byte) 0x01);
        bb.put((byte) 0x02);
        bb.put((byte) 0xFF);
        bb.put((byte) 0x04);
        bb.put((byte) 0xFF);
        bb.put((byte) 0x55);
        bb.put(new GjMessageText("123456").toByteArray());
        bb.put(new GjMessageText("abcdefghijklmnopqrstuvwxysABCDEFGHIJKLMNOPQRSTUVWXYZ").toByteArray());
        bb.put(new GjMessageStatusRequest().toByteArray());
        bb.put(new GjMessageMode(GjMessage.Mode.Buffered).toByteArray());
        bb.put(new GjMessageMode(GjMessage.Mode.NonBuffered).toByteArray());
        bb.put(new GjMessageReportGps(5, new LatLng(40.7888, -119.20315)).toByteArray());

        bb.limit(bb.position() - 5);  // IMPORTANT !!!
        bb.rewind(); // IMPORTANT !!!

        List<GjMessage> list = parseAll(bb);
        for (GjMessage m : list) {
            Log.e(TAG, m.toString());
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
                break;
            } catch (GjMessage.ChecksumException e) {
                // too bad, but continue
                break;
            } catch (GjMessage.PreambleNotFoundException e) {
                // garbage all the way thru
                break;
            } catch (GjMessage.ParserException e) {
                // some parameter value is off, continue
                break;
            }
        }
        return list;
    }
}

