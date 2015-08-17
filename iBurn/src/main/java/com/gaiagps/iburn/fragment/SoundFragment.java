package com.gaiagps.iburn.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.gaiagps.iburn.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by liorsaar on 4/19/15
 */

/*
Input (pack=0 / ipod=1)	Internal 	External	Name	Menu Option #
0	0	0	Mute	3
0	0	1	Pack External	4
0	1	0	Pack Internal	5
0	1	1	PACK	7
1	0	0	Mute	3
1	0	1	Ipod External	1
1	1	0	Ipod Internal	2
1	1	1	IPOD	6

The UI looks like this:
0	* Init *
1	I pod ex
2	I pod internal
3	Mute
4	Pack ext
5	Pack in
6	ipod
7	pack
*/

public class SoundFragment extends Fragment {
    private static final String TAG = "SoundFragment";
    private Switch inputIpodSwitch;
    private Switch inputPackSwitch;
    private Switch outputInternalSwitch;
    private Switch outputExternalSwitch;
    private Button soundActivateButton;
    private WebView webview;

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
        webview = (WebView) view.findViewById(R.id.GjSoundMixerWebview);
        webview.setWebViewClient(webViewClient);
        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);

        try {
            Method m1 = WebSettings.class.getMethod("setDomStorageEnabled", new Class[]{Boolean.TYPE});
            m1.invoke(webSettings, Boolean.TRUE);

            Method m2 = WebSettings.class.getMethod("setDatabaseEnabled", new Class[]{Boolean.TYPE});
            m2.invoke(webSettings, Boolean.TRUE);

            Method m3 = WebSettings.class.getMethod("setDatabasePath", new Class[]{String.class});
            m3.invoke(webSettings, "/data/data/" + getActivity().getApplicationContext().getPackageName() + "/databases/");

            Method m4 = WebSettings.class.getMethod("setAppCacheMaxSize", new Class[]{Long.TYPE});
            m4.invoke(webSettings, 1024*1024*8);

            Method m5 = WebSettings.class.getMethod("setAppCachePath", new Class[]{String.class});
            m5.invoke(webSettings, "/data/data/" + getActivity().getApplicationContext().getPackageName() + "/cache/");

            Method m6 = WebSettings.class.getMethod("setAppCacheEnabled", new Class[]{Boolean.TYPE});
            m6.invoke(webSettings, Boolean.TRUE);

            Log.d(TAG, "Enabled HTML5-Features");
        }
        catch (NoSuchMethodException e) {
            Log.e(TAG, "Reflection fail", e);
        }
        catch (InvocationTargetException e) {
            Log.e(TAG, "Reflection fail", e);
        }
        catch (IllegalAccessException e) {
            Log.e(TAG, "Reflection fail", e);
        }

        webview.loadUrl("http://10.10.2.1/mixer.html");

        return view;
    }

    WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            //super.onReceivedError(view, errorCode, description, failingUrl);
            return;
        }
    };


    public View onCreateViewOLD(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        int mixerMenuOption = mixerMenuOptionsMatrix[ipod][internal][external];

        tasker(mixerMenuOption);
    }

    //2180/320
    //2180/630
    private void tasker(final int mixerMenuOption) {
        Toast.makeText(getActivity(), "OPTION " + mixerMenuOption, Toast.LENGTH_LONG).show();
        Intent intent = new Intent("com.joaomgcd.lior.ACTION_CLICK");
        intent.putExtra("click", "2180,320");
        getActivity().sendBroadcast(intent);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent("com.joaomgcd.lior.ACTION_CLICK");
                intent.putExtra("file", ""+mixerMenuOption);
                getActivity().sendBroadcast(intent);
            }
        }, 2000);
        return;
    }

    private static int[][][] mixerMenuOptionsMatrix =
            {
                    {
                            {3, 4},
                            {5, 7}
                    },
                    {
                            {3, 1},
                            {2, 6}
                    },
            };
}

