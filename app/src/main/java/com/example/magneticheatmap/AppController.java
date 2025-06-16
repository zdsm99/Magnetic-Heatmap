package com.example.magneticheatmap;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;

public class AppController {
    private static AppController instance;
    private GridDrawer gridDrawer;
    private IMapController imapController;
    private MapController mapController;
    private MenuController menuController;
    private GetSensorsValues sensorsValues;
    private LocationData locationData;
    private GeoPoint lastLocation;

    private double gridSize = 1.0;
    private double defaultZoom = 23.0;
    private long animationDuration = 1000;
    private double startZoom = 7.4;
    private double startLat = 52.1;
    private double startLon = 19.2;
    private String language = "pl";

    private boolean isGridDrawerEnabled = false;
    private boolean isFixedPosition = false;
    private boolean isFollowLocationEnable = true;

    private AppController() {}

    public static AppController getInstance() {
        if (instance == null) {
            instance = new AppController();
        }
        return instance;
    }


    public void setGridDrawer(GridDrawer gridDrawer) {
        this.gridDrawer = gridDrawer;
    }

    public void setIMapController(IMapController imapController) {
        this.imapController = imapController;
    }
    public void setMapController(MapController mapController) {
        this.mapController = mapController;
    }

    public void setMenuController(MenuController menuController) {
        this.menuController = menuController;
    }

    public void setSensorsValues(GetSensorsValues sensorsValues) {
        this.sensorsValues = sensorsValues;
    }

    public void setLocationData(LocationData locationData) {
        this.locationData = locationData;
    }
    public void setIsFixedPossition(Boolean isFixedPosition){
        this.isFixedPosition = isFixedPosition;
    }
    public void setIsGridDrawerEnabled(Boolean isGridDrawerEnabled){
        this.isGridDrawerEnabled = isGridDrawerEnabled;
    }
    public void setIsFollowLocationEnable(Boolean isFollowLocationEnable){
        this.isFollowLocationEnable = isFollowLocationEnable;
    }
    public void setLastLocation(GeoPoint lastLocation){
        this.lastLocation = lastLocation;
    }
    public void setStartZoom(double startZoom){
        this.startZoom = startZoom;
    }
    public void setStartLat(double startLat){
        this.startLat = startLat;
    }
    public void setStartLon(double startLon){
        this.startLon = startLon;
    }
    public void setGridSize(double gridSize){
        this.gridSize = gridSize;
    }
    public void setDefaultZoom(double defaultZoom){
        this.defaultZoom = defaultZoom;
    }
    public void setAnimationDuration(long animationDuration){
        this.animationDuration = animationDuration;
    }
    public void setLanguage(String language){
        this.language = language;
    }

    public GridDrawer getGridDrawer() { return gridDrawer; }
    public MapController getMapController() { return mapController; }
    public IMapController getIMapController() { return imapController; }
    public MenuController getMenuController() { return menuController; }
    public GetSensorsValues getSensorsValues() { return sensorsValues; }

    public LocationData getLocationData() { return locationData; }
    public Boolean getIsFixedPosition(){ return isFixedPosition; }
    public Boolean getIsGridDrowerEnabled(){ return isGridDrawerEnabled; }
    public Boolean getIsFollowLocationEnable(){ return isFollowLocationEnable; }
    public GeoPoint getLastLocation(){ return lastLocation; }
    public double getGridSize(){ return gridSize; }
    public double getDefaultZoom(){ return defaultZoom; }
    public double getStartZoom(){ return startZoom; }
    public double getStartLat(){ return startLat; }
    public double getStartLon(){ return startLon; }
    public long getAnimationDuration(){ return animationDuration; }
    public String getLanguage(){ return language; }

    public void clearContextSensitiveData() {
        this.mapController = null;
        this.menuController = null;
        this.imapController = null;
        this.gridDrawer = null;
    }
}
