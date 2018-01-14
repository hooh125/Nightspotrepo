package com.anedma.nightspot.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.anedma.nightspot.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationHandler;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class LoginActivity extends Activity implements View.OnClickListener {

    private static final int REQUESTCODE_GOOGLE_SIGNIN = 123; //Este es el código de vuelta que usara la API para saber que el usuario ha conseguido loguearse.
    private static final int REQUESTCODE_SPOTIFY_SIGNIN = 1337;
    private static final String SPOTIFY_CLIENT_ID = "d075f2e5efcc4be6b5cb879a49950de5";
    private static final String REDIRECT_SPOTIFY_URI = "nightspot://callback";
    private static final String[] SCOPES = {"playlist-read-private", "playlist-read-collaborative", "user-follow-read", "user-library-read", "user-top-read"};
    private GoogleSignInClient mGoogleSignIn;
    private AuthenticationRequest.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupUI();

        //Google Login
        GoogleSignInOptions mGoogleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        mGoogleSignIn = GoogleSignIn.getClient(this, mGoogleSignInOptions);

        //Spotify Login
        builder = new AuthenticationRequest.Builder(SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_SPOTIFY_URI);
        builder.setScopes(SCOPES);
    }

    private void setupUI() {
        ImageButton loginGoogleButton = findViewById(R.id.googleSignInButton);
        ImageButton loginSpotifyButton = findViewById(R.id.spotifySignInButton);
        loginGoogleButton.setOnClickListener(this);
        loginSpotifyButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account != null) {
            //TODO: Gestionar la entrada del usuario con Google
            //El usuario ya se ha logueado previamente, puede acceder.
            Log.d("GOOGLE", "LOGIN SATISFACTORIO");
        }
        super.onStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("LOGIN", "Se ha devuelto el resultado de una petición: " + requestCode);
        //Si el resultado de la petición es correcto
        if(requestCode == REQUESTCODE_GOOGLE_SIGNIN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        } else if (requestCode == REQUESTCODE_SPOTIFY_SIGNIN) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            switch (response.getType()) {
                case TOKEN:
                    //TODO: Login satisfactorio para Spotify
                    Log.d("SPOTIFY", "LOGIN SATISFACTORIO");
                    String accessToken = AuthenticationClient.getResponse(resultCode, getIntent()).getAccessToken();
                    break;
                case ERROR:
                    //TODO: Login erroneo para Spotify
                    Log.d("SPOTIFY", "LOGIN ERRONEO");
                    Toast.makeText(this, "Error al iniciar sesión con Spotify", Toast.LENGTH_LONG).show();
                    break;
                default:
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AuthenticationClient.stopLoginActivity(this, REQUESTCODE_SPOTIFY_SIGNIN);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            //TODO: Gestionar la entrada del usuario con Google
            //Si esto no retorna un fallo, el usuario se ha logueado correctamente, ya puede acceder
            Log.d("GOOGLE", "LOGIN SATISFACTORIO");
        } catch (ApiException e) {
            //Si hay un error, la API devolverá un resultado según corresponda.
            Log.w("GOOGLESIGNIN", "El Login con Google ha fallado con el código: " + e.getStatusCode());
            Log.d("GOOGLE", "LOGIN ERRONEO");
            Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.googleSignInButton:
                loginWithGoogle();
                break;
            case R.id.spotifySignInButton:
                loginWIthSpotify();
                break;
            default:
                    break;
        }
    }

    private void loginWithGoogle() {
        Intent signInIntent = mGoogleSignIn.getSignInIntent();
        startActivityForResult(signInIntent, REQUESTCODE_GOOGLE_SIGNIN);
    }

    private void loginWIthSpotify() {
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUESTCODE_SPOTIFY_SIGNIN, request);
    }
}
