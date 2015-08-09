package com.gaiagps.iburn.gj.ftdi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageFactory;
import com.gaiagps.iburn.gj.message.GjMessageListener;
import com.gaiagps.iburn.gj.message.GjMessageResponse;
import com.gaiagps.iburn.gj.message.internal.GjMessageConsole;
import com.gaiagps.iburn.gj.message.internal.GjMessageError;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by liorsaar on 2015-05-06
 */
public class FtdiServiceManager {
    public static final String ACTION_VIEW = "com.gaiagps.iburn.gj.ftdi.VIEW";
    private static final String TAG = FtdiServiceManager.class.getSimpleName();
    private final ScheduledExecutorService readScheduler = Executors.newScheduledThreadPool(1);
    private final ByteBuffer bb = ByteBuffer.allocate(FtdiService.FTDI_BUFFER_SIZE + 400);
    private final byte[] ftdiInputBuffer = new byte[FtdiService.FTDI_BUFFER_SIZE];
    private final List<GjMessageListener> ftdiListeners;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            System.out.println("BA intent:" + intent);
            String action = intent.getAction();
            byte[] bytes;
            if (ACTION_VIEW.equals(action)) {
                bytes = intent.getByteArrayExtra(FtdiService.FTDI_SERVICE_MESSSAGE);
                if (bytes != null) {
                    dispatch(bytes);
                    return;
                }
                bytes = intent.getByteArrayExtra(FtdiService.FTDI_SERVICE_BYTES);
                if (bytes != null) {
                    incoming(bytes);
                    return;
                }
            }
        }
    };

    private FtdiService mService;
    private boolean mBound = false;
    private ServiceConnection mConnection;

    public FtdiServiceManager(List<GjMessageListener> ftdiListeners) {
        this.ftdiListeners = ftdiListeners;
    }

    public boolean isBound() {
        return mBound;
    }

    public void onStart(Activity activity) {
        console("onStart: bound:" + mBound);
        // listen to the service
        IntentFilter filter = new IntentFilter(ACTION_VIEW);
        activity.registerReceiver(receiver, filter);
    }

    public void onStop(Activity activity) {
        console("onStop: bound:" + mBound);
        // NEVER Unbind from the service
        if (false) {
            activity.unbindService(mConnection);
            mBound = false;
        }
    }

    public void onPause(Activity activity) {
        console("onPause: bound:" + mBound);
    }

    public void onResume(Activity activity) {
        console("onResume: bound:" + mBound);
        // Bind to LocalService
        if (!mBound) {

            //
            mConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className, IBinder service) {
                    // We've bound to LocalService, cast the IBinder and get LocalService instance
                    FtdiService.LocalBinder binder = (FtdiService.LocalBinder) service;
                    mService = binder.getService();
                    mBound = true;
                    console("Service Bound");
                    mService.open();
                }

                @Override
                public void onServiceDisconnected(ComponentName arg0) {
                    mService = null;
                    mBound = false;
                }
            };

            Intent intent = new Intent(activity, FtdiService.class);
            activity.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void incoming(byte[] bytes) {
        for (GjMessageListener listener : ftdiListeners) {
            try {
                listener.incoming(bytes);
            } catch (Throwable t) {
            }
        }
    }

    private void console(String string) {
        Log.e(TAG, "console: " + string);
        GjMessageConsole message = new GjMessageConsole(string);
        dispatch(message);
    }

    private void error(String string) {
        Log.e(TAG, "error: " + string);
        GjMessageError message = new GjMessageError(string);
        dispatch(message);
    }

    private void dispatch(GjMessage message) {
        if (message instanceof GjMessageResponse) {
            historyHandle((GjMessageResponse)message);
        }
        for (GjMessageListener listener : ftdiListeners) {
            try {
                listener.onMessage(message);
            } catch (Throwable t) {
            }
        }
    }

    private void dispatch(byte[] bytes) {
        dispatch(GjMessageFactory.parseAll(bytes));
    }

    private void dispatch(List<GjMessage> list) {
        for (GjMessage message : list) {
            dispatch(message);
        }
    }

    public static int outgoingPacketNumber = 1;

    private Handler handler = new Handler();

    public void send(final GjMessage message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                // console
                console("Send:" + message.toString());
                // save in history
                if (! (message instanceof GjMessageResponse))
                    message.setPacketNumber((byte) outgoingPacketNumber++);
                historyPut(message);
                // send
                int written = send(message.toByteArray());
                if (written == message.toByteArray().length) {
                    console("Sent:" + message.toString());
                } else {
                    error("Send ERROR: expected: " + message.toByteArray().length + " written:" + written);
                }
            }
        });
        //return written;
    }

    public int send(ByteBuffer bb, int count, long time) {
        byte[] bytes = new byte[bb.limit()];
        bb.get(bytes, 0, bb.limit());
        int written=-1;
        for (int i = 0; i < count; i++) {
            written = send(bytes);
            if (written == bb.limit()) {
            } else {
                error("ERROR: expected: " + bb.limit() + " written:" + written);
                break;
            }
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        console("Sent: written:" + written + " " + count);
        return written;
    }

    public int send(ByteBuffer bb) {
        byte[] bytes = new byte[bb.limit()];
        bb.get(bytes, 0, bb.limit());
        int written = send(bytes);
        if (written == bb.limit()) {
            console("Sent: written:" + written);
        } else {
            error("ERROR: expected: " + bb.limit() + " written:" + written);
        }
        return written;
    }

    public int send(byte[] bytes) {
        return mService.send(bytes);
    }

    // history
    private List<GjMessage> history = new ArrayList<>();

    private void historyPut(GjMessage message) {
        history.add(message);
        if (history.size() > 20) {
            history.remove(0);
        }
    }

    private void historyHandle(GjMessageResponse response) {
        // why did we get this ?
        if (response.isOK()) {
            console("CHEKSUM OK: " + response.getPacketNumber());
            return;
        }
        // error
        //console("CHEKSUM ERROR: " + response.getPacketNumber());
        GjMessage resend = historyGet(response.getPacketNumber());
        if (resend == null) {
            console("CHEKSUM ERROR: not found" + response.getPacketNumber());
            return;
        }
        console("CHEKSUM ERROR: resending " + response.getPacketNumber());
        send(resend);
    }

    private GjMessage historyGet(byte packetNumber) {
        for (GjMessage message : history) {
            if (message.getPacketNumber() == packetNumber) {
                return message;
            }
        }
        return null;
    }

    public static void loopback(Activity activity, byte[] bytes) {
        FtdiServiceManager.outgoingPacketNumber++;

        Intent intent = new Intent(FtdiServiceManager.ACTION_VIEW);
        intent.putExtra(FtdiService.FTDI_SERVICE_MESSSAGE, bytes);
        activity.sendBroadcast(intent);
    }
}
