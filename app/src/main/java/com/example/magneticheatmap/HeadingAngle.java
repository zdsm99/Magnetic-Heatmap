package com.example.magneticheatmap;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class HeadingAngle implements SensorEventListener {

        private final SensorManager sensorManager;
        private final Sensor accelerometer;
        private final Sensor magnetometer;
        private final float[] gravity = new float[3];
        private final float[] geomagnetic = new float[3];
        private float heading;
        private float magneticValue = 0.0f;

        public interface OnHeadingChangedListener {
            void onHeadingChanged(float headingDegrees);
        }

        private final OnHeadingChangedListener listener;

        public HeadingAngle(Context context, OnHeadingChangedListener listener) {
            this.listener = listener;
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        public void start() {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }

        public void stop() {
            sensorManager.unregisterListener(this);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            final float[] R = new float[9];
            final float[] I = new float[9];

            if (event.sensor == accelerometer){
                System.arraycopy(event.values, 0, gravity, 0, gravity.length);
            }
            else if (event.sensor == magnetometer) {
                // Wartość pola magnetycznego
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                magneticValue = (float) Math.sqrt(x * x + y * y + z * z);
                System.arraycopy(event.values, 0, geomagnetic, 0, geomagnetic.length);
            }
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                heading = (float) Math.toDegrees(orientation[0]);

                if (heading < 0) heading += 360;

                if (listener != null) listener.onHeadingChanged(heading);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }

        public float getHeading() {
            return heading;
        }
        public double getMagneticValue(){
            return magneticValue;
        }
    }
