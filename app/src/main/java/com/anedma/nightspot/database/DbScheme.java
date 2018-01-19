package com.anedma.nightspot.database;

import android.provider.BaseColumns;

/**
 * Created by a-edu on 24/10/2017.
 */

class DbScheme implements BaseColumns {

    static final String PRINT_TABLE_NAME = "fingerprint";
    static final String LIBRARY_TABLE_NAME = "library";
    static final String COLUMN_NAME_ARTIST = "artist";
    static final String COLUMN_NAME_ALBUM = "album";
    static final String COLUMN_NAME_SONG = "song";
    static final String COLUMN_NAME_GENDER = "gender";

    static final String PRINT_SQL_DELETE_SENTENCE = "DROP TABLE IF EXISTS " + PRINT_TABLE_NAME;
    static final String PRINT_SQL_CREATE_SENTENCE = "CREATE TABLE " + PRINT_TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY," +
            COLUMN_NAME_ARTIST + " TEXT, " +
            COLUMN_NAME_SONG + " TEXT, " +
            COLUMN_NAME_ALBUM + " TEXT)";

    static final String LIBRARY_SQL_DELETE_SENTENCE = "DROP TABLE IF EXISTS " + LIBRARY_TABLE_NAME;
    static final String LIBRARY_SQL_CREATE_SENTENCE = "CREATE TABLE " + LIBRARY_TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY," +
            COLUMN_NAME_ARTIST + " TEXT, " +
            COLUMN_NAME_SONG + " TEXT, " +
            COLUMN_NAME_ALBUM + " TEXT)";

}