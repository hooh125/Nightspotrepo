package com.anedma.nightspot.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.anedma.nightspot.dto.Fingerprint;
import com.anedma.nightspot.exception.FingerprintInsertException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by a-edu on 24/10/2017.
 */

public class FingerprintDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1; //Cambiar este número si se cambia el esquema de la base de datos
    public static final String DATABASE_NAME = "fingerprint_db";
    public static final String URL = "https://nightspot.000webhostapp.com/dbmanager.php";


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
        //Ahora intentamos insertar el fingerprint en la base de datos MySQL
        insertPrintOnline(fp);
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
            writer.write("genre=" + fp.getGenre()+ "&");
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
