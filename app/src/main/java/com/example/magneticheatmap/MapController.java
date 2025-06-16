package com.example.magneticheatmap;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;


public class MapController {
    //private final View rootView;
    private final IMapController imapController;
    private GetSensorsValues headingAngle;
    private final Context context;
    private final MapView map;
    private Marker locationMarker;
    private final LinearLayout followLocationBtn
;


    public MapController(Context context){
        View rootView = ((Activity) context).findViewById(android.R.id.content);
        this.context = context;
        // Pobieranie ustawień
        double startZoom = AppController.getInstance().getStartZoom();
        double startLat = AppController.getInstance().getStartLat();
        double startLon = AppController.getInstance().getStartLon();

        // Inicjalizacja mapy i kontrolera
        map = rootView.findViewById(R.id.map);
        map.setMultiTouchControls(true);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(false); // wyłaczenie przycisków zoomowania ([+][-])
        imapController = map.getController();
        imapController.setZoom(startZoom);
        imapController.setCenter(new GeoPoint(startLat, startLon));
        AppController.getInstance().setIMapController(imapController);
        followLocationBtn = rootView.findViewById(R.id.follow_position);
        headingAngle = AppController.getInstance().getSensorsValues();
    }
    public void centerMap() {
        GeoPoint location = AppController.getInstance().getLastLocation();
        double zoom = Math.max(AppController.getInstance().getDefaultZoom(), map.getZoomLevelDouble());
        if (location != null) {
            imapController.animateTo(location);
            imapController.setZoom(zoom);
        }
    }

    public void animateMarker(GeoPoint start, GeoPoint end) {
        long duration = AppController.getInstance().getAnimationDuration();
        boolean isGridEnabled = AppController.getInstance().getIsGridDrowerEnabled();
        GridDrawer gridDrawer = AppController.getInstance().getGridDrawer();
        long startTime = System.currentTimeMillis();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                float t = Math.min(1f, (float)(System.currentTimeMillis() - startTime) / duration);
                double lat = start.getLatitude() + t * (end.getLatitude() - start.getLatitude());
                double lon = start.getLongitude() + t * (end.getLongitude() - start.getLongitude());

                locationMarker.setPosition(new GeoPoint(lat, lon));
                headingAngle = AppController.getInstance().getSensorsValues();
                if (isGridEnabled && gridDrawer != null && headingAngle  != null) {
                    gridDrawer.addDataPoint(lat, lon, headingAngle.getMagneticValue());
                }
                refreshMap();

                if (t < 1f) {
                    new Handler().postDelayed(this, 16);
                }
            }
        });
    }
    public void updateMarkerPosition(Location location) {
        GeoPoint newPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (locationMarker == null) {
            locationMarker = new Marker(map);
            locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            locationMarker.setIcon(ContextCompat.getDrawable(context, R.drawable.arrow));
            map.getOverlays().add(locationMarker);
            imapController.animateTo(newPoint);
            double defaultZoom = AppController.getInstance().getDefaultZoom();
            imapController.setZoom(defaultZoom);
        }

        GeoPoint lastPoint = AppController.getInstance().getLastLocation();
        if (lastPoint == null) lastPoint = newPoint;

        if (AppController.getInstance().getIsFollowLocationEnable()) {
            imapController.animateTo(newPoint);
        }

        locationMarker.setInfoWindow(null); // Wyłaczenie domyślnej chmurki

        // Animacja markera i dodanie punktu do siatki
        animateMarker(lastPoint, newPoint);

        AppController.getInstance().setLastLocation(newPoint);
    }
    public void followLocation() {
        Boolean isFollow = AppController.getInstance().getIsFollowLocationEnable();
        AppController.getInstance().setIsFollowLocationEnable(!isFollow);

        int colorRes = !isFollow ? R.color.t_blue : R.color.t_grey;
        followLocationBtn
.setBackgroundColor(context.getColor(colorRes));
    }
    public void followLocationOn() {
        AppController.getInstance().setIsFollowLocationEnable(true);
        followLocationBtn
.setBackgroundColor(context.getColor(R.color.t_blue));
    }
    public void followLocationOf() {
        AppController.getInstance().setIsFollowLocationEnable(false);
        followLocationBtn
.setBackgroundColor(context.getColor(R.color.t_grey));
    }

    public Marker getLocationMarker() {
        return locationMarker;
    }
    public MapView getCurrentMap(){
        return map;
    }
    public void refreshMap(){
        map.invalidate();
    }
}
