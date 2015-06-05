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

    private static ModeAdapter modeAdapter;
    private static PaletteAdapter paletteAdapter;
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
                modeAdapter.setSelected(modeAdapter.getSelected());
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
        if (modeAdapter == null) {
            modeAdapter = new ModeAdapter();
            modeAdapter.setSelected(0);
        }
        modeGrid.setAdapter(modeAdapter);

        // PALETTE
        GridView paletteGrid = (GridView) view.findViewById(R.id.lightPaletteGrid);
        if (paletteAdapter == null) {
            paletteAdapter = new PaletteAdapter();
            paletteAdapter.setSelected(0);
        }
        paletteGrid.setAdapter(paletteAdapter);

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

    private static String[] palettePresets = {
            "0x000080,0x000a80,0x001480,0x001e80,0x002880,0x003280,0x003c80,0x004680,0x005080,0x005a80,0x006480,0x006e80,0x007880,0x008280,0x008c80,0x009680",
            "0x0a0080,0x0a0a80,0x0a1480,0x0a1e80,0x0a2880,0x0a3280,0x0a3c80,0x0a4680,0x0a5080,0x0a5a80,0x0a6480,0x0a6e80,0x0a7880,0x0a8280,0x0a8c80,0x0a9680",
            "0x140080,0x140a80,0x141480,0x141e80,0x142880,0x143280,0x143c80,0x144680,0x145080,0x145a80,0x146480,0x146e80,0x147880,0x148280,0x148c80,0x149680",
            "0x1e0080,0x1e0a80,0x1e1480,0x1e1e80,0x1e2880,0x1e3280,0x1e3c80,0x1e4680,0x1e5080,0x1e5a80,0x1e6480,0x1e6e80,0x1e7880,0x1e8280,0x1e8c80,0x1e9680",
            "0x280080,0x280a80,0x281480,0x281e80,0x282880,0x283280,0x283c80,0x284680,0x285080,0x285a80,0x286480,0x286e80,0x287880,0x288280,0x288c80,0x289680",
            "0x320080,0x320a80,0x321480,0x321e80,0x322880,0x323280,0x323c80,0x324680,0x325080,0x325a80,0x326480,0x326e80,0x327880,0x328280,0x328c80,0x329680",
            "0x3c0080,0x3c0a80,0x3c1480,0x3c1e80,0x3c2880,0x3c3280,0x3c3c80,0x3c4680,0x3c5080,0x3c5a80,0x3c6480,0x3c6e80,0x3c7880,0x3c8280,0x3c8c80,0x3c9680",
            "0x460080,0x460a80,0x461480,0x461e80,0x462880,0x463280,0x463c80,0x464680,0x465080,0x465a80,0x466480,0x466e80,0x467880,0x468280,0x468c80,0x469680",
            "0x500080,0x500a80,0x501480,0x501e80,0x502880,0x503280,0x503c80,0x504680,0x505080,0x505a80,0x506480,0x506e80,0x507880,0x508280,0x508c80,0x509680",
            "0x5a0080,0x5a0a80,0x5a1480,0x5a1e80,0x5a2880,0x5a3280,0x5a3c80,0x5a4680,0x5a5080,0x5a5a80,0x5a6480,0x5a6e80,0x5a7880,0x5a8280,0x5a8c80,0x5a9680"
    };

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
                readRowPreset(view, i);
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
            views.get(selected).setBackground(getActivity().getResources().getDrawable(R.drawable.light_palette_background));
        }

        private void readRowPreset(View rowView, int position) {
            String presets = palettePresets[position];
            String[] rgbStrings = presets.split(",");
            for (int i = 0; i < rgbStrings.length; i++) {
                int rgb = 0xFF000000 + Integer.decode(rgbStrings[i]);
                View colorView = rowView.findViewById(getColResId(i));
                colorView.setBackgroundColor(rgb);
            }
        }

        private void presetRow(View rowView, int position) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < 16; i++) {
                int r = (position * 10) << 16;
                int g = (i * 10) << 8;
                int b = 128;
                int rgb = r+g+b;
                sb.append(String.format("0x%06x,", rgb));
                int color = 0xFF000000 + r + g + b;
                View colorView = rowView.findViewById(getColResId(i));
                colorView.setBackgroundColor(color);
            }
            System.out.println(sb.toString());
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
