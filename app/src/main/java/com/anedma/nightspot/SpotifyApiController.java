package com.anedma.nightspot;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.PlaylistTracksInformation;
import kaaes.spotify.webapi.android.models.UserPrivate;
import kaaes.spotify.webapi.android.models.UserPublic;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by a-edu on 16/01/2018.
 */

public class SpotifyApiController {

    private static SpotifyApiController instance = null;
    private static SpotifyApi api;
    private static SpotifyService service;
    private static String accessToken;
    private static UserPrivate user = null;
    private static List<PlaylistSimple> myPlaylists = new ArrayList<>();
    private static List<PlaylistTrack> myPlaylistTracks = new ArrayList<>();
    private static int pendingCalls = 0;

    private SpotifyApiController() {
        api = new SpotifyApi();
        if (!accessToken.isEmpty()) {
            api.setAccessToken(accessToken);
        }
        service = api.getService();
        getUserData();
    }

    public static SpotifyApiController getInstance() {
        if (instance == null) {
            instance = new SpotifyApiController();
        }
        return instance;
    }

    public static void setAccessToken(String _accessToken) {
        accessToken = _accessToken;
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
                }
            }

            @Override
            public void failure(SpotifyError error) {
                Log.d("SPOTIFYAPI", "Error al obtener las canciones de la playlist " + playlist.name + " código de error: " + error.getErrorDetails().status);
                Log.d("SPOTIFYAPI", "PlaylistID: " + playlist.id + " UserID: " + user.id);
            }
        });
    }
}
