package com.anedma.nightspot.async;

import android.content.Context;
import android.util.Log;

import com.anedma.nightspot.async.response.GracenoteResponse;
import com.gracenote.gnsdk.GnException;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 24/10/2017.
 *
 */

public class FingerprinterThread extends Thread {

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
        if(controller == null) {
            try {
                controller = GracenoteApiController.getInstance(context, delegate);
            } catch (GnException e) {
                e.printStackTrace();
            }
        }
        controller.startAudioProcessing();
        do {
            if(!controller.isProcessing()) {
                Log.d("Fingerprint", "Identificando...");
                controller.startIdentify();
            }
            try {
                sleep(10000);
            } catch (InterruptedException e) {
                break;
            }
        } while(!isInterrupted());
        controller.killInstance();
    }

}
