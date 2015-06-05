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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private List<Marker> markers;

    static GoogleMap gjMap ;

    private void initGJ() {
        gjMap = getMap();
//        40.7888
//        -119.20315
//        lat=40.7843037788468
//        lon=-119.19632155448197
        final LatLng test1 = new LatLng(40.7888, -119.20315);
        final LatLng test2 = new LatLng(40.7843, 119.1963);

        getMap().addMarker(new MarkerOptions()
                .position(test1)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .title("Current Location"));

        markers = new ArrayList<Marker>();

        for (int i = 1; i < 5 ; i++) {
            LatLng ll = new LatLng(40.7888+i*0.003, -119.20315+i*0.003);

            MarkerOptions mops = new MarkerOptions()
                    .position(ll)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    .title("** " + i);
            markers.add( getMap().addMarker(mops) );
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                for (Marker marker : markers) {
                    marker.setPosition(test1);
                }
            }
        }, 5000);
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
            final LatLng latLng = new LatLng(gps.getLat(), gps.getLong());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    marker.setPosition(latLng);
                }
            }, 500);
        }
    }

    private Map<String, Marker> vehicles = new HashMap<String, Marker>();
    private LatLng bmLatLong = new LatLng(40.7888, -119.20315);

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
                .title("Vehicle " + vehicle );
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
