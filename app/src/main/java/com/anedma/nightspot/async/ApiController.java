package com.anedma.nightspot.async;

import android.util.Log;

import com.anedma.nightspot.async.response.ApiResponse;
import com.anedma.nightspot.async.response.AsyncResponse;
import com.anedma.nightspot.async.response.LoginResponse;
import com.anedma.nightspot.async.response.PubPrintTracksResponse;
import com.anedma.nightspot.async.response.PubRegResponse;
import com.anedma.nightspot.async.response.PubResponse;
import com.anedma.nightspot.async.response.SpotifyResponse;
import com.anedma.nightspot.async.response.UserResponse;
import com.anedma.nightspot.async.task.DbTask;
import com.anedma.nightspot.dto.Pub;
import com.anedma.nightspot.dto.Track;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 04/02/2018.
 */

public class ApiController implements AsyncResponse {

    private static ApiController instance = null;
    private static final String LOG_TAG = "APICONTROLLER";
    private ApiResponse delegate = null;

    private ApiController() {

    }

    public static ApiController getInstance() {
        if (instance == null) {
            instance = new ApiController();
        }
        return instance;
    }

    public void setDelegate(ApiResponse delegate) {
        this.delegate = delegate;
    }



    //OPERACIONES

    public void requestLogin(String name, String lastName, String email) {
        JSONObject json = new JSONObject();
        try {
            json.put("operation", "insertUser");
            json.put("name", name);
            json.put("lastName", lastName);
            json.put("email", email);
            requestOperation(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void requestInsertUserTracks(boolean requestUpdate, String email, JSONArray tracks) {
        JSONArray tracksPackage = new JSONArray();
        int tracksLeft = tracks.length() - 1;
        int totalTracks = tracksLeft;
        for (int i = 1; i < tracks.length(); i++) {
            try {
                tracksPackage.put(tracks.get(i - 1));
                tracksLeft--;
                if (i % 300 == 0) {
                    insertUserTracksPackage(requestUpdate, email, tracksPackage, tracksLeft, totalTracks);
                    if (requestUpdate) requestUpdate = false;
                    tracksPackage = new JSONArray();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (tracksPackage.length() > 0) {
            insertUserTracksPackage(requestUpdate, email, tracksPackage, tracksLeft, totalTracks);
        }


    }

    public void requestInsertPub(Pub pub, String email) {
        JSONObject json = new JSONObject();
        try {
            json.put("operation", "insertPub");
            if(pub.getId() != 0)
                json.put("id", pub.getId());
            json.put("name", pub.getName());
            json.put("description", pub.getDescription());
            json.put("phone", pub.getPhone());
            json.put("lat", pub.getLatLng().latitude);
            json.put("lng", pub.getLatLng().longitude);
            json.put("address", pub.getAddress());
            json.put("email", email);
            requestOperation(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void requestInsertPubTracks(List<Track> tracks, String email) {
        JSONObject json = new JSONObject();
        JSONArray jsonTracks = new JSONArray();
        try {
            for (Track track : tracks) {
                JSONObject jsonTrack = new JSONObject();
                jsonTrack.put("song", track.getSong());
                jsonTrack.put("artist", track.getArtist());
                jsonTrack.put("album", track.getAlbum());
                jsonTracks.put(jsonTrack);
            }
            json.put("operation", "insertPubTracks");
            json.put("email", email);
            json.put("tracks", jsonTracks);
            requestOperation(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void requestUser(String email) {
        JSONObject json = new JSONObject();
        try {
            json.put("operation", "getUser");
            json.put("email", email);
            requestOperation(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void requestUserPubs(String email) {
        JSONObject json = new JSONObject();
        try {
            json.put("operation", "getUserPubs");
            json.put("email", email);
            requestOperation(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void requestUserPub(String email) {
        JSONObject json = new JSONObject();
        try {
            json.put("operation", "getUserPub");
            json.put("email", email);
            requestOperation(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void requestDeleteUser(String email) {
        JSONObject json = new JSONObject();
        try {
            json.put("operation", "deleteUser");
            json.put("email", email);
            requestOperation(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void requestCalculateUserAffinity(String email) {
        JSONObject json = new JSONObject();
        try {
            json.put("operation", "calculateUserAffinity");
            json.put("email", email);
            requestOperation(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void requestNotifyPlaylistsCompiled() {
        SpotifyResponse spotifyResponse = (SpotifyResponse) delegate;
        spotifyResponse.requestUserPlaylistsCompleted();
    }

    //RESULTADOS

    private void requestOperation(JSONObject jsonObject) {
        Log.d(LOG_TAG, jsonObject.toString());
        DbTask dbTask = new DbTask(this);
        dbTask.execute(jsonObject);
    }

    @Override
    public void processFinish(JSONObject jsonObject) {
        try {
            boolean error = jsonObject.getBoolean("error");
            String message = jsonObject.getString("message");
            String operation = jsonObject.getString("operation");
            if (!error) {
                switch (operation) {
                    case "insertUser":
                        insertUserResponse(jsonObject);
                        break;
                    case "insertUserTracks":
                        insertUserTracksResponse(jsonObject);
                        break;
                    case "insertPub":
                        insertPubResponse(jsonObject);
                        break;
                    case "insertPubTracks":
                        insertPubTracksResponse(jsonObject);
                        break;
                    case "deleteUser":
                        deleteUserResponse(jsonObject);
                        break;
                    case "getUser":
                        getUserResponse(jsonObject);
                        break;
                    case "getUserPubs":
                        getUserPubsResponse(jsonObject);
                        break;
                    case "getUserPub":
                        getUserPubResponse(jsonObject);
                        break;
                    case "calculateUserAffinity":
                        calculateUserAffinityResponse(jsonObject);
                        break;
                }
            } else {
                Log.d(LOG_TAG, "El servidor ha devuelto un error con mensaje " + message);
            }
        } catch (JSONException e) {
            Log.d(LOG_TAG, "El servidor no ha devuelto respuesta");
        }
    }

    //RESPUESTAS


    private void calculateUserAffinityResponse(JSONObject jsonObject) {
        UserResponse userResponse = (UserResponse) delegate;
        userResponse.calculateAffinityDone();
    }

    private void insertUserResponse(JSONObject jsonObject) throws JSONException {
        LoginResponse loginDelegate = (LoginResponse) delegate;
        boolean isPub = jsonObject.getBoolean("isPub");
        boolean alreadyRegistered = jsonObject.getBoolean("alreadyRegistered");
        loginDelegate.loginResponse(alreadyRegistered, isPub);
    }

    private void insertUserTracksResponse(JSONObject jsonObject) throws JSONException {
        SpotifyResponse spotifyResponse = (SpotifyResponse) delegate;
        int tracksLeft = jsonObject.getInt("tracksLeft");
        int totalTracks = jsonObject.getInt("totalTracks");
        int tracksRecorded = jsonObject.getInt("tracksRecorded");
        Log.d(LOG_TAG, "El servidor ha insertado:" + tracksRecorded + " canciones y faltan " + tracksLeft + " por insertar");
        spotifyResponse.insertTracksProgressUpdate(totalTracks, tracksLeft);
        if (tracksLeft == 0) spotifyResponse.requestInsertTracksResponse();
    }

    private void insertPubResponse(JSONObject jsonObject) {
        PubRegResponse pubRegDelegate = (PubRegResponse) delegate;
        pubRegDelegate.pubRegResponse();
    }

    private void insertPubTracksResponse(JSONObject jsonObject) {
        PubPrintTracksResponse pubPrintTracksDelegate = (PubPrintTracksResponse) delegate;
        pubPrintTracksDelegate.printTracksResponse();
    }

    private void deleteUserResponse(JSONObject jsonObject) {
        UserResponse userResponse = (UserResponse) delegate;
        userResponse.deleteUserResponse();
    }

    private void getUserResponse(JSONObject jsonObject) throws JSONException {
        JSONObject user = jsonObject.getJSONObject("user");
        int id = Integer.parseInt(user.getString("id"));
        String name = user.getString("name");
        String lastName = user.getString("last_name");
        boolean isPub = user.getInt("isPub") != 0;
        int tracks = Integer.parseInt(user.getString("tracks"));
        UserResponse userDelegate = (UserResponse) delegate;
        userDelegate.userResponse(id, name, lastName, isPub, tracks);
    }

    private void getUserPubsResponse(JSONObject jsonObject) throws JSONException {
        JSONArray pubs = jsonObject.getJSONArray("pubs");
        UserResponse userDelegate = (UserResponse) delegate;
        HashMap<Integer, Pub> pubList = new HashMap<>();
        for (int i = 0; i < pubs.length(); i++) {
            JSONObject jsonPub = pubs.getJSONObject(i);
            int idPub = jsonPub.getInt("id_pub");
            String name = jsonPub.getString("name");
            String description = jsonPub.getString("description");
            String address = jsonPub.getString("address");
            String phone = jsonPub.getString("phone");
            String lat = jsonPub.getString("lat");
            String lng = jsonPub.getString("lng");
            String affinity = jsonPub.getString("affinity");
            int tracks = jsonPub.getInt("tracks");
            LatLng position = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            pubList.put(idPub, new Pub(idPub, name, description, address, position, phone, affinity, tracks));
        }
        userDelegate.userPubsResponse(pubList);
    }

    private void getUserPubResponse(JSONObject jsonObject) throws JSONException {
        JSONObject jsonPub = jsonObject.getJSONObject("pub");
        int pubId = Integer.parseInt(jsonPub.getString("id"));
        String name = jsonPub.getString("name");
        String description = jsonPub.getString("description");
        String phone = jsonPub.getString("phone");
        String lat = jsonPub.getString("lat");
        String lng = jsonPub.getString("lng");
        String address = jsonPub.getString("address");
        LatLng position = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
        int tracks = Integer.parseInt(jsonPub.getString("tracks"));
        Pub pub = new Pub(pubId, name, description, address, position, phone, tracks);
        Log.d(LOG_TAG, "El pub se ha cargado correctamente, intentando rellenar controles");
        PubResponse pubResponse = (PubResponse) delegate;
        pubResponse.pubResponse(pub);
    }

    //MÉTODOS INTERNOS

    private void insertUserTracksPackage(boolean requestUpdate, String email, JSONArray tracks, int tracksLeft, int totalTracks) {
        JSONObject json = new JSONObject();
        try {
            json.put("operation", "insertUserTracks");
            json.put("requestUpdate", requestUpdate);
            json.put("tracksLeft", tracksLeft);
            json.put("totalTracks", totalTracks);
            json.put("email", email);
            json.put("tracks", tracks);
            requestOperation(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
