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

import java.util.ArrayList;
import java.util.List;

public class Insta360 extends AppCompatActivity implements ICameraChangedCallback,
        IPreviewStatusListener, ICaptureStatusListener, ILiveStatusListener {

    private ViewGroup mLayoutLoading;
    private Group mLayoutPlayer;
    private InstaCapturePlayerView mCapturePlayerView;

    private Button mBtnCameraWork;
    ToggleButton connectionSwitch;

    private boolean mIsCaptureButtonClicked;
    private int mCurPreviewType = -1;
    private PreviewStreamResolution mCurPreviewResolution = null;
    private Toolbar toolbar;
    private LinearLayout layoutCameraArea;
    private boolean mConnectionSwitchEnabled = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_insta360);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        toolbar = (Toolbar) findViewById(R.id.tbHeader);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        layoutCameraArea = (LinearLayout) findViewById(R.id.shoot_area);
        mLayoutLoading = findViewById(R.id.layout_loading);
        connectionSwitch = toolbar.findViewById(R.id.connection_switch);
        mCapturePlayerView = findViewById(R.id.player_capture);
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
                    layoutCameraArea.setVisibility(View.VISIBLE);
                    openCamera(InstaCameraManager.CONNECT_TYPE_WIFI);
                } else {
                    layoutCameraArea.setVisibility(View.GONE);
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
       // mLayoutPlayer.setVisibility(enabled ? View.VISIBLE : View.GONE);

        // After connecting the camera, open preview stream and register listeners
        if (enabled) {
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
       // mLayoutPlayer.setVisibility(View.VISIBLE);
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
                .setCameraSelfie(InstaCameraManager.getInstance().isCameraSelfie());
    }

   /* Capture delegate or sdk methods */
    @Override
    public void onCaptureStopping() {
        mLayoutLoading.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCaptureFinish(String[] filePaths) {
        mLayoutLoading.setVisibility(View.GONE);
        // After capture, the file paths will be returned. Then download, play and export operations can be performed
        // If it is HDR Capture, you must download images from the camera to the local to perform HDR stitching operation
        Insta360ImageViewer.launchActivity(this, filePaths);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        InstaCameraManager.getInstance().unregisterCameraChangedCallback(this);
    }
}