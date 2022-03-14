package com.example.insta360;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.ICameraChangedCallback;

public class BaseObserveCameraActivity extends AppCompatActivity implements ICameraChangedCallback {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InstaCameraManager.getInstance().registerCameraChangedCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        InstaCameraManager.getInstance().unregisterCameraChangedCallback(this);
    }

    /**
     * Camera status changed
     *
     * @param enabled Whether the camera is available
     */
    @Override
    public void onCameraStatusChanged(boolean enabled) {
    }

    /**
     * Camera connection failed
     * <p>
     * A common situation is that other phones or other applications of this phone have already
     * established a connection with this camera, resulting in this establishment failure,
     * and other phones need to disconnect from this camera first.
     */
    @Override
    public void onCameraConnectError() {
    }

    /**
     * SD card insertion notification
     *
     * @param enabled Whether the current SD card is available
     */
    @Override
    public void onCameraSDCardStateChanged(boolean enabled) {
    }

    /**
     * SD card storage status changed
     *
     * @param freeSpace  Currently available size
     * @param totalSpace Total size
     */
    @Override
    public void onCameraStorageChanged(long freeSpace, long totalSpace) {
    }

    /**
     * Low battery notification
     */
    @Override
    public void onCameraBatteryLow() {
    }

    /**
     * Camera power change notification
     *
     * @param batteryLevel Current power (0-100, always returns 100 when charging)
     * @param isCharging   Whether the camera is charging
     */
    @Override
    public void onCameraBatteryUpdate(int batteryLevel, boolean isCharging) {
    }

    /**
     * Just for OneX2, when change its camera sensor
     *
     * @param cameraSensorMode equals to InstaCameraManager.getInstance().getCurrentCameraMode();
     */
    @Override
    public void onCameraSensorModeChanged(int cameraSensorMode) {
    }
}


