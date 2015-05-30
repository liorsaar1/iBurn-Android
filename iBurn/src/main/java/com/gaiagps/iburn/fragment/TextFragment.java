package com.gaiagps.iburn.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.activity.MainActivity;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageListener;
import com.gaiagps.iburn.gj.message.GjMessageStatusResponse;
import com.gaiagps.iburn.gj.message.GjMessageText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by liorsaar on 4/19/15
 */
public class TextFragment extends Fragment implements GjMessageListener {
    private static final String TAG = "TextFragment";
    private static String[] values = new String[] { "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890",
            "Android", "iPhone", "WindowsMobile",
            "Android", "iPhone", "WindowsMobile",
            "Blackberry", "WebOS", "Ubuntu" };

    private static final ArrayList<TextMessage> list = new ArrayList<TextMessage>();

    private static ArrayAdapter adapter;
    private static ListView listView;
    private static EditText sendTextEditText;
    private static Button sendTextButton;
    private static byte sVehicle = 0;


    public static TextFragment newInstance() {
        return new TextFragment();
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
        View view = inflater.inflate(R.layout.fragment_text, container, false);

        sendTextEditText = (EditText) view.findViewById(R.id.GjTextEditText);
        sendTextEditText.setPadding(20,0,0,0); //must be here, doesnt work in xml

        sendTextButton = (Button)view.findViewById(R.id.GjTextSendButton);
        sendTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSendText(v);
            }
        });

        listView = (ListView) view.findViewById(R.id.textListView);
        for (int i = 0; i < values.length; ++i) {
            list.add(new TextMessage(values[i]));
        }
        adapter = new TextArrayAdapter(getActivity(), list);
        listView.setAdapter(adapter);

        return view;
    }

    private void onClickSendText(View v) {
        String text = sendTextEditText.getText().toString();
        sendTextEditText.setText("");
        if (text.trim().length() ==0) {
            return;
        }
        GjMessageText message = new GjMessageText(text);
        MainActivity.ftdiServiceManager.send(message);
        onMessage(message);
//        FtdiServiceManager.loopback(getActivity(), message.toByteArray());
    }


    @Override
    public void onMessage(GjMessage message) {
        if (message instanceof GjMessageStatusResponse) {
            GjMessageStatusResponse s = (GjMessageStatusResponse)message;
            // store my own vehcile ID as reported by the controller
            sVehicle = s.getVehicle();
            return;
        }
        if (message instanceof GjMessageText) {
            GjMessageText m = (GjMessageText)message;
            list.add(new TextMessage(m.getVehicle(), m.getString()));
            adapter.notifyDataSetChanged();
            listView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    listView.setSelection(adapter.getCount() - 1);
                }
            },250);
        }
    }

    @Override
    public void incoming(byte[] bytes) {
        // notin'
    }

    public boolean isMe(int vehicle) {
        return (vehicle == 0);
    }

    public class TextArrayAdapter extends ArrayAdapter<TextMessage> {
        private final Context context;
        private final ArrayList<TextMessage> values;

        public TextArrayAdapter(Context context, ArrayList<TextMessage> values) {
            super(context, -1, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.fragment_text_list_item, parent, false);
            TextView text = (TextView) rowView.findViewById(R.id.textMessage);
            TextView date = (TextView) rowView.findViewById(R.id.textMessageDate);
            ImageView avatarLeft = (ImageView) rowView.findViewById(R.id.textAvatarLeft);
            ImageView avatarRight = (ImageView) rowView.findViewById(R.id.textAvatarRight);

            LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams) text.getLayoutParams();
            LinearLayout.LayoutParams dateParams = (LinearLayout.LayoutParams) date.getLayoutParams();

            TextMessage message = values.get(position);
            if(isMe(message.vehicle)) {
                text.setGravity(Gravity.RIGHT);
                avatarLeft.setVisibility(View.GONE);
                avatarRight.setVisibility(View.VISIBLE);
                avatarRight.setImageResource(getAvatarResId(message.vehicle));
                textParams.gravity = Gravity.RIGHT;
                dateParams.gravity = Gravity.RIGHT;
            } else {
                text.setGravity(Gravity.LEFT);
                avatarLeft.setVisibility(View.VISIBLE);
                avatarLeft.setImageResource(getAvatarResId(message.vehicle));
                avatarRight.setVisibility(View.GONE);
                textParams.gravity = Gravity.LEFT;
                dateParams.gravity = Gravity.LEFT;
            }

            text.setText( message.text );
            date.setText( message.date );

            return rowView;
        }
    }

    private int getAvatarResId(byte vehicle) {
        switch(vehicle) {
            case 0: return R.drawable.ic_v0;
            case 1: return R.drawable.ic_v1;
            case 2: return R.drawable.ic_v2;
            case 3: return R.drawable.ic_v3;
            case 4: return R.drawable.ic_v4;
            case 5: return R.drawable.ic_v5;
        }
        return R.drawable.ic_v0;
    }

    SimpleDateFormat format = new SimpleDateFormat("EEE, HH:mm a");

    class TextMessage {
        public String text;
        public String date;
        public byte vehicle;

        public TextMessage(String text) {
            this.text = text;
            this.vehicle = 0;
            this.date = format.format(new Date());
        }

        public TextMessage(byte vehicle, String text) {
            this.text = text;
            this.vehicle = vehicle;
            this.date = format.format(new Date());
        }
    }
}