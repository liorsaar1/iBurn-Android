package com.gaiagps.iburn.gj.ftdi;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageFactory;
import com.gaiagps.iburn.gj.message.GjMessageText;

import java.nio.ByteBuffer;
import java.util.List;

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

        console("onCreate");
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
        ftdiServiceManager.onResume(this, ftdiMessageHandler);
    }

    private Handler ftdiMessageHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            if (inputMessage.obj == null) {
                console("Error: null");
                return;
            }
            if (!(inputMessage.obj instanceof List)) {
                console("Error: " + inputMessage.obj);
                return;
            }
            List<GjMessage> list = (List<GjMessage>) inputMessage.obj;
            console("list !!!!" + list.size());
            for (GjMessage message : list) {
                Log.e(TAG, message.toString());
                console( ">>>"+message.toString()+"\n");
            }

        }
    };


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
            console("Sent: written:" + written);
        } else {
            console("ERROR: expected: " + bb.limit() + " written:" + written);
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
        console("send:" + msgText.toString());
        int written = ftdiServiceManager.send(msgText.toByteArray());
        int length = msgText.toByteArray().length;
        if (written == length) {
            console("Sent: written:" + written);
        } else {
            console("ERROR: expected: " + length + " written:" + written);
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
                console("Writing: 1000");
            }

            @Override
            protected Object doInBackground(Object[] params) {
                for (int i = 0; i < 1000; i++) {
                    String s = counter + "-ABCDEFGHIJKLMNOP-" + counter++ + "\n";
                    int written = ftdiServiceManager.send(s.getBytes());
                    if (written != s.length()) {
                        return "ERROR: expected: " + s.length() + " written:" + written;
                    }
                    if (i % 100 == 0) {
                        onProgressUpdate(i);
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

    private void console(String string) {
        messageConsole.append(string + "\n");
        FtdiServiceManager.scrollToEnd(messageConsole);
    }

    private void incoming(byte[] bytes) {
        bytesConsole.append(new String(bytes));
        FtdiServiceManager.scrollToEnd(bytesConsole);
    }

}