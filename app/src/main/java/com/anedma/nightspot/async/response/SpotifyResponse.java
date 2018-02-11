package com.anedma.nightspot.async.response;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 10/02/2018.
 */

public interface SpotifyResponse extends ApiResponse {

    void requestInsertTracksResponse();
    void insertTracksProgressUpdate(int totalTracks, int tracksRemaining);
}
