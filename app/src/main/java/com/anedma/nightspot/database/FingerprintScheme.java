package com.anedma.nightspot.database;

import android.provider.BaseColumns;

/**
 * Created by a-edu on 24/10/2017.
 */

public class FingerprintScheme implements BaseColumns {

    public static final String TABLE_NAME = "fingerprint";
    public static final String COLUMN_NAME_ARTIST = "artist";
    public static final String COLUMN_NAME_ALBUM = "album";
    public static final String COLUMN_NAME_SONG = "song";
    public static final String COLUMN_NAME_GENDER = "gender";

    public static final String SQL_DELETE_SENTENCE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    public static final String SQL_CREATE_SENTENCE = "CREATE TABLE " + TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY," +
            COLUMN_NAME_ARTIST + " TEXT, " +
            COLUMN_NAME_SONG + " TEXT, " +
            COLUMN_NAME_GENDER + " TEXT, " +
            COLUMN_NAME_ALBUM + " TEXT)";
}