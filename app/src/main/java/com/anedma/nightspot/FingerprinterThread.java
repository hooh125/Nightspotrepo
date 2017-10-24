package com.anedma.nightspot;

import android.app.Activity;
import android.util.Log;

import com.gracenote.gnsdk.GnException;

/**
 * Created by a-edu on 24/10/2017.
 */

public class FingerprinterThread extends Thread {

    private boolean status = true;
    private Activity activity;
    private GracenoteApiController controller;

    public FingerprinterThread(Activity activity) {
        this.activity = activity;
        try {
            controller = GracenoteApiController.getInstance(activity.getApplicationContext(), activity);
        } catch (GnException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();
        controller.startAudioProcessing();
        while(status) {
            if(!controller.isProcessing()) {
                Log.d("Fingerprint", "Identificando...");
                controller.startIdentify();
            }
            try {
                sleep(10000);
            } catch (InterruptedException e) {
                controller.stopAudioProcessing();
                break;
            }
        }
    }
}
