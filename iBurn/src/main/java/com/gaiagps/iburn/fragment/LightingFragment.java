package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.SeekBar;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.activity.MainActivity;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageLighting;
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
    private static SeekBar[] seekBar = new SeekBar[8];
    private static View[] paletteRow = new View[4];
    private static int modeSelected;
    private static int paletteSelected;

    private View.OnClickListener modeOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = (int) v.getTag();
//            effectButton[modeSelected].setSelected(false);
            modeSelected = index;
//            v.setSelected(true);
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
            // throttle the sliders to 30 messages per second
            if (isOkToSendNextMessage()) {
                onChange();
            }
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
        modeSelected = 0;

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

        GridView modeGrid = (GridView) view.findViewById(R.id.lightModeGrid);
        modeGrid.setAdapter(new lightModeAdapter());
        modeGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((Button)parent.getItemAtPosition(modeSelected)).setSelected(false);
                modeOnClick(position);
                ((Button)parent.getItemAtPosition(modeSelected)).setSelected(true);
            }
        });

        return view;
    }

    private void modeOnClick(int position) {
        modeSelected = position;
        onChange();

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

    private Handler seekbarHandler = new Handler();

    private void onChange() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int value = seekBar[i].getProgress();
            sb.append(value).append(",");
        }
        final GjMessageLighting message = new GjMessageLighting("   "+System.currentTimeMillis());

        seekbarHandler.post(new Runnable() {
            @Override
            public void run() {
//        System.out.println(System.currentTimeMillis() + ":" + message.toString());
        MainActivity.ftdiServiceManager.send(message);
//                FtdiServiceManager.loopback(getActivity(), message.toByteArray());
            }
        });
    }

    private long seekbarTimeOfLastUpdate = 0;
    private boolean isOkToSendNextMessage() {
        if (System.currentTimeMillis() - seekbarTimeOfLastUpdate > 200) {
            seekbarTimeOfLastUpdate = System.currentTimeMillis();
            return true;
        }
        return false;
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

    public class lightModeAdapter extends BaseAdapter {

        public View getView(int position, View convertView, ViewGroup parent) {
            Button button = new Button(getActivity());
            button.setLayoutParams(new GridView.LayoutParams((int) (116 * 2.0), (int) (80 * 2.0)));
            button.setBackgroundResource(R.drawable.light_button);
            button.setTag(position);
            button.setText("" + (position + 1));
            button.setOnClickListener(modeOnClick);
            return button;
        }

        public final int getCount() {
            return 5*4;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public final long getItemId(int position) {
            return position;
        }
    }

}