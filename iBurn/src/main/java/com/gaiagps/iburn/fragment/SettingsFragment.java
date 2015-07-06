package com.gaiagps.iburn.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.activity.MainActivity;
import com.gaiagps.iburn.gj.GjLogoAnimation;
import com.gaiagps.iburn.gj.ftdi.FtdiService;
import com.gaiagps.iburn.gj.ftdi.FtdiServiceManager;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageFactory;
import com.gaiagps.iburn.gj.message.GjMessageGps;
import com.gaiagps.iburn.gj.message.GjMessageLighting;
import com.gaiagps.iburn.gj.message.GjMessageListener;
import com.gaiagps.iburn.gj.message.GjMessageResponse;
import com.gaiagps.iburn.gj.message.GjMessageStatusResponse;
import com.gaiagps.iburn.gj.message.GjMessageText;
import com.gaiagps.iburn.gj.message.internal.GjMessageFtdi;
import com.gaiagps.iburn.gj.message.internal.GjMessageUsb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by liorsaar on 4/19/15
 */
public class SettingsFragment extends Fragment implements GjMessageListener {
    private static final String TAG = "SettingsFragment";

    private static EditText sendTextEditText;
    private static Button sendTextButton;
    private static TextView messageConsole;
    private static TextView messageIncoming;
    private static Button testSendResponse, testSendStatus, testSendGps, testChecksum, testReadFile;
    private static Spinner logoAnimalSpinner;
    private static int statusClickCounter = 0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTheme(R.style.Theme_GJ);
    }

    private static View sView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (sView != null)
            return sView;

        final View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sendTextEditText = (EditText) view.findViewById(R.id.GjMessageEditText);
        sendTextButton = (Button)view.findViewById(R.id.GjMessageSendTextButton);
        sendTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSendText(v);
            }
        });
        messageConsole = (TextView) view.findViewById(R.id.GjMessageConsole);
        messageConsole.setMovementMethod(new ScrollingMovementMethod());
        messageIncoming = (TextView) view.findViewById(R.id.GjIncoming);
        messageIncoming.setMovementMethod(new ScrollingMovementMethod());

        view.findViewById(R.id.GjTestContainer).setVisibility(View.VISIBLE);
        testSendResponse = (Button)view.findViewById(R.id.GjTestSendResponse);
        testSendResponse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTestSendResponse();
            }
        });
        testChecksum = (Button)view.findViewById(R.id.GjTestChecksum);
        testChecksum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTestChecksum();
            }
        });
        testSendStatus = (Button)view.findViewById(R.id.GjTestSendStatus);
        testSendStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTestSendStatus(v);
            }
        });
        testSendGps = (Button)view.findViewById(R.id.GjTestSendGps);
        testSendGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTestSendGps(v);
            }
        });
        testReadFile = (Button)view.findViewById(R.id.GjTestReadFile);
        testReadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTestReadFile(v);
            }
        });

        logoAnimalSpinner = (Spinner)view.findViewById(R.id.GjSetLogoAnimal);

        List<String> list = new ArrayList<String>(Arrays.asList("Tiger", "Elephant", "Lion", "Rhino", "Zebra"));
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        logoAnimalSpinner.setAdapter(dataAdapter);
        final int animalId = SettingsFragment.getPrefAnimal(getActivity());
        logoAnimalSpinner.setSelection(animalId);

        logoAnimalSpinner.post(new Runnable() {
            @Override
            public void run() {
                logoAnimalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        SettingsFragment.setPrefAnimal(getActivity(), position);
                        GjLogoAnimation logoAnimation = new GjLogoAnimation();
                        logoAnimation.start(getActivity(), position);

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
        });

        view.findViewById(R.id.GjConsoleTitle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (++statusClickCounter >= 7) {
                    view.findViewById(R.id.GjTestContainer).setVisibility(View.VISIBLE);
                }
            }
        });

        queueDispatch();
        setOutgoingText();

        sView = view;
        return view;
    }

    private byte fakeStatus = 0;

    private void onClickTestSendStatus(View v) {
        if (fakeStatus == 0) {
            fakeStatus = 1;
        } else {
            fakeStatus <<= 1;
        }

        GjMessageStatusResponse status = new GjMessageStatusResponse(fakeStatus);
        MainActivity.ftdiServiceManager.send(status);
        loopback(status.toByteArray());
    }

    private void onClickTestSendGps(View v) {
//        MainActivity.ftdiServiceManager.send(GjMessageFactory.createGps());
        loopback(GjMessageFactory.createGps().array());

    }

    private void onClickTestReadFile(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, 1234);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1234  &&  data != null ) {
            Uri uri = data.getData();
            try {
                StringBuffer sb = GalacticJungleFragment.logRead(getActivity(), uri);
                ByteBuffer bb = GjMessageFactory.fromString(sb.toString());
                List<GjMessage> list = GjMessageFactory.parseAll(bb);
                for (GjMessage message : list) {
                    Log.e(TAG, message.toString());
                }
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void onClickSendText(View v) {
        int vehicle = (int)(5*Math.random());
        String text = sendTextEditText.getText().toString();
        GjMessageText message = new GjMessageText(text, (byte)vehicle);
        MainActivity.ftdiServiceManager.send(message);
        console(">>> " + message);
        loopback(message.toByteArray());
        setOutgoingText();
    }

    private void setOutgoingText() {
        String defaultText = "Text Message # " + FtdiServiceManager.outgoingPacketNumber + " ";
        sendTextEditText.setText(defaultText);
    }

    public void console(String string) {
        if (messageConsole == null ) {
            return;
        }
        messageConsole.append(string + "\n\n");
        scrollToEnd(messageConsole);
    }

    /*
     * listener
     */

    @Override
    public void incoming(byte[] bytes) {
        // if UI not created yet - queue the messages
        if (messageIncoming == null) {
            return;
        }
        String hex = GjMessage.toHexString(bytes);
        console("<<< Incoming:" + hex + "\n");
    }

    private static int lastPacket = -1;

    @Override
    public void onMessage(GjMessage message) {
        // if UI not created yet - queue the messages
        if (messageConsole == null) {
            queue(message);
            return;
        }

        if (message instanceof GjMessageFtdi) {
            console("### " + message.toString());
            return;
        }
        if (message instanceof GjMessageUsb) {
            console("### " + message.toString());
            return;
        }
        if (message instanceof GjMessageResponse) {
            GjMessageResponse response = (GjMessageResponse)message;
            console("<<< " + message.toString());
            return;
        }
        if (message instanceof GjMessageStatusResponse) {
            console("<<< " + message.toString());
            return;
        }
        if (message instanceof GjMessageGps) {
            // report packet number
            console("<<< " + message.toString());
            return;
        }
        if (message instanceof GjMessageText) {
            console("<<< " + message.toString());
            return;
        }
        if (message instanceof GjMessageLighting) {
            console("<<< " + message.getTime() + ":" + message.toString());
            return;
        }
        // otherwise
        console(message.toString());
    }

    private static List<GjMessage> queue = new ArrayList<>();

    private void queue(GjMessage message) {
        queue.add(message);
    }

    // once the UI is up - dispatch the queue
    private void queueDispatch() {
        for ( GjMessage message : queue) {
            onMessage(message);
        }
    }

    public static void scrollToEnd(final TextView tv) {
        tv.post(new Runnable() {
            @Override
            public void run() {
                final int scrollAmount = tv.getLayout().getLineTop(tv.getLineCount()) - tv.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0)
                    tv.scrollTo(0, scrollAmount);
                else
                    tv.scrollTo(0, 0);
            }
        });
    }

    public void onClickTestSendResponse() {
        if (!MainActivity.ftdiServiceManager.isBound()) {
            return;
        }
        console("Fake Checksum Error for packet " + lastPacket);
        int written = MainActivity.ftdiServiceManager.send(new GjMessageResponse((byte)lastPacket, (byte)9, new byte[]{0} ));
    }

    public void onClickTestChecksum() {
        GjMessageResponse response = new GjMessageResponse((byte)lastPacket, (byte)9, new byte[]{0} );
        console("Fake Checksum Error for packet " + lastPacket);
        loopback(response.toByteArray());
    }

    public void loopback(byte[] bytes) {
        FtdiServiceManager.outgoingPacketNumber++;

        Intent intent = new Intent(FtdiServiceManager.ACTION_VIEW);
        intent.putExtra(FtdiService.FTDI_SERVICE_MESSSAGE, bytes);
        getActivity().sendBroadcast(intent);
    }

    public static void setPrefAnimal(Activity activity, int vehicle) {
        SharedPreferences prefs = activity.getSharedPreferences("gj", Context.MODE_PRIVATE);
        prefs.edit().putInt("animal", vehicle).commit();
    }

    public static int getPrefAnimal(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("gj", Context.MODE_PRIVATE);
        return prefs.getInt("animal",0);
    }


}