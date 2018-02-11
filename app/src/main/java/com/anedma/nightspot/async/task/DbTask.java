package com.anedma.nightspot.async.task;

import android.os.AsyncTask;
import android.util.Log;

import com.anedma.nightspot.async.response.AsyncResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 20/1/18.
 *
 */

public class DbTask extends AsyncTask<JSONObject, Void, JSONObject> {

    private AsyncResponse delegate = null;
    private static final String URL = "http://ec2-35-178-12-161.eu-west-2.compute.amazonaws.com/dbmanager.php";

    public DbTask(AsyncResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    protected JSONObject doInBackground(JSONObject[] jsonObjects) {
        Log.d("MYSQL", "Intentando realizar operacion en MySQL");
        if(jsonObjects[0] == null)
            return null;
        JSONObject json = jsonObjects[0];
        JSONObject jsonResponse = null;
        try {
            URL url = new URL(URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //Establecemos las propiedades de la llamada a la API
            conn.setReadTimeout(30000);
            conn.setConnectTimeout(30000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("charset", "utf-8");
            conn.addRequestProperty("Accept", "application/json");
            conn.addRequestProperty("Content-Type", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            //Creamos un flujo de salida
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            //Escribimos los parámetros POST en la llamada
            wr.write(json.toString());
            wr.flush();
            wr.close();
            //Log.d("MYSQL", "JSON: " + json.toString());
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String response;
                //Reading server response
                while ((response = br.readLine()) != null) {
                    sb.append(response);
                }
                jsonResponse = new JSONObject(sb.toString());
                //Log.d("MYSQL", "El servidor ha dicho: " + jsonResponse.toString());
            } else {
                Log.d("MYSQL", "El servidor ha respondido: " + responseCode);
            }
        } catch (java.io.IOException | JSONException e) {
            e.printStackTrace();
        }
        return jsonResponse;
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        delegate.processFinish(jsonObject);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}
