package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageMode;
import com.gaiagps.iburn.gj.message.GjMessageReportGps;
import com.gaiagps.iburn.gj.message.GjMessageRequestGps;
import com.gaiagps.iburn.gj.message.GjMessageStatusRequest;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by liorsaar on 4/19/15
 */
public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";

    private View messageEditTextContainer;
    private EditText messageEditText;
    private TextView messageConsole;
    private TextView messageIncoming;
    private Button messageTextButton;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
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
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        messageEditText = (EditText) view.findViewById(R.id.GjMessageEditText);
        messageConsole = (TextView) view.findViewById(R.id.GjMessageConsole);
        messageConsole.setMovementMethod(new ScrollingMovementMethod());
        messageIncoming = (TextView) view.findViewById(R.id.GjIncoming);
        messageIncoming.setMovementMethod(new ScrollingMovementMethod());

        view.findViewById(R.id.GjMessageStatusRequest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickStatusRequest(v);
            }
        });
        view.findViewById(R.id.GjMessageRequestGps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onClickRequestGps(v); }
        });
        view.findViewById(R.id.GjMessageReportGps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onClickReportGps(v); }
        });
        view.findViewById(R.id.GjMessageModeBuffered).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onClickModeBuffered(v) ; }
        });
        view.findViewById(R.id.GjMessageModeNonBuffered).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onClickModeNonBuffered(v); }
        });

        return view;
    }

    ///////////////////////////////////////////////
    // SEND
    ///////////////////////////////////////////////
    Handler sendHandler = new Handler();

    public void onClickStatusRequest(View v) {
        send( new GjMessageStatusRequest() );
    }
    public void onClickRequestGps(View v) {
        send( new GjMessageRequestGps() );
    }
    public void onClickReportGps(View v) {
        int id = 5;
        LatLng latLng = new LatLng(40.7888, -119.20315);
        send(new GjMessageReportGps(id, latLng));
    }
    public void onClickModeBuffered(View v) {
        send( new GjMessageMode(GjMessage.Mode.Buffered) );
    }
    public void onClickModeNonBuffered(View v) {
        send( new GjMessageMode(GjMessage.Mode.NonBuffered) );
    }

    private void send( final GjMessage message ) {
        // queue requests to avoid ftdi jamming
        sendHandler.post(new Runnable() {
            @Override
            public void run() {
                console(message);
            }
        });
    }


    private void console(GjMessage message) {
        messageConsole.append(">>> " + message.toString() + "\n");
        messageConsole.append(">>> " + message.toHexString() + "\n");

        messageConsole.post(new Runnable() {
            @Override
            public void run() {
                final int scrollAmount = messageConsole.getLayout().getLineTop(messageConsole.getLineCount()) - messageConsole.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0)
                    messageConsole.scrollTo(0, scrollAmount);
                else
                    messageConsole.scrollTo(0, 0);
            }
        });
    }

}