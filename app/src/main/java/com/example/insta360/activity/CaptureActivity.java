package com.example.insta360.activity;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.arashivision.insta360.basecamera.camera.CameraType;
import com.arashivision.sdk.demo.util.TimeFormat;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.ICaptureStatusListener;
import com.example.insta360.R;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.Response;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class CaptureActivity extends BaseObserveCameraActivity implements ICaptureStatusListener {

    private TextView mTvCaptureStatus;
    private TextView mTvCaptureTime;
    private TextView mTvCaptureCount;
    private Button mBtnPlayCameraFile;
    private Button mBtnPlayLocalFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        setTitle(R.string.capture_toolbar_title);
        bindViews();



        findViewById(R.id.btn_normal_capture).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                InstaCameraManager.getInstance().startNormalCapture(false);
            }
        });


        // Capture Status Callback
        InstaCameraManager.getInstance().setCaptureStatusListener(this);
    }

    private void bindViews() {
//        mTvCaptureStatus = findViewById(R.id.tv_capture_status);
//        mTvCaptureTime = findViewById(R.id.tv_capture_time);
//        mTvCaptureCount = findViewById(R.id.tv_capture_count);
//        mBtnPlayCameraFile = findViewById(R.id.btn_play_camera_file);
//        mBtnPlayLocalFile = findViewById(R.id.btn_play_local_file);
    }

    private boolean isOneX2() {
        return CameraType.getForType(InstaCameraManager.getInstance().getCameraType()) == CameraType.ONEX2;
    }

    private boolean supportInstaPanoCapture() {
        return isOneX2() && InstaCameraManager.getInstance().getCurrentCameraMode() == InstaCameraManager.CAMERA_MODE_PANORAMA;
    }

    private boolean checkSdCardEnabled() {
        if (!InstaCameraManager.getInstance().isSdCardEnabled()) {
            Toast.makeText(this, R.string.capture_toast_sd_card_error, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onCameraStatusChanged(boolean enabled) {
        super.onCameraStatusChanged(enabled);
        if (!enabled) {
            finish();
        }
    }

    @Override
    public void onCameraSensorModeChanged(int cameraSensorMode) {
        super.onCameraSensorModeChanged(cameraSensorMode);
//        findViewById(R.id.btn_normal_pano_capture).setEnabled(supportInstaPanoCapture());
//        findViewById(R.id.btn_hdr_pano_capture).setEnabled(supportInstaPanoCapture());
    }

    @Override
    public void onCaptureStarting() {
        mTvCaptureStatus.setText(R.string.capture_capture_starting);
        mBtnPlayCameraFile.setVisibility(View.GONE);
        mBtnPlayLocalFile.setVisibility(View.GONE);
    }

    @Override
    public void onCaptureWorking() {
        mTvCaptureStatus.setText(R.string.capture_capture_working);
    }

    @Override
    public void onCaptureStopping() {
        mTvCaptureStatus.setText(R.string.capture_capture_stopping);
    }

    @Override
    public void onCaptureFinish(String[] filePaths) {
        mTvCaptureStatus.setText(R.string.capture_capture_finished);
        mTvCaptureTime.setVisibility(View.GONE);
        mTvCaptureCount.setVisibility(View.GONE);
        if (filePaths != null && filePaths.length > 0) {
            File mediaFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());
            File filePath = new File(mediaFolder + File.separator
                    + "IMG_" + timeStamp + "_high.jpg");

            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(filePath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            mBtnPlayCameraFile.setVisibility(View.VISIBLE);
            mBtnPlayCameraFile.setOnClickListener(v -> {
                StitchActivity.launchActivity(this, filePaths);
            });
            mBtnPlayLocalFile.setVisibility(View.VISIBLE);
            mBtnPlayLocalFile.setOnClickListener(v -> {
                downloadFilesAndPlay(filePaths);
            });
        } else {
            mBtnPlayCameraFile.setVisibility(View.GONE);
            mBtnPlayCameraFile.setOnClickListener(null);
            mBtnPlayLocalFile.setVisibility(View.GONE);
            mBtnPlayLocalFile.setOnClickListener(null);
        }
    }

    @Override
    public void onCaptureTimeChanged(long captureTime) {
        mTvCaptureTime.setVisibility(View.VISIBLE);
        mTvCaptureTime.setText(getString(R.string.capture_capture_time, TimeFormat.durationFormat(captureTime)));
    }

    @Override
    public void onCaptureCountChanged(int captureCount) {
        mTvCaptureCount.setVisibility(View.VISIBLE);
        mTvCaptureCount.setText(getString(R.string.capture_capture_count, captureCount));
    }

    private void downloadFilesAndPlay(String[] urls) {
        if (urls == null || urls.length == 0) {
            return;
        }

        String localFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/SDK_DEMO_CAPTURE";
        String[] fileNames = new String[urls.length];
        String[] localPaths = new String[urls.length];
        boolean needDownload = false;
        for (int i = 0; i < localPaths.length; i++) {
            fileNames[i] = urls[i].substring(urls[i].lastIndexOf("/") + 1);
            localPaths[i] = localFolder + "/" + fileNames[i];
            if (!new File(localPaths[i]).exists()) {
                needDownload = true;
            }
        }

        if (!needDownload) {
            StitchActivity.launchActivity(this, localPaths);
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.osc_dialog_title_downloading)
                .content(getString(R.string.osc_dialog_msg_downloading, urls.length, 0, 0))
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .show();

        AtomicInteger successfulCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        for (int i = 0; i < localPaths.length; i++) {
            String url = urls[i];
            OkGo.<File>get(url)
                    .execute(new FileCallback(localFolder, fileNames[i]) {

                        @Override
                        public void onError(Response<File> response) {
                            super.onError(response);
                            errorCount.incrementAndGet();
                            checkDownloadCount();
                        }

                        @Override
                        public void onSuccess(Response<File> response) {
                            successfulCount.incrementAndGet();
                            checkDownloadCount();
                        }

                        private void checkDownloadCount() {
                            dialog.setContent(getString(R.string.osc_dialog_msg_downloading, urls.length, successfulCount.intValue(), errorCount.intValue()));
                            if (successfulCount.intValue() + errorCount.intValue() >= urls.length) {
//                                PlayAndExportActivity.launchActivity(CaptureActivity.this, localPaths);
                                dialog.dismiss();
                            }
                        }
                    });
        }
    }

}
