package com.gaiagps.iburn.gj.ftdi;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.gj.message.GjMessageFactory;
import com.gaiagps.iburn.gj.message.GjMessageText;

import java.nio.ByteBuffer;

public class FtdiActivity extends Activity {
    private static final String TAG = FtdiActivity.class.getSimpleName();
    private TextView messageConsole;
    private TextView bytesConsole;

    private int counter = 1;
    private Button manyButton;
    private Button readButton;
    private Button readLoopButton;

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
        readLoopButton = (Button) findViewById(R.id.ftdiReadLoop);

        console("onCreate \n");
        // service manager
        if (ftdiServiceManager == null) {
            ftdiServiceManager = new FtdiServiceManager();
            ftdiServiceManager.setConsole(messageConsole);
            ftdiServiceManager.setIncoming(bytesConsole);
        }
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

        ByteBuffer bb = GjMessageFactory.create2();
        byte[] bytes = new byte[bb.limit()];
        bb.get(bytes, 0, bb.limit());
        int written = ftdiServiceManager.send(bytes);
        if (written == bb.limit()) {
            console("Sent: written:" + written + "\n");
        } else {
            console("ERROR: expected: " + bb.limit() + " written:" + written + "\n");
        }

//
//        String s = counter + "-ABCDEFGHIJKLMNOP-" + counter++ + "\n";
//        console("send:" + s + "\n");
//        int written = ftdiServiceManager.send(s.getBytes());
//        if (written == s.length()) {
//            console("Sent: written:" + written + "\n");
//        } else {
//            console("ERROR: expected: " + s.length() + " written:" + written + "\n");
//        }
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

        for (int i = 0 ; i < 10; i++) {
            sendGjMessage();
        }
    }

    private void sendGjMessage() {
        String s = counter + "-ABCDEFGHIJKLMNOP-" + counter++;
        GjMessageText msgText = new GjMessageText(s);
        console("send:" + msgText.toString() + "\n");
        int written = ftdiServiceManager.send(msgText.toByteArray());
        int length = msgText.toByteArray().length;
        if (written == length) {
            console("Sent: written:" + written + "\n");
        } else {
            console("ERROR: expected: " + length + " written:" + written + "\n");
        }
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
                console("Writing: 1000 \n");
            }

            @Override
            protected Object doInBackground(Object[] params) {
                for (int i = 0; i < 1000; i++) {
                    String s = counter + "-ABCDEFGHIJKLMNOP-" + counter++ + "\n";
                    int written = ftdiServiceManager.send(s.getBytes());
                    if (written != s.length()) {
                        return "ERROR: expected: " + s.length() + " written:" + written + "\n";
                    }
                    if (i % 100 == 0) {
                        onProgressUpdate(i);
                    }
                }
                return "DONE\n";
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
            console("Bytes read: " + length + "\n");
            if (length > 0) {
                incoming(bytes);
            }
        } catch (Throwable t) {
            console("ERROR:" + t.getMessage());
        }
        readButton.setEnabled(true);
    }

    public void onClickReadLoop(View v) {
        if (!ftdiServiceManager.isBound()) {
            return;
        }
        readLoopButton.setEnabled(false);

        ftdiServiceManager.scheduleRead(FtdiActivity.this);
    }

    private void console(String s) {
        messageConsole.append(s);
        messageConsole.post(new Runnable() {
            @Override
            public void run() {
                final int scrollAmount = messageConsole.getLayout().getLineTop(messageConsole.getLineCount()) - messageConsole.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0)
                    messageConsole.scrollTo(0, scrollAmount);
                else
                    messageConsole.scrollTo(0, 0);
            }
        });
    }

    private void incoming(byte[] bytes) {
        bytesConsole.append(new String(bytes));
        bytesConsole.post(new Runnable() {
            @Override
            public void run() {
                final int scrollAmount = bytesConsole.getLayout().getLineTop(bytesConsole.getLineCount()) - bytesConsole.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0)
                    bytesConsole.scrollTo(0, scrollAmount);
                else
                    bytesConsole.scrollTo(0, 0);
            }
        });
    }

}