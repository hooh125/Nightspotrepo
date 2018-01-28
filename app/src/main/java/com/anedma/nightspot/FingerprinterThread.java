package com.anedma.nightspot;

import android.content.Context;
import android.util.Log;

import com.anedma.nightspot.activities.PrintTracksActivity;
import com.anedma.nightspot.async.GracenoteResponse;
import com.anedma.nightspot.dto.Track;
import com.gracenote.gnsdk.GnException;

/**
 * Created by a-edu on 24/10/2017.
 */

public class FingerprinterThread extends Thread {

    private boolean status = true;
    private Context context;
    private GracenoteResponse delegate;
    private GracenoteApiController controller;

    public FingerprinterThread(Context context, GracenoteResponse delegate) {
        this.context = context;
        this.delegate = delegate;
        try {
            controller = GracenoteApiController.getInstance(context, delegate);
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
