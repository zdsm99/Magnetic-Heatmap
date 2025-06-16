package com.example.magneticheatmap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import org.osmdroid.views.overlay.Marker;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private View fixStatus;
    private MenuController menuController;
    private MapController mapController;
    private LocationData locationData;
    private GetSensorsValues headingAngle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Status FIX
        fixStatus = findViewById(R.id.fixStatus);

        // inicjalizacja mapy i kontrolera
        mapController = new MapController(this);
        AppController.getInstance().setMapController(mapController);

        // inicjalizacja menuController
        menuController = new MenuController(this);
        AppController.getInstance().setMenuController(menuController);

        // Śledzenie zwrotu
        headingAngle = new GetSensorsValues(this, heading ->{
            Marker locationMarker = mapController.getLocationMarker();
            if (locationMarker != null) {
                locationMarker.setRotation(heading);
                mapController.refreshMap();
            }
        });
        AppController.getInstance().setSensorsValues(headingAngle);

        // Śledzenie lokalizacji
        locationData = new LocationData(this);
        AppController.getInstance().setLocationData(locationData);
        locationData.setCallback(new LocationData.OnLocationChangedCallback() {
            @Override
            public void onLocationChanged(Location location) {

                mapController.updateMarkerPosition(location);
                fixStatus.setBackgroundResource(R.drawable.green_circle);

                // Przekazanie informacji o określeniu pozycji
                AppController.getInstance().setIsFixedPossition(true);
            }

            @Override
            public void onGpsDisabled() {
                fixStatus.setBackgroundResource(R.drawable.red_circle);
                AppController.getInstance().setIsFixedPossition(false);
                if (AppController.getInstance().getGridDrawer() != null){
                    menuController.pauseMeasurement();
                }
            }
        });

        // Request GPS enable if needed
        locationData.requestEnableGpsIfNeeded();

        // Ask for permissions if needed
        if (locationData.hasLocationPermission()) {
            locationData.start();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
        ImageButton settingsBtn = findViewById(R.id.settingsBtn);
        settingsBtn.setOnClickListener(v -> {
            Intent settings = new Intent(MainActivity.this, Settings.class);
            settings.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(settings);
        });
    }
    public static void setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getString("app_language", Locale.getDefault().getLanguage());
    }
    public static Context updateBaseContextLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(MainActivity.updateBaseContextLocale(newBase, MainActivity.getSavedLanguage(newBase)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapController.getCurrentMap().onResume();
        headingAngle.start();
        if (locationData.hasLocationPermission()) {
            locationData.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapController.getCurrentMap().onPause();
        headingAngle.stop();
        locationData.stop();
    }

    // Obsługa wyniku zapytania o uprawnienia
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationData.start();
            } else {
                Toast.makeText(this, getString(R.string.noLocationPermission), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Implementacja nieużywanych metod LocationListener
    @Override public void onLocationChanged(Location location) {}


}


