package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.activity.MainActivity;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageLighting;
import com.gaiagps.iburn.gj.message.GjMessageListener;
import com.gaiagps.iburn.gj.message.GjMessageStatusResponse;
import com.gaiagps.iburn.gj.message.internal.GjMessageFtdi;

import java.util.ArrayList;
import java.util.List;

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

    private ModeAdapter modeAdapter;
    private PaletteAdapter paletteAdapter;
    private Handler seekbarHandler = new Handler();
    private long seekbarTimeOfLastUpdate = 0;
    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // throttle the sliders to 30 messages per second
            if (isOkToSendNextMessage()) {
                onChange();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
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
    public void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                modeAdapter.setSelected(0);
            }
        }, 1000);
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

        // MODE
        GridView modeGrid = (GridView) view.findViewById(R.id.lightModeGrid);
        modeAdapter = new ModeAdapter();
        modeGrid.setAdapter(modeAdapter);
        modeAdapter.setSelected(0);

        // PALETTE
        GridView paletteGrid = (GridView) view.findViewById(R.id.lightPaletteGrid);
        paletteAdapter = new PaletteAdapter();
        paletteGrid.setAdapter(paletteAdapter);
        paletteAdapter.setSelected(0);

        return view;
    }

    private void onChange() {
        StringBuilder sb = new StringBuilder();
        sb.append(modeAdapter.getSelected()).append(",");
        sb.append(paletteAdapter.getSelected()).append(",");
        for (int i = 0; i < 6; i++) {
            int value = seekBar[i].getProgress();
            sb.append(value).append(",");
        }
        final GjMessageLighting message = new GjMessageLighting(sb.toString());

        seekbarHandler.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.ftdiServiceManager.send(message);
                //FtdiServiceManager.loopback(getActivity(), message.toByteArray());
            }
        });
    }

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

    public class ModeAdapter extends BaseAdapter {

        private static final int WIDTH = 5;
        private static final int HEIGHT = 4;
        private List<Button> buttons;
        private int selected = 0;
        private View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = (int) v.getTag();
                setSelected(index);
                onChange();
            }
        };

        public ModeAdapter() {
            init();
        }

        private void init() {
            buttons = new ArrayList<>();
            for (int i = 0; i < getCount(); i++) {
                Button button = new Button(getActivity());
                button.setLayoutParams(new GridView.LayoutParams((int) (116 * 2.0), (int) (80 * 2.0)));
                button.setBackgroundResource(R.drawable.light_button);
                button.setTag(i);
                button.setText("" + (i + 1));
                button.setOnClickListener(onClick);
                buttons.add(button);
            }
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return buttons.get(position);
        }

        public final int getCount() {
            return WIDTH * HEIGHT;
        }

        @Override
        public Object getItem(int position) {
            return buttons.get(position);
        }

        @Override
        public final long getItemId(int position) {
            return position;
        }

        public int getSelected() {
            return selected;
        }

        public void setSelected(int index) {
            buttons.get(selected).setSelected(false);
            selected = index;
            buttons.get(selected).setSelected(true);
        }
    }

    public class PaletteAdapter extends BaseAdapter {

        private static final int WIDTH = 1;
        private static final int HEIGHT = 10;
        private List<LinearLayout> views;
        private int selected = 0;
        private View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = (int) v.getTag();
                setSelected(index);
                onChange();
            }
        };

        public PaletteAdapter() {
            init();
        }

        private void init() {
            views = new ArrayList<>();
            LayoutInflater inflater = getActivity().getLayoutInflater();
            for (int i = 0; i < getCount(); i++) {
                LinearLayout view = (LinearLayout) inflater.inflate(R.layout.lighting_palette_row, null);
                view.setTag(i);
                view.setOnClickListener(onClick);
                presetRow(view, i);
                views.add(view);
            }
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return views.get(position);
        }

        public final int getCount() {
            return WIDTH * HEIGHT;
        }

        @Override
        public Object getItem(int position) {
            return views.get(position);
        }

        @Override
        public final long getItemId(int position) {
            return position;
        }

        public int getSelected() {
            return selected;
        }

        public void setSelected(int index) {
            views.get(selected).setBackgroundColor(0x00000000);
            selected = index;
            views.get(selected).setBackgroundColor(0xFFFFFFFF);
        }

        private void presetRow(View rowView, int position) {
            for (int i = 0; i < 16; i++) {
                int r = (position * 10) << 16;
                int g = (i * 10) << 8;
                int b = 128;
                int color = 0xFF000000 + r + g + b;
                View colorView = rowView.findViewById(getColResId(i));
                colorView.setBackgroundColor(color);
            }
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
    }

}
