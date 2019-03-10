package aetherapps.com.smartstreamer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class testActivity extends AppCompatActivity {
    static final String APP_ID = "961";
    static final String AUTH_KEY = "PBZxXW3WgGZtFZv";
    static final String AUTH_SECRET = "vvHjRbVFF6mmeyJ";
    static final String ACCOUNT_KEY = "961";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);


    }
//    protected void quickBloxInit(){
//        QBSettings.getInstance().init(getApplicationContext(), APP_ID, AUTH_KEY, AUTH_SECRET);
//        QBSettings.getInstance().setAccountKey(ACCOUNT_KEY);
//
//
////        QBSettings.getInstance().setStoringMehanism(StoringMechanism.UNSECURED); //call before init method for QBSettings
//        QBSettings.getInstance().init(getApplicationContext(), APP_ID, AUTH_KEY, AUTH_SECRET);
//        QBSettings.getInstance().setAccountKey(ACCOUNT_KEY);
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.30.30.1", 3128));
//        QBHttpConnectionConfig.setProxy(proxy);
//
//
//
//    }
}
