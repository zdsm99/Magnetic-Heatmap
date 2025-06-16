package com.example.magneticheatmap;


import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Environment;

import android.widget.Toast;


import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;



public class GridDrawer {
    private static final int RASTER_SIZE = 1000;
    private static final int CENTER_INDEX = RASTER_SIZE / 2;
    private static final double METERS_PER_DEGREE = 111_000.0; // zgrubnie, dla szerokości
    private final GridCell[][] grid = new GridCell[RASTER_SIZE][RASTER_SIZE];
    private final Polygon[][] polygons = new Polygon[RASTER_SIZE][RASTER_SIZE];
    private Polygon selectionFramePolygon;
    private double centerLat;
    private double centerLon;
    private boolean initialized = false;

    private final MapView map;
    private final MenuController menuController;
    private final MapController mapController;
    private final IMapController imapController;

    private final String coordinateSystem = "WGS 84";

    public GridDrawer() {


        mapController = AppController.getInstance().getMapController();
        imapController = AppController.getInstance().getIMapController();
        menuController =  AppController.getInstance().getMenuController();
        this.map = mapController.getCurrentMap();
    }

    public void addDataPoint(double lat, double lon, double value) {

        if (!initialized) {
            centerLat = lat;
            centerLon = lon;
            initialized = true;
        }

        double distanceMeters = getDistanceMeters(lat, lon, centerLat, centerLon);
        if (distanceMeters > 400) {
            resetRaster(lat, lon);
        }

        int y = latToIndex(lat);
        int x = lonToIndex(lon);

        if (x < 0 || x >= RASTER_SIZE || y < 0 || y >= RASTER_SIZE) return;

        if (grid[x][y] == null) grid[x][y] = new GridCell();
        grid[x][y].add(value);

        // Zabezpieczenie: nie aktualizuj jeśli mapa niegotowa
        if (map != null && map.getWidth() > 0) {
            updatePolygon(x, y);
        }
    }

    private double getDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        float[] result = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, result);
        return result[0];
    }

    private void resetRaster(double newLat, double newLon) {
        for (int x = 0; x < RASTER_SIZE; x++) {
            for (int y = 0; y < RASTER_SIZE; y++) {
                grid[x][y] = null;
                if (polygons[x][y] != null) {
                    map.getOverlays().remove(polygons[x][y]);
                    polygons[x][y] = null;
                }
            }
        }
        centerLat = newLat;
        centerLon = newLon;
    }

    private void updatePolygon(int x, int y) {
        if (map == null || grid[x][y] == null) return;

        double lat = centerLat + ((y - CENTER_INDEX) / METERS_PER_DEGREE);
        double lon = centerLon + ((x - CENTER_INDEX) / (METERS_PER_DEGREE * Math.cos(Math.toRadians(centerLat))));

        double gridSize = AppController.getInstance().getGridSize();
        double deltaLat = gridSize / METERS_PER_DEGREE;
        double metersPerDegreeLon = METERS_PER_DEGREE * Math.cos(Math.toRadians(centerLat));
        double deltaLon = gridSize / metersPerDegreeLon;

        ArrayList<GeoPoint> corners = new ArrayList<>();
        corners.add(new GeoPoint(lat - deltaLat / 2, lon - deltaLon / 2));
        corners.add(new GeoPoint(lat + deltaLat / 2, lon - deltaLon / 2));
        corners.add(new GeoPoint(lat + deltaLat / 2, lon + deltaLon / 2));
        corners.add(new GeoPoint(lat - deltaLat / 2, lon + deltaLon / 2));

        Polygon p = polygons[x][y];

        if (p == null) {
            // Nowy poligon
            try {
                p = new Polygon(map);
                p.setStrokeColor(Color.TRANSPARENT);
                p.setInfoWindow(null);
                p.setPoints(corners); // set before adding to map
                p.setFillColor(getColor(grid[x][y].getAvg()));
                map.getOverlays().add(p);
                polygons[x][y] = p;

                setupPolygonClick(p, grid[x][y], new GeoPoint(lat, lon));
            } catch (Exception e) {
                e.printStackTrace(); // loguj, jeśli coś pójdzie nie tak
            }
        } else {
            // Aktualizacja istniejącego
            try {
                p.setPoints(corners);
                p.setFillColor(getColor(grid[x][y].getAvg()));
            } catch (Exception e) {
                e.printStackTrace(); // ważne przy debugowaniu
            }
        }
    }

    private void setupPolygonClick(Polygon polygon, GridCell cell, GeoPoint location) {
        polygon.setOnClickListener((poly, mapView, eventPos) -> {
            if (cell != null && cell.count > 0) {
                String lon = String.format(Locale.US, "%.5f", location.getLongitude());
                String lat = String.format(Locale.US, "%.5f", location.getLatitude());

                String magnetValue = String.format(Locale.US, "%.1f µT ", cell.getAvg());
                String magneticCount = String.format(Locale.US, "%d",cell.count);
                menuController.showInfoPanel(lon, lat, coordinateSystem, magnetValue, magneticCount);
                drawBoundingBoxAroundPolygon(lonToIndex(location.getLongitude()), latToIndex(location.getLatitude()));
                mapController.followLocationOf();
            }
            return true;
        });
    }

    public void drawBoundingBoxAroundPolygon(int x, int y) {
        if (polygons[x][y] == null) return;

        // 1. Usuń poprzednią ramkę (z animacją zanikania)
        if (selectionFramePolygon != null) {
            fadeOutAndRemove(selectionFramePolygon);
            selectionFramePolygon = null;
        }

        // 2. Wyznacz granice klikniętego poligonu
        Polygon originalPolygon = polygons[x][y];
        List<GeoPoint> points = originalPolygon.getActualPoints();
        if (points == null || points.size() < 4) return;

        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

        for (GeoPoint pt : points) {
            double lat = pt.getLatitude();
            double lon = pt.getLongitude();
            if (lat < minLat) minLat = lat;
            if (lat > maxLat) maxLat = lat;
            if (lon < minLon) minLon = lon;
            if (lon > maxLon) maxLon = lon;
        }

        // 3. Stwórz ramkę
        ArrayList<GeoPoint> framePoints = new ArrayList<>();
        framePoints.add(new GeoPoint(maxLat, minLon)); // NW
        framePoints.add(new GeoPoint(maxLat, maxLon)); // NE
        framePoints.add(new GeoPoint(minLat, maxLon)); // SE
        framePoints.add(new GeoPoint(minLat, minLon)); // SW
        framePoints.add(new GeoPoint(maxLat, minLon)); // zamknięcie obwodu

        Polygon frame = new Polygon(map);
        frame.setPoints(framePoints);
        frame.setStrokeWidth(4f);
        frame.setStrokeColor(Color.BLACK);
        frame.setFillColor(Color.TRANSPARENT);
        frame.setInfoWindow(null);

        selectionFramePolygon = frame;
        map.getOverlays().add(selectionFramePolygon);
        mapController.refreshMap();

        // 4. Animacja pojawiania się (fade-in)
        fadeIn(selectionFramePolygon);

        // 5. Centrum mapy
        double centerLat = (minLat + maxLat) / 2.0;
        double centerLon = (minLon + maxLon) / 2.0;
        map.getController().animateTo(new GeoPoint(centerLat, centerLon));

        double defaultZoom = AppController.getInstance().getDefaultZoom();
        imapController.setZoom(Math.max(map.getZoomLevelDouble(), defaultZoom));
    }
    private void fadeIn(Polygon polygon) {
        final ValueAnimator animator = ValueAnimator.ofInt(0, 255);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int alpha = (int) animation.getAnimatedValue();
            polygon.setStrokeColor(Color.argb(alpha, 0, 0, 0)); // czarna ramka z przezroczystością
            mapController.refreshMap();
        });
        animator.start();
    }

    private void fadeOutAndRemove(Polygon polygon) {
        final ValueAnimator animator = ValueAnimator.ofInt(255, 0);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int alpha = (int) animation.getAnimatedValue();
            polygon.setStrokeColor(Color.argb(alpha, 0, 0, 0));
            mapController.refreshMap();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                map.getOverlays().remove(polygon);
                mapController.refreshMap();
            }
        });
        animator.start();
    }

    public int getColor (double value){
        // Zakres: 20 - 80 µT jako przykład
        float ratio = (float) ((value - 20) / 60.0);
        ratio = Math.max(0f, Math.min(1f, ratio)); // 0.0 - 1.0

        // Gradient od zielonego do czerwonego
        return Color.HSVToColor(new float[]{
                (1 - ratio) * 120, // odcień
                1f,
                1f
        });
    }



    public int latToIndex(double lat) {
        return CENTER_INDEX + (int) ((lat - centerLat) * METERS_PER_DEGREE);
    }

    public int lonToIndex(double lon) {
        double metersPerDegreeLon = METERS_PER_DEGREE * Math.cos(Math.toRadians(centerLat));
        return CENTER_INDEX + (int) ((lon - centerLon) * metersPerDegreeLon);
    }

    public double indexToLat(int y) {
        return centerLat + y * AppController.getInstance().getGridSize() * 0.00001;
    }

    public double indexToLon(int x) {
        return centerLon + x * AppController.getInstance().getGridSize() * 0.00001;
    }
    // Eksport do pliku csV
    public void exportGridToCsv(Context context, String fileName) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File myFolder = new File(downloadsDir , context.getString(R.string.magneticheatmap));
        if (!myFolder.exists()) {
            myFolder.mkdirs();
        }
        File file = new File(myFolder, fileName);
        try {
            FileWriter writer = new FileWriter(file);
            writer.append("lat,lon,avg,count\n");

            for (int x = 0; x < RASTER_SIZE; x++) {
                for (int y = 0; y < RASTER_SIZE; y++) {
                    GridCell cell = grid[x][y];
                    if (cell != null && cell.count > 0) {
                        double lat = indexToLat(y); // y to oś północ–południe
                        double lon = indexToLon(x); // x to oś wschód–zachód
                        double avg = cell.getAvg();
                        int count = cell.count;

                        writer.append(lat + "," + lon + "," + avg + "," + count + "\n");
                    }
                }
            }

            writer.flush();
            writer.close();

            Toast.makeText(context, context.getString(R.string.saveTo) + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.saveError) + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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

