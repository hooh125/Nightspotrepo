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
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.anedma.nightspot.R;
import com.anedma.nightspot.async.ApiController;
import com.anedma.nightspot.async.FingerprinterThread;
import com.anedma.nightspot.async.response.DownloadImageResponse;
import com.anedma.nightspot.async.response.GracenoteResponse;
import com.anedma.nightspot.async.response.PubPrintTracksResponse;
import com.anedma.nightspot.async.task.DownloadImageTask;
import com.anedma.nightspot.dto.Track;
import com.anedma.nightspot.dto.User;

import java.util.ArrayList;
import java.util.List;

public class PrintTracksActivity extends AppCompatActivity implements View.OnClickListener, GracenoteResponse, PubPrintTracksResponse {

    private static final String LOG_TAG = "PRINTTRACKSACTIVITY";
    private List<Track> tracks = new ArrayList<>();
    private FingerprinterThread fingerprintThread;
    private TrackAdapter adapter;
    private ListView lvTracks;
    private LinearLayout pbLayout;
    private Button buttonSendTracks;
    private Button buttonSkipSend;
    private Switch switch_fingerprint;
    private User user;
    private ApiController apiController;
    private GracenoteResponse delegate;
    private boolean requestStopProcessing = false;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_tracks);
        apiController = ApiController.getInstance();
        apiController.setDelegate(this);
        user = User.getInstance();
        delegate = this;
        context = this;

        pbLayout = findViewById(R.id.layout_loading_tracks);
        buttonSendTracks = findViewById(R.id.button_send_pub_tracks);
        buttonSkipSend = findViewById(R.id.button_skip_send_tracks);
        buttonSendTracks.setOnClickListener(this);
        buttonSkipSend.setOnClickListener(this);
        lvTracks = findViewById(R.id.lv_tracks);

        tracks.clear();
        adapter = new TrackAdapter(tracks, this);
        lvTracks.setAdapter(adapter);


        switch_fingerprint = findViewById(R.id.sw_start_prints);
        switch_fingerprint.setEnabled(true);


        fingerprintThread = new FingerprinterThread(context, delegate);
        fingerprintThread.interrupt();


        switch_fingerprint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    togglePrintProcess(true);
                } else {
                    togglePrintProcess(false);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        togglePrintProcess(false);
        super.onPause();
    }

    private void togglePrintProcess(boolean start) {
        if (start && !fingerprintThread.isAlive()) {
            switch_fingerprint.setText(R.string.tv_switch_starting);
            startProgressBar();
            if(fingerprintThread.getState() == Thread.State.NEW) {
                fingerprintThread.start();
            } else if (fingerprintThread.getState() == Thread.State.TERMINATED) {
                fingerprintThread = new FingerprinterThread(context, delegate);
                fingerprintThread.start();
            }
        } else if (!start) {
            switch_fingerprint.setText(R.string.tv_switch_stopping);
            requestStopProcessing = true;
            startProgressBar();
            fingerprintThread.interrupt();
        }
        switch_fingerprint.setEnabled(false);
    }


    @Override
    public void trackReturned(final Track track) {
        Log.d(LOG_TAG, "Canción encontrada: " + track.getSong());
        if (!checkIfAlreadyExist(tracks, track)) {
            tracks.add(track);
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (lvTracks.getAdapter() == null) {
                        adapter = new TrackAdapter(tracks, context);
                        lvTracks.setAdapter(adapter);
                    }
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public void processingFinished() {
        Log.d(LOG_TAG, "processingFinished");
    }



    @Override
    public void audioProcessStopped() {
        Log.d(LOG_TAG, "audioProcessStopped");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch_fingerprint.setEnabled(true);
                switch_fingerprint.setChecked(false);
                switch_fingerprint.setText(R.string.status_stopped);
                stopProgressBar();
            }
        });
    }

    @Override
    public void audioProcessStarted() {
        Log.d(LOG_TAG, "audioProcessStarted");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch_fingerprint.setEnabled(true);
                switch_fingerprint.setChecked(true);
                switch_fingerprint.setText(R.string.status_started);
                stopProgressBar();
            }
        });
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
        if (v.getId() == R.id.button_send_pub_tracks) {
            if (fingerprintThread != null && fingerprintThread.isAlive()) {
                fingerprintThread.interrupt();
                startProgressBar();
            }
            apiController.requestInsertPubTracks(tracks, user.getEmail());
        } else if (v.getId() == R.id.button_skip_send_tracks) {
            if(fingerprintThread.isAlive()) fingerprintThread.interrupt();
            startMainActivity();
        }
    }

    private void startProgressBar() {
        pbLayout.setVisibility(View.VISIBLE);
        //Bloqueamos la interacción del usuario
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void stopProgressBar() {
        pbLayout.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    @Override
    public void apiRequestError(String message) {
        pbLayout.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        Log.d(LOG_TAG, "Error de respuesta en la API -> " + message);
        Toast.makeText(this, "Se ha producido un error", Toast.LENGTH_LONG).show();
    }

    @Override
    public void printTracksResponse() {
        if (fingerprintThread.isAlive()) fingerprintThread.interrupt();
        startMainActivity();
    }

    private void startMainActivity() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
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

        private TrackAdapter(List<Track> tracks, Context context) {
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
            if (track.getAlbumImageUrl() != null) {
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
