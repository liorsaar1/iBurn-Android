package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.os.Handler;
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
        vehicles.put(""+vehicle, marker);
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
        // notin'
    }
}
