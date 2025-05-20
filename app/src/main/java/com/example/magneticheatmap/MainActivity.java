package com.example.magneticheatmap;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private MapView map;
    private IMapController mapController;
    private View fixStatus;
    private TextView gpsStatus;
    private LocationData locationData;
    private HeadingAngle headingAngle;
    private Marker locationMarker;
    private List<GeoPoint> points = new ArrayList<>();
    private GeoPoint lastLocation;
    private long animationDuration = 1000;
    GridOverlayManager gridManager;

    private void animateMarker(GeoPoint start, GeoPoint end) {
        long startTime = System.currentTimeMillis();
        android.os.Handler handler = new android.os.Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                float t = Math.min(1f, (float) elapsed / animationDuration);

                double lat = start.getLatitude() + t * (end.getLatitude() - start.getLatitude());
                double lon = start.getLongitude() + t * (end.getLongitude() - start.getLongitude());

                GeoPoint interpolated = new GeoPoint(lat, lon);
                locationMarker.setPosition(interpolated);
                //createPoint(map, interpolated);
                gridManager.addDataPoint(lat, lon, headingAngle.getMagneticValue());
                map.invalidate();

                if (t < 1f) {
                    handler.postDelayed(this, 16); // ~60 FPS
                }
            }
        });
    }

    // MAPA
    public void setMapPosition(GeoPoint point, IMapController mapController){
        mapController.animateTo(point);
        mapController.setZoom(21.5);
    }
    public void createBuffer(MapView map, GeoPoint center, int color, double radius) {
        Polygon buffer = new Polygon();
        buffer.setPoints(Polygon.pointsAsCircle(center, radius));
        buffer.setFillColor(color);
        buffer.setStrokeWidth(0);
        buffer.setTitle("Bufor");
        map.getOverlayManager().add(buffer);
    }
    public void createPoint(MapView map, GeoPoint point) {
        if (points.contains(point)) {
            Log.d("Points", "Punkt jest już utworzony");
        } else {
            double mValue = headingAngle.getMagneticValue();
            String label = String.format("%.2f", mValue);
            points.add(point);
            Marker marker = new Marker(map);
            marker.setPosition(point);
            marker.setTextIcon(label);
            marker.setTextLabelForegroundColor(Color.BLACK);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            marker.setPanToView(true);
            map.getOverlays().add(marker);
            int fillColor = getBufferColor(mValue, 30f, 100f);
            createBuffer(map, point, fillColor,2.0);
        }
    }
    public int getBufferColor(double value, float minValue, float maxValue) {
        // Normalizacja: 0.0 – 1.0
        double ratio = Math.min(1f, Math.max(0f, (value - minValue) / (maxValue - minValue)));

        // Interpolacja RGB (zielony → żółty → czerwony)
        int red, green;
        if (ratio < 0.5f) {
            // zielony → żółty
            red = (int)(2 * ratio * 255);
            green = 255;
        } else {
            // żółty → czerwony
            red = 255;
            green = (int)((1 - 2 * (ratio - 0.5f)) * 255);
        }

        int alpha = 0x44; // przezroczystość
        return Color.argb(alpha, red, green, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);

        // Map initialization
        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);
        map.setTileSource(TileSourceFactory.MAPNIK);
        mapController = map.getController();
        mapController.setZoom(7.4);
        mapController.setCenter(new GeoPoint(52.1, 19.2));

        //Grid
        gridManager = new GridOverlayManager(map, 0.00001);


        // UI references
        fixStatus = findViewById(R.id.fixStatus);
        gpsStatus = findViewById(R.id.gpsStatus);

        // Śledzenie zwrotu
        headingAngle = new HeadingAngle(this, heading ->{
            if (locationMarker != null) {
                locationMarker.setRotation(heading);
                map.invalidate();
            }
        });

        // Śledzenie lokalizacji
        locationData = new LocationData(this);
        locationData.setCallback(new LocationData.OnLocationChangedCallback() {
            @Override
            public void onLocationChanged(Location location) {
                updateMap(location);
                fixStatus.setBackgroundColor(getResources().getColor(R.color.green));
                gpsStatus.setText(R.string.statusOn);
            }

            @Override
            public void onGpsDisabled() {
                fixStatus.setBackgroundColor(getResources().getColor(R.color.red));
                gpsStatus.setText(R.string.statusOff);
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
    }

    private void updateMap(Location location) {
        GeoPoint newPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapController.animateTo(newPoint);
        mapController.setZoom(21.5);

        //Marker
        if (locationMarker == null) {
            locationMarker = new Marker(map);
            locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        }
        Drawable icon = ContextCompat.getDrawable(this, R.drawable.arrow);
        icon.setBounds(0, 0, 16, 16);
        locationMarker.setIcon(icon);

        // Przypisywanie pozycji
        if (lastLocation == null) {
            lastLocation = newPoint;
            locationMarker.setPosition(newPoint);
            map.invalidate();
            return;
        }

        animateMarker(lastLocation, newPoint);
        lastLocation = newPoint;

        map.getOverlays().add(locationMarker);
        map.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        headingAngle.start();
        if (locationData.hasLocationPermission()) {
            locationData.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
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
                Toast.makeText(this, "Brak uprawnień do lokalizacji", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Implementacja nieużywanych metod LocationListener (opcjonalnie usuń, jeśli nie używasz)
    @Override public void onLocationChanged(Location location) {}
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(String provider) {
        gpsStatus.setText(R.string.statusOn);
    }
    @Override public void onProviderDisabled(String provider) {
        gpsStatus.setText(R.string.statusOff);
    }
}
