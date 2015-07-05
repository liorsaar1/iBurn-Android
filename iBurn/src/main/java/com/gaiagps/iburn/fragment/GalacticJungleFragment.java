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

/**
 * Created by liorsaar on 4/18/15.
 */
public class GalacticJungleFragment extends GoogleMapFragment implements GjMessageListener {
    private static final String TAG = "GalacticJungleFragment";

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
    }

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
            final Marker marker = getMarker(message.getVehicle());
            marker.setRotation((float) gps.getHead());
            final LatLng latLng = new LatLng(gps.getLat(), gps.getLong());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    marker.setPosition(latLng);
                }
            }, 500);
        }
        if (message instanceof GjMessageStatusResponse) {
            GjMessageStatusResponse status = (GjMessageStatusResponse) message;
            sGjVehicleId = status.getVehicle();
        }
    }

    private static int sGjVehicleId = 0;

    private Map<String, Marker> vehicles = new HashMap<String, Marker>();
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
                .icon(BitmapDescriptorFactory.fromResource(vehicleResId[vehicle]))
                .anchor(0.5f, 0.5f)
                .title("Vehicle " + vehicle);
        Marker marker = gjMap.addMarker(mops);
        vehicles.put("" + vehicle, marker);
        return marker;
    }

    private int vehicleResId[] = new int[] {
            R.drawable.vehicle_0,
            R.drawable.vehicle_1,
            R.drawable.vehicle_2,
            R.drawable.vehicle_3,
            R.drawable.vehicle_4,
            R.drawable.vehicle_5,
            R.drawable.vehicle_0,
    };

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
