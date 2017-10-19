package com.anedma.nightspot;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.gracenote.gnsdk.*;


public class MainActivity extends AppCompatActivity implements IGnSystemEvents {

    private Context context;
    final String CLIENT_ID = "1505815973";
    final String CLIENT_TAG = "6AFDD772D5ED569CD42DE802F22EBCF4";

    private GnManager gnManager;
    private GnUser gnUser;
    private GnLocale gnLocale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this.getApplicationContext();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initGraceNoteAPI();


        Button botonPrueba = (Button) findViewById(R.id.button_prueba);
        botonPrueba.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("DEBUG", "El botón se ha presionado correctamente");
            }
        });
    }

    private void initGraceNoteAPI() {
        try {
            gnManager = new GnManager(context, "license.txt", GnLicenseInputMode.kLicenseInputModeFilename);
            GnUserStore userStore = new GnUserStore(context);
            gnUser = new GnUser(userStore, CLIENT_ID, CLIENT_TAG, "1.0");
            gnLocale = new GnLocale(GnLocaleGroup.kLocaleGroupMusic,
                            GnLanguage.kLanguageSpanish,
                            GnRegion.kRegionGlobal,
                            GnDescriptor.kDescriptorDefault,
                            gnUser);
            gnLocale.setGroupDefault();
            gnManager.systemEventHandler(this);
            gnUser.options().lookupMode(GnLookupMode.kLookupModeOnline);
        } catch (GnException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void localeUpdateNeeded(GnLocale gnLocale) {
        try {
            gnLocale.update(gnUser);
        } catch (GnException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void listUpdateNeeded(GnList gnList) {
        try {
            gnList.update(gnUser);
        } catch (GnException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void systemMemoryWarning(long l, long l1) {

    }
}
