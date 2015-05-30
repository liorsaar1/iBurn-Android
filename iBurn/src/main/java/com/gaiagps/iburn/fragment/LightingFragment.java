package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageListener;
import com.gaiagps.iburn.gj.message.GjMessageStatusResponse;
import com.gaiagps.iburn.gj.message.internal.GjMessageFtdi;

/**
 * Created by liorsaar on 4/19/15
 */

/*
a. Effect Mode - grid of buttons (2x4 for now - can be changed later)
Brightness - slider
Saturation - slider
Hue shift(color) - slider with color button
Effect speed - slider
Color Palette - drop down menu with bitmap for every palette
Effect parameter 1 - slider
Effect parameter 2 - slider
 */
public class LightingFragment extends Fragment implements GjMessageListener {
    private static final String TAG = "LightingFragment";

    public static LightingFragment newInstance() {
        return new LightingFragment();
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
        View view = inflater.inflate(R.layout.fragment_lighting, container, false);
        return view;
    }

    @Override
    public void onMessage(GjMessage message) {
        if (message instanceof GjMessageStatusResponse) {
            GjMessageStatusResponse s = (GjMessageStatusResponse)message;
            return;
        }
        if (message instanceof GjMessageFtdi) {
        }
    }

    @Override
    public void incoming(byte[] bytes) {
        // notin'
    }

}