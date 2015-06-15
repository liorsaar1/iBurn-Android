package com.gaiagps.iburn.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageListener;
import com.gaiagps.iburn.gj.message.GjMessageStatusResponse;

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

        final View view = inflater.inflate(R.layout.fragment_status, container, false);

        sView = view;
        return view;
    }

    private void setStatus(int resId, boolean error) {
        TableRow row = (TableRow) sView.findViewById(resId);
        View button = row.getChildAt(0);
        button.setBackgroundColor(getColorError(error));
        TextView textView = (TextView) row.getChildAt(1);
        String text = error ? errorMessagesMap.get(resId) : "Check";
        textView.setText(text);
        int backResId = error ? R.drawable.status_background_error : R.drawable.status_background_ok;
        textView.setBackground(getActivity().getResources().getDrawable(backResId));
    }

    private int getColorOnOff(boolean offOn) {
        return offOn ? 0xFF00FF00 : 0xFFFF0000;
    }

    private int getColorError(boolean error) {
        return error ? 0xFFFF0000 : 0xFF00FF00;
    }

    public static HashMap<Integer, String> errorMessagesMap = new HashMap<Integer, String>() {{
        put(R.id.GjErrorRadio, "That's it!  You broke it.  \nWe'll never get our deposit now.");
        put(R.id.GjErrorVoltage, "Cart batteries dangerously low, switch into charge \nmode immediately");
        put(R.id.GjErrorTabletBattery, "Tablet battery not charging properly. \nAre you watching porn again ?");
        put(R.id.GjErrorTemp, "Wow dude !  Way too hot !! \nFind some shade and chill out for a few hours");
        put(R.id.GjErrorCompass, "Compass is broken. We are lost. They will never find us.");
        put(R.id.GjErrorGps, "GPS is broken. We are lost. They will never find us.");
    }};

    @Override
    public void onMessage(GjMessage message) {
        if (message instanceof GjMessageStatusResponse) {
            onMessageStatus((GjMessageStatusResponse) message);
            return;
        }
    }

    private void onMessageStatus(GjMessageStatusResponse s) {
        setStatus(R.id.GjErrorRadio, s.getErrorRadio());
        setStatus(R.id.GjErrorVoltage, s.getErrorVoltage());
        setStatus(R.id.GjErrorTemp, s.getErrorTemp());
        setStatus(R.id.GjErrorCompass, s.getErrorCompass());
        setStatus(R.id.GjErrorGps, s.getErrorGps());
//        setStatusVehicle(s.getVehicle());
//        setStatusPacketNumber(s.getPacketNumber());
//        // store my own vehcile ID as reported by the controller
//        setStatusVehicle(s.getVehicle());
//        // report packet number
//        setStatusPacketNumber(s.getPacketNumber());
        // check charging status
//        boolean isBatteryError = statusBatteryError(getActivity());
//        setStatusTabletBattery(isBatteryError);
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
        return false;
//        return true;
    }

}