package com.anedma.nightspot;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.anedma.nightspot.database.FingerprintDbHelper;
import com.gracenote.gnsdk.GnException;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private Activity activity;
    private FingerprintDbHelper dbHelper;
    private GracenoteApiController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        context = this.getApplicationContext();
        dbHelper = new FingerprintDbHelper(context);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = new Toolbar(this);
        setSupportActionBar(toolbar);

        try {
            controller = GracenoteApiController.getInstance(getApplicationContext(), activity);
        } catch (GnException e) {
            e.printStackTrace();
        }

        TextView tv_songName = (TextView) findViewById(R.id.tv_songName);
        Button botonPrueba = (Button) findViewById(R.id.button_prueba);
        botonPrueba.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("DEBUG", "El botón se ha presionado correctamente");
                controller.startIdentify();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        controller.startAudioProcessing();
    }

    @Override
    protected void onPause() {
        super.onPause();
        controller.stopAudioProcessing();
    }
}
