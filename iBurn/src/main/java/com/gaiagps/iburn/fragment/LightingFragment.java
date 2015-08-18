package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.activity.MainActivity;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageLighting;
import com.gaiagps.iburn.gj.message.GjMessageListener;
import com.gaiagps.iburn.gj.message.GjMessageStatusResponse;
import com.gaiagps.iburn.gj.message.internal.GjMessageFtdi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

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
    private static final int LIGHTING_MESSAGE_REFRESH_RATE = 5; // seconds
    private static SeekBar[] seekBar;

    private static ModeAdapter modeAdapter;
    private static PaletteAdapter paletteAdapter;
    private static View sView;
    private static String[] buttonPresets = {
            "button 1/1", "button 1/2", "button 1/3", "button 1/4",
            "button 2/1", "button 2/2", "button 2/3", "button 2/4",
            "button 3/1", "button 3/2", "button 3/3", "button 3/4",
            "button 4/1", "button 4/2", "button 4/3", "button 4/4",
            "button 5/1", "button 5/2", "button 5/3", "button 5/4",
    };
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
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private Handler seekbarHandler = new Handler();
    private long seekbarTimeOfLastUpdate = 0;
    private ScheduledFuture scheduledFuture;
    private GjMessage lastLightMessage;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (sView != null) {
            return sView;
        }
        // read the assets in
        readAssets("lighting.txt");

        View view = inflater.inflate(R.layout.fragment_lighting, container, false);

        seekBar = new SeekBar[6];
        seekBar[0] = (SeekBar) view.findViewById(R.id.lightHue);
        seekBar[1] = (SeekBar) view.findViewById(R.id.lightBrightness);
        seekBar[2] = (SeekBar) view.findViewById(R.id.lightSaturation);
        seekBar[3] = (SeekBar) view.findViewById(R.id.lightSpeed);
        seekBar[4] = (SeekBar) view.findViewById(R.id.lightParameter1);
        seekBar[5] = (SeekBar) view.findViewById(R.id.lightParameter2);
        for (int i = 0; i < seekBar.length; i++) {
            seekBar[i].setOnSeekBarChangeListener(seekBarChangeListener);
        }

        // MODE
        GridView modeGrid = (GridView) view.findViewById(R.id.lightModeGrid);
        modeAdapter = new ModeAdapter();
        modeAdapter.setSelected(0);
        modeGrid.setAdapter(modeAdapter);

        // PALETTE
        GridView paletteGrid = (GridView) view.findViewById(R.id.lightPaletteGrid);
        paletteAdapter = new PaletteAdapter();
        paletteAdapter.setSelected(0);
        paletteGrid.setAdapter(paletteAdapter);

        sView = view;
        return view;
    }

    private void readAssets(String filename) {
        File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(sdcard, filename);
        if (!file.exists()) return;

        BufferedReader reader = null;
        try {
            //reader = new BufferedReader(new InputStreamReader(getActivity().getAssets().open(filename)));
            reader = new BufferedReader(new FileReader(file));

            for (int i=0; i < 4*5; i++) {
                String line = reader.readLine();
                buttonPresets[i] = line;
            }
            for (int i=0; i < 10; i++) {
                String line = reader.readLine();
                palettePresets[i] = line;
            }
        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }    }

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
                startRefresh(message);
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
        // noop
        if (message instanceof GjMessageStatusResponse) {
            GjMessageStatusResponse s = (GjMessageStatusResponse) message;
            return;
        }
        // an external light command - we are not master anymore
        if (message instanceof GjMessageLighting) {
            // stop refreshing
            stopRefresh();
        }
        // disable controls if ftdi stopped
        if (message instanceof GjMessageFtdi) {
        }
    }

    @Override
    public void incoming(byte[] bytes) {
        // notin'
    }

    // the last unit to send a ligthing message is the master until another unit takes over
    // the master is responsible to generate a refresh message every 5 seconds
    // to sync any new addition to the pack
    private void startRefresh(final GjMessage message) {
        Log.e(TAG, "start Refresh");
        // save the message
        lastLightMessage = message;
        // if a refresh is already scheduled, remove it
        stopRefresh();
        // arm the next update
        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "Refresh");
                MainActivity.ftdiServiceManager.send(lastLightMessage);
                //FtdiServiceManager.loopback(getActivity(), lastLightMessage.toByteArray());
            }
        }, LIGHTING_MESSAGE_REFRESH_RATE, LIGHTING_MESSAGE_REFRESH_RATE, SECONDS);

    }

    private void stopRefresh() {
        Log.e(TAG, "stop Refresh");
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    public class ModeAdapter extends BaseAdapter {

        private static final int WIDTH = 4;
        private static final int HEIGHT = 5;
        //        private List<Button> buttons;
        private List<TextView> views;
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
            views = new ArrayList<>();
            for (int i = 0; i < getCount(); i++) {
                TextView view = new TextView(getActivity());
                view.setLayoutParams(new GridView.LayoutParams((int) (145 * 2.0), (int) (60 * 2.0)));
                view.setBackgroundResource(R.drawable.light_button);
                view.setTag(i);
                view.setLines(2);
                view.setPadding(20,0,20,0);
                view.setEllipsize(TextUtils.TruncateAt.END);
                view.setGravity(Gravity.CENTER);
                view.setText(buttonPresets[i]);
                view.setOnClickListener(onClick);
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
            views.get(selected).setBackgroundResource(R.drawable.light_button_normal);
            selected = index;
            views.get(selected).setBackgroundResource(R.drawable.light_button_selected);
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
                int rgb = r + g + b;
                sb.append(String.format("0x%06x,", rgb));
                int color = 0xFF000000 + r + g + b;
                View colorView = rowView.findViewById(getColResId(i));
                colorView.setBackgroundColor(color);
            }
            Log.e(TAG, sb.toString());
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
