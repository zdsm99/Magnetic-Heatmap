package com.example.magneticheatmap;

import android.graphics.Color;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.HashMap;

public class GridOverlayManager {

    private final MapView mapView;
    private final double gridSize;
    private final HashMap<String, GridCell> grid = new HashMap<>();
    private final HashMap<String, Polygon> polygons = new HashMap<>();

    public GridOverlayManager(MapView mapView, double gridSize) {
        this.mapView = mapView;
        this.gridSize = gridSize;
    }

    public void addDataPoint(double lat, double lon, double value) {
        int latIndex = (int)(lat / gridSize);
        int lonIndex = (int)(lon / gridSize);
        String key = latIndex + "_" + lonIndex;

        // Dodaj do siatki
        grid.putIfAbsent(key, new GridCell());
        grid.get(key).add(value);

        // Oblicz rogi komórki
        double baseLat = latIndex * gridSize;
        double baseLon = lonIndex * gridSize;

        ArrayList<GeoPoint> corners = new ArrayList<>();
        corners.add(new GeoPoint(baseLat, baseLon));
        corners.add(new GeoPoint(baseLat + gridSize, baseLon));
        corners.add(new GeoPoint(baseLat + gridSize, baseLon + gridSize));
        corners.add(new GeoPoint(baseLat, baseLon + gridSize));

        // Stwórz lub zaktualizuj poligon
        if (polygons.containsKey(key)) {
            Polygon poly = polygons.get(key);
            poly.setFillColor(getColor(grid.get(key).getAvg()));
        } else {
            Polygon polygon = new Polygon(mapView);
            polygon.setPoints(corners);
            polygon.setStrokeColor(Color.TRANSPARENT);
            polygon.setFillColor(getColor(grid.get(key).getAvg()));
            polygon.setTitle("Val: " + String.format("%.1f", grid.get(key).getAvg()));
            polygons.put(key, polygon);
            mapView.getOverlays().add(polygon);
        }

        mapView.invalidate();
    }

//    private int getColor(double value) {
//        // Zakres: 20 - 80 µT jako przykład
//        float ratio = (float)((value - 20) / 60.0);
//        ratio = Math.max(0f, Math.min(1f, ratio)); // 0.0 - 1.0
//
//        // Gradient od niebieskiego (zimno) do czerwonego (ciepło)
//        return Color.HSVToColor(new float[]{
//                (1 - ratio) * 240, // odcień
//                1f,
//                1f
//        });
//    }
    private int getColor(double value){
        float minValue = 30;
        float maxValue = 100;
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

    // Pomocnicza klasa bufora wartości
    private static class GridCell {
        double sum = 0;
        int count = 0;

        void add(double value) {
            sum += value;
            count++;
        }

        double getAvg() {
            return (count == 0) ? 0 : sum / count;
        }
    }
}

