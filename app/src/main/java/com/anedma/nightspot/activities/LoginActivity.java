package com.anedma.nightspot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.anedma.nightspot.R;
import com.anedma.nightspot.async.ApiController;
import com.anedma.nightspot.async.SpotifyApiController;
import com.anedma.nightspot.async.response.LoginResponse;
import com.anedma.nightspot.async.response.SpotifyResponse;
import com.anedma.nightspot.dto.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, LoginResponse, SpotifyResponse, GoogleApiClient.OnConnectionFailedListener {

    private static final String LOG_TAG = "LOGINACTIVITY";
    private static final int REQUESTCODE_GOOGLE_SIGNIN = 123; //Este es el código de vuelta que usara la API para saber que el usuario ha conseguido loguearse.
    private static final int REQUESTCODE_SPOTIFY_SIGNIN = 1337;
    private static final String SPOTIFY_CLIENT_ID = "d075f2e5efcc4be6b5cb879a49950de5";
    private static final String REDIRECT_SPOTIFY_URI = "nightspot://callback";
    private static final String[] SCOPES = {"playlist-read-private", "playlist-read-collaborative", "user-follow-read", "user-library-read", "user-top-read", "user-read-email", "playlist-read-collaborative"};
    private Button loginGoogleButton;
    private Button loginSpotifyButton;
    private static boolean loginGoogle = false;
    private static boolean loginSpotify = false;
    private static boolean isPub = false;
    public static boolean requestSpotifyData = false;
    public GoogleSignInAccount account;
    public ProgressBar progressBar;
    private GoogleSignInClient mGoogleSignIn;
    private AuthenticationRequest.Builder builder;
    private SpotifyApiController controller;
    private ApiController apiController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        apiController = ApiController.getInstance();
        apiController.setDelegate(this);
        setupUI();

        //Google Login
        GoogleSignInOptions mGoogleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        mGoogleSignIn = GoogleSignIn.getClient(this, mGoogleSignInOptions);

        //Spotify Login
        builder = new AuthenticationRequest.Builder(SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_SPOTIFY_URI);
        builder.setScopes(SCOPES);
    }

    @Override
    protected void onStart() {
        account = GoogleSignIn.getLastSignedInAccount(this);
        if(account != null) {
            loginGoogleButton.setEnabled(false);
            sendGoogleSignInRequest();
        }
        super.onStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(LOG_TAG, "Se ha devuelto el resultado de una petición: " + requestCode);
        //Si el resultado de la petición es correcto
        if(requestCode == REQUESTCODE_GOOGLE_SIGNIN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        } else if (requestCode == REQUESTCODE_SPOTIFY_SIGNIN) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            handleSpotifySignInResult(response);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AuthenticationClient.stopLoginActivity(this, REQUESTCODE_SPOTIFY_SIGNIN);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.googleSignInButton:
                loginWithGoogle();
                break;
            case R.id.spotifySignInButton:
                loginWithSpotify();
                break;
            default:
                break;
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            account = completedTask.getResult(ApiException.class);
            sendGoogleSignInRequest();
        } catch (ApiException e) {
            //Si hay un error, la API devolverá un resultado según corresponda.
            Log.w(LOG_TAG, "El Login con Google ha fallado con el código: " + e.getStatusCode());
            Log.w(LOG_TAG, "Mensaje: " + e.getMessage());
            Log.d(LOG_TAG, "LOGIN ERRONEO");
            Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_LONG).show();
        }
    }

    private void handleSpotifySignInResult(AuthenticationResponse response) {
        switch (response.getType()) {
            case TOKEN:
                //TODO: Login satisfactorio para Spotify
                Log.d(LOG_TAG, "LOGIN DE SPOTIFY SATISFACTORIO");
                String accessToken = response.getAccessToken();
                Log.d(LOG_TAG, "ACCESS TOKEN SPOTIFY: " + accessToken);
                controller = SpotifyApiController.getInstance();
                controller.setAccessToken(accessToken);
                loginSpotifyButton.setEnabled(false);
                loginSpotify = true;
                checkLoginStatus();
                break;
            case ERROR:
                //TODO: Login erroneo para Spotify
                Log.d(LOG_TAG, "LOGIN DE SPOTIFY ERRONEO");
                Toast.makeText(this, "Error al iniciar sesión con Spotify", Toast.LENGTH_LONG).show();
                break;
            default:
        }
    }

    private void setupUI() {
        loginGoogleButton = findViewById(R.id.googleSignInButton);
        loginSpotifyButton = findViewById(R.id.spotifySignInButton);
        loginGoogleButton.setOnClickListener(this);
        loginSpotifyButton.setOnClickListener(this);
    }

    private void sendGoogleSignInRequest() {
        apiController.requestLogin(account.getGivenName(), account.getFamilyName(), account.getEmail());
    }

    private void loginWithGoogle() {
        Intent signInIntent = mGoogleSignIn.getSignInIntent();
        startActivityForResult(signInIntent, REQUESTCODE_GOOGLE_SIGNIN);
    }

    private void loginWithSpotify() {
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUESTCODE_SPOTIFY_SIGNIN, request);
    }

    public void checkLoginStatus() {
        loginGoogleButton.setEnabled(!loginGoogle);
        loginSpotifyButton.setEnabled(!loginSpotify);
        if(loginGoogle && (loginSpotify || isPub)) {
            if(requestSpotifyData) {
                Log.d("LOGINACTIVITY", "Intentando recoger librería del usuario");
                controller.setRequestUpdate();
                controller.getUserData();
                if(progressBar == null) {
                    LinearLayout layoutLoadingLibrary = findViewById(R.id.layout_loading_library);
                    layoutLoadingLibrary.setVisibility(View.VISIBLE);
                    progressBar = findViewById(R.id.pb_load_tracks);
                    //Bloquamos la interacción del usuario con la interfaz
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            } else {
                Log.d("LOGINACTIVITY", "No se necesita recoger libreria del usuario");
                Toast.makeText(this, "El usuario se ha logueado", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    public void loginResponse(boolean alreadyRegistered, boolean isPub) {
        User user = User.getInstance();
        user.setEmail(account.getEmail());
        user.setName(account.getGivenName());
        user.setLastName(account.getFamilyName());
        user.setPub(isPub);
        user.setPhotoUri(account.getPhotoUrl());
        requestSpotifyData = !alreadyRegistered && !isPub;
        loginGoogle = true;
        checkLoginStatus();
        Log.d("GOOGLE", "LOGIN SATISFACTORIO");
    }

    @Override
    public void requestInsertTracksResponse() {
        requestSpotifyData = false;
        LinearLayout progressBarLayout = findViewById(R.id.layout_loading_library);
        progressBarLayout.setVisibility(View.GONE);
        checkLoginStatus();
    }

    @Override
    public void insertTracksProgressUpdate(int totalTracks, int tracksRemaining) {
        if(progressBar != null) {
            progressBar.setMax(totalTracks);
            progressBar.setProgress(totalTracks - tracksRemaining);
        }
    }

    @Override
    public void apiRequestError(String message) {
        loginGoogleButton.setEnabled(true);
        loginSpotifyButton.setEnabled(true);
        Log.d(LOG_TAG, "Error de respuesta en la API -> " + message);
        Toast.makeText(this, "Se ha producido un error", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /*@Override
    public void processFinish(JSONObject jsonObject) {
        if(jsonObject == null) {
            Log.d("LOGINACTIVITY", "El servidor no ha devuelto respuesta alguna");
        } else {
            Log.d("JSON", jsonObject.toString());
            try {
                String operation = jsonObject.getString("operation");
                switch (operation) {
                    case "insertUser":
                        boolean error = jsonObject.getBoolean("error");
                        loginGoogleButton.setEnabled(error);
                        if(!error) {
                            boolean alreadyRegistered;
                            alreadyRegistered = jsonObject.getBoolean("alreadyRegistered");
                            isPub = jsonObject.getBoolean("isPub");
                            User user = User.getInstance();
                            user.setEmail(account.getEmail());
                            user.setName(account.getGivenName());
                            user.setLastName(account.getFamilyName());
                            user.setPub(isPub);
                            user.setPhotoUri(account.getPhotoUrl());
                            requestSpotifyData = !alreadyRegistered && !isPub;
                            loginGoogle = true;
                            checkLoginStatus();
                            Log.d("GOOGLE", "LOGIN SATISFACTORIO");
                        } else {
                            loginGoogleButton.setEnabled(true);
                            Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case "insertUserTracks":
                        int tracksLeft = jsonObject.getInt("tracksLeft");
                        int tracksRecorded = jsonObject.getInt("tracksRecorded");
                        Log.d("LOGINACTIVITY", "El servidor ha insertado:" + tracksRecorded + " canciones y faltan " + tracksLeft + " por insertar");
                        if(tracksLeft == 0) {
                            requestSpotifyData = false;
                            progressBar.setProgress(controller.getTracksNumber());
                            checkLoginStatus();
                        } else {
                            progressBar.setProgress(controller.getTracksNumber() - tracksLeft);
                        }
                        break;
                    default:
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }*/

}
