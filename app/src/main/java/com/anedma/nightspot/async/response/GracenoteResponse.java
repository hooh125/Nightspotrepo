package com.anedma.nightspot.async.response;

import com.anedma.nightspot.dto.Track;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 28/01/2018.
 */

public interface GracenoteResponse {

    void trackReturned(Track track);
    void processingFinished();
    void audioProcessStopped();
    void audioProcessStarted();

}
