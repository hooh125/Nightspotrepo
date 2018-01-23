package com.anedma.nightspot;

import android.content.Context;
import android.util.Log;

import com.anedma.nightspot.activities.LoginActivity;
import com.anedma.nightspot.database.DbHelper;
import com.anedma.nightspot.exception.SQLiteInsertException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.PlaylistTracksInformation;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class SpotifyApiController {

    private static SpotifyService service;
    private static UserPrivate user = null;
    private static List<PlaylistSimple> myPlaylists = new ArrayList<>();
    private static List<PlaylistTrack> myPlaylistTracks = new ArrayList<>();
    private static int pendingCalls = 0;
    private Context context;

    public SpotifyApiController(String accessToken) {
        SpotifyApi api = new SpotifyApi();
        if (!accessToken.isEmpty()) {
            api.setAccessToken(accessToken);
        }
        service = api.getService();
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void getUserData() {
        getUser();
        getPlaylists();
    }

    private void getUser() {
        service.getMe(new Callback<UserPrivate>() {
            @Override
            public void success(UserPrivate userPrivate, Response response) {
                user = userPrivate;
                Log.d("SPOTIFYAPI", "Usuario: " + user.display_name + " - email: " + user.email);
                if (myPlaylists.size() > 0 && user != null && myPlaylistTracks.size() == 0)
                    getPlaylistsTracks();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SPOTIFYAPI", "Error al obtener el usuario");
            }
        });
    }

    private void getPlaylists() {
        service.getMyPlaylists(new Callback<Pager<PlaylistSimple>>() {
            @Override
            public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                myPlaylists = playlistSimplePager.items;
                Log.d("SPOTIFYAPI", "Encontradas " + myPlaylists.size() + " playlists");
                if (myPlaylists.size() > 0 && user != null && myPlaylistTracks.size() == 0)
                    getPlaylistsTracks();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SPOTIFYAPI", "Error al obtener las playlists");
            }
        });
    }

    private void getPlaylistsTracks() {
        for (final PlaylistSimple playlist : myPlaylists) {
            PlaylistTracksInformation info = playlist.tracks;
            Log.d("SPOTIFYAPI", "La lista " + playlist.name + " contiene " + info.total + " canciones");
            if (info.total < 100) {
                getPlaylistTracks(playlist, 0);
            } else {
                for (int i = 0; i <= (info.total / 100); i++) {
                    getPlaylistTracks(playlist, i * 100);
                }
            }


        }
    }

    private void getPlaylistTracks(final PlaylistSimple playlist, final int indice) {
        Map<String, Object> options = new HashMap<>();
        options.put("offset", indice);
        pendingCalls++;
        service.getPlaylistTracks(playlist.owner.id, playlist.id, options, new SpotifyCallback<Pager<PlaylistTrack>>() {
            @Override
            public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                myPlaylistTracks.addAll(playlistTrackPager.items);
                pendingCalls--;
                Log.d("SPOTIFYAPI", "Pendientes: " + pendingCalls + " - Añadiendo " + playlistTrackPager.items.size() + " canciones de la lista " + playlist.name + " ID: " + playlist.id);
                if (pendingCalls == 0) {
                    Log.d("SPOTIFYAPI", "La actualización se ha completado");
                    insertTracksIntoDB();
                }
            }

            @Override
            public void failure(SpotifyError error) {
                Log.d("SPOTIFYAPI", "Error al obtener las canciones de la playlist " + playlist.name + " código de error: " + error.getErrorDetails().status);
                Log.d("SPOTIFYAPI", "PlaylistID: " + playlist.id + " UserID: " + user.id);
            }
        });
    }

    private void insertTracksIntoDB() {
        if(context != null) {
            DbHelper dbHelper = new DbHelper(context);
            LoginActivity loginActivity = (LoginActivity) context;
            JSONArray jsonTracks = new JSONArray();
            try {
                for(PlaylistTrack plTrack : myPlaylistTracks) {
                    JSONObject track = new JSONObject();
                    try {
                        dbHelper.insert(plTrack.track);
                        track.put("artist", removeSpecialCharacters(getArtists(plTrack.track.artists)));
                        track.put("song", removeSpecialCharacters(plTrack.track.name));
                        track.put("album", removeSpecialCharacters(plTrack.track.album.name));
                        jsonTracks.put(track);
                        if(jsonTracks.length() == 10) {
                            insertUserTracksOnline(dbHelper, loginActivity.account.getEmail(), jsonTracks);
                            jsonTracks = new JSONArray();
                        }
                    } catch (SQLiteInsertException e) {
                        e.printStackTrace();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("DB", "Error al intentar insertar las canciones en la BBDD local, contexto no inicializado");
        }
    }

    private void insertUserTracksOnline(DbHelper dbHelper, String email, JSONArray tracks) {
        JSONObject json = new JSONObject();
        try {
            json.put("operation", "insertUserTracks");
            json.put("email", email);
            json.put("tracks", tracks);
            Log.d("SPOTIFYAPI", "Intentando enviar JSON a MySQL para su guardado");
            Log.d("SPOTIFYAPI", json.toString());
            dbHelper.mySqlRequest(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public static String getArtists(List<ArtistSimple> ts) {
        StringBuilder artists = new StringBuilder();
        for(ArtistSimple artist : ts) {
            if(!artists.toString().contains(artist.name)) {
                if(!artists.toString().isEmpty()) {
                    artists.append(", ").append(artist.name);
                } else {
                    artists.append(artist.name);
                }
            }
        }
        return artists.toString();
    }

    private String removeSpecialCharacters(String string) {
        return string.replaceAll("[^a-zA-Z0-9\\s]+","");
    }
}
