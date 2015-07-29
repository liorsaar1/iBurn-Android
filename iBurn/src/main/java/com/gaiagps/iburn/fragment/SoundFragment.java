package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.gaiagps.iburn.R;

/**
 * Created by liorsaar on 4/19/15
 */

/*
Input (pack=0 / ipod=1)	Internal 	External	Name
0	0	0	Mute
0	0	1	Pack External
0	1	0	Pack Internal
0	1	1	PACK
1	0	0	Mute
1	0	1	Ipod External
1	1	0	Ipod Internal
1	1	1	IPOD
 */
public class SoundFragment extends Fragment {
    private static final String TAG = "SoundFragment";
    private Switch inputIpodSwitch;
    private Switch inputPackSwitch;
    private Switch outputInternalSwitch;
    private Switch outputExternalSwitch;
    private Button soundActivateButton;

    public static SoundFragment newInstance() {
        return new SoundFragment();
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
        View view = inflater.inflate(R.layout.fragment_sound, container, false);

        inputIpodSwitch = (Switch) view.findViewById(R.id.GjSoundInputIpod);
        inputPackSwitch = (Switch) view.findViewById(R.id.GjSoundInputPack);
        outputInternalSwitch = (Switch) view.findViewById(R.id.GjSoundOutputInternal);
        outputExternalSwitch = (Switch) view.findViewById(R.id.GjSoundOutputExternal);

        inputIpodSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                inputIpodOnClick(isChecked);
            }
        });

        inputPackSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                inputPackOnClick(isChecked);
            }
        });

        soundActivateButton = (Button) view.findViewById(R.id.GjSoundActivate);
        soundActivateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activate();
            }
        });

        inputIpodSwitch.setChecked(true);
        outputInternalSwitch.setChecked(true);
        outputExternalSwitch.setChecked(true);

        return view;
    }

    private void inputIpodOnClick(boolean isChecked) {
        inputPackSwitch.setChecked(!isChecked);
    }

    private void inputPackOnClick(boolean isChecked) {
        inputIpodSwitch.setChecked(!isChecked);
    }

    private void activate() {
        int ipod = inputIpodSwitch.isChecked() ? 1 : 0;
        int internal = outputInternalSwitch.isChecked() ? 1 : 0;
        int external = outputExternalSwitch.isChecked() ? 1 : 0;
        int value = matrix[ipod][internal][external];

        tasker(value);
    }

    private void tasker(int value) {
        return;
    }

    private static int[][][] matrix =
            {
                    {
                            {0, 1},
                            {2, 3}
                    },
                    {
                            {4, 5},
                            {6, 7}
                    },
            };
}