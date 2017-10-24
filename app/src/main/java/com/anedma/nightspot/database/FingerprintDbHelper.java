package com.anedma.nightspot.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.anedma.nightspot.dto.Fingerprint;
import com.anedma.nightspot.exception.FingerprintInsertException;

/**
 * Created by a-edu on 24/10/2017.
 */

public class FingerprintDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1; //Cambiar este número si se cambia el esquema de la base de datos
    public static final String DATABASE_NAME = "fingerprint_db";

    public FingerprintDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(FingerprintScheme.SQL_CREATE_SENTENCE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(FingerprintScheme.SQL_DELETE_SENTENCE);
        onCreate(db);
    }

    public void insertFingerprint(Fingerprint fp) throws FingerprintInsertException {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FingerprintScheme.COLUMN_NAME_ARTIST, fp.getArtist());
        values.put(FingerprintScheme.COLUMN_NAME_SONG, fp.getSong());
        values.put(FingerprintScheme.COLUMN_NAME_GENDER, fp.getGenre());
        values.put(FingerprintScheme.COLUMN_NAME_ALBUM, fp.getAlbum());
        long result = db.insert(FingerprintScheme.TABLE_NAME, null, values);
        if (result > 0) {
            Log.d("DB", "Se ha insertado un fingerprint en la BD correctamente -> " + result);
            Log.d("DB", "Artista -> " + fp.getArtist());
            Log.d("DB", "Cancion -> " + fp.getSong());
            Log.d("DB", "Genero -> " + fp.getGenre());
            Log.d("DB", "Album -> " + fp.getAlbum());
        } else {
            throw new FingerprintInsertException();
        }
    }
}
