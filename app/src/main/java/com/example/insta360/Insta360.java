package com.example.insta360;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.arashivision.sdk.demo.util.FileUtils;
import com.arashivision.sdk.demo.util.TimeFormat;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.ICameraChangedCallback;
import com.arashivision.sdkcamera.camera.callback.ICaptureStatusListener;
import com.arashivision.sdkcamera.camera.callback.ILiveStatusListener;
import com.arashivision.sdkcamera.camera.callback.IPreviewStatusListener;
import com.arashivision.sdkcamera.camera.live.LiveParamsBuilder;
import com.arashivision.sdkcamera.camera.preview.ExposureData;
import com.arashivision.sdkcamera.camera.preview.GyroData;
import com.arashivision.sdkcamera.camera.preview.VideoData;
import com.arashivision.sdkcamera.camera.resolution.PreviewStreamResolution;
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder;
import com.arashivision.sdkmedia.player.capture.InstaCapturePlayerView;
import com.arashivision.sdkmedia.player.listener.PlayerViewListener;
import com.example.insta360.util.NetworkManager;

import java.util.List;

public class Insta360 extends AppCompatActivity implements ICameraChangedCallback,
        IPreviewStatusListener, ICaptureStatusListener, ILiveStatusListener {

    private ViewGroup mLayoutLoading;
    private Group mLayoutPromptConnectCamera;
    private Group mLayoutPlayer;
    private InstaCapturePlayerView mCapturePlayerView;
    private TextView mTvSdCard;
    private TextView mTvSdBattery;
    private TextView mTvRecordDuration;
    private RadioGroup mRgCaptureMode;
    private ImageView mBtnCameraWork;

    private TextView mTvExposureTimestamp;
    private TextView mTvExposureTime;
    private TextView mTvGyroTimestamp;
    private TextView mTvGyroAX;
    private TextView mTvGyroAY;
    private TextView mTvGyroAZ;
    private TextView mTvGyroGX;
    private TextView mTvGyroGY;
    private TextView mTvGyroGZ;
    private TextView mTvVideoTimestamp;
    private TextView mTvVideoSize;

    private boolean mNeedToRestartPreview;
    private boolean mIsCaptureButtonClicked;
    private int mCurPreviewType = -1;
    private PreviewStreamResolution mCurPreviewResolution = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_insta360);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        bindNormalViews();
        bindPlayerViews();

        InstaCameraManager cameraManager = InstaCameraManager.getInstance();
        if (isCameraConnected()) {
            onCameraStatusChanged(true);
            onCameraBatteryUpdate(cameraManager.getCameraCurrentBatteryLevel(), cameraManager.isCameraCharging());
            onCameraSDCardStateChanged(cameraManager.isSdCardEnabled());
            onCameraStorageChanged(cameraManager.getCameraStorageFreeSpace(), cameraManager.getCameraStorageTotalSpace());
        }
        cameraManager.registerCameraChangedCallback(this);
    }

    private void bindNormalViews() {
        mLayoutLoading = findViewById(R.id.layout_loading);
        mLayoutPromptConnectCamera = findViewById(R.id.group_prompt_connect_camera);

        findViewById(R.id.btn_connect_by_wifi).setOnClickListener(v -> {
            openCamera(InstaCameraManager.CONNECT_TYPE_WIFI);
        });

    }

    private void bindPlayerViews() {

        mLayoutPlayer = findViewById(R.id.group_player);
        mCapturePlayerView = findViewById(R.id.player_capture);
        mCapturePlayerView.setLifecycle(getLifecycle());


        mBtnCameraWork = findViewById(R.id.btn_camera_work);
        mBtnCameraWork.setOnClickListener(v -> {
            mIsCaptureButtonClicked = true;
            if (!checkToRestartCameraPreviewStream()) {
                doCameraWork();
            }
        });
    }

    private boolean isCameraConnected() {
        return InstaCameraManager.getInstance().getCameraConnectedType() != InstaCameraManager.CONNECT_TYPE_NONE;
    }

    private void resetState() {
        mTvRecordDuration.setText(null);
        mIsCaptureButtonClicked = false;
        mCurPreviewType = -1;
        mCurPreviewResolution = null;

//        mNeedToRestartPreview = false;
//        int captureType = InstaCameraManager.getInstance().getCurrentCaptureType();
//        if (captureType == InstaCameraManager.CAPTURE_TYPE_NORMAL_RECORD) {
//            mRgCaptureMode.check(R.id.rb_record);
//            mBtnCameraWork.setChecked(true);
//        } else if (captureType == InstaCameraManager.CAPTURE_TYPE_NORMAL_CAPTURE) {
//            mRgCaptureMode.check(R.id.rb_capture);
//            mLayoutLoading.setVisibility(View.VISIBLE);
//            mBtnCameraWork.setChecked(true);
//        }
        mNeedToRestartPreview = true;
    }

    // 获取当前要开启的预览模式
    // Get the preview mode currently to be turned on
    private int getNewPreviewType() {
        return InstaCameraManager.PREVIEW_TYPE_NORMAL;
    }

    // 获取预览的分辨率
    // 此处为，录像选择5.7k，其他从支持列表中选择默认
    // Get preview resolution
    // Here, select 5.7k for recording, and select default from the support list for others
    private PreviewStreamResolution getPreviewResolution(int previewType) {
        // 自选分辨率（只要您觉着效果OK即可）
        // Optional resolution (as long as you feel the effect is OK)
        if (previewType == InstaCameraManager.PREVIEW_TYPE_RECORD) {
            return PreviewStreamResolution.STREAM_5760_2880_30FPS;
        }

        // 或从当前相机的拍摄模式支持列表中任选其一
        // Or choose one of the supported shooting modes of the current camera
        return InstaCameraManager.getInstance().getSupportedPreviewStreamResolution(previewType).get(0);
    }

    private void openCamera(int connectType) {
        mLayoutLoading.setVisibility(View.VISIBLE);
        InstaCameraManager.getInstance().openCamera(connectType);
    }

    @Override
    public void onCameraStatusChanged(boolean enabled) {
        mLayoutLoading.setVisibility(View.GONE);
        mLayoutPlayer.setVisibility(enabled ? View.VISIBLE : View.GONE);
        mLayoutPromptConnectCamera.setVisibility(enabled ? View.GONE : View.VISIBLE);
        resetState();

        // 连接相机后自动开启预览、注册拍照监听
        // After connecting the camera, open preview stream and register listeners
        if (enabled) {
            InstaCameraManager.getInstance().setCaptureStatusListener(this);
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(this);
            checkToRestartCameraPreviewStream();
        }
    }

    @Override
    public void onCameraConnectError() {
        mLayoutLoading.setVisibility(View.GONE);
        mLayoutPlayer.setVisibility(View.GONE);
        mLayoutPromptConnectCamera.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCameraBatteryUpdate(int batteryLevel, boolean isCharging) {
        String text = getString(R.string.full_demo_battery_level, batteryLevel);
        if (isCharging) {
            text += "  " + getString(R.string.full_demo_charging);
        }
        mTvSdBattery.setText(text);
    }

    @Override
    public void onCameraSDCardStateChanged(boolean enabled) {
        if (!enabled) {
            mTvSdCard.setText(R.string.full_demo_sd_error);
        }
    }

    @Override
    public void onCameraStorageChanged(long freeSpace, long totalSpace) {
        if (!(freeSpace <= 0 && totalSpace <= 0)) {
            mTvSdCard.setText(getString(R.string.full_demo_sd_remaining_space, FileUtils.computeFileSize(freeSpace) + "/" + FileUtils.computeFileSize(totalSpace)));
        }
    }

    private boolean checkToRestartCameraPreviewStream() {
        if (isCameraConnected()) {
            int newPreviewType = getNewPreviewType();
            PreviewStreamResolution newResolution = getPreviewResolution(newPreviewType);
            if (mCurPreviewType != newPreviewType || mCurPreviewResolution != newResolution) {
                mCurPreviewType = newPreviewType;
                mCurPreviewResolution = newResolution;
                InstaCameraManager.getInstance().closePreviewStream();
                InstaCameraManager.getInstance().startPreviewStream(newResolution, newPreviewType);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onOpening() {
        mLayoutLoading.setVisibility(View.VISIBLE);
        mLayoutPlayer.setVisibility(View.VISIBLE);
        mLayoutPromptConnectCamera.setVisibility(View.GONE);
    }

    @Override
    public void onOpened() {
        mLayoutLoading.setVisibility(View.GONE);

        InstaCameraManager.getInstance().setStreamEncode();
        mCapturePlayerView.setPlayerViewListener(new PlayerViewListener() {
            @Override
            public void onLoadingFinish() {
                InstaCameraManager.getInstance().setPipeline(mCapturePlayerView.getPipeline());
            }

            @Override
            public void onReleaseCameraPipeline() {
                InstaCameraManager.getInstance().setPipeline(null);
            }
        });
        mCapturePlayerView.prepare(createCaptureParams());
        mCapturePlayerView.play();
        mCapturePlayerView.setKeepScreenOn(true);

        // 预览开启后再录像
        // Record after preview is opened
        if (mIsCaptureButtonClicked) {
            mIsCaptureButtonClicked = false;
            doCameraWork();
        }
    }

    private CaptureParamsBuilder createCaptureParams() {
        return new CaptureParamsBuilder()
                .setCameraType(InstaCameraManager.getInstance().getCameraType())
                .setMediaOffset(InstaCameraManager.getInstance().getMediaOffset())
                .setCameraSelfie(InstaCameraManager.getInstance().isCameraSelfie())
                .setLive(mCurPreviewType == InstaCameraManager.PREVIEW_TYPE_LIVE)  // 是否为直播模式
                .setResolutionParams(mCurPreviewResolution.width, mCurPreviewResolution.height, mCurPreviewResolution.fps);
    }

    @Override
    public void onIdle() {
        mCapturePlayerView.destroy();
        mCapturePlayerView.setKeepScreenOn(false);
    }

    @Override
    public void onExposureData(ExposureData exposureData) {
        mTvExposureTimestamp.setText("timestamp: " + exposureData.timestamp);
        mTvExposureTime.setText("exposure_time: " + exposureData.exposureTime);
    }

    @Override
    public void onGyroData(List<GyroData> gyroList) {
        if (!gyroList.isEmpty()) {
            GyroData gyroData = gyroList.get(gyroList.size() - 1);
            mTvGyroTimestamp.setText("timestamp: " + gyroData.timestamp);
            mTvGyroAX.setText("ax: " + gyroData.ax);
            mTvGyroAY.setText("ay: " + gyroData.ay);
            mTvGyroAZ.setText("az: " + gyroData.az);
            mTvGyroGX.setText("gx: " + gyroData.gx);
            mTvGyroGY.setText("gy: " + gyroData.gy);
            mTvGyroGZ.setText("gz: " + gyroData.gz);
        }
    }

    @Override
    public void onVideoData(VideoData videoData) {
        mTvVideoTimestamp.setText("timestamp: " + videoData.timestamp);
        mTvVideoSize.setText("data size: " + videoData.size);
    }

    private void doCameraWork() {
        if (mCurPreviewType != InstaCameraManager.PREVIEW_TYPE_LIVE && !InstaCameraManager.getInstance().isSdCardEnabled()) {
            Toast.makeText(this, R.string.capture_toast_sd_card_error, Toast.LENGTH_SHORT).show();
            mIsCaptureButtonClicked = false;
//            mBtnCameraWork.setChecked(false);
            return;
        }
        switch (mCurPreviewType) {
            case InstaCameraManager.PREVIEW_TYPE_RECORD:
                InstaCameraManager.getInstance().startNormalRecord();
                break;
            case InstaCameraManager.PREVIEW_TYPE_NORMAL:
                mLayoutLoading.setVisibility(View.VISIBLE);
                InstaCameraManager.getInstance().startNormalCapture(false);
                break;
            case InstaCameraManager.PREVIEW_TYPE_LIVE:
                NetworkManager.getInstance().exchangeNetToMobile();
                InstaCameraManager.getInstance().startLive(createLiveParams(), this);
                break;
        }
    }

    // 所有直播推流参数根据您的产品需求任意填写
    // All live streaming parameters are arbitrarily filled in according to your product requirements
    private LiveParamsBuilder createLiveParams() {
        return new LiveParamsBuilder()
                .setRtmp("rtmp://txy.live-send.acg.tv/live-txy/?streamname=live_23968708_6785332&key=6abecd453e112c38f190f69fabc6d3da")
                .setWidth(1440)
                .setHeight(720)
                .setFps(30)
                .setBitrate(2 * 1024 * 1024)
                .setPanorama(true)
                // 设置网络ID即可在使用WIFI连接相机时使用4G网络推流
                // set NetId to use 4G to push live streaming when connecting camera by WIFI
                .setNetId(NetworkManager.getInstance().getMobileNetId());
    }

    private void stopCameraWork() {
        switch (mCurPreviewType) {
            case InstaCameraManager.PREVIEW_TYPE_RECORD:
                InstaCameraManager.getInstance().stopNormalRecord();
                break;
            case InstaCameraManager.PREVIEW_TYPE_LIVE:
                InstaCameraManager.getInstance().stopLive();
                NetworkManager.getInstance().clearBindProcess();
                break;
        }
    }

    @Override
    public void onCaptureStopping() {
        mLayoutLoading.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCaptureFinish(String[] filePaths) {
        mLayoutLoading.setVisibility(View.GONE);
        mTvRecordDuration.setText(null);
//        mBtnCameraWork.setChecked(false);
        checkToRestartCameraPreviewStream();
        // 拍摄结束返回文件路径，可执行下载、播放、导出操作，任君选择
        // 如果是HDR拍照则必须从相机下载到本地才可进行HDR合成操作
        // After capture, the file paths will be returned. Then download, play and export operations can be performed
        // If it is HDR Capture, you must download images from the camera to the local to perform HDR stitching operation
//        PlayAndExportActivity.launchActivity(this, filePaths);
    }

    @Override
    public void onCaptureTimeChanged(long captureTime) {
        mTvRecordDuration.setText(TimeFormat.durationFormat(captureTime));
    }

    @Override
    public void onLivePushStarted() {
        Toast.makeText(this, R.string.full_demo_live_start, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLivePushFinished() {
//        mBtnCameraWork.setChecked(false);
    }

    @Override
    public void onLivePushError() {
//        mBtnCameraWork.setChecked(false);
        Toast.makeText(this, R.string.full_demo_live_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            // 退出页面时销毁预览
            // Destroy the preview when exiting the page
            InstaCameraManager.getInstance().stopLive();
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(null);
            InstaCameraManager.getInstance().closePreviewStream();
            mCapturePlayerView.destroy();
            NetworkManager.getInstance().clearBindProcess();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        InstaCameraManager.getInstance().unregisterCameraChangedCallback(this);
    }
}