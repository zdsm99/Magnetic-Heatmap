package com.example.magneticheatmap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

public class MenuController {
    private final Context context;
    private final FragmentActivity activity;

    // Widoki przekazane przez konstruktor
    private final LinearLayout measureBtn;
    private final TextView measureText;
    private final LinearLayout saveMeasurementBtn;
    private final TextView saveMeasurementText;
    private final LinearLayout centerMapBtn;
    private final LinearLayout followLocationBtn;
    private final View rootView;
    private final FrameLayout infoPanel;
    private final TextView closeInfoPanelBtn;
    private final TextView textPosition;
    private final TextView textSystem;
    private final TextView textMagnetValue;

    // AppManager komponenty
    private final MapController mapController;
    private GridDrawer gridDrawer;

    public MenuController(Context context) {
        this.activity = (FragmentActivity) context;
        this.rootView = ((Activity) context).findViewById(android.R.id.content);
        this.context = context;

        //---BUTTONS---

        // Przycisk do wykonywania pomiarów
        this.measureBtn = rootView.findViewById(R.id.measurement);
        this.measureText = rootView.findViewById(R.id.measurementText);
        // Przycisk zapisujący
        this.saveMeasurementBtn = rootView.findViewById(R.id.save_measurement);
        this.saveMeasurementText = rootView.findViewById(R.id.save_measurementText);
        // Przycisk centrujacy
        this.centerMapBtn = rootView.findViewById(R.id.center_map);
        // Przycisk śledzący pozycję
        this.followLocationBtn = rootView.findViewById(R.id.follow_position);

        //---TEXT-VIEWS---

        // Informacje o punkcie
        this.textPosition = rootView.findViewById(R.id.textPosition);
        this.textSystem = rootView.findViewById(R.id.textCoordinateSystem);
        this.textMagnetValue = rootView.findViewById(R.id.textMagnetometer);

        // Panel informacyjny
        this.infoPanel = rootView.findViewById(R.id.infoPanel);
        closeInfoPanelBtn = rootView.findViewById(R.id.closePanel);
        this.infoPanel.setVisibility(View.GONE);
        infoPanel.setOnClickListener(v -> {});


        // Wczytanie obecnych instancje klas
        mapController = AppController.getInstance().getMapController();
        gridDrawer = AppController.getInstance().getGridDrawer();


        //---NASŁUCHIWANIE PRZYCISKÓW---

        //
        measureBtn.setOnClickListener(v -> {
            if (AppController.getInstance().getIsFixedPosition()) {
                if (AppController.getInstance().getIsGridDrowerEnabled()) this.pauseMeasurement();
                else this.startMeasurement();
            } else Toast.makeText(this.context, context.getString(R.string.noPosition), Toast.LENGTH_SHORT).show();
        });

        // Zapisz pomiar
        saveMeasurementBtn.setOnClickListener(v -> {
            if (AppController.getInstance().getGridDrawer() != null) this.saveMeasurement();
            else  Toast.makeText(this.context, context.getString(R.string.noMeasurements), Toast.LENGTH_SHORT).show();
        });

        // Centrowanie mapy
        centerMapBtn.setOnClickListener(v -> {
            if (AppController.getInstance().getIsFixedPosition()) mapController.centerMap();
            else  Toast.makeText(this.context, context.getString(R.string.noPosition), Toast.LENGTH_SHORT).show();
        });

        // Śledzenie pozycji
        followLocationBtn.setOnClickListener(v -> mapController.followLocation());

        // Zamykanie infoPanel
        closeInfoPanelBtn.setOnClickListener(v -> {
            hideInfoPanel();
            mapController.followLocationOn();
        });
    }

    public void startMeasurement() {
        gridDrawer = AppController.getInstance().getGridDrawer();
        if (gridDrawer == null) {
            gridDrawer = new GridDrawer();

            // Aktualizujemy instancję w AppControllerze
            AppController.getInstance().setGridDrawer(gridDrawer);
            Toast.makeText(context, context.getString(R.string.startedMeasurements), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, context.getString(R.string.resumedMeasurements), Toast.LENGTH_SHORT).show();
        }

        AppController.getInstance().setIsGridDrawerEnabled(true);
        saveMeasurementText.setTextColor(context.getColor(R.color.t_red));
        measureBtn.setBackgroundColor(context.getColor(R.color.t_red));
        measureText.setText(context.getString(R.string.pauseBtn));
    }

    public void pauseMeasurement() {
        AppController.getInstance().setIsGridDrawerEnabled(false);
        measureBtn.setBackgroundColor(context.getColor(R.color.t_yellow));
        measureText.setText(context.getString(R.string.resumeBtn));
        Toast.makeText(context, context.getString(R.string.pausedMeasurements), Toast.LENGTH_SHORT).show();
    }

    public void saveMeasurement() {
        pauseMeasurement();
        showSaveDialog();

    }

    public void showSaveDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme);
            LayoutInflater inflater = LayoutInflater.from(context);
            View dialogView = inflater.inflate(R.layout.dialog_save_file, null);
            builder.setView(dialogView);

            EditText input = dialogView.findViewById(R.id.inputFileName);

            builder.setPositiveButton(context.getString(R.string.saveBtn), (dialog, which) -> {
                String fileName = input.getText().toString().trim();
                if (!fileName.endsWith(".csv")) {
                    fileName += ".csv";
                }
                gridDrawer.exportGridToCsv(context, fileName);
            });

            builder.setNegativeButton(context.getString(R.string.cancel), (dialog, which) -> dialog.cancel());

            builder.show();

        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.error) + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    // ====== OBSŁUGA PANELU INFORMACYJNEGO ======

    public void showInfoPanel(String lon, String lat,String system, String magnetValue, String  magneticCount) {
        String posText = activity.getString(R.string.lat) + "\t" + lat + "\n" + activity.getString(R.string.lon) + "\t" + lon;
        String sysText = activity.getString(R.string.coordinateSystem) + system;
        String magnetText = activity.getString(R.string.magentoReading) + magnetValue + "(" + magneticCount + ")";

        textPosition.setText(posText);
        textSystem.setText(sysText);
        textMagnetValue.setText(magnetText);

        if (infoPanel.getVisibility() != View.VISIBLE) {
            infoPanel.setAlpha(0f);
            infoPanel.setVisibility(View.VISIBLE);
            infoPanel.animate().alpha(1f).translationY(0).setDuration(300).start();
        }
        infoPanel.setOnClickListener(v -> {
        });
    }

    public void hideInfoPanel() {
        if (infoPanel.getVisibility() == View.VISIBLE) {
            infoPanel.animate().alpha(0f).translationY(infoPanel.getHeight()).setDuration(300)
                    .withEndAction(() -> infoPanel.setVisibility(View.GONE)).start();
        }
    }

    public boolean isInfoPanelVisible() {
        return infoPanel.getVisibility() == View.VISIBLE;
    }
}
