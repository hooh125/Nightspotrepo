package com.anedma.nightspot;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.anedma.nightspot.database.FingerprintDbHelper;
public class MainActivity extends AppCompatActivity {

    private Context context;
    private Activity activity;
    private FingerprintDbHelper dbHelper;
    private FingerprinterThread fingerprintThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        context = this.getApplicationContext();
        dbHelper = new FingerprintDbHelper(context);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = new Toolbar(this);
        setSupportActionBar(toolbar);

        Switch switch_fingerprint = (Switch) findViewById(R.id.switch_fingerprint);
        switch_fingerprint.setEnabled(true);
        fingerprintThread = new FingerprinterThread(this);
        switch_fingerprint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    if(!fingerprintThread.isAlive()) {
                        fingerprintThread = new FingerprinterThread(activity);
                    }
                    fingerprintThread.start();
                    buttonView.setText(R.string.status_started);
                } else if(fingerprintThread.isAlive()) {
                    fingerprintThread.interrupt();
                    buttonView.setText(R.string.status_stopped);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
