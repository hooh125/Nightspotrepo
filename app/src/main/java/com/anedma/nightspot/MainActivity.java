package com.anedma.nightspot;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.gracenote.gnsdk.GnException;

public class MainActivity extends Activity {

    private Context context;
    private Activity activity;
    private final String CLIENT_ID = "933788263";
    private final String CLIENT_TAG = "A8B4ECE6DAE23A832C82CB041EAE6EBF";
    private String gnsdkLogFilename = "apiLog.txt";
    private GracenoteApiController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        context = this.getApplicationContext();
        setContentView(R.layout.activity_main);

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
        System.out.println("Llamada a onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("Llamada a onPause");
        controller.stopAudioProcessing();
    }
}
