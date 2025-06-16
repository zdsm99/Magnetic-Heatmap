package com.example.magneticheatmap;

import android.app.LauncherActivity;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.slider.Slider;

import java.util.Locale;

public class Settings extends AppCompatActivity {
    String  setLanguage = "pl";
    boolean isFromSlider = false; // żeby uniknąć zapętlenia
    boolean isFromEditText = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        ImageButton backBtn = findViewById(R.id.backBtn);

        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        RadioGroup radioLang = findViewById(R.id.radioLang);
        String currentLang = AppController.getInstance().getLanguage();
        if ("pl".equals(currentLang)) {
            radioLang.check(R.id.radioLangPol);
        } else if ("en".equals(currentLang)) {
            radioLang.check(R.id.radioLangEng);
        }
        radioLang.setOnCheckedChangeListener((group, checkedId) -> {
            if(checkedId == R.id.radioLangPol){
                setLanguage = "pl";
                AppController.getInstance().setLanguage("pl");
            } else if (checkedId == R.id.radioLangEng) {
                setLanguage = "en";
                AppController.getInstance().setLanguage("en");
            }
            MainActivity.setLocale(Settings.this, setLanguage);
            AppController.getInstance().clearContextSensitiveData();
            AppController.getInstance().setIsGridDrawerEnabled(false);
            Intent intent = new Intent(Settings.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        EditText gridSizeNum = findViewById(R.id.gridSizeNum);
        EditText startZoomNum = findViewById(R.id.startZoomNum);
        EditText defaultZoomNum = findViewById(R.id.defaultZoomNum);

        Slider gridSizeSlider = findViewById(R.id.gridSizeSlider);
        Slider startZoomSlider = findViewById(R.id.startZoomSlider);
        Slider defaultZoomSlider = findViewById(R.id.defaultZoomSlider);

        gridSizeSlider.setValue((float) (AppController.getInstance().getGridSize()));
        startZoomSlider.setValue((float) (AppController.getInstance().getStartZoom()));
        defaultZoomSlider.setValue((float) (AppController.getInstance().getDefaultZoom()));

        gridSizeNum.setText(String.format(Locale.US,"%.1f",AppController.getInstance().getGridSize()));
        startZoomNum.setText(String.format(Locale.US,"%.1f",AppController.getInstance().getStartZoom()));
        defaultZoomNum.setText(String.format(Locale.US,"%.1f",AppController.getInstance().getDefaultZoom()));


        // Nasłuchiwanie Sliderów
        gridSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if(!isFromEditText){
                isFromSlider = true;
                double currentGridSize = gridSizeSlider.getValue();
                AppController.getInstance().setGridSize(currentGridSize);
                gridSizeNum.setText(String.format(Locale.US,"%.1f", currentGridSize));
                isFromSlider = false;
            }
        });

        startZoomSlider.addOnChangeListener((slider, value, fromUser) -> {
            if(!isFromEditText){
                isFromSlider = true;
                double currentStartZoom = startZoomSlider.getValue();
                AppController.getInstance().setStartZoom(currentStartZoom);
                startZoomNum.setText(String.format(Locale.US,"%.1f", currentStartZoom));
                isFromSlider = false;
            }
        });
        defaultZoomSlider.addOnChangeListener((slider, value, fromUser) -> {
            if(!isFromEditText){
                isFromSlider = true;
                double currentDefaultZoom = defaultZoomSlider.getValue();
                AppController.getInstance().setDefaultZoom(currentDefaultZoom);
                defaultZoomNum.setText(String.format(Locale.US,"%.1f", currentDefaultZoom));
                isFromSlider = false;
            }
        });

        gridSizeNum.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isFromSlider) {
                    isFromEditText = true;
                    try {
                        float newValue = Float.parseFloat(s.toString());
                        float min = gridSizeSlider.getValueFrom();
                        float max = gridSizeSlider.getValueTo();

                        // ograniczamy wartość do zakresu slidera
                        if (newValue >= min && newValue <= max) {
                            gridSizeSlider.setValue(newValue);
                        }
                    } catch (NumberFormatException e) {
                        // ignoruj błędy parsowania (np. puste pole)
                    }

                    isFromEditText = false;
                }
            }
        });

        startZoomNum.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isFromSlider) {
                    isFromEditText = true;
                    try {
                        float newValue = Float.parseFloat(s.toString());
                        float min = startZoomSlider.getValueFrom();
                        float max = startZoomSlider.getValueTo();

                        // ograniczamy wartość do zakresu slidera
                        if (newValue >= min && newValue <= max) {
                            startZoomSlider.setValue(newValue);
                        }
                        else{
                            startZoomNum.setError("Wrtość poza zakresem");
                        }
                    } catch (NumberFormatException e) {

                    }

                    isFromEditText = false;
                }
            }
        });

        defaultZoomNum.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isFromSlider) {
                    isFromEditText = true;
                    try {
                        float newValue = Float.parseFloat(s.toString());
                        float min = defaultZoomSlider.getValueFrom();
                        float max = defaultZoomSlider.getValueTo();

                        // ograniczamy wartość do zakresu slidera
                        if (newValue >= min && newValue <= max) {
                            defaultZoomSlider.setValue(newValue);
                        }
                    } catch (NumberFormatException e) {
                        // ignoruj błędy parsowania (np. puste pole)
                    }

                    isFromEditText = false;
                }
            }
        });

        Button saveBtn = findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(MainActivity.updateBaseContextLocale(newBase, MainActivity.getSavedLanguage(newBase)));
    }




}