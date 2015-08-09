package com.gaiagps.iburn.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageGps;
import com.gaiagps.iburn.gj.message.GjMessageListener;
import com.gaiagps.iburn.gj.message.GjMessageStatusResponse;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by liorsaar on 4/18/15.
 */
public class GalacticJungleFragment extends GoogleMapFragment implements GjMessageListener {
    private static final String TAG = "GalacticJungleFragment";
    private Timer gpsTimer;

    public static GalacticJungleFragment newInstance() {
        return new GalacticJungleFragment();
    }

    public GalacticJungleFragment() {
        super();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parent = super.onCreateView(inflater, container, savedInstanceState);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initGJ();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (gpsTimer == null) {
            gpsTimer = new Timer();
            gpsTimer.scheduleAtFixedRate(gpsTimerTask, 5000, 5000 );
        }
//        Long timeMillis = System.currentTimeMillis();
//        gpsLastUpdate.put("0", timeMillis);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    TimerTask gpsTimerTask = new TimerTask() {
        @Override
        public void run() {
            for (int i = 0; i < 5 ; i++) {
                // if not an active marker - bail
                if (!vehicles.containsKey("" + i)) {
                    continue;
                }
                Log.e(TAG, "gps timer found marker " + i);

                // check timeout
                if (!gpsLastUpdate.containsKey(""+i)) {
                    continue;
                }
                long now = System.currentTimeMillis();
                long lastUpdate = gpsLastUpdate.get(""+i);

                long delta = now - lastUpdate;
                Log.e(TAG, "gps timer delta " + delta);
                if ( delta < 15000) {
                    continue;
                }
                gpsLastUpdate.remove(""+i);

                // timeout - change icon
                final Marker marker = vehicles.get("" + i);
                if (marker == null) {
                    continue;
                }
                final int resId = getVehicleGreyResId(i);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        marker.setIcon(BitmapDescriptorFactory.fromResource(resId));
                    }
                });
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void initGJ() {
        // allow status indicator to operate before the status fragment is attached
        StatusFragment.sActivity = getActivity();

        gjMap = getMap();
        gjMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                Marker marker = getMarker(sGjVehicleId);
                LatLng latLng = marker.getPosition();
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
                gjMap.animateCamera(cameraUpdate);
                return true;
            }
        });
    }

    @Override
    public void onMessage(GjMessage message) {
        if (message instanceof GjMessageGps) {
            GjMessageGps gps = (GjMessageGps)message;
            int vehicle = message.getVehicle();
            if (vehicle > 5) {
                return;
            }
            final Marker marker = getMarker(vehicle);
            marker.setRotation((float) gps.getHead());
            final LatLng latLng = new LatLng(gps.getLat(), gps.getLong());
            final int resId = getVehicleResId(vehicle);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    marker.setPosition(latLng);
                    marker.setIcon(BitmapDescriptorFactory.fromResource(resId));
                }
            }, 500);
            // set update time
            Long timeMillis = System.currentTimeMillis();
            gpsLastUpdate.put("" + message.getVehicle(), timeMillis);
        }
        if (message instanceof GjMessageStatusResponse) {
            GjMessageStatusResponse status = (GjMessageStatusResponse) message;
            sGjVehicleId = status.getVehicle();
        }
    }

    private static int sGjVehicleId = 0;

    private Map<String, Marker> vehicles = new HashMap<String, Marker>();
    private Map<String, Long> gpsLastUpdate = new HashMap<String, Long>();
    private LatLng bmLatLong = new LatLng(40.7888, -119.20315);
    private static GoogleMap gjMap ;

    private Marker getMarker(int vehicle) {
        // look for existing
        if (vehicles.containsKey(""+vehicle)) {
            return vehicles.get(""+vehicle);
        }
        // if doesnt exist - create a new one
        MarkerOptions mops = new MarkerOptions()
                .position(bmLatLong)
                .icon(BitmapDescriptorFactory.fromResource(getVehicleResId(vehicle)))
                .anchor(0.5f, 0.5f)
                .title("Vehicle " + vehicle);
        Marker marker = gjMap.addMarker(mops);
        vehicles.put("" + vehicle, marker);
        return marker;
    }
    /*
    Lion	3143757	1
    Elephant	3143756	2
    Tiger	3143758	3
    Zebra	3143754	4
    Rhino	3143755	5
    Spare	3143753	n/a
     */

    private static int vehicleResId[] = new int[] {
            R.drawable.map_icon_elephant, // default
            R.drawable.map_icon_lion,
            R.drawable.map_icon_elephant,
            R.drawable.map_icon_tiger,
            R.drawable.map_icon_zebra,
            R.drawable.map_icon_rhino,
    };

    private static int vehicleGreyResId[] = new int[] {
            R.drawable.map_icon_elephant_grey, // default
            R.drawable.map_icon_lion_grey,
            R.drawable.map_icon_elephant_grey,
            R.drawable.map_icon_tiger_grey,
            R.drawable.map_icon_zebra_grey,
            R.drawable.map_icon_rhino_grey,
    };

    public static int getVehicleResId(int vehicle) {
        if (vehicle < 0) vehicle = 0;
        if (vehicle >= vehicleResId.length) vehicle = vehicleResId.length-1;
        return vehicleResId[vehicle];
    }

    public static int getVehicleGreyResId(int vehicle) {
        if (vehicle < 0) vehicle = 0;
        if (vehicle >= vehicleGreyResId.length) vehicle = vehicleGreyResId.length-1;
        return vehicleGreyResId[vehicle];
    }

    @Override
    public void incoming(byte[] bytes) {
        try {
            logWrite(bytes);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private static FileOutputStream logFOS;
    private static int logCounter = 0;
    private static String logName = new SimpleDateFormat("yyyy-MM-dd-hh-MM").format(new Date());

    private void logWrite(byte[] bytes) throws IOException {
        if (logFOS == null) {
            logFOS = new FileOutputStream("sdcard/Download/GalacticJungle_" + logName +".txt");
        }
        OutputStreamWriter osw = new OutputStreamWriter(logFOS,"UTF-8");
        BufferedWriter out = new BufferedWriter(osw);
        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            String s = String.format("%02X ", b);
            ++logCounter;
            if (logCounter%20 == 0) {
                sb.append("\n");
            }
            sb.append(s);
        }
        out.write(sb.toString());
        out.flush();
        logFOS.flush();
    }

    public static StringBuffer logRead(String filename) throws IOException {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        StringBuffer sb = new StringBuffer();
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb;
    }

    public static StringBuffer logRead(Context context, Uri uri) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(context.getContentResolver().openInputStream(uri)));
        String line;
        StringBuffer sb = new StringBuffer();
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb;
    }
}
