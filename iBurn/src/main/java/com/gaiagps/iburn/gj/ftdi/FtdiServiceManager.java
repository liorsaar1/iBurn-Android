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
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by liorsaar on 2015-05-06
 */
public class FtdiServiceManager {
    private static final String TAG = FtdiServiceManager.class.getSimpleName();
    public static final String ACTION_VIEW = "com.gaiagps.iburn.gj.ftdi.VIEW";
    private FtdiService mService;
    private boolean mBound = false;

    private final ScheduledExecutorService readScheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> readScheduledFuture;

    public boolean isBound() {
        return mBound;
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection ;

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
        if (readScheduledFuture != null) {
            if (!readScheduledFuture.isCancelled()) {
                readScheduledFuture.cancel(false);
            }
            readScheduledFuture = null;
        }
    }

    public void onResume(Activity activity, final Handler handler) {
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
                    console("Service bound");
                    scheduleRead(handler);
                }

                @Override
                public void onServiceDisconnected(ComponentName arg0) {
                    mService = null;
                    mBound = false;
                }
            };

            Intent intent = new Intent(activity, FtdiService.class);
            activity.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } else {
            // start/restart reading
            scheduleRead(handler);
        }
    }

    public int send(byte[] bytes) {
        return mService.send(bytes);
    }

    public int read(byte[] bytes) {
        return mService.read(bytes);
    }

    private final ByteBuffer bb = ByteBuffer.allocate(FtdiService.FTDI_BUFFER_SIZE+400);
    private final byte[] ftdiInputBuffer = new byte[FtdiService.FTDI_BUFFER_SIZE];

    public void scheduleRead(final Handler handler) {
//        if( true ) return ;

        if (!mBound) {
            console("scheduleRead: not bound");
            return;
        }
        if (readScheduledFuture != null) {
            console("scheduleRead: already running");
            return;
        }

        console("scheduleRead: start");
        final Runnable readLoopRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final int length = mService.read(ftdiInputBuffer);
                    if (length > 0) {
                        bb.put(ftdiInputBuffer, 0, length);
                        bb.limit(bb.position());
                        bb.rewind();
                        List<GjMessage> list = GjMessageFactory.parseAll(bb);
                        bb.compact();
                        Message handlerMessage = new Message();
                        handlerMessage.obj = list;
                        handler.sendMessage(handlerMessage);
                    }
                } catch (Throwable t) {
                    console("ERROR:" + t.getMessage());
                }
            }
        };
        readScheduledFuture = readScheduler.scheduleAtFixedRate(readLoopRunnable, 1, 2, TimeUnit.SECONDS);
    }


    private TextView messageConsole;
    public void setConsole(TextView messageConsole) {
        this.messageConsole = messageConsole;
    }

    private void console(String string) {
        Log.e(TAG, string);
        if (messageConsole == null) {
            return;
        }
        messageConsole.append(string + "\n");
        scrollToEnd(messageConsole);
    }

    public static void scrollToEnd(final TextView tv) {
        tv.post(new Runnable() {
            @Override
            public void run() {
                final int scrollAmount = tv.getLayout().getLineTop(tv.getLineCount()) - tv.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0)
                    tv.scrollTo(0, scrollAmount);
                else
                    tv.scrollTo(0, 0);
            }
        });
    }

    private TextView bytesConsole;
    public void setIncoming(TextView bytesConsole) {
        this.bytesConsole = bytesConsole;
    }

    private void incoming(byte[] bytes) {
        if (bytesConsole == null) {
            return;
        }
        bytesConsole.append(new String(bytes));
        scrollToEnd(bytesConsole);
    }

    private void incoming(String string) {
        if (bytesConsole == null) {
            return;
        }
        bytesConsole.append(string);
        scrollToEnd(bytesConsole);
    }


    BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            System.out.println("BA intent:" + intent);
            String action = intent.getAction();
            if (ACTION_VIEW.equals(action)) {
                String message = intent.getStringExtra("message");
                String error = intent.getStringExtra("error");
                if (error != null) {
                    console("Service: ERROR: " + error);
                    return;
                }
                if (message != null) {
                    console("Service:" + message);
                    return;
                }
                byte[] bytes = intent.getByteArrayExtra("bytes");
                if (bytes != null) {
                    console("Service: bytes: " + new String(bytes));
                    incoming(bytes);
                    return;
                }
            }
        }
    };

}
