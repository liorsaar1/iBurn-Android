package com.gaiagps.iburn.gj.ftdi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageFactory;
import com.gaiagps.iburn.gj.message.internal.GjMessageConsole;
import com.gaiagps.iburn.gj.message.internal.GjMessageError;
import com.gaiagps.iburn.gj.message.internal.GjMessageFtdi;
import com.gaiagps.iburn.gj.message.internal.GjMessageUsb;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.List;

public class FtdiService extends Service {
    public static final int FTDI_BUFFER_SIZE = 1024 * 1024 * 4; // 4 meg
    private final static String TAG = "FtdiService";
    public static final String FTDI_SERVICE_MESSSAGE = "message";
    public static final String FTDI_SERVICE_BYTES = "bytes";
    private static D2xxManager ftD2xx = null;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    boolean mThreadIsStopped = true;
    ByteBuffer bb;
    private FT_Device ftDev;
    private Runnable mLoop = new Runnable() {
        @Override
        public void run() {
            int readSize;
            long sleepDuration = 100;
            mThreadIsStopped = false;
            while (true) {
                if (mThreadIsStopped) {
                    console("thread stopped");
                    break;
                }

                synchronized (ftDev) {
                    readSize = ftDev.getQueueStatus();
                    if (readSize > 0) {
                        console("read:" + readSize);
                        byte[] inputBytes = new byte[readSize];
                        ftDev.read(inputBytes, readSize);
                        incoming(inputBytes);
                        if (readSize > bb.remaining()) {
                            error("INPUT OVERFLOW !!! :( ");
                            bb.rewind();
                        } else {
                            bb.put(inputBytes); // collect the input
                        }
                    } // end of if(readSize>0)
                } // end of synchronized

                // after every read - process
                if (readSize > 0) {
                    bb.limit(bb.position());
                    bb.rewind();
                    List<GjMessage> list = GjMessageFactory.parseAll(bb); // keeps unprocessed bytes in the buffer
                    bb.compact(); // remove only the used bytes
                    broadcastMessage(list);
                }

                // if nothing came in - sleep more
                if (readSize <= 0) {
                    sleepDuration += 100;
                } else {
                    sleepDuration = 100;
                }

                // always sleep a little
                try {
                    Thread.sleep(sleepDuration);
                } catch (InterruptedException e) {
                }

            }
        }
    };

    private BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                broadcastMessage(new GjMessageUsb(true));
                // open on send - not here
                openDevice();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                broadcastMessage(new GjMessageUsb(false));
                closeDevice();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        bb = ByteBuffer.allocate(FTDI_BUFFER_SIZE);

        try {
            ftD2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException ex) {
            error("D2XX:"+ex.toString());
        }

        IntentFilter usbFiler = new IntentFilter();
        usbFiler.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFiler.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, usbFiler);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThreadIsStopped = true;
        unregisterReceiver(usbReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        console("onStartCommand: " + START_STICKY);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void broadcastException(byte[] bytes, Throwable t) {
        error("Exception: " + t.getMessage());
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        t.printStackTrace(printWriter);
        byte[] stack = stringWriter.toString().getBytes();
        int length = stack.length;
        System.arraycopy(stack, 0, bytes, 0, length);
        error("ERROR:" + stringWriter.toString());
    }

    public int send(byte[] bytes) {
        //console("send:" + new String(bytes));
        if (ftDev == null) {
            openDevice();
        }
        return write(bytes);
    }

    private void incoming(byte[] bytes) {
        Intent intent = new Intent(FtdiServiceManager.ACTION_VIEW);
        intent.putExtra(FTDI_SERVICE_BYTES, bytes);
        sendBroadcast(intent);
    }

    private void console(String string) {
        broadcastMessage(new GjMessageConsole(string));
    }
    private void error(String string) {
        broadcastMessage(new GjMessageError(string));
    }

    private void broadcastMessage(GjMessage message) {
        Intent intent = new Intent(FtdiServiceManager.ACTION_VIEW);
        intent.putExtra(FTDI_SERVICE_MESSSAGE, message.toByteArray());
        sendBroadcast(intent);
    }

    private void broadcastMessage(List<GjMessage> list) {
        for (GjMessage message : list) {
            broadcastMessage(message);
        }
    }

    public int write(byte[] bytes) {
        if (ftDev == null) {
            error("FTDI device not found");
            return -1;
        }

        synchronized (ftDev) {
            if (!ftDev.isOpen()) {
                error("Device not open");
                return -1;
            }

            ftDev.setLatencyTimer((byte) 16);
            int written = ftDev.write(bytes, bytes.length);
            return written;
        }
    }

    private void openDevice() {
        if (ftDev != null) {
            console("open: device not null");
            if (ftDev.isOpen()) {
                console("open: already opened");
                if (mThreadIsStopped) {
                    console("open: thread stopped");
                    SetConfig(115200, (byte) 8, (byte) 1, (byte) 0, (byte) 3);
                    ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
                    ftDev.restartInTask();
                    new Thread(mLoop).start();
                }
                return;
            }
        }

        int devCount = ftD2xx.createDeviceInfoList(this);
        console("devCount: " + devCount);
        if (devCount <= 0) {
            error("No Devices found: " + devCount);
            return;
        }

        D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[devCount];
        ftD2xx.getDeviceInfoList(devCount, deviceList);

        for (int i = 0; i < deviceList.length; i++) {
            D2xxManager.FtDeviceInfoListNode node = deviceList[i];
            String info = "#" + i + ", id:" + node.id + ", serial:" + node.serialNumber + ", desc:" + node.description;
            console(info);
        }

        int deviceIndex = 0; // FIXME - must identify the right device

        if (ftDev == null) {
            ftDev = ftD2xx.openByIndex(this, deviceIndex);
        } else {
            synchronized (ftDev) {
                ftDev = ftD2xx.openByIndex(this, deviceIndex);
            }
        }

        if (ftDev.isOpen()) {
            broadcastMessage(new GjMessageFtdi(true));
            if (mThreadIsStopped) {
                SetConfig(115200, (byte) 8, (byte) 1, (byte) 0, (byte) 3);
                ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
                ftDev.restartInTask();
                new Thread(mLoop).start();
            }
        }
    }

    private void closeDevice() {
        broadcastMessage(new GjMessageFtdi(false));
        mThreadIsStopped = true;
        if (ftDev != null) {
            console("device closed");
            ftDev.close();
        }
        ftDev = null;
    }

    public void SetConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl) {
        if (!ftDev.isOpen()) {
            return;
        }

        // configure our port
        // reset to UART mode for 232 devices
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

        ftDev.setBaudRate(baud);

        switch (dataBits) {
            case 7:
                dataBits = D2xxManager.FT_DATA_BITS_7;
                break;
            case 8:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
            default:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
        }

        switch (stopBits) {
            case 1:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
            case 2:
                stopBits = D2xxManager.FT_STOP_BITS_2;
                break;
            default:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
        }

        switch (parity) {
            case 0:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
            case 1:
                parity = D2xxManager.FT_PARITY_ODD;
                break;
            case 2:
                parity = D2xxManager.FT_PARITY_EVEN;
                break;
            case 3:
                parity = D2xxManager.FT_PARITY_MARK;
                break;
            case 4:
                parity = D2xxManager.FT_PARITY_SPACE;
                break;
            default:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
        }

        ftDev.setDataCharacteristics(dataBits, stopBits, parity);

        short flowCtrlSetting;
        switch (flowControl) {
            case 0:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
            case 1:
                flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
                break;
            case 2:
                flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
                break;
            case 3:
                flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
                break;
            default:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
        }

        // see doc
        ftDev.setFlowControl(flowCtrlSetting, (byte) 0x11, (byte) 0x13);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        FtdiService getService() {
            return FtdiService.this;
        }
    }

}