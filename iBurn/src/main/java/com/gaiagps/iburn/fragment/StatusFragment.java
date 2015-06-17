package com.gaiagps.iburn.fragment;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.activity.MainActivity;
import com.gaiagps.iburn.gj.ftdi.FtdiService;
import com.gaiagps.iburn.gj.ftdi.FtdiServiceManager;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageListener;
import com.gaiagps.iburn.gj.message.GjMessageResponse;
import com.gaiagps.iburn.gj.message.GjMessageStatusResponse;
import com.gaiagps.iburn.gj.message.internal.GjMessageFtdi;
import com.gaiagps.iburn.gj.message.internal.GjMessageUsb;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liorsaar on 4/19/15
 */
public class StatusFragment extends Fragment implements GjMessageListener {
    private static final String TAG = "StatusFragment";

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

    @SuppressLint("UseSparseArrays")
    public static HashMap<Integer, String> errorMessagesMap = new HashMap<Integer, String>() {{
        put(R.id.GjErrorRadio, "That's it!  You broke it. We'll never get our deposit now.");
        put(R.id.GjErrorVoltage, "Cart batteries dangerously low, switch into charge mode immediately");
        put(R.id.GjErrorTabletBattery, "Tablet battery not charging properly. Are you watching porn again ?");
        put(R.id.GjErrorTemp, "Wow dude !  Way too hot !! Find some shade and chill out for a few hours");
        put(R.id.GjErrorCompass, "Compass is broken. We are lost. They will never find us.");
        put(R.id.GjErrorGps, "GPS is broken. We are lost. They will never find us.");

        put(R.id.GjErrorUsb, "USB device not found. Check the tablet cable.");
        put(R.id.GjErrorFtdi, "FTDI port unavailable. Check the tablet cable.");
    }};

    private static View sView;
    public static byte sVehicleNumber =0;
    private static int sChecksumErrorCounter =0;

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
        if (sView != null)
            return sView;

        sView = inflater.inflate(R.layout.fragment_status, container, false);
        sView.findViewById(R.id.GjErrorVersion).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTestSendStatus(v);
            }
        });

        setVersion(getActivity());
        checkUsb(getActivity());
        setStatusChecksumErrorCounter(sChecksumErrorCounter);

        return sView;
    }

    byte fakeStatus = 0;
    private void onClickTestSendStatus(View v) {
        if (fakeStatus ==0) {
            fakeStatus = 1;
        } else {
            fakeStatus <<= 1;
        }
        GjMessageStatusResponse status = new GjMessageStatusResponse(fakeStatus);
        MainActivity.ftdiServiceManager.send(status);
        loopback(status.toByteArray());
    }


    private void setVersion(Activity activity) {
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            setColorGrey(R.id.GjErrorVersion);
            setText(R.id.GjErrorVersion, pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    // on launch, make sure a usb device is available
    // it will not be available if the unit is off,
    // and will generate a USB intention when attached/detached
    private void checkUsb(FragmentActivity activity) {
        UsbManager manager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        //console("USB devices: " + deviceList.keySet().size());
        for (String key : deviceList.keySet()) {
            UsbDevice device = deviceList.get(key);
            //console(key + " " + device);
        }
        if (deviceList.keySet().size() > 0) {
            onMessage(new GjMessageUsb(true));
        }
    }

    private void setStatusChecksumErrorCounter(int counter) {
        boolean error = counter != 0;
        setColor(R.id.GjErrorChecksum, error);
        setText(R.id.GjErrorChecksum, ""+counter);
    }

    private void setColorGrey(int resId) {
        TableRow row = (TableRow) sView.findViewById(resId);
        View button = row.getChildAt(0);
        button.setBackgroundColor(0xFFCCCCCC);
        TextView textView = (TextView) row.getChildAt(1);
        int backResId = R.drawable.status_background_text;
        textView.setBackground(getActivity().getResources().getDrawable(backResId));
    }

    private void setText(int resId, String text) {
        TableRow row = (TableRow) sView.findViewById(resId);
        TextView textView = (TextView) row.getChildAt(1);
        textView.setText(text);
    }

    private void setColor(int resId, boolean error) {
        TableRow row = (TableRow) sView.findViewById(resId);
        View button = row.getChildAt(0);
        button.setBackgroundColor(getColorError(error));
        TextView textView = (TextView) row.getChildAt(1);
        int backResId = error ? R.drawable.status_background_error : R.drawable.status_background_ok;
        textView.setBackground(getActivity().getResources().getDrawable(backResId));
    }

    private void setStatus(int resId, boolean error) {
        setColor(resId, error);
        String text = error ? errorMessagesMap.get(resId) : "Check";
        setText(resId, text);
    }

    private int getColorOnOff(boolean offOn) {
        return offOn ? 0xFF00FF00 : 0xFFFF0000;
    }

    private int getColorError(boolean error) {
        return error ? 0xFFFF0000 : 0xFF00FF00;
    }

    @Override
    public void onMessage(GjMessage message) {
        // USB
        if (message instanceof GjMessageUsb) {
            boolean status = ((GjMessageUsb)message).getStatus();
            boolean error = status ? false : true;
            setStatus(R.id.GjErrorUsb, error);
            return;
        }
        // FTDI
        if (message instanceof GjMessageFtdi) {
            boolean status = ((GjMessageFtdi)message).getStatus();
            boolean error = status ? false : true;
            setStatus(R.id.GjErrorFtdi, error);
            return;
        }

        if (message instanceof GjMessageStatusResponse) {
            onMessageStatus((GjMessageStatusResponse) message);
            return;
        }
        if (message instanceof GjMessageResponse) {
            GjMessageResponse response = (GjMessageResponse)message;
            if (!response.isOK()) {
                setStatusChecksumErrorCounter(++sChecksumErrorCounter);
            }
            return;
        }
    }

    private void onMessageStatus(GjMessageStatusResponse s) {
        setStatus(R.id.GjErrorRadio, s.getErrorRadio());
        setStatus(R.id.GjErrorVoltage, s.getErrorVoltage());
        setStatus(R.id.GjErrorTemp, s.getErrorTemp());
        setStatus(R.id.GjErrorCompass, s.getErrorCompass());
        setStatus(R.id.GjErrorGps, s.getErrorGps());
        // store my own vehcile ID as reported by the controller
        sVehicleNumber = s.getVehicle();
        // vehicle
        setStatus(R.id.GjErrorVehicle, false);
        setText(R.id.GjErrorVehicle, "" + sVehicleNumber);
        // packet
        setStatus(R.id.GjErrorPacket, false);
        setText(R.id.GjErrorPacket, "" + s.getPacketNumber());
        // check charging status
        boolean isBatteryError = statusBatteryError(getActivity());
        setStatus(R.id.GjErrorTabletBattery, isBatteryError);
//        // display critical error dialog if needed
//        statusShowErrorDialog(getActivity(), s, isBatteryError);
        return;
    }

    @Override
    public void incoming(byte[] bytes) {
        // notin'
    }

    private boolean statusBatteryError(Activity activity) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = activity.registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
        int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        if (isCharging)
            return false;
        return true;
    }

    public void loopback(byte[] bytes) {
        FtdiServiceManager.outgoingPacketNumber++;

        Intent intent = new Intent(FtdiServiceManager.ACTION_VIEW);
        intent.putExtra(FtdiService.FTDI_SERVICE_MESSSAGE, bytes);
        getActivity().sendBroadcast(intent);
    }

}