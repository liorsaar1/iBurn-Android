package com.gaiagps.iburn.fragment;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.activity.MainActivity;
import com.gaiagps.iburn.gj.ftdi.FtdiService;
import com.gaiagps.iburn.gj.ftdi.FtdiServiceManager;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageFactory;
import com.gaiagps.iburn.gj.message.GjMessageGps;
import com.gaiagps.iburn.gj.message.GjMessageLighting;
import com.gaiagps.iburn.gj.message.GjMessageListener;
import com.gaiagps.iburn.gj.message.GjMessageResponse;
import com.gaiagps.iburn.gj.message.GjMessageStatusResponse;
import com.gaiagps.iburn.gj.message.GjMessageText;
import com.gaiagps.iburn.gj.message.internal.GjMessageFtdi;
import com.gaiagps.iburn.gj.message.internal.GjMessageUsb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by liorsaar on 4/19/15
 */
public class SettingsFragment extends Fragment implements GjMessageListener {
    private static final String TAG = "SettingsFragment";

    private static EditText sendTextEditText;
    private static Button sendTextButton;
    private static TextView messageConsole;
    private static TextView messageIncoming;
    private static TextView statusUsb, statusFtdi;
    private static TextView statusRadio, statusVoltage, statusTemp, statusCompass, statusGps;
    private static TextView statusPacketNumber, statusVehicle, statusVersion, statusChecksumErrorCounter;
    private static TextView statusTabletBattery;
    private static Button testSendResponse, testSendStatus, testSendGps;
    public static byte sVehicleNumber =0;
    public static int sChecksumErrorCounter =0;
    private static int statusPacketClickCounter = 0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTheme(R.style.Theme_GJ);
    }

    private static View sView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (sView != null)
            return sView;

        final View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sendTextEditText = (EditText) view.findViewById(R.id.GjMessageEditText);
        sendTextButton = (Button)view.findViewById(R.id.GjMessageSendTextButton);
        sendTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSendText(v);
            }
        });
        messageConsole = (TextView) view.findViewById(R.id.GjMessageConsole);
        messageConsole.setMovementMethod(new ScrollingMovementMethod());
        messageIncoming = (TextView) view.findViewById(R.id.GjIncoming);
        messageIncoming.setMovementMethod(new ScrollingMovementMethod());

        view.findViewById(R.id.GjTestContainer).setVisibility(View.GONE);
        testSendResponse = (Button)view.findViewById(R.id.GjTestSendResponse);
        testSendResponse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTestSendResponse(v);
            }
        });
        testSendStatus = (Button)view.findViewById(R.id.GjTestSendStatus);
        testSendStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTestSendStatus(v);
            }
        });
        testSendGps = (Button)view.findViewById(R.id.GjTestSendGps);
        testSendGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTestSendGps(v);
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
        statusTabletBattery = (TextView)view.findViewById(R.id.GjStatusTabletBattery);

        statusPacketNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (++statusPacketClickCounter >= 7) {
                    view.findViewById(R.id.GjTestContainer).setVisibility(View.VISIBLE);
                }
            }
        });

        setVersion(getActivity());
        checkUsb(getActivity());
        queueDispatch();
        setStatusChecksumErrorCounter(sChecksumErrorCounter);
        setOutgoingText();

        sView = view;
        return view;
    }

    private byte fakeStatus = 1;

    private void onClickTestSendStatus(View v) {
        GjMessageStatusResponse status = new GjMessageStatusResponse(fakeStatus++);
        MainActivity.ftdiServiceManager.send(status);
        loopback(status.toByteArray());
    }

    private void onClickTestSendGps(View v) {
//        MainActivity.ftdiServiceManager.send(GjMessageFactory.createGps());
        loopback(GjMessageFactory.createGps().array());

    }

    private void onClickSendText(View v) {
        int vehicle = (int)(5*Math.random());
        String text = sendTextEditText.getText().toString();
        GjMessageText message = new GjMessageText(text, (byte)vehicle);
        MainActivity.ftdiServiceManager.send(message);
        console(">>> " + message);
        loopback(message.toByteArray());
        setOutgoingText();
    }

    private void setOutgoingText() {
        String defaultText = "Text Message # " + FtdiServiceManager.outgoingPacketNumber + " ";
        sendTextEditText.setText(defaultText);
    }

    public void console(String string) {
        if (messageConsole == null ) {
            return;
        }
        messageConsole.append(string + "\n\n");
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

    private void setStatusTabletBattery(boolean error) {
        statusTabletBattery.setBackgroundColor(getColorError(error));
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
        console("<<< Incoming:" + hex + "\n");
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
            return;
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
            // check charging status
            boolean isBatteryError = statusBatteryError(getActivity());
            setStatusTabletBattery(isBatteryError);
            // display critical error dialog if needed
//            statusShowErrorDialog(getActivity(), s, isBatteryError);
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
        if (message instanceof GjMessageLighting) {
            console("<<< " + message.getTime() + ":" + message.toString());
            return;
        }
        // otherwise
        console(message.toString());
    }

    @SuppressLint("UseSparseArrays")
    private static final Map<Integer, String> batteryStatusStrings = new HashMap<Integer, String>() {{
        put(BatteryManager.BATTERY_STATUS_UNKNOWN, "UNKNOWN");
        put(BatteryManager.BATTERY_STATUS_CHARGING, "CHARGING");
        put(BatteryManager.BATTERY_STATUS_DISCHARGING, "DISCHARGING");
        put(BatteryManager.BATTERY_STATUS_NOT_CHARGING, "NOT_CHARGING");
        put(BatteryManager.BATTERY_STATUS_FULL, "FULL");
    }};

    @SuppressLint("UseSparseArrays")
    private static final Map<Integer, String> batteryHealthStrings = new HashMap<Integer, String>() {{
        put(BatteryManager.BATTERY_HEALTH_UNKNOWN, "UNKNOWN");
        put(BatteryManager.BATTERY_HEALTH_GOOD, "GOOD");
        put(BatteryManager.BATTERY_HEALTH_OVERHEAT, "OVERHEAT");
        put(BatteryManager.BATTERY_HEALTH_DEAD, "DEAD");
        put(BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE, "OVER_VOLTAGE");
        put(BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE, "UNSPECIFIED_FAILURE");
        put(BatteryManager.BATTERY_HEALTH_COLD, "COLD");
    }};

    private boolean statusBatteryError(Activity activity) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = activity.registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        console("Tablet Battery: status:" + batteryStatusStrings.get(status) + " health:" + batteryHealthStrings.get(health));
        if (isCharging)
            return false;
        return false;
//        return true;
    }

    private void statusShowErrorDialog(Activity activity, GjMessageStatusResponse status, boolean isBatteryError) {
        View view = activity.findViewById(R.id.GjErrorContainer);
        boolean isCriticalError = status.isCriticalError() || isBatteryError;
        if (!isCriticalError) {
            view.setVisibility(View.GONE);
            stopStatusErrorAnimation();
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.findViewById(R.id.GjErrorGps).setVisibility(status.getErrorGps() | status.getErrorCompass() ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.GjErrorRadio).setVisibility(status.getErrorRadio() ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.GjErrorTemp).setVisibility(status.getErrorTemp() ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.GjErrorVoltage).setVisibility(status.getErrorVoltage() ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.GjErrorTabletBattery).setVisibility(isBatteryError ? View.VISIBLE : View.GONE);
        startStatusErrorAnimation(view);
    }

    ValueAnimator colorAnimation;
    private void startStatusErrorAnimation(View view) {
        if (colorAnimation == null) {
            final ImageButton statusView = (ImageButton) getTabChildView(2);
            Integer colorFrom = 0x00000000; // transparent
            Integer colorTo = 0xFFFF0000; // red
            colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            colorAnimation.setDuration(500);
            colorAnimation.setRepeatCount(ValueAnimator.INFINITE);
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    statusView.setBackgroundColor((Integer) animator.getAnimatedValue());
                }

            });
        }
        if (!colorAnimation.isRunning())
            colorAnimation.start();
    }

    private void stopStatusErrorAnimation() {
        if (colorAnimation != null)
            colorAnimation.cancel();
        final ImageButton statusView = (ImageButton)getTabChildView(2);
        statusView.setBackgroundColor(0x00000000);
    }

    public View getTabChildView(int index) {
        PagerSlidingTabStrip mTabs = ((MainActivity)getActivity()).getTabs();
        LinearLayout tabView = (LinearLayout) mTabs.getChildAt(0);
        return tabView.getChildAt(index);
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

    public void loopback(byte[] bytes) {
        FtdiServiceManager.outgoingPacketNumber++;

        Intent intent = new Intent(FtdiServiceManager.ACTION_VIEW);
        intent.putExtra(FtdiService.FTDI_SERVICE_MESSSAGE, bytes);
        getActivity().sendBroadcast(intent);
    }

}