package com.example.insta360;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.afollestad.materialdialogs.MaterialDialog;
import com.arashivision.sdk.demo.util.FileUtils;
import com.arashivision.sdk.demo.util.TimeFormat;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.ICameraChangedCallback;
import com.arashivision.sdkcamera.camera.callback.ICaptureStatusListener;
import com.arashivision.sdkcamera.camera.callback.ILiveStatusListener;
import com.arashivision.sdkcamera.camera.callback.IPreviewStatusListener;
import com.arashivision.sdkcamera.camera.resolution.PreviewStreamResolution;
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder;
import com.arashivision.sdkmedia.player.capture.InstaCapturePlayerView;
import com.arashivision.sdkmedia.player.listener.PlayerViewListener;
import com.example.insta360.util.NetworkManager;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Insta360CameraPreviewActivity extends AppCompatActivity implements ICameraChangedCallback,
        IPreviewStatusListener, ICaptureStatusListener {

    private ViewGroup mLayoutLoading;

    private InstaCapturePlayerView mCapturePlayerView;
    private Button mBtnCameraWork;
    private ToggleButton connectionSwitch;

    private Toolbar toolbar;
    private boolean mConnectionSwitchEnabled = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_insta360_camera_preview);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        toolbar = (Toolbar) findViewById(R.id.tbHeader);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mLayoutLoading = findViewById(R.id.layout_loading);
        connectionSwitch = toolbar.findViewById(R.id.connection_switch);
        mCapturePlayerView = findViewById(R.id.player_capture);
        mBtnCameraWork = findViewById(R.id.btn_camera_work);
        mBtnCameraWork.setOnClickListener(v -> {
            InstaCameraManager.getInstance().setCaptureStatusListener(this);
            int funcMode = InstaCameraManager.FUNCTION_MODE_HDR_CAPTURE;
            InstaCameraManager.getInstance().setAEBCaptureNum(funcMode, 3);
            InstaCameraManager.getInstance().setExposureEV(funcMode, 2f);
            InstaCameraManager.getInstance().startHDRCapture(false);
        });
        setToggleListener();
        InstaCameraManager cameraManager = InstaCameraManager.getInstance();
        if (isCameraConnected()) {
            onCameraStatusChanged(true);
            onCameraBatteryUpdate(cameraManager.getCameraCurrentBatteryLevel(), cameraManager.isCameraCharging());
            onCameraSDCardStateChanged(cameraManager.isSdCardEnabled());
            onCameraStorageChanged(cameraManager.getCameraStorageFreeSpace(), cameraManager.getCameraStorageTotalSpace());
        }
        cameraManager.registerCameraChangedCallback(this);
        forceConnectToWifi();
    }

    /**
     * Force this applicatioin to connect to Wi-Fi
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void forceConnectToWifi() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if ((info != null) && info.isAvailable()) {
                NetworkRequest.Builder builder = new NetworkRequest.Builder();
                builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                NetworkRequest requestedNetwork = builder.build();

                ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);

                        ConnectivityManager.setProcessDefaultNetwork(network);
                        mConnectionSwitchEnabled = true;
                        setToggleChange();

                    }

                    @Override
                    public void onLost(Network network) {
                        super.onLost(network);

                        mConnectionSwitchEnabled = false;
                        setToggleChange();
                    }
                };

                cm.registerNetworkCallback(requestedNetwork, callback);
            }
        } else {
            mConnectionSwitchEnabled = true;
            setToggleChange();
        }
    }

    /*Toggle codes*/
    private void setToggleListener() {
        connectionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    openCamera(InstaCameraManager.CONNECT_TYPE_WIFI);
                } else {
                   // Need toc check
                }
            }
        });
        setToggleChange();
    }

    private void setToggleChange() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (!mConnectionSwitchEnabled) {
                    connectionSwitch.setChecked(false);
                }
                connectionSwitch.setEnabled(mConnectionSwitchEnabled);
            }
        });
    }

    private boolean isCameraConnected() {
        return InstaCameraManager.getInstance().getCameraConnectedType() != InstaCameraManager.CONNECT_TYPE_NONE;
    }

    private void openCamera(int connectType) {
        mLayoutLoading.setVisibility(View.VISIBLE);
        InstaCameraManager.getInstance().openCamera(connectType);
    }

    @Override
    public void onCameraStatusChanged(boolean enabled) {
        mLayoutLoading.setVisibility(View.GONE);
        if (enabled) {
           // List<PreviewStreamResolution> supportedList = InstaCameraManager.getInstance().getSupportedPreviewStreamResolution(InstaCameraManager.PREVIEW_TYPE_NORMAL);
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(this);
            InstaCameraManager.getInstance().startPreviewStream(PreviewStreamResolution.STREAM_1440_720_30FPS, InstaCameraManager.PREVIEW_TYPE_NORMAL);
        }
    }

    @Override
    public void onCameraConnectError() {
        mLayoutLoading.setVisibility(View.GONE);
        //  mLayoutPlayer.setVisibility(View.GONE);
    }


    /* Preview stream */
    @Override
    public void onOpening() {
        mLayoutLoading.setVisibility(View.VISIBLE);
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
    }

    @Override
    public void onIdle() {
        mCapturePlayerView.destroy();
        mCapturePlayerView.setKeepScreenOn(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            // Auto close preview after page loses focus
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(null);
            InstaCameraManager.getInstance().closePreviewStream();
        }
    }

    @Override
    public void onError() {
        // Preview Failed
    }

    /*Capture setting*/
    private CaptureParamsBuilder createCaptureParams() {
        return new CaptureParamsBuilder()
                .setCameraType(InstaCameraManager.getInstance().getCameraType())
                .setMediaOffset(InstaCameraManager.getInstance().getMediaOffset())
                .setCameraSelfie(InstaCameraManager.getInstance().isCameraSelfie())
                .setLive(false);
    }

    /* Capture delegate or sdk methods */
    @Override
    public void onCaptureStarting() {
        mLayoutLoading.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCaptureWorking() {
        mLayoutLoading.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCaptureStopping() {
        mLayoutLoading.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCaptureFinish(String[] filePaths) {
        Insta360ImagePreviewActivity.launchActivity(this, filePaths);
    }

    @Override
    public void onCaptureCountChanged(int captureCount) {
        // Interval shots
        // Only Interval Capture type will callback this
        mLayoutLoading.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCaptureTimeChanged(long captureTime) {
        // Record Duration, in ms
        // Only Record type will callback this
        mLayoutLoading.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        InstaCameraManager.getInstance().unregisterCameraChangedCallback(this);
    }
}