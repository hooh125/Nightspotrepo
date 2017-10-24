package com.anedma.nightspot.dto;

/**
 * Created by a-edu on 24/10/2017.
 */

public class Fingerprint {

    private String artist;
    private String song;
    private String genre;
    private String album;

    public Fingerprint(String artist, String song, String genre, String album) {
        this.artist = artist;
        this.song = song;
        this.genre = genre;
        this.album = album;
    }

    public Fingerprint(String artist, String song, String genre) {
        this.artist = artist;
        this.song = song;
        this.genre = genre;
        this.album = null;
    }

    public String getArtist() {
        return artist;
    }

    public String getSong() {
        return song;
    }

    public String getGenre() {
        return genre;
    }

    public String getAlbum() {
        return album;
    }
}
