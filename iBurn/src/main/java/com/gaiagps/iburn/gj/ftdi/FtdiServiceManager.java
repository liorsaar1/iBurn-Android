package com.gaiagps.iburn.gj.ftdi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageFactory;
import com.gaiagps.iburn.gj.message.GjMessageListener;
import com.gaiagps.iburn.gj.message.GjMessageText;
import com.gaiagps.iburn.gj.message.internal.GjMessageConsole;

import java.nio.ByteBuffer;
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
                    send(new GjMessageText("Online"));
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
            listener.incoming(bytes);
        }
    }

    private void console(String string) {
        GjMessageConsole message = new GjMessageConsole(string);
        dispatch(message);
    }

    private void dispatch(GjMessage message) {
        for (GjMessageListener listener : ftdiListeners) {
            listener.onMessage(message);
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

    public int send(GjMessage message) {
        int written = send(message.toByteArray());
        if (written == message.toByteArray().length) {
            console("Sent: written:" + written);
        } else {
            console("ERROR: expected: " + message.toByteArray().length + " written:" + written);
        }
        return written;
    }

    public int send(ByteBuffer bb) {
        byte[] bytes = new byte[bb.limit()];
        bb.get(bytes, 0, bb.limit());
        int written = send(bytes);
        if (written == bb.limit()) {
            console("Sent: written:" + written);
        } else {
            console("ERROR: expected: " + bb.limit() + " written:" + written);
        }
        return written;
    }

    public int send(byte[] bytes) {
        return mService.send(bytes);
    }

    public int read(byte[] bytes) {
        return mService.read(bytes);
    }

}
