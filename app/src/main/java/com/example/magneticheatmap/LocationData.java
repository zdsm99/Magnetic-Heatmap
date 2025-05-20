package com.example.magneticheatmap;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

public class LocationData implements LocationListener{
    public interface OnLocationChangedCallback {
        void onLocationChanged(Location location);
        void onGpsDisabled(); // np. pokaż alert
    }
    private final Context context;
    private final LocationManager locationManager;
    private Location lastLocation;
    private OnLocationChangedCallback callback;

    public LocationData(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void setCallback(OnLocationChangedCallback callback) {
        this.callback = callback;
    }

    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isLocationEnabled() {
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void requestEnableGpsIfNeeded() {
        if (!isLocationEnabled()) {
            if (context instanceof android.app.Activity) {
                new AlertDialog.Builder(context)
                        .setTitle("Lokalizacja wyłączona")
                        .setMessage("Włącz lokalizację, aby korzystać z aplikacji.")
                        .setPositiveButton("Włącz", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            context.startActivity(intent);
                        })
                        .setNegativeButton("Anuluj", null)
                        .setCancelable(false)
                        .show();
            }

            if (callback != null) {
                callback.onGpsDisabled();
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    public void start() {
        if (hasLocationPermission() && locationManager != null) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    1,
                    this
            );
        }
    }

    public void stop() {
        locationManager.removeUpdates(this);
    }
    public double getLat() {
        return (lastLocation != null) ? lastLocation.getLatitude() : 0.0;
    }

    public double getLon() {
        return (lastLocation != null) ? lastLocation.getLongitude() : 0.0;
    }

    @Override
    public void onLocationChanged(Location location) {
        this.lastLocation = location;
        if (callback != null) {
            callback.onLocationChanged(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(String provider) {}
    @Override
    public void onProviderDisabled(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider) && callback != null) {
            callback.onGpsDisabled();
        }
    }


}

