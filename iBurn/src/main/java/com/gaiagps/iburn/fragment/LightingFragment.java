package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;

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
    private static Button[] effectButton = new Button[8];
    private static SeekBar[] seekBar = new SeekBar[8];

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
        effectButton[0] = (Button) view.findViewById(R.id.lightEffect_1_1);
        effectButton[1] = (Button) view.findViewById(R.id.lightEffect_1_2);
        effectButton[2] = (Button) view.findViewById(R.id.lightEffect_1_3);
        effectButton[3] = (Button) view.findViewById(R.id.lightEffect_1_4);
        effectButton[4] = (Button) view.findViewById(R.id.lightEffect_2_1);
        effectButton[5] = (Button) view.findViewById(R.id.lightEffect_2_2);
        effectButton[6] = (Button) view.findViewById(R.id.lightEffect_2_3);
        effectButton[7] = (Button) view.findViewById(R.id.lightEffect_2_4);
        seekBar[0] = (SeekBar)view.findViewById(R.id.lightHue);
        seekBar[1] = (SeekBar)view.findViewById(R.id.lightBrightness);
        seekBar[2] = (SeekBar)view.findViewById(R.id.lightSaturation);
        seekBar[3] = (SeekBar)view.findViewById(R.id.lightSpeed);
        seekBar[4] = (SeekBar)view.findViewById(R.id.lightParameter1);
        seekBar[5] = (SeekBar)view.findViewById(R.id.lightParameter2);

        for (int i=0 ; i<8 ; i++) {
            effectButton[i].setOnClickListener(effectOnClick);
        }

        for (int i = 0; i < 6; i++) {
            seekBar[i].setOnSeekBarChangeListener(seekBarChangeListener);
        }

        return view;
    }

    private int effectSelected;
    private View.OnClickListener effectOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = Integer.parseInt((String) v.getTag());
            effectButton[effectSelected].setSelected(false);
            effectSelected = index;
            v.setSelected(true);
            onChange();
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            onChange();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void onChange() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int value = seekBar[i].getProgress();
            sb.append(value).append(",");
        }
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