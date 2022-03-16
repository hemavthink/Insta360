package com.example.insta360;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

public class MainActivity extends AppCompatActivity {

    private ImageView camerIcon;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camerIcon = findViewById(R.id.camerIcon);
        checkStoragePermission();
        camerIcon.setOnClickListener(new View.OnClickListener() {
            //@Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Insta360.class));
            }
        });
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