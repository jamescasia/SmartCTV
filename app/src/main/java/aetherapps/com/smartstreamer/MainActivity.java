package aetherapps.com.smartstreamer;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.opentok.android.BaseVideoCapturer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
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

    private static String API_KEY = "46283042";
    private static String SESSION_ID = "1_MX40NjI4MzA0Mn5-MTU1MjAxOTA0Nzc3Nn4xN0EzN0ZueXd5S0UvS3J4OUNqTWRkOWx-fg";
    private static String TOKEN = "T1==cGFydG5lcl9pZD00NjI4MzA0MiZzaWc9MTBiMzdkMjdiODlhYzE2ZWMxNTgxZTQzNTBhNmZkN2QxMDMyYTkxNTpzZXNzaW9uX2lkPTFfTVg0ME5qSTRNekEwTW41LU1UVTFNakF4T1RBME56YzNObjR4TjBFek4wWnVlWGQ1UzBVdlMzSjRPVU5xVFdSa09XeC1mZyZjcmVhdGVfdGltZT0xNTUyMDE5MDcwJm5vbmNlPTAuODY0MTY0NDE0NTQzMTU5NyZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNTU0NjA3NDY5JmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";
    private static final boolean QUANT = true;
    private static final int INPUT_SIZE = 300;

    Date when = new Date(System.currentTimeMillis());


    private static final String MODEL_PATH = "detect.tflite";
    private static final String LABEL_PATH = "coco_labels_list.txt";
    //
    private CameraView cameraView;
    private Classifier classifier;
    Boolean inited = false;
    private Executor executor = Executors.newSingleThreadExecutor();
    public static final int RC_SETTINGS = 123;
    private Session session;
    Button cnctBtn;
    String IMEI;
    Boolean CONNECTED_TO_HC05 = false;
    Button infer;
    FirebaseDatabase database;
    DatabaseReference myRef;
    DatabaseReference activeUsers;
    DatabaseReference detections;
    String strgLink = "";
    private StorageReference storage;
    private BluetoothAdapter myBt;


    private Publisher publisher;

    private Subscriber subscriber;
    private Boolean streamingOrNot = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        initTensorFlowAndLoadModel();

        cameraView = findViewById(R.id.cameraView);
        cameraView.setVisibility(View.INVISIBLE);
        cnctBtn = findViewById(R.id.cnctBtn);
//        infer = findViewById(R.id.inferBtn);
//
//        infer.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                cameraView.captureImage();
////                infer();
//            }
//        });

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
//        Toast.makeText(getApplicationContext(), deviceUniqueIdentifier, Toast.LENGTH_SHORT).show();
        return deviceUniqueIdentifier;
    }

    protected void initObjectDetector() {

//        protected Interpreter tflite;
//        tflite = new Interpreter(loadModelFile(activity));
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
//    private void

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
//                    Toast.makeText(getApplicationContext(), "adf" + which, Toast.LENGTH_SHORT);


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
//        initialize();
//        Toast.makeText(getApplicationContext(), "onPermissionGranted", Toast.LENGTH_SHORT).show();

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
//        if (inited) {
//            return;
//        }
//
//        inited = true;


        database = FirebaseDatabase.getInstance();


//        Toast.makeText(getApplicationContext(), "dafaf", Toast.LENGTH_SHORT).show();


//        Toast.makeText(getApplicationContext(), getBitmapFromAsset(  "assets\\images\\allcp.png").getHeight()  , Toast.LENGTH_LONG).show();


        myBt = BluetoothAdapter.getDefaultAdapter();
        if (myBt == null) {
//            Toast.makeText(getApplicationContext(), "No Bluetooth", Toast.LENGTH_SHORT).show();
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
        myRef.setValue(0);
//        write();

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                try {
//                    Toast.makeText(getApplicationContext(), dataSnapshot.getValue().toString(), Toast.LENGTH_LONG).show();
                    write((dataSnapshot.getValue() + "").charAt(0) + "");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        storage = FirebaseStorage.getInstance().getReference();

        Toast.makeText(getApplicationContext(), "accepdted", Toast.LENGTH_SHORT).show();
//        cameraView.


        activeUsers = database.getReference("users/userID/viewers");
//cameraView.setTo
        activeUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (Integer.parseInt(dataSnapshot.getValue().toString()) <= 0) {

                    if (streamingOrNot) {
                        stopStreaming();
                    }

                    startDetecting();
                } else if (Integer.parseInt(dataSnapshot.getValue().toString()) >= 1) {

                    stopDetecting();
                    if (!streamingOrNot) {
                        startStreaming();
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
//        startDetecting();
//        initbtm

    }

    private void stopDetecting() {
        cameraView.stop();
    }


    private String uploadFile(Bitmap bitmap, String pushKey) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        final StorageReference storageRef = storage.getReferenceFromUrl("gs://smart-streamer.appspot.com");
        final StorageReference currentPicRef = storageRef.child("userID/" + pushKey + ".jpg");
//        final StoragepicRef = storageRef.child("userID/" + pushKey + ".jpg");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = currentPicRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                currentPicRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Uri downloadUrl = uri;
                        strgLink = downloadUrl.toString();
                        //Do what you want with the url
                    }
                });
            }
        });
        return strgLink;
    }

//    private void uploadPic(String path) {
//
//        Uri file = Uri.fromFile(new File("path/to/images/rivers.jpg"));
//        Uri s = Uri.
//        StorageReference riversRef = storage.child("images/rivers.jpg");
//
//        riversRef.putFile(file)
//                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                        // Get a URL to the uploaded content
////                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception exception) {
//                        // Handle unsuccessful uploads
//                        // ...
//                    }
//                });
//    }


    private void startDetecting() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(getApplicationContext(), "Permissions Granted", Toast.LENGTH_SHORT).show();

                cameraView.start();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1234);
            }
        }

        cameraView.addCameraKitListener(new CameraKitEventListener() {
//            public void o/**/

            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
//                Toast.makeText(getApplicationContext(), "Image!", Toast.LENGTH_SHORT).show();
                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
                processImage(bitmap);


//                }

//cameraView.captureImage();

            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {



//                cameraKitVideo.get

            }
        });

//cameraView.captureImage();
        Toast.makeText(getApplicationContext(), "DETECTING", Toast.LENGTH_SHORT).show();
        final Handler handler = new Handler();
        final int delay = 1500; //milliseconds

        handler.postDelayed(new Runnable() {
            public void run() {

//                cameraView.captureImage();
                takeImage();
                handler.postDelayed(this, delay);
            }
        }, delay);

    }
    private void capturePersonVideo(){

        cameraView.captureVideo();


    }

    private void processImage(Bitmap bitmap) {
        database = FirebaseDatabase.getInstance();
        detections = database.getReference("users/userID/Images");

        final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
        Toast.makeText(getApplicationContext(), results.get(0).getTitle(), Toast.LENGTH_LONG).show();
        Boolean hasPerson = false;

        for (Classifier.Recognition r : results) {
            if (r.getTitle().equals("person") && r.getConfidence() >= 0.5f) {
                hasPerson = true;
                break;
            }

        }
        if (hasPerson) {
            String pushKey = detections.push().getKey();
            String url = uploadFile(bitmap, pushKey);
            if (!url.equals("")) {
                detections.child(pushKey).setValue(new DetectedImage(new Date().toString(), url, IMEI));
            }

        }


    }

    private void takeImage() {
//        Toast.makeText(this, cameraView.isStarted()+"", Toast.LENGTH_SHORT).show();

        cameraView.captureImage();
    }

    private void stopStreaming() {
        streamingOrNot = false;
        session.disconnect();
        publisher.destroy();

    }

    private void startStreaming() {
        Toast.makeText(getApplicationContext(), "STREAMING", Toast.LENGTH_SHORT).show();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        streamingOrNot = true;

        session = new Session.Builder(this, API_KEY, SESSION_ID).build();
        session.setSessionListener(this);
        session.connect(TOKEN);

        BaseVideoCapturer.CaptureSettings cs = new BaseVideoCapturer.CaptureSettings();
//        cs.format = NfcV;
        publisher = new Publisher.Builder(this).build();
//        publisher.setPublisherVideoType(PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen);

        publisher.cycleCamera();
//    publisher.getRenderer().setStyle(BaseVideoRenderer.);
//
//        ScreensharingCapturer screenCapturer = new ScreensharingCapturer(MainActivity.this, null);
//        publisher.setCapturer(screenCapturer);
        publisher.setPublisherListener(this);
        publisher.setName(IMEI);

//        PublisherContainer.addView(publisher.getView());
        session.publish(publisher);

    }

    @AfterPermissionGranted(RC_SETTINGS)
    private void requestPermissions() {

        String[] perm = {Manifest.permission.CHANGE_NETWORK_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.INTERNET, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};

        if (EasyPermissions.hasPermissions(this, perm)) {
            Toast.makeText(getApplicationContext(), "(EasyPermissions.hasPermissions(this, perm))", Toast.LENGTH_SHORT).show();
            initialize();

        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            123);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                Toast.makeText(getApplicationContext(), "else", Toast.LENGTH_SHORT).show();
                initialize();
                // Permission has already been granted
            }


//            Toast.makeText(getApplicationContext(), "rejected", Toast.LENGTH_SHORT).show();
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
//        publisher.setPublisherVideoType(PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen);

        publisher.cycleCamera();
//    publisher.getRenderer().setStyle(BaseVideoRenderer.);
//
//        ScreensharingCapturer screenCapturer = new ScreensharingCapturer(MainActivity.this, null);
//        publisher.setCapturer(screenCapturer);
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


    @Override
    protected void onResume() {
        super.onResume();
//        cameraView.start();
//        cameraView.stop();

    }

    @Override
    protected void onPause() {
//        cameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
//        cameraView.stop();
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            MODEL_PATH,
                            LABEL_PATH,
                            INPUT_SIZE,
                            QUANT);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

}


