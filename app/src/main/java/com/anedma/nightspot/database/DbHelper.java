package com.anedma.nightspot.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.anedma.nightspot.dto.Fingerprint;
import com.anedma.nightspot.exception.SQLiteInsertException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by a-edu on 24/10/2017.
 */

public class DbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1; //Cambiar este número si se cambia el esquema de la base de datos
    private static final String DATABASE_NAME = "fingerprint_db";
    private static final String URL = "https://nightspot.000webhostapp.com/dbmanager.php";


    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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

    public void insert(Fingerprint fp) throws SQLiteInsertException {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbScheme.COLUMN_NAME_ARTIST, fp.getArtist());
        values.put(DbScheme.COLUMN_NAME_SONG, fp.getSong());
        values.put(DbScheme.COLUMN_NAME_GENDER, fp.getGenre());
        values.put(DbScheme.COLUMN_NAME_ALBUM, fp.getAlbum());
        long result = db.insert(DbScheme.PRINT_TABLE_NAME, null, values);
        if (result > 0) {
            Log.d("DB", "Se ha insertado un fingerprint en la BD correctamente -> " + result);
            Log.d("DB", "Artista -> " + fp.getArtist());
            Log.d("DB", "Cancion -> " + fp.getSong());
            Log.d("DB", "Genero -> " + fp.getGenre());
            Log.d("DB", "Album -> " + fp.getAlbum());
        } else {
            throw new SQLiteInsertException();
        }
        //Ahora intentamos insertar el fingerprint en la base de datos MySQL
        insertPrintOnline(fp);
    }

    public void insert(Track ts) throws SQLiteInsertException {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        StringBuilder artists = new StringBuilder();
        for(ArtistSimple artist : ts.artists) {
            if(!artists.toString().contains(artist.name)) {
                if(!artists.toString().isEmpty()) {
                    artists.append(", ").append(artist.name);
                } else {
                    artists.append(artist.name);
                }
            }
        }
        values.put(DbScheme.COLUMN_NAME_ARTIST, artists.toString());
        values.put(DbScheme.COLUMN_NAME_SONG, ts.name);
        values.put(DbScheme.COLUMN_NAME_ALBUM, ts.album.name);
        long result = db.insert(DbScheme.LIBRARY_TABLE_NAME, null, values);
        if (result > 0) {
            Log.d("DB", "Inserción local correcta -> " + result + " - Canción: " + ts.name + " - Artista: " + artists.toString() + " - Album: " + ts.album.name);
        } else {
            throw new SQLiteInsertException();
        }
    }

    private void insertPrintOnline(Fingerprint fp) {
        try {
            URL url = new URL(URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //Establecemos las propiedades de la llamada a la API
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            //Creamos un flujo de salida
            OutputStream os = conn.getOutputStream();
            //Escribimos los parámetros POST en la llamada
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write("print=newprint&");
            writer.write("artist=" + fp.getArtist()+ "&");
            writer.write("song=" + fp.getSong()+ "&");
            writer.write("album=" + fp.getAlbum()+ "&");
            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String response;
                //Reading server response
                while ((response = br.readLine()) != null) {
                    sb.append(response);
                }
                Log.d("MYSQL", "El servidor ha dicho: " + sb.toString());
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
