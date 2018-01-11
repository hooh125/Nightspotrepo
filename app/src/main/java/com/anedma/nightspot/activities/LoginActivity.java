package com.anedma.nightspot.activities;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

public class LoginActivity extends Activity implements View.OnClickListener {

    private static final int RC_SIGN_IN = 123; //Este es el código de vuelta que usara la API para saber que el usuario ha conseguido loguearse.
    GoogleSignInClient mGoogleSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupUI();

        GoogleSignInOptions mGoogleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        mGoogleSignIn = GoogleSignIn.getClient(this, mGoogleSignInOptions);
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
        }
        super.onStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Si el resultado de la petición es correcto
        if(resultCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            //TODO: Gestionar la entrada del usuario con Google
            //Si esto no retorna un fallo, el usuario se ha logueado correctamente, ya puede acceder
        } catch (ApiException e) {
            //Si hay un error, la API devolverá un resultado según corresponda.
            Log.w("GOOGLESIGNIN", "El Login con Google ha fallado con el código: " + e.getStatusCode());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.googleSignInButton:
                loginWithGoogle();
                break;
            case R.id.spotifySignInButton:
                Toast.makeText(this, "TEST LOGIN SPOTIFY", Toast.LENGTH_SHORT).show();
                break;
            default:
                    break;
        }
    }

    private void loginWithGoogle() {
        Intent signInIntent = mGoogleSignIn.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
}
