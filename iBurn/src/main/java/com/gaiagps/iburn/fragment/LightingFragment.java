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
    private static View[] paletteRow = new View[4];
    private static int effectSelected;
    private static int paletteSelected;

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

    private View.OnClickListener paletteOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = Integer.parseInt((String) v.getTag());
            paletteSelect(index);
            onChange();
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            onChange();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    };

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
        for (int i = 0; i < 8; i++) {
            effectButton[i].setOnClickListener(effectOnClick);
        }

        seekBar[0] = (SeekBar) view.findViewById(R.id.lightHue);
        seekBar[1] = (SeekBar) view.findViewById(R.id.lightBrightness);
        seekBar[2] = (SeekBar) view.findViewById(R.id.lightSaturation);
        seekBar[3] = (SeekBar) view.findViewById(R.id.lightSpeed);
        seekBar[4] = (SeekBar) view.findViewById(R.id.lightParameter1);
        seekBar[5] = (SeekBar) view.findViewById(R.id.lightParameter2);
        for (int i = 0; i < 6; i++) {
            seekBar[i].setOnSeekBarChangeListener(seekBarChangeListener);
        }

        paletteRow[0] = view.findViewById(R.id.lightPaletteRow0);
        paletteRow[1] = view.findViewById(R.id.lightPaletteRow1);
        paletteRow[2] = view.findViewById(R.id.lightPaletteRow2);
        paletteRow[3] = view.findViewById(R.id.lightPaletteRow3);
        for (int i = 0; i < 4; i++) {
            paletteRow[i].setOnClickListener(paletteOnClick);
            paletteRow[i].setTag(""+i);
        }


        // select effect
        effectSelected = 0;
        effectButton[effectSelected].setSelected(true);

        // create palettes
        for (int row = 0 ; row < 4; row++) {
            for (int col = 0; col < 16; col++) {
                int r = row *50;
                int g = col * 10;
                int b = (int)((float)255 / (col+1));
                setColor(view, row,col,r,g,b);
            }
        }
        // select palette
        paletteSelect(0);

        return view;
    }

    private void paletteSelect(int newSelection) {
        paletteRow[paletteSelected].setBackgroundColor(0xFF000000);
        paletteSelected = newSelection;
        paletteRow[paletteSelected].setBackgroundColor(0xFFFFFFFF);
    }

    private void setColor(View view, int row, int col, int r, int g, int b) {
        View rowView = view.findViewById(getRowResId(row));
        View color = rowView.findViewById(getColResId(col));
        int rgb = 0xFF000000 + (r<<16) + (g<<8) + b;
        color.setBackgroundColor(rgb);
    }

    private int getRowResId(int row) {
        switch (row) {
            case 0: return R.id.lightPaletteRow0;
            case 1: return R.id.lightPaletteRow1;
            case 2: return R.id.lightPaletteRow2;
            case 3: return R.id.lightPaletteRow3;
        }
        return R.id.lightPaletteRow0;
    }

    private int getColResId(int col) {
        switch (col) {
            case 0:
                return R.id.lightPaletteColor0;
            case 1:
                return R.id.lightPaletteColor1;
            case 2:
                return R.id.lightPaletteColor2;
            case 3:
                return R.id.lightPaletteColor3;
            case 4:
                return R.id.lightPaletteColor4;
            case 5:
                return R.id.lightPaletteColor5;
            case 6:
                return R.id.lightPaletteColor6;
            case 7:
                return R.id.lightPaletteColor7;
            case 8:
                return R.id.lightPaletteColor8;
            case 9:
                return R.id.lightPaletteColor9;
            case 10:
                return R.id.lightPaletteColor10;
            case 11:
                return R.id.lightPaletteColor11;
            case 12:
                return R.id.lightPaletteColor12;
            case 13:
                return R.id.lightPaletteColor13;
            case 14:
                return R.id.lightPaletteColor14;
            case 15:
                return R.id.lightPaletteColor15;
        }
        return R.id.lightPaletteColor0;
    }

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
            GjMessageStatusResponse s = (GjMessageStatusResponse) message;
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