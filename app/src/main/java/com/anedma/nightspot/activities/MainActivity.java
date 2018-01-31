package com.anedma.nightspot.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.anedma.nightspot.FingerprinterThread;
import com.anedma.nightspot.R;
import com.anedma.nightspot.async.AsyncResponse;
import com.anedma.nightspot.async.DbTask;
import com.anedma.nightspot.dto.Pub;
import com.anedma.nightspot.dto.User;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, AsyncResponse {

    private static final String LOG_TAG = "MAINACTIVITY";
    private static final int PERMISSION_REQUEST_LOCATION = 1;
    private Context context;
    private User user;
    private Button buttonAddPub;
    private DrawerLayout mDrawerLayout;
    private GoogleMap map;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar mToolbar;
    private ArrayList<Pub> pubList = new ArrayList<>();
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.context = this;
        this.user = User.getInstance();

        mToolbar = findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        setupNavigationDrawer();
        setupFAB();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        loadUserPubs();


    }

    private void setupFAB() {
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("FAB", "Se ha pulsado el FAB");
            }
        });
    }

    private void setupNavigationDrawer() {
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerToggle = new DrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mDrawerToggle.syncState();
        buttonAddPub = findViewById(R.id.include_left_drawer).findViewById(R.id.button_add_pub);
        buttonAddPub.setVisibility(!user.isPub() ? View.VISIBLE : View.INVISIBLE);
        buttonAddPub.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPubRegActivity();
                }
            });
    }

    private void loadUserPubs() {
        if(!user.isPub()) {
            DbTask dbTask = new DbTask(this);
            JSONObject json = new JSONObject();
            try {
                json.put("operation", "getUserPubs");
                json.put("email", user.getEmail());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            dbTask.execute(json);
        } else {
            Log.d(LOG_TAG, "El usuario es un Pub, no cargará nada en el mapa");
        }
    }

    private void startPubRegActivity() {
        Log.d(LOG_TAG, "Iniciando actividad para registro de Pub");
        Intent intent = new Intent(context, PubRegActivity.class);
        startActivity(intent);
    }

    private void fillMapWithPubs() {
        if(map != null && pubList.size() > 0) {
            for(Pub pub : pubList)
                map.addMarker(new MarkerOptions().position(pub.getLatLng()).title(pub.getName()));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings_option) {
            Toast.makeText(this, "Prueba de boton", Toast.LENGTH_SHORT).show();
        } else if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mDrawerToggle.onConfigurationChanged(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
            // TODO: Revisar permisos correctamente
            return;
        }
        map.setMyLocationEnabled(true);
        this.map = map;
        fillMapWithPubs();
    }

    @Override
    public void processFinish(JSONObject jsonObject) {
        try {
            String operacion = jsonObject.getString("operation");
            if(operacion.equals("getUserPubs")) {
                JSONArray pubs = jsonObject.getJSONArray("pubs");
                pubList = new ArrayList<>();
                for(int i=0; i<pubs.length(); i++) {
                    JSONObject jsonPub = pubs.getJSONObject(i);
                    String name = jsonPub.getString("name");
                    String description = jsonPub.getString("description");
                    String phone = jsonPub.getString("phone");
                    String lat = jsonPub.getString("lat");
                    String lng = jsonPub.getString("lng");
                    int affinity = Integer.parseInt(jsonPub.getString("affinity"));
                    LatLng position = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
                    pubList.add(new Pub(name, description, position, phone, affinity));
                }
                fillMapWithPubs();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class DrawerToggle extends ActionBarDrawerToggle {

        private DrawerToggle(Activity activity, DrawerLayout drawerLayout, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
            super(activity, drawerLayout, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            Log.d("DRAWER", "PANEL LATERAL ABIERTO");
            super.onDrawerOpened(drawerView);
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            Log.d("DRAWER", "PANEL LATERAL CERRADO");
            super.onDrawerClosed(drawerView);
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            if (fab != null)
                fab.setTranslationX(slideOffset * 250);
            super.onDrawerSlide(drawerView, slideOffset);
        }
    }
}
