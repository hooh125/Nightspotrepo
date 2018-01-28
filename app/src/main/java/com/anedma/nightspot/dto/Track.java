package com.anedma.nightspot.dto;

import java.net.URL;

/**
 * Created by a-edu on 24/10/2017.
 */

public class Track {

    private String artist;
    private String song;
    private String album;
    private URL albumImageUrl;

    public Track(String artist, String song, String album, URL albumImageUrl) {
        this.artist = artist;
        this.song = song;
        this.album = album;
        this.albumImageUrl = albumImageUrl;
    }

    public Track(String artist, String song, String album) {
        this.artist = artist;
        this.song = song;
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public String getSong() {
        return song;
    }

    public String getAlbum() {
        return album;
    }

    public URL getAlbumImageUrl() {
        return albumImageUrl;
    }
}
