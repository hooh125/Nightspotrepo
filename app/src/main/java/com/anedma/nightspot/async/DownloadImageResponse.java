package com.anedma.nightspot.async;


import android.graphics.Bitmap;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 04/02/2018.
 */

public interface DownloadImageResponse {

    void downloadFinished(Bitmap bitmap);

}
