package com.anedma.nightspot.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.anedma.nightspot.AutocompleteAdapter;
import com.anedma.nightspot.R;
import com.anedma.nightspot.async.AsyncResponse;
import com.anedma.nightspot.async.DbTask;
import com.anedma.nightspot.dto.Pub;
import com.anedma.nightspot.dto.User;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PubRegActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, OnMapReadyCallback, AsyncResponse {

    public static final String LOG_TAG = "PUBREGACTIVITY";
    public static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    public static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    public static final String TYPE_PLACE_DETAILS = "/details";
    public static final String OUT_JSON = "/json";
    public static final String API_KEY = "AIzaSyAW2_BIVFwv3-EaP4cQr1d9lKYTiOArJ3s";
    private EditText etName;
    private EditText etDescription;
    private EditText etPhone;
    private LatLng position;
    private JSONObject placeInfo;
    private Button buttonSend;
    private AutoCompleteTextView autoCompleteTextView;
    private GoogleMap map;
    private AutocompleteAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pub_reg);
        setupUI();


    }

    private void setupUI() {
        etName = findViewById(R.id.et_name);
        etDescription = findViewById(R.id.et_description);
        etPhone = findViewById(R.id.et_phone);
        buttonSend = findViewById(R.id.button_send_pub);
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Pub pub;
                if((pub =checkValidPub()) != null) {
                    sendPubOnline(pub);
                }
            }
        });

        autoCompleteTextView = findViewById(R.id.autocomplete_places);
        adapter = new AutocompleteAdapter(this, R.layout.address_item);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setOnItemClickListener(this);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_pub_reg);
        mapFragment.getMapAsync(this);
    }

    private void sendPubOnline(Pub pub) {
        Log.d(LOG_TAG, "Intentando insertar Pub online");
        DbTask task = new DbTask(this);
        User user = User.getInstance();
        JSONObject json = new JSONObject();
        try {
            json.put("operation", "insertPub");
            json.put("name", pub.getName());
            json.put("description", pub.getDescription());
            json.put("phone", pub.getPhone());
            json.put("lat", pub.getLatLng().latitude);
            json.put("lng", pub.getLatLng().longitude);
            json.put("email", user.getEmail());
            Log.d(LOG_TAG, json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        task.execute(json);
    }

    @Nullable
    private Pub checkValidPub() {
        String name = etName.getText().toString();
        String description = etDescription.getText().toString();
        String phone = etPhone.getText().toString();
        String address = null;
        try {
            address = placeInfo.getString("formatted_address");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        LatLng latLng = position;
        if(name.isEmpty() || description.isEmpty() || phone.isEmpty() || latLng == null) {
            Toast.makeText(this, "Te falta rellenar algún campo", Toast.LENGTH_SHORT).show();
            return null;
        }
        return new Pub(name, description, address, latLng, phone);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Cerrar el teclado
        String placeId = adapter.getPlaceID(position);
        new PlacesAPIRequest().execute(placeId);
    }

    private void refreshMap(JSONObject info) {
        try {
            JSONObject latlngJson = info.getJSONObject("geometry").getJSONObject("location");
            hideKeyboard(this);
            position = new LatLng(latlngJson.getDouble("lat"), latlngJson.getDouble("lng"));
            placeInfo = info;
            if(map != null) {
                map.addMarker(new MarkerOptions().position(position));
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(position)
                        .zoom(17).build();
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        if(imm != null)
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void processFinish(JSONObject jsonObject) {
        if(jsonObject != null) {
            try {
                if(!jsonObject.getBoolean("error")) {
                    Log.d(LOG_TAG, "Pub registrado correctamente");
                    Log.d(LOG_TAG, jsonObject.toString());
                    startPrintTracksActivity();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void startPrintTracksActivity() {
        Intent intent = new Intent(this, PrintTracksActivity.class);
        startActivity(intent);
        finish();
    }

    class PlacesAPIRequest extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... strings) {
            String placeId = strings[0];
            return getPlaceInfo(placeId);
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            refreshMap(jsonObject);
            super.onPostExecute(jsonObject);
        }

        private JSONObject getPlaceInfo(String placeId) {
            HttpURLConnection conn = null;
            JSONObject results = null;
            StringBuilder jsonResults = new StringBuilder();
            try {
                String sb = PLACES_API_BASE +
                        TYPE_PLACE_DETAILS +
                        OUT_JSON +
                        "?placeid=" + placeId +
                        "&key=" + API_KEY;
                URL url = new URL(sb);
                conn = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(conn.getInputStream());

                // Load the results into a StringBuilder
                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1) {
                    jsonResults.append(buff, 0, read);
                }
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Error processing Places API URL", e);
                return null;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error connecting to Places API", e);
                return null;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            try {
                // Create a JSON object hierarchy from the results
                //Log.d("yo",jsonResults.toString());
                JSONObject jsonObj = new JSONObject(jsonResults.toString());
                results = jsonObj.getJSONObject("result");

                //saveArray(resultList.toArray(new String[resultList.size()]), "predictionsArray", getContext());
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Cannot process JSON results", e);
            }

            return results;
        }
    }


}
