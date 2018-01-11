package com.anedma.nightspot.activities;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.anedma.nightspot.R;

public class LoginActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ImageButton loginGoogleButton = findViewById(R.id.googleSignInButton);
        ImageButton loginSpotifyButton = findViewById(R.id.spotifySignInButton);
        loginGoogleButton.setOnClickListener(this);
        loginSpotifyButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.googleSignInButton:
                Toast.makeText(this, "TEST LOGIN GOOGLE", Toast.LENGTH_SHORT).show();
                break;
            case R.id.spotifySignInButton:
                Toast.makeText(this, "TEST LOGIN SPOTIFY", Toast.LENGTH_SHORT).show();
                break;
            default:
                    break;
        }
    }
}
