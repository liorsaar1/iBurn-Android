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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.gaiagps.iburn.gj.message.internal.GjMessageConsole;
import com.gaiagps.iburn.gj.message.internal.GjMessageError;
import com.gaiagps.iburn.gj.message.internal.GjMessageFtdi;
import com.gaiagps.iburn.gj.message.internal.GjMessageUsb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by liorsaar on 4/19/15
 */
public class StatusFragment extends Fragment implements GjMessageListener {
    private static final String TAG = "StatusFragment";
    public static Activity sActivity; // horrible hack sorry

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
        put(R.id.GjErrorTemp, "Wow dude !  Way too hot !! Find some shade and chill out for a few hours");
        put(R.id.GjErrorCompass, "Compass is broken. We are lost. They will never find us.");
        put(R.id.GjErrorGps, "GPS is broken. We are lost. They will never find us.");

        put(R.id.GjErrorUsb, "USB device not found. Check the tablet cable.");
        put(R.id.GjErrorFtdi, "FTDI port unavailable. Check the tablet cable.");
    }};
    public static byte sVehicleNumber = 0;
    private static View sView;
    private static int sChecksumErrorCounterTotal = 0, sChecksumErrorCounterCurrent = 0;
    private static List<GjMessage> queue = new ArrayList<>();
    private byte fakeStatus = 0;
    private ValueAnimator warningAnimation;
    private AlphaAnimation criticalAnimation;
    private static boolean errorUsb=true, errorFtdi=true, errorBattery=true;
    private static String errorBatteryText = "ERROR";
    private static GjMessageStatusResponse lastStatusResponse;

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
        // reset the hack - use the fragments activity from now on
        sActivity = null;

        sView = inflater.inflate(R.layout.fragment_status, container, false);
        sView.findViewById(R.id.GjErrorVersion).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTestSendStatus();
            }
        });
        sView.findViewById(R.id.GjErrorChecksum).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickChecksum();
            }
        });

        setVersion(getActivity());
        checkUsb(getActivity());
        updateView();
        queueDispatch();

        return sView;
    }

    private void updateView() {
        if (sView == null)
            return;

        setStatusResponse((GjMessageStatusResponse) lastStatusResponse);
        setStatus(R.id.GjErrorUsb, errorUsb);
        setStatus(R.id.GjErrorFtdi, errorFtdi);
        setStatusChecksumErrorCounter();
        setText(R.id.GjErrorTabletBattery, errorBatteryText);
        setColor(R.id.GjErrorTabletBattery, errorBattery);
    }

    private void onClickChecksum() {
        sChecksumErrorCounterCurrent = 0;
        updateView();
    }

    private void onClickTestSendStatus() {
//        if (true) return;
        if (fakeStatus == 0) {
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
    private void checkUsb(Activity activity) {
        UsbManager manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
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

    private void setStatusChecksumErrorCounter() {
        boolean error = sChecksumErrorCounterCurrent != 0;
        setColor(R.id.GjErrorChecksum, error);
        setText(R.id.GjErrorChecksum, "Total: " + sChecksumErrorCounterTotal + "  Current: " + sChecksumErrorCounterCurrent);
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
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) textView.getLayoutParams();
        params.gravity = Gravity.CENTER_VERTICAL;
        textView.setLayoutParams(params);
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
        if (message instanceof GjMessageConsole) return;
        if (message instanceof GjMessageError) return;

        Activity activity = getActivity();
        if (activity == null) {
            activity = sActivity;
        }

        // USB
        if (message instanceof GjMessageUsb) {
            boolean status = ((GjMessageUsb) message).getStatus();
            errorUsb = status ? false : true;
        }
        // FTDI
        if (message instanceof GjMessageFtdi) {
            boolean status = ((GjMessageFtdi) message).getStatus();
            errorFtdi = status ? false : true;
        }
        // Status
        if (message instanceof GjMessageStatusResponse) {
            lastStatusResponse = (GjMessageStatusResponse) message;
            // check charging status - here as good place as any
            errorBattery = statusBatteryError(activity);
        }
        // checksum
        if (message instanceof GjMessageResponse) {
            GjMessageResponse response = (GjMessageResponse) message;
            if (!response.isOK()) {
                sChecksumErrorCounterTotal++;
                sChecksumErrorCounterCurrent++;
            }
        }
        // update indicators
        updateStatusIndicators(activity);
        updateView();
    }

    private void updateStatusIndicators(Activity activity) {
        boolean isCritical = (lastStatusResponse != null) ? lastStatusResponse.isCriticalError() : false;
        boolean isWarning = errorBattery || errorFtdi || errorUsb || (sChecksumErrorCounterCurrent != 0);

        // blink the tab on any sign of error
        if (isWarning || isCritical) {
            startStatusWarningAnimation();
        } else {
            stopStatusWarningAnimation();
        }
        // display critical
        View view = activity.findViewById(R.id.GjCriticalError);
        if (isCritical) {
            startStatusCriticalAnimation(view);
        } else {
            stopStatusCriticalAnimation(view);
        }
    }

    private void startStatusWarningAnimation() {
        if (warningAnimation == null) {
            final ImageButton statusView = getTab();
            Integer colorFrom = 0x00000000; // transparent
            Integer colorTo = 0xFFFF0000; // red
            warningAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            warningAnimation.setDuration(500);
            warningAnimation.setRepeatCount(ValueAnimator.INFINITE);
            warningAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    statusView.setBackgroundColor((Integer) animator.getAnimatedValue());
                }

            });
        }
        if (!warningAnimation.isRunning())
            warningAnimation.start();
    }

    private void stopStatusWarningAnimation() {
        if (warningAnimation != null)
            warningAnimation.cancel();
        getTab().setBackgroundColor(0x00000000);
    }

    private void startStatusCriticalAnimation(View view) {
        if (criticalAnimation == null) {
            criticalAnimation = new AlphaAnimation(1, 0);
            criticalAnimation.setDuration(1000); // duration - half a second
            criticalAnimation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
            criticalAnimation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
            view.startAnimation(criticalAnimation);
            view.setVisibility(View.VISIBLE);
        }
    }

    private void stopStatusCriticalAnimation(final View view) {
        if (criticalAnimation != null) {
            view.clearAnimation();
            view.setVisibility(View.GONE);
            criticalAnimation.cancel();
            criticalAnimation = null;
        }
    }

    private ImageButton getTab() {
        if (getActivity() != null)
            return ((MainActivity) getActivity()).getTabChildView(3);
        else
            return ((MainActivity) sActivity).getTabChildView(3);
    }

    private void setStatusResponse(GjMessageStatusResponse s) {
        if (s==null) return;
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
    }

    @Override
    public void incoming(byte[] bytes) {
        // notin'
    }

    private boolean statusBatteryError(Activity activity) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = activity.registerReceiver(null, ifilter);
        // charging status
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
        // health
        int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        boolean isHealthy = health == BatteryManager.BATTERY_HEALTH_GOOD ;
        // battery overall
        boolean isBatteryError = (isCharging && isHealthy) ? false : true;
        // status text
        errorBatteryText = "Status: " + batteryStatusStrings.get(status);
        if (!isHealthy) {
            errorBatteryText += "  Health: " + batteryHealthStrings.get(health);
        }
        return isBatteryError;
    }

    public void loopback(byte[] bytes) {
        FtdiServiceManager.outgoingPacketNumber++;

        Intent intent = new Intent(FtdiServiceManager.ACTION_VIEW);
        intent.putExtra(FtdiService.FTDI_SERVICE_MESSSAGE, bytes);
        getActivity().sendBroadcast(intent);
    }

    private void queue(GjMessage message) {
        queue.add(message);
    }

    // once the UI is up - dispatch the queue
    private void queueDispatch() {
        for (GjMessage message : queue) {
            onMessage(message);
        }
    }

}