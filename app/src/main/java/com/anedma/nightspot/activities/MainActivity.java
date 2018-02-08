package com.anedma.nightspot.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.anedma.nightspot.DownloadImageTask;
import com.anedma.nightspot.PubInfoDialog;
import com.anedma.nightspot.R;
import com.anedma.nightspot.ResourceUtil;
import com.anedma.nightspot.async.AsyncResponse;
import com.anedma.nightspot.async.DbTask;
import com.anedma.nightspot.async.DownloadImageResponse;
import com.anedma.nightspot.dto.Pub;
import com.anedma.nightspot.dto.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, AsyncResponse, GoogleMap.OnMarkerClickListener {

    private static final String LOG_TAG = "MAINACTIVITY";
    private static final int PERMISSION_REQUEST_LOCATION = 154;
    private Context context;
    private User user;
    private Pub pub;
    private DrawerLayout mDrawerLayout;
    private GoogleMap map;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar mToolbar;
    private HashMap<Integer, Pub> pubList = new HashMap<>();
    private FloatingActionButton fab;
    private FusedLocationProviderClient mFusedLocationClient;
    private TextView tvPubName;
    private TextView tvPubTracks;
    private TextView tvUserTracks;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.context = this;
        this.user = User.getInstance();

        mToolbar = findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupNavigationDrawer();
        setupFAB();

        requestPermissionForLocation();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        loadUserPubs(false);
        loadUserData();
    }

    private void loadUserData() {
        DbTask dbTask = new DbTask(this);
        JSONObject json = new JSONObject();
        try {
            json.put("operation", "getUser");
            json.put("email", user.getEmail());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        dbTask.execute(json);
    }

    private void setupFAB() {
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUserPubs(true);
            }
        });
        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_refresh));
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
        View leftDrawer = findViewById(R.id.include_left_drawer);
        //Cargamos los datos del usuario y su foto en el navigation drawer
        final ImageView ivUserPhoto = leftDrawer.findViewById(R.id.iv_user_photo);
        if (user.getPhotoURL() != null) {
            DownloadImageTask task = new DownloadImageTask(new DownloadImageResponse() {
                @Override
                public void downloadFinished(Bitmap bitmap) {
                    ivUserPhoto.setImageBitmap(ResourceUtil.getCircleBitmap(bitmap));
                }
            });
            task.execute(user.getPhotoURL().toString());
        }
        TextView tvUserName = leftDrawer.findViewById(R.id.tv_drawer_user);
        tvUserName.setText(String.format("%s %s", user.getName(), user.getLastName()));
        //Ahora comprobamos si el usuario es Pub o no e inflamos el drawer en función de eso
        FrameLayout contentLayout = leftDrawer.findViewById(R.id.content_drawer);
        View content = getLayoutInflater().inflate((user.isPub()) ? R.layout.fragment_drawer_pub : R.layout.fragment_drawer_user, null);
        contentLayout.addView(content);
        if (user.isPub()) {
            Button buttonPrintTracks = content.findViewById(R.id.button_print_tracks);
            buttonPrintTracks.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPrintTracksActivity();
                }
            });
            Button buttonEditPub = content.findViewById(R.id.button_edit_pub);
            tvPubName = content.findViewById(R.id.tv_pub_name);
            tvPubTracks = content.findViewById(R.id.tv_pub_tracks);
        } else {
            Button buttonAddPub = content.findViewById(R.id.button_add_pub);
            buttonAddPub.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPubRegActivity();
                }
            });
            tvUserTracks = content.findViewById(R.id.tv_user_tracks);
        }
    }

    private void startPrintTracksActivity() {
        Log.d(LOG_TAG, "Iniciando actividad para crear una huella musical en el Pub");
        Intent intent = new Intent(context, PrintTracksActivity.class);
        startActivity(intent);
    }

    private void startPubRegActivity() {
        Log.d(LOG_TAG, "Iniciando actividad para registro de Pub");
        Intent intent = new Intent(context, PubRegActivity.class);
        startActivity(intent);
    }

    private void loadUserPubs(boolean recalculate) {
        DbTask dbTask = new DbTask(this);
        JSONObject json = new JSONObject();
        try {
            if (!user.isPub()) {
                json.put("operation", (recalculate) ? "calculateUserAffinity" : "getUserPubs");
            } else {
                json.put("operation", "getUserPub");
            }
            json.put("email", user.getEmail());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        dbTask.execute(json);
    }

    private void loadDataIntoNavigationDrawer() {
        if (user.isPub()) {
            if (pub != null) {
                tvPubName.setText(pub.getName());
                tvPubTracks.setText(String.valueOf(pub.getTracks()));
            }
        } else {
            tvUserTracks.setText(String.valueOf(user.getTracks()));
        }
    }

    private void fillMap() {
        if (map != null && pubList.size() > 0) {
            map.clear();
            map.setOnMarkerClickListener(this);
            LatLngBounds.Builder bld = new LatLngBounds.Builder();
            for (Integer pubId : pubList.keySet()) {
                Pub pub = pubList.get(pubId);
                Marker marker = map.addMarker(new MarkerOptions().position(pub.getLatLng())
                        .title(pub.getName())
                        .icon(BitmapDescriptorFactory.fromBitmap(ResourceUtil.getBitmap(this, pub.getResourceFromAffinity()))));
                marker.setTag(pub.getId());
                bld.include(pub.getLatLng());
            }
            LatLngBounds bounds = bld.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
            MarkerPubInfo adapter = new MarkerPubInfo();
            map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    Integer pubId = (Integer) marker.getTag();
                    Pub pub = pubList.get(pubId);
                    PubInfoDialog pubInfoDialog = PubInfoDialog.newInstance(pub);
                    pubInfoDialog.show(getSupportFragmentManager(), "pubinfodialog" + marker.getTag());
                }
            });
            map.setInfoWindowAdapter(adapter);
        }
    }

    private void requestPermissionForLocation() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                Toast.makeText(this, "Se necesita permiso para poder cargar tu ubicación", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_LOCATION);

            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(LOG_TAG, "LLamada a onRequestPermissionResult");
        switch (requestCode) {
            case PERMISSION_REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateUserLocation();
                    Log.d(LOG_TAG, "Intentando cargar la localización del usuario");
                }
                break;
        }
    }

    @SuppressLint("MissingPermission")
    private void updateUserLocation() {
        mFusedLocationClient.getLastLocation().addOnCompleteListener(this, new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                Log.d(LOG_TAG, "La localización del usuario se ha obtenido correctamente " + location.getLatitude() + " - " + location.getLongitude());
                if (map != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 11));
                }
            }
        });
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
        this.map = map;
        fillMap();
    }

    @Override
    public void processFinish(JSONObject jsonObject) {
        try {
            String operation = jsonObject.getString("operation");
            boolean error = jsonObject.getBoolean("error");
            String message = jsonObject.getString("message");
            if (!error) {
                switch (operation) {
                    case "getUserPubs":
                        JSONArray pubs = jsonObject.getJSONArray("pubs");
                        pubList = new HashMap<>();
                        for (int i = 0; i < pubs.length(); i++) {
                            JSONObject jsonPub = pubs.getJSONObject(i);
                            int idPub = jsonPub.getInt("id_pub");
                            String name = jsonPub.getString("name");
                            String description = jsonPub.getString("description");
                            String address = jsonPub.getString("address");
                            String phone = jsonPub.getString("phone");
                            String lat = jsonPub.getString("lat");
                            String lng = jsonPub.getString("lng");
                            String affinity = jsonPub.getString("affinity");
                            int tracks = jsonPub.getInt("tracks");
                            LatLng position = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
                            pubList.put(idPub, new Pub(idPub, name, description, address, position, phone, affinity, tracks));
                        }
                        fillMap();
                        break;
                    case "calculateUserAffinity":
                        loadUserPubs(false);
                        Toast.makeText(this, "Se ha actualizado correctamente", Toast.LENGTH_LONG).show();
                        break;
                    case "getUserPub":
                        JSONObject jsonPub = jsonObject.getJSONObject("pub");
                        int pubId = Integer.parseInt(jsonPub.getString("id"));
                        String name = jsonPub.getString("name");
                        String description = jsonPub.getString("description");
                        String phone = jsonPub.getString("phone");
                        String lat = jsonPub.getString("lat");
                        String lng = jsonPub.getString("lng");
                        LatLng position = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
                        int tracks = Integer.parseInt(jsonPub.getString("tracks"));
                        pub = new Pub(pubId, name, description, position, phone, tracks);
                        Log.d(LOG_TAG, "El pub se ha cargado correctamente, intentando rellenar controles");
                        loadDataIntoNavigationDrawer();
                        break;
                    case "getUser":
                        Log.d(LOG_TAG, "Se han recogido los datos del usuario, intentando actualziar controles");
                        JSONObject userJson = jsonObject.getJSONObject("user");
                        int userTracks = Integer.parseInt(userJson.getString("tracks"));
                        User user = User.getInstance();
                        user.setTracks(userTracks);
                        loadDataIntoNavigationDrawer();
                        break;
                }
            } else {
                Log.d(LOG_TAG, "Ha habido un error " + message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(LOG_TAG, "Se ha pulsado el marcador " + marker.getTag());
        marker.showInfoWindow();
        return true;
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

    private class MarkerPubInfo implements GoogleMap.InfoWindowAdapter {

        @Override
        public View getInfoWindow(Marker marker) {
            Integer pubId = (Integer) marker.getTag();
            return (pubId != null) ? fillInfoPubWindow(pubId) : null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            Integer pubId = (Integer) marker.getTag();
            return (pubId != null) ? fillInfoPubWindow(pubId) : null;
        }

        private View fillInfoPubWindow(int pubId) {
            Pub pub = pubList.get(pubId);
            View view = getLayoutInflater().inflate(R.layout.pub_info_window, null);
            if (pub != null) {
                TextView tvPubName = view.findViewById(R.id.tv_pub_name_info);
                TextView tvAffinity = view.findViewById(R.id.tv_affinity);
                tvPubName.setText(pub.getName());
                if (pub.getAffinity() != null) tvAffinity.setText(pub.getAffinity());
            }
            return view;
        }
    }
}
