package com.anedma.nightspot.async;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

/**
 * Created by andreseduardomataperez on 20/1/18.
 */

public class DbTask extends AsyncTask<HashMap<String, String>, Void, String> {

    private static final String URL = "http://ec2-52-56-196-109.eu-west-2.compute.amazonaws.com/dbmanager.php";

    @Override
    protected String doInBackground(HashMap<String, String>[] hash) {
        Log.d("MYSQL", "Intentando insertar usuario en MySQL");
        try {
            URL url = new URL(URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //Establecemos las propiedades de la llamada a la API
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("charset", "utf-8");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            //Creamos un flujo de salida
            OutputStream os = conn.getOutputStream();
            //Escribimos los parámetros POST en la llamada
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            HashMap<String, String> map = hash[0];
            for (String key : map.keySet()) {
                writer.write(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(map.get(key), "UTF-8") + "&");
            }
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
            } else {
                Log.d("MYSQL", "El servidor ha respondido: " + responseCode);
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
