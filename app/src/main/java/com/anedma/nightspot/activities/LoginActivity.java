package com.anedma.nightspot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.anedma.nightspot.R;
import com.anedma.nightspot.SpotifyApiController;
import com.anedma.nightspot.async.DbTask;
import com.anedma.nightspot.database.DbHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, DbTask.AsyncResponse {

    private static final int REQUESTCODE_GOOGLE_SIGNIN = 123; //Este es el código de vuelta que usara la API para saber que el usuario ha conseguido loguearse.
    private static final int REQUESTCODE_SPOTIFY_SIGNIN = 1337;
    private static final String SPOTIFY_CLIENT_ID = "d075f2e5efcc4be6b5cb879a49950de5";
    private static final String REDIRECT_SPOTIFY_URI = "nightspot://callback";
    private static final String[] SCOPES = {"playlist-read-private", "playlist-read-collaborative", "user-follow-read", "user-library-read", "user-top-read", "user-read-email", "playlist-read-collaborative"};
    private static boolean loginGoogle = false;
    private static boolean loginSpotify = false;
    public static boolean requestSpotifyData = false;
    public GoogleSignInAccount account;
    private GoogleSignInClient mGoogleSignIn;
    private AuthenticationRequest.Builder builder;
    private SpotifyApiController controller;

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
            handleSpotifySignInResult(response);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AuthenticationClient.stopLoginActivity(this, REQUESTCODE_SPOTIFY_SIGNIN);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            account = completedTask.getResult(ApiException.class);
            //TODO: Gestionar la entrada del usuario con Google
            DbHelper dbHelper = new DbHelper(this);
            JSONObject json = new JSONObject();
            HashMap<String, String> args = new HashMap<>();
            json.put("operation", "insertUser");
            json.put("name", account.getGivenName());
            json.put("lastName", account.getFamilyName());
            json.put("email", account.getEmail());
            dbHelper.mySqlRequest(json);
            /*ProgressBar progressBar = new ProgressBar(this,null,android.R.attr.progressBarStyleLarge);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100,100);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            LinearLayout layout = findViewById(R.id.login_activity);
            layout.addView(progressBar,params);
            progressBar.setVisibility(View.VISIBLE);  //To show ProgressBar
            //progressBar.setVisibility(View.GONE);     // To Hide ProgressBar
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            //Si esto no retorna un fallo, el usuario se ha logueado correctamente, ya puede acceder*/
            Log.d("GOOGLE", "LOGIN SATISFACTORIO");
            loginGoogle = true;
        } catch (ApiException e) {
            //Si hay un error, la API devolverá un resultado según corresponda.
            Log.w("GOOGLESIGNIN", "El Login con Google ha fallado con el código: " + e.getStatusCode());
            Log.w("GOOGLESIGNIN", "Mensaje: " + e.getMessage());
            Log.d("GOOGLE", "LOGIN ERRONEO");
            Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_LONG).show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleSpotifySignInResult(AuthenticationResponse response) {
        switch (response.getType()) {
            case TOKEN:
                //TODO: Login satisfactorio para Spotify
                Log.d("SPOTIFYAPI", "LOGIN SATISFACTORIO");
                String accessToken = response.getAccessToken();
                Log.d("SPOTIFYAPI", "ACCESS TOKEN: " + accessToken);
                controller = new SpotifyApiController(accessToken);
                controller.setContext(this);
                loginSpotify = true;
                checkLoginStatus();
                break;
            case ERROR:
                //TODO: Login erroneo para Spotify
                Log.d("SPOTIFYAPI", "LOGIN ERRONEO");
                Toast.makeText(this, "Error al iniciar sesión con Spotify", Toast.LENGTH_LONG).show();
                break;
            default:
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

    private void checkLoginStatus() {
        if(loginGoogle && loginSpotify) {
            if(requestSpotifyData) {
                Log.d("LOGINACTIVITY", "Intentando recoger librería del usuario");
                controller.getUserData();
            } else {
                Log.d("LOGINACTIVITY", "No se necesita recoger libreria del usuario");

            }
            Log.d("LOGINACTIVITY", "El usuario se ha logueado correctamente");
        }
    }

    @Override
    public void processFinish(JSONObject jsonObject) {
        if(jsonObject == null) {
            Log.d("LOGINACTIVITY", "El servidor no ha devuelto respuesta alguna");
        } else {
            try {
                String operation = jsonObject.getString("operation");
                switch (operation) {
                    case "insertUser":
                        if(!jsonObject.getBoolean("alreadyRegistered")) {
                            requestSpotifyData = true;
                        }
                        checkLoginStatus();
                        break;
                    case "insertUserTracks":
                        Log.d("LOGINACTIVITY", "El servidor ha insertado:" + jsonObject.getInt("tracksRecorded") + " canciones");
                        break;
                    default:
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
