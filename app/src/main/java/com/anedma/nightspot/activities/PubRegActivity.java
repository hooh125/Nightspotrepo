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
import com.anedma.nightspot.async.ApiController;
import com.anedma.nightspot.async.response.PubRegResponse;
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

public class PubRegActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, OnMapReadyCallback, PubRegResponse {

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
    private String address;
    private User user;
    private JSONObject placeInfo;
    private Button buttonSend;
    private AutoCompleteTextView autoCompleteTextView;
    private ApiController apiController;
    private GoogleMap map;
    private AutocompleteAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pub_reg);
        apiController = ApiController.getInstance();
        apiController.setDelegate(this);
        user = User.getInstance();

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
                if (checkValidPub()) {
                    sendPubOnline(createPubFromForm());
                }
            }
        });

        autoCompleteTextView = findViewById(R.id.autocomplete_places);
        adapter = new AutocompleteAdapter(this, R.layout.address_item);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setOnItemClickListener(this);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_pub_info);
        mapFragment.getMapAsync(this);
    }

    private void sendPubOnline(Pub pub) {
        apiController.requestInsertPub(pub, user.getEmail());
    }

    private Pub createPubFromForm() {
        String name = etName.getText().toString();
        String description = etDescription.getText().toString();
        String phone = etPhone.getText().toString();
        String mAddress = address;
        LatLng latLng = position;
        return new Pub(name, description, mAddress, latLng, phone);
    }

    @Nullable
    private boolean checkValidPub() {
        String name = etName.getText().toString();
        String description = etDescription.getText().toString();
        String phone = etPhone.getText().toString();
        String address = null;
        try {
            if (placeInfo != null) {
                address = placeInfo.getString("formatted_address");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        LatLng latLng = position;
        if (name.isEmpty() || description.isEmpty() || phone.isEmpty() || latLng == null || address == null) {
            Toast.makeText(this, "Te falta rellenar algún campo", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Cerrar el teclado
        String placeId = adapter.getPlaceID(position);
        new PlacesAPIRequest().execute(placeId);
    }

    private void refreshMap(JSONObject info) {
        try {
            placeInfo = info;
            JSONObject latlngJson = info.getJSONObject("geometry").getJSONObject("location");
            position = new LatLng(latlngJson.getDouble("lat"), latlngJson.getDouble("lng"));
            hideKeyboard(this);
            address = placeInfo.getString("formatted_address");
            if (map != null) {
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
        if (imm != null)
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void startPrintTracksActivity() {
        Intent intent = new Intent(this, PrintTracksActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void apiRequestError(String message) {
        Log.d(LOG_TAG, "Error de respuesta en la API -> " + message);
        Toast.makeText(this, "Se ha producido un error", Toast.LENGTH_LONG).show();
    }

    @Override
    public void pubRegResponse() {
        user.setPub(true);
        startPrintTracksActivity();
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
