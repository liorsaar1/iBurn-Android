package com.gaiagps.iburn.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageListener;
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

        listView = (ListView) view.findViewById(R.id.textListView);
        for (int i = 0; i < values.length; ++i) {
            list.add(new TextMessage(values[i]));
        }
        adapter = new TextArrayAdapter(getActivity(), list);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onMessage(GjMessage message) {
        if (message instanceof GjMessageText) {
            GjMessageText m = (GjMessageText)message;
            list.add(new TextMessage(m.getVehicle(), m.getString()));
            adapter.notifyDataSetChanged();
            listView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    listView.setSelection(adapter.getCount() - 1);
                }
            },2000);
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

            TextMessage textMessage = values.get(position);
            if(isMe(position % 2)) {//textMessage.vehicle)) {
                avatarLeft.setVisibility(View.GONE);
                avatarRight.setVisibility(View.VISIBLE);
                avatarRight.setImageResource(R.drawable.ic_v0);
                textParams.gravity = Gravity.RIGHT;
                dateParams.gravity = Gravity.RIGHT;
            } else {
                avatarLeft.setVisibility(View.VISIBLE);
                avatarLeft.setImageResource(R.drawable.ic_v1);
                avatarRight.setVisibility(View.GONE);
                textParams.gravity = Gravity.LEFT;
                dateParams.gravity = Gravity.LEFT;
            }

            text.setText( textMessage.text );
            date.setText( textMessage.date );

            return rowView;
        }
    }

    SimpleDateFormat format = new SimpleDateFormat("EEE, HH:mm a");

    class TextMessage {
        public String text;
        public String date;
        public int vehicle;

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