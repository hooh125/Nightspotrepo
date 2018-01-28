package com.anedma.nightspot.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.anedma.nightspot.SpotifyApiController;
import com.anedma.nightspot.activities.LoginActivity;
import com.anedma.nightspot.async.DbTask;
import com.anedma.nightspot.exception.SQLiteInsertException;

import org.json.JSONObject;

import kaaes.spotify.webapi.android.models.Track;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 24/10/2017.
 *
 */

public class DbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3; //Cambiar este número si se cambia el esquema de la base de datos
    private static final String DATABASE_NAME = "fingerprint_db";
    private Context context = null;


    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DbScheme.PRINT_SQL_CREATE_SENTENCE);
        db.execSQL(DbScheme.LIBRARY_SQL_CREATE_SENTENCE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DbScheme.PRINT_SQL_DELETE_SENTENCE);
        db.execSQL(DbScheme.LIBRARY_SQL_DELETE_SENTENCE);
        onCreate(db);
    }

    /*public void insert(Track track) throws SQLiteInsertException {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbScheme.COLUMN_NAME_ARTIST, track.getArtist());
        values.put(DbScheme.COLUMN_NAME_SONG, track.getSong());
        values.put(DbScheme.COLUMN_NAME_GENDER, track.getGenre());
        values.put(DbScheme.COLUMN_NAME_ALBUM, track.getAlbum());
        long result = db.insert(DbScheme.PRINT_TABLE_NAME, null, values);
        if (result > 0) {
            Log.d("DBHELPER", "Se ha insertado un fingerprint en la BD correctamente -> " + result);
            Log.d("DBHELPER", "Artista -> " + track.getArtist());
            Log.d("DBHELPER", "Cancion -> " + track.getSong());
            Log.d("DBHELPER", "Genero -> " + track.getGenre());
            Log.d("DBHELPER", "Album -> " + track.getAlbum());
        } else {
            throw new SQLiteInsertException();
        }
        //Ahora intentamos insertar el fingerprint en la base de datos MySQL
        //insertPrintOnline(fp);
    }*/

    public void insert(Track ts) throws SQLiteInsertException {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbScheme.COLUMN_NAME_ARTIST, SpotifyApiController.getArtists(ts.artists));
        values.put(DbScheme.COLUMN_NAME_SONG, ts.name);
        values.put(DbScheme.COLUMN_NAME_ALBUM, ts.album.name);
        long result = db.insert(DbScheme.LIBRARY_TABLE_NAME, null, values);
        if (result <= 0) {
            throw new SQLiteInsertException();
        } else {
            //Log.d("DBHELPER", "Inserción local correcta -> " + result + " - Canción: " + ts.name + " - Artista: " + SpotifyApiController.getArtists(ts.artists) + " - Album: " + ts.album.name);
        }
    }

    public void mySqlRequest(JSONObject jsonObject) {
        DbTask dbTask = new DbTask((LoginActivity) context);
        dbTask.execute(jsonObject);
    }

}
