package aetherapps.com.smartstreamer;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Connect extends AppCompatActivity {

    String[] listItems;
    Button mbtn;
    TextView tv;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        mbtn = findViewById(R.id.btn1);
        tv = findViewById(R.id.tv);

        mbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listItems = new String[]{"Item 1", "Item 2", "Item 3", "Item 4"};
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(Connect.this);
                mBuilder.setTitle("Connect to Mount");
                mBuilder.setIcon(R.drawable.icon);
                mBuilder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tv.setText(listItems[which]);
                        dialog.dismiss();




                    }
                });

                AlertDialog mDialog = mBuilder.create();
                mDialog.show();
            }
        });
    }
}
