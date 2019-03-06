package aetherapps.com.smartstreamer;

import android.Manifest;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.LocaleDisplayNames;
import android.net.wifi.WifiManager;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.style.SubscriptSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.BaseVideoCapturer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.BaseVideoCapturer;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.VideoRenderFactory;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.opentok.android.Publisher.CameraCaptureFrameRate.FPS_30;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

//
//private class ServerClass extends Thread{
//
//    private BluetoothServerSocket serverSocket;
//    public ServerClass(){
//
//        serverSocket = bluetoothAdapter.listenUsin
//    }
//}

/*
* First open app
* start stream and connect to bluetooth
* If wifi not on, turn on, if BT not on, turn on
* Request all the Permissions
* If rejected, then warn and close app
* If accepted,
*
*
*
*
* */

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, Session.SessionListener, PublisherKit.PublisherListener, BaseVideoCapturer.CaptureSwitch {

    private static String API_KEY = "46278792";
    private static String SESSION_ID = "1_MX40NjI3ODc5Mn5-MTU1MTQ1MjEwOTY5Mn4zWmpFSVE5V2xoNWwvL2liaWE3cEgyWmZ-fg";
    private static String TOKEN = "T1==cGFydG5lcl9pZD00NjI3ODc5MiZzaWc9OGFjN2ZmMTgzOGFiYjg1YmE0OWNkZjVjMTUyNzY5MTA4NDYwNDAzNTpzZXNzaW9uX2lkPTFfTVg0ME5qSTNPRGM1TW41LU1UVTFNVFExTWpFd09UWTVNbjR6V21wRlNWRTVWMnhvTld3dkwybGlhV0UzY0VneVdtWi1mZyZjcmVhdGVfdGltZT0xNTUxNDUyMTQxJm5vbmNlPTAuNDc0NzA0Mjk2Njc4Mzk4MyZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNTU0MDQwNTM4JmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";

    public static final int RC_SETTINGS = 123;
    private Session session;
    Button cnctBtn;
    String IMEI;
    Boolean CONNECTED_TO_HC05 = false;

    FirebaseDatabase database;
    DatabaseReference myRef;


    private BluetoothAdapter myBt;


    private Publisher publisher;

    private Subscriber subscriber;

    private void disableButton() {

        cnctBtn.setEnabled(false);
        cnctBtn.setClickable(false);

    }


    public String getDeviceIMEI() {
        String deviceUniqueIdentifier = null;
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (null != tm) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            }
            deviceUniqueIdentifier = tm.getDeviceId();
        }
        if (null == deviceUniqueIdentifier || 0 == deviceUniqueIdentifier.length()) {
            deviceUniqueIdentifier = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        IMEI = deviceUniqueIdentifier;
        return deviceUniqueIdentifier;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        cnctBtn = findViewById(R.id.cnctBtn);
        database = FirebaseDatabase.getInstance();
        requestPermissions();
        cnctBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBt = BluetoothAdapter.getDefaultAdapter();
                if (myBt == null) {
                    Toast.makeText(getApplicationContext(), "No Bluetooth", Toast.LENGTH_SHORT).show();
                } else if (!myBt.isEnabled()) {
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
                }

                WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifi.isWifiEnabled()) {

                } else {
                    Intent turnWifiOn = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(turnWifiOn);
                }
                ListPairedDevices();
                requestPermissions();
            }
        });


    }

    private void connectToMount() throws Exception {

        BluetoothAdapter btA = BluetoothAdapter.getDefaultAdapter();
        final Set<BluetoothDevice> bt = btA.getBondedDevices();

        Object[] devices = bt.toArray();
        BluetoothDevice device = (BluetoothDevice) devices[0];
        ParcelUuid[] uuids = device.getUuids();
        BluetoothSocket socket = null;
        try {
            socket = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            inStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        disableButton();

    }

    private void ListPairedDevices() {

        BluetoothAdapter btA = BluetoothAdapter.getDefaultAdapter();
        final Set<BluetoothDevice> bt = btA.getBondedDevices();

        String[] names = new String[bt.size()];
        int i = 0;
        if (bt.size() > 0) {
            for (BluetoothDevice d : bt) {
                names[i] = d.getName();
                i++;
            }
//            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, names);
            AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
            mBuilder.setTitle("Connect to Mount");
            mBuilder.setIcon(R.drawable.icon);

            mBuilder.setSingleChoiceItems(names, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getApplicationContext(), "adf" + which, Toast.LENGTH_SHORT);


                    Object[] devices = bt.toArray();
                    BluetoothDevice device = (BluetoothDevice) devices[which];
                    ParcelUuid[] uuids = device.getUuids();
                    BluetoothSocket socket = null;
                    try {
                        socket = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.connect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        outputStream = socket.getOutputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        inStream = socket.getInputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    dialog.dismiss();
                    disableButton();

                }
            });

            AlertDialog mDialog = mBuilder.create();
            mDialog.show();


        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        initialize();

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
//        Log.d(  "onPermissionsDenied:" + requestCode + ":" + perms.size());

//        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
//            new AppSettingsDialog.Builder(this)
//                    .setTitle(getString(R.string.title_settings_dialog))
//                    .setRationale(getString(R.string.rationale_ask_again))
//                    .setPositiveButton("Settings")
//                    .setNegativeButton("Cancel")
//                    .setRequestCode(RC_SETTINGS)
//                    .build()
//                    .show();
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);


    }

//    public void send(String msg){
//        try {
//
//            out = socket.getOutputStream();
//            out.write(msg.getBytes());
//        } catch (IOException e) {
//            CONNECTED_TO_HC05 = false;
//        }
//    }

    private void initialize() {



        myBt = BluetoothAdapter.getDefaultAdapter();
        if (myBt == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth", Toast.LENGTH_SHORT).show();
        } else if (!myBt.isEnabled()) {
//            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
        }

        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled()) {

        } else {
//            Intent turnWifiOn = new Intent(Settings.ACTION_WIFI_SETTINGS);
//            startActivity(turnWifiOn);
        }


        try {
            connectToMount();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            write("I'm all geared up!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        myRef = database.getReference("users/userID/cameras/" + getDeviceIMEI() + "/action");
        myRef.child("action").setValue(0);
//        write();

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                try {
                    Toast.makeText(getApplicationContext(), dataSnapshot.getValue().toString(), Toast.LENGTH_LONG).show();
                    write((dataSnapshot.getValue() + "").charAt(0) + "");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        Toast.makeText(getApplicationContext(), "accepted", Toast.LENGTH_SHORT).show();

        session = new Session.Builder(this, API_KEY, SESSION_ID).build();
        session.setSessionListener(this);

        session.connect(TOKEN);


    }

    @AfterPermissionGranted(RC_SETTINGS)
    private void requestPermissions() {

        String[] perm = {Manifest.permission.CHANGE_NETWORK_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.INTERNET, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_PRIVILEGED, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (EasyPermissions.hasPermissions(this, perm)) {
            initialize();

        } else {


            Toast.makeText(getApplicationContext(), "rejected", Toast.LENGTH_SHORT).show();
//            finish();
            EasyPermissions.requestPermissions(this, "This app needs to access your camera, microphone and Bluetooth", RC_SETTINGS, perm);

        }

    }

    @Override
    public void onConnected(Session session) {
//        BaseVideoCapturer mCapturer =new BaseVideoCapturer().;

        BaseVideoCapturer.CaptureSettings cs = new BaseVideoCapturer.CaptureSettings();
//        cs.format = NfcV;
        publisher = new Publisher.Builder(this).build();

        publisher.cycleCamera();
//    publisher.getRenderer().setStyle(BaseVideoRenderer.);

        publisher.setPublisherListener(this);
        publisher.setName(IMEI);

//        PublisherContainer.addView(publisher.getView());
        session.publish(publisher);



    }

    @Override
    public void onDisconnected(Session session) {

    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {

        if (subscriber == null) {
            subscriber = new Subscriber.Builder(this, stream).build();
            session.subscribe(subscriber);

//            SubscriberContainer.addView(subscriber.getView());


        }

    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {

        if (subscriber != null) {
            subscriber = null;
//            SubscriberContainer.removeAllViews();

        }

    }

    @Override
    public void onError(Session session, OpentokError opentokError) {

    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }

    @Override
    public void cycleCamera() {

        swapCamera(0);

    }

    @Override
    public int getCameraIndex() {
        return 0;
    }

    @Override
    public void swapCamera(int i) {

    }


    private OutputStream outputStream;
    private InputStream inStream;

//    private void init() throws IOException {
//        BluetoothAdapter blueAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (blueAdapter != null) {
//            if (blueAdapter.isEnabled()) {
//                Set<BluetoothDevice> bondedDevices = blueAdapter.getBondedDevices();
//
//                if(bondedDevices.size() > 0) {
//                    Object[] devices = (Object []) bondedDevices.toArray();
//                    BluetoothDevice device = (BluetoothDevice) devices[position];
//                    ParcelUuid[] uuids = device.getUuids();
//                    BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
//                    socket.connect();
//                    outputStream = socket.getOutputStream();
//                    inStream = socket.getInputStream();
//                }
//
//                Log.e("error", "No appropriate paired devices.");
//            } else {
//                Log.e("error", "Bluetooth is disabled.");
//            }
//        }
//    }

    public void write(String s) throws IOException {
        try {
            outputStream.write(s.getBytes());
        } catch (Exception e) {

        }
    }

    public void run() {
        final int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytes = 0;
        int b = BUFFER_SIZE;

        while (true) {
            try {
                bytes = inStream.read(buffer, bytes, BUFFER_SIZE - bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


