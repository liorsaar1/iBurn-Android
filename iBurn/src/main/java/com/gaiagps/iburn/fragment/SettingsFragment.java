package com.gaiagps.iburn.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.activity.MainActivity;
import com.gaiagps.iburn.gj.ftdi.FtdiServiceManager;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageGps;
import com.gaiagps.iburn.gj.message.GjMessageListener;
import com.gaiagps.iburn.gj.message.GjMessageResponse;
import com.gaiagps.iburn.gj.message.GjMessageStatusResponse;
import com.gaiagps.iburn.gj.message.GjMessageText;
import com.gaiagps.iburn.gj.message.internal.GjMessageFtdi;
import com.gaiagps.iburn.gj.message.internal.GjMessageUsb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by liorsaar on 4/19/15
 */
public class SettingsFragment extends Fragment implements GjMessageListener {
    private static final String TAG = "SettingsFragment";

    private View messageEditTextContainer;
    private EditText messageEditText;
    private static TextView messageConsole;
    private static TextView messageIncoming;
    private static TextView statusUsb, statusFtdi;
    private static TextView statusRadio, statusVoltage, statusTemp, statusCompass, statusGps;
    private static TextView statusPacketNumber, statusVehicle, statusVersion, statusChecksumErrorCounter;
    private static Button testSendResponse, testSendText;
    public static byte sVehicleNumber =0;
    public static int sChecksumErrorCounter =0;


    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTheme(R.style.Theme_GJ);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        messageEditText = (EditText) view.findViewById(R.id.GjMessageEditText);
        messageConsole = (TextView) view.findViewById(R.id.GjMessageConsole);
        messageConsole.setMovementMethod(new ScrollingMovementMethod());
        messageIncoming = (TextView) view.findViewById(R.id.GjIncoming);
        messageIncoming.setMovementMethod(new ScrollingMovementMethod());

        testSendResponse = (Button)view.findViewById(R.id.GjTestSendResponse);
        testSendResponse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTestSendResponse(v);
            }
        });
        testSendText = (Button)view.findViewById(R.id.GjTestSendText);
        testSendText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTestSendText(v);
            }
        });

        statusUsb = (TextView)view.findViewById(R.id.GjStatusUsb);
        statusFtdi = (TextView)view.findViewById(R.id.GjStatusFtdi);
        statusRadio = (TextView)view.findViewById(R.id.GjStatusRadio);
        statusVoltage = (TextView)view.findViewById(R.id.GjStatusVoltage);
        statusTemp = (TextView)view.findViewById(R.id.GjStatusTemp);
        statusCompass = (TextView)view.findViewById(R.id.GjStatusCompass);
        statusGps = (TextView)view.findViewById(R.id.GjStatusGps);
        statusVehicle = (TextView)view.findViewById(R.id.GjStatusVehicle);
        statusPacketNumber = (TextView)view.findViewById(R.id.GjStatusPacketNumber);
        statusVersion = (TextView)view.findViewById(R.id.GjStatusVersion);
        statusChecksumErrorCounter = (TextView)view.findViewById(R.id.GjStatusChecksumErrorCounter);

        setVersion(getActivity());
        checkUsb(getActivity());
        queueDispatch();
        setStatusChecksumErrorCounter(sChecksumErrorCounter);

        return view;
    }

    private void broadcastMessage(GjMessage message) {
        Intent intent = new Intent(FtdiServiceManager.ACTION_VIEW);
        intent.putExtra("message", message.toByteArray());
        getActivity().sendBroadcast(intent);
    }
    private byte fakeStatus = 1;

    private void onClickTestMessageStatus(View v) {
        broadcastMessage(new GjMessageStatusResponse(fakeStatus++));
    }

    private void onClickReportGps(View v) {
//        int id = 5;
//        LatLng latLng = new LatLng(40.7888, -119.20315);
//        send(new GjMessageReportGps(id, latLng));
    }

    private void onClickSendText(View v) {
        String text = messageEditText.getText().toString();
        messageEditText.setText("");
        send(new GjMessageText(text));
    }

    ///////////////////////////////////////////////
    // SEND
    ///////////////////////////////////////////////
    Handler sendHandler = new Handler();

    private void send( final GjMessage message ) {
        // queue requests to avoid ftdi jamming
        sendHandler.post(new Runnable() {
            @Override
            public void run() {
                console(message);
            }
        });
    }

    public void console(GjMessage message) {
        console(message.toString());
        console(message.toHexString());
    }

    public void console(String string) {
        if (messageConsole == null ) {
            return;
        }
        messageConsole.append(string + "\n");
        scrollToEnd(messageConsole);
    }

    private void setStatusUsb(boolean onOff) {
        statusUsb.setBackgroundColor(getColorOnOff(onOff));
    }

    private void setStatusFtdi(boolean onOff) {
        statusFtdi.setBackgroundColor(getColorOnOff(onOff));
    }

    private void setStatusRadio(boolean error) {
        statusRadio.setBackgroundColor(getColorError(error));
    }

    private void setStatusVoltage(boolean error) {
        statusVoltage.setBackgroundColor(getColorError(error));
    }

    private void setStatusTemp(boolean error) {
        statusTemp.setBackgroundColor(getColorError(error));
    }

    private void setStatusCompass(boolean error) {
        statusCompass.setBackgroundColor(getColorError(error));
    }

    private void setStatusGps(boolean error) {
        statusGps.setBackgroundColor(getColorError(error));
    }

    private void setStatusVehicle(byte number) {
        sVehicleNumber = number;
        statusVehicle.setBackgroundColor(getColorError(false));
        statusVehicle.setText("Vehicle:" + number);
    }

    private void setStatusPacketNumber(byte packetNumber) {
        statusPacketNumber.setBackgroundColor(0xFFCCCCCC);
        int number = (int)packetNumber & 0x000000FF;
        statusPacketNumber.setText("Packet:" + number);
    }

    private void setStatusChecksumErrorCounter(int counter) {
        int color = getColorError(counter!=0);
        statusChecksumErrorCounter.setBackgroundColor(color);
        statusChecksumErrorCounter.setText("Checksum:" + counter);
    }

    private void setStatusVersion(String version) {
        statusVersion.setBackgroundColor(0xFFCCCCCC);
        statusVersion.setText("Ver " + version);
    }

    private int getColorOnOff(boolean offOn) {
        return offOn ? 0xFF00FF00 : 0xFFFF0000;
    }

    private int getColorError(boolean error) {
        return error ? 0xFFFF0000 : 0xFF00FF00;
    }

    /*
     * listener
     */

    @Override
    public void incoming(byte[] bytes) {
        // if UI not created yet - queue the messages
        if (messageIncoming == null) {
            return;
        }
        String hex = GjMessage.toHexString(bytes);
        console("<<< Incoming:" + hex);
    }

    private static int lastPacket = -1;

    @Override
    public void onMessage(GjMessage message) {
        // if UI not created yet - queue the messages
        if (messageConsole == null) {
            queue(message);
            return;
        }

        if (message instanceof GjMessageFtdi) {
            boolean status = ((GjMessageFtdi)message).getStatus();
            setStatusFtdi(status);
            console("### " + message.toString());
            return;
        }
        if (message instanceof GjMessageUsb) {
            boolean status = ((GjMessageUsb)message).getStatus();
            setStatusUsb(status);
            console("### " + message.toString());
            return;
        }
        if (message instanceof GjMessageResponse) {
            GjMessageResponse response = (GjMessageResponse)message;
            if (!response.isOK()) {
                sChecksumErrorCounter++;
                setStatusChecksumErrorCounter(sChecksumErrorCounter);
            }
        }
        if (message instanceof GjMessageStatusResponse) {
            GjMessageStatusResponse s = (GjMessageStatusResponse)message;
            setStatusRadio(s.getErrorRadio());
            setStatusVoltage(s.getErrorVoltage());
            setStatusTemp(s.getErrorTemp());
            setStatusCompass(s.getErrorCompass());
            setStatusGps(s.getErrorGps());
            setStatusVehicle(s.getVehicle());
            setStatusPacketNumber(s.getPacketNumber());
            // store my own vehcile ID as reported by the controller
            setStatusVehicle(s.getVehicle());
            // report packet number
            setStatusPacketNumber(s.getPacketNumber());
            console("<<< " + message.toString());
            return;
        }
        if (message instanceof GjMessageGps) {
            // report packet number
            setStatusPacketNumber(message.getPacketNumber());
            console("<<< " + message.toString());
            return;
        }
        if (message instanceof GjMessageText) {
            // report packet number
            setStatusPacketNumber(message.getPacketNumber());
            // DEBUG save last text #
            lastPacket = message.getPacketNumber();
            console("<<< " + message.toString());
            return;
        }
        // otherwise
        console(message.toString());
    }

    private static List<GjMessage> queue = new ArrayList<>();

    private void queue(GjMessage message) {
        queue.add(message);
    }

    // once the UI is up - dispatch the queue
    private void queueDispatch() {
        for ( GjMessage message : queue) {
            onMessage(message);
        }
    }

    // on launch, make sure a usb device is available
    // it will not be available if the unit is off,
    // and will generate a USB intention when attached/detached
    private void checkUsb(FragmentActivity activity) {
        UsbManager manager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        console("USB devices: " + deviceList.keySet().size());
        for (String key : deviceList.keySet()) {
            UsbDevice device = deviceList.get(key);
            console(key + " " + device);
        }
        if (deviceList.keySet().size() > 0) {
            onMessage(new GjMessageUsb(true));
        }
    }

    private void setVersion(FragmentActivity activity) {
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            setStatusVersion(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            setStatusVersion("666");
        }
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

    public void onClickTestSendResponse(View v) {
        if (!MainActivity.ftdiServiceManager.isBound()) {
            return;
        }
        console( "Fake Checksum Error for packet " + lastPacket);
        int written = MainActivity.ftdiServiceManager.send(new GjMessageResponse((byte)lastPacket, (byte)9, new byte[]{0} ));
    }

    public void onClickTestSendText(View v) {
        if (!MainActivity.ftdiServiceManager.isBound()) {
            return;
        }
        int written = MainActivity.ftdiServiceManager.send(new GjMessageText("*** " + FtdiServiceManager.outgoingPacketNumber + " ****************"));
    }

//    public void loopback(byte[] bytes) {
//        Intent intent = new Intent(FtdiServiceManager.ACTION_VIEW);
//        intent.putExtra(FtdiService.FTDI_SERVICE_MESSSAGE, bytes);
//        getActivity().sendBroadcast(intent);
//    }

}