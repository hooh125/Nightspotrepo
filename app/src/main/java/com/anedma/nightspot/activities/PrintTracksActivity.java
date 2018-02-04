package com.anedma.nightspot.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.anedma.nightspot.DownloadImageTask;
import com.anedma.nightspot.FingerprinterThread;
import com.anedma.nightspot.R;
import com.anedma.nightspot.async.AsyncResponse;
import com.anedma.nightspot.async.DbTask;
import com.anedma.nightspot.async.DownloadImageResponse;
import com.anedma.nightspot.async.GracenoteResponse;
import com.anedma.nightspot.dto.Track;
import com.anedma.nightspot.dto.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PrintTracksActivity extends AppCompatActivity implements View.OnClickListener, GracenoteResponse, AsyncResponse {

    private static final String LOG_TAG = "PRINTTRACKSACTIVITY";
    private List<Track> tracks = new ArrayList<>();
    private FingerprinterThread fingerprintThread;
    private TrackAdapter adapter;
    private ListView lvTracks;
    private Button buttonSendTracks;
    private GracenoteResponse delegate;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_tracks);
        delegate = this;
        context = this;

        buttonSendTracks = findViewById(R.id.button_send_pub_tracks);
        buttonSendTracks.setOnClickListener(this);
        lvTracks = findViewById(R.id.lv_tracks);
        adapter = new TrackAdapter(tracks, this);
        lvTracks.setAdapter(adapter);


        Switch switch_fingerprint = findViewById(R.id.sw_start_prints);
        switch_fingerprint.setEnabled(true);
        fingerprintThread = new FingerprinterThread(context, delegate);
        switch_fingerprint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!fingerprintThread.isAlive()) {
                        fingerprintThread = new FingerprinterThread(context, delegate);
                    }
                    fingerprintThread.start();
                    buttonView.setText(R.string.status_started);
                } else if (fingerprintThread.isAlive()) {
                    fingerprintThread.interrupt();
                    buttonView.setText(R.string.status_stopped);
                }
            }
        });

    }

    @Override
    public void trackReturned(Track track) {
        Log.d(LOG_TAG, "Canción encontrada: " + track.getSong());
        if (!checkIfAlreadyExist(tracks, track)) {
            tracks.add(track);
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    private boolean checkIfAlreadyExist(List<Track> tracks, Track track) {
        for (Track t : tracks) {
            if (t.getSong().equals(track.getSong()) && t.getAlbum().equals(track.getAlbum()) && t.getArtist().equals(track.getArtist())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        User user = User.getInstance();
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
            json.put("email", user.getEmail());
            json.put("tracks", jsonTracks);
            DbTask task = new DbTask(this);
            task.execute(json);
            Log.d(LOG_TAG, json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void processFinish(JSONObject jsonObject) {
        try {
            boolean error = jsonObject.getBoolean("error");
            if(!error) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Log.d(LOG_TAG, "Ha ocurrido un error al insertar los tracks del pub");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class TrackAdapter extends BaseAdapter {

        private List<Track> tracks;
        private Context mContext;

        private class ViewHolder {
            TextView tvSong;
            TextView tvArtist;
            ImageView ivAlbumImg;

            private ViewHolder(View v) {
                this.tvSong = v.findViewById(R.id.tv_song);
                this.tvArtist = v.findViewById(R.id.tv_artist);
                this.ivAlbumImg = v.findViewById(R.id.iv_album);
            }
        }

        public TrackAdapter(List<Track> tracks, Context context) {
            this.tracks = tracks;
            this.mContext = context;
        }

        @Override
        public int getCount() {
            return tracks.size();
        }

        @Override
        public Object getItem(int position) {
            return tracks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final ViewHolder viewHolder;

            if (convertView == null) {

                LayoutInflater layoutInflater = LayoutInflater.from(mContext);
                convertView = layoutInflater.inflate(R.layout.track_list_item, parent, false);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            Track track = (Track) getItem(position);
            viewHolder.tvSong.setText(track.getSong());
            viewHolder.tvArtist.setText(track.getArtist());
            if(track.getAlbumImageUrl() != null) {
                DownloadImageTask task = new DownloadImageTask(new DownloadImageResponse() {
                    @Override
                    public void downloadFinished(Bitmap bitmap) {
                        viewHolder.ivAlbumImg.setImageBitmap(bitmap);
                    }
                });
                task.execute(track.getAlbumImageUrl().toString());
            }

            return convertView;
        }
    }


}
