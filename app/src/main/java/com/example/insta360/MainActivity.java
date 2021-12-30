package com.example.insta360;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.insta360.activity.BaseObserveCameraActivity;
import com.example.insta360.activity.CaptureActivity;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

public class MainActivity extends BaseObserveCameraActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkStoragePermission();
        if (InstaCameraManager.getInstance().getCameraConnectedType() != InstaCameraManager.CONNECT_TYPE_NONE) {
            onCameraStatusChanged(true);
        }
        findViewById(R.id.btn_connect_by_wifi).setOnClickListener(v -> {
            InstaCameraManager.getInstance().openCamera(InstaCameraManager.CONNECT_TYPE_WIFI);

        });
        findViewById(R.id.btn_capture).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CaptureActivity.class));
        });
    }
    @Override
    public void onCameraConnectError() {
        super.onCameraConnectError();
        findViewById(R.id.btn_capture).setEnabled(true);
        Toast.makeText(this, "Error while connectin Camera", Toast.LENGTH_SHORT).show();

    }
    @Override
    public void onCameraStatusChanged(boolean enabled) {
        super.onCameraStatusChanged(enabled);
        findViewById(R.id.btn_capture).setEnabled(enabled);
    }
    private void checkStoragePermission() {
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.STORAGE)
                .onDenied(permissions -> {
                    if (AndPermission.hasAlwaysDeniedPermission(this, permissions)) {
                        AndPermission.with(this)
                                .runtime()
                                .setting()
                                .start(1000);
                    }
                    finish();
                })
                .start();
    }
}