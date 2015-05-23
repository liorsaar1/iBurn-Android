package com.gaiagps.iburn.gj.ftdi;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageFactory;
import com.gaiagps.iburn.gj.message.GjMessageListener;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FtdiActivity extends Activity implements GjMessageListener {
    private TextView messageConsole;
    private TextView bytesConsole;

    private int counter = 1;
    private Button manyButton;
    private Button readButton;

    private static FtdiServiceManager ftdiServiceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftdi);
        messageConsole = (TextView) findViewById(R.id.messageConsole);
        messageConsole.setMovementMethod(new ScrollingMovementMethod());

        bytesConsole = (TextView) findViewById(R.id.bytesConsole);
        bytesConsole.setMovementMethod(new ScrollingMovementMethod());

        manyButton = (Button) findViewById(R.id.ftdiSendMany);
        readButton = (Button) findViewById(R.id.ftdiRead);

        console("onCreate");
        // service manager
        if (ftdiServiceManager == null) {
            ftdiServiceManager = new FtdiServiceManager(getFtdiListeners());
        }
    }

    private List<GjMessageListener> getFtdiListeners() {
        List<GjMessageListener> list = new ArrayList<>();
        list.add(this);
        return list;
    }

    @Override
    protected void onStart() {
        super.onStart();
        ftdiServiceManager.onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ftdiServiceManager.onStop(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ftdiServiceManager.onPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ftdiServiceManager.onResume(this);
    }

    public void onClickSend(View v) {
        if (!ftdiServiceManager.isBound()) {
            return;
        }
        sendOne();
    }

    private void sendOne() {
        ByteBuffer bb = GjMessageFactory.create3();
        int written = ftdiServiceManager.send(bb);
    }

    public void onClickSendTen(View v) {
        if (!ftdiServiceManager.isBound()) {
            return;
        }
        for (int i = 0; i < 10; i++) {
            sendOne();
        }
    }

    public void onClickSendMessage(View v) {
        if (!ftdiServiceManager.isBound()) {
            return;
        }

        for (int i = 0 ; i < 1; i++) {
            sendGjMessage();
        }
    }

    private void sendGjMessage() {
        ByteBuffer bb = GjMessageFactory.create4();
        loopback(bb.array());
        return;
    }

    private void loopback(byte[] bytes) {
        Intent intent = new Intent(FtdiServiceManager.ACTION_VIEW);
        intent.putExtra(FtdiService.FTDI_SERVICE_MESSSAGE, bytes);
        sendBroadcast(intent);
    }

    public void onClickSendMany(View v) {
        if (!ftdiServiceManager.isBound()) {
            return;
        }
        manyButton.setEnabled(false);

        AsyncTask sendManyTask = new AsyncTask() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                console("Writing: 1-1000");
            }

            @Override
            protected Object doInBackground(Object[] params) {
                ByteBuffer bb = GjMessageFactory.create1();
                byte[] bytes = new byte[bb.limit()];
                bb.get(bytes, 0, bb.limit());
                for (int i = 0; i < 1000; i++) {
                    int written = ftdiServiceManager.send(bytes);
                    if (written != bb.limit()) {
                        return "ERROR: written " + written;
                    }
                }
                return "DONE";
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                console(o.toString());
                manyButton.setEnabled(true);
            }
        };

        sendManyTask.execute();
    }

    public void onClickRead(View v) {
        if (!ftdiServiceManager.isBound()) {
            return;
        }
        readButton.setEnabled(false);
        try {
            byte[] bytes = new byte[4096];
            int length = ftdiServiceManager.read(bytes);
            console("Bytes read: " + length);
            if (length > 0) {
                incoming(bytes);
            }
        } catch (Throwable t) {
            console("ERROR:" + t.getMessage());
        }
        readButton.setEnabled(true);
    }

    public void onClickReadLoop(View v) {
//        if (!ftdiServiceManager.isBound()) {
//            return;
//        }
//        readLoopButton.setEnabled(false);
//
//        ftdiServiceManager.scheduleRead(FtdiActivity.this);
    }

    public void console(String string) {
        messageConsole.append(string + "\n");
        scrollToEnd(messageConsole);
    }

    public void incoming(byte[] bytes) {
        String hex = GjMessage.toHexString(bytes);
        bytesConsole.append(hex + "\n");
        scrollToEnd(bytesConsole);
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


    @Override
    public void onMessage(GjMessage message) {
        console(message.toString());
    }
}