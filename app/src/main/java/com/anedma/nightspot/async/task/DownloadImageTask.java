package com.anedma.nightspot.async.task;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.anedma.nightspot.async.response.DownloadImageResponse;

import java.io.InputStream;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 04/02/2018.
 */

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

    private DownloadImageResponse delegate;

    public DownloadImageTask(DownloadImageResponse delegate) {
        this.delegate = delegate;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        delegate.downloadFinished(result);
    }
}
