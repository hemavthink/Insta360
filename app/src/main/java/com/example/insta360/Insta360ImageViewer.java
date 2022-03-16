package com.example.insta360;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.arashivision.sdkmedia.player.image.ImageParamsBuilder;
import com.arashivision.sdkmedia.player.image.InstaImagePlayerView;
import com.arashivision.sdkmedia.player.listener.PlayerViewListener;
import com.arashivision.sdkmedia.stitch.StitchUtils;
import com.arashivision.sdkmedia.work.WorkWrapper;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.Response;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

public class Insta360ImageViewer extends BaseObserveCameraActivity {
    InstaImagePlayerView mImagePlayerView;
    private static final String WORK_URLS = "CAMERA_FILE_PATH";
    String mHDROutputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/SDK_DEMO_OSC/generate_hdr_" + System.currentTimeMillis() + ".jpg";
    private WorkWrapper mWorkWrapper;
    private StitchTask mStitchTask;
    boolean mIsStitchHDRSuccessful;

    public static void launchActivity(Context context, String[] urls) {
        Intent intent = new Intent(context, Insta360ImageViewer.class);
        intent.putExtra(WORK_URLS, urls);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insta360_image_viewer);

        String[] urls = getIntent().getStringArrayExtra(WORK_URLS);
        if (urls == null) {
            finish();
            Toast.makeText(this, R.string.play_toast_empty_path, Toast.LENGTH_SHORT).show();
            return;
        }
        mImagePlayerView = findViewById(R.id.player_image);
        mWorkWrapper = new WorkWrapper(urls);
        if(mWorkWrapper.isHDRPhoto()){
            mStitchTask = new StitchTask(this);
            mStitchTask.execute();
        }else{
            mImagePlayerView.prepare(mWorkWrapper, new ImageParamsBuilder());
            mImagePlayerView.play();
        }
    }

    private void showGenerateResult() {
        ImageParamsBuilder builder = new ImageParamsBuilder()
                // 如果HDR合成成功，则将其文件路径设置为播放参数
                // If HDR stitching is successful then set it as the playback proxy
                .setUrlForPlay(mIsStitchHDRSuccessful ? mHDROutputPath : null);
        mImagePlayerView.prepare(mWorkWrapper, builder);
        mImagePlayerView.play();
    }

    private static class StitchTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<Insta360ImageViewer> activityWeakReference;
        private MaterialDialog mDialog;

        private StitchTask(Insta360ImageViewer activity) {
            super();
            activityWeakReference = new WeakReference<>(activity);
            mDialog = new MaterialDialog.Builder(activity)
                    .content(R.string.export_dialog_msg_hdr_stitching)
                    .progress(true, 100)
                    .canceledOnTouchOutside(false)
                    .cancelable(false)
                    .build();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Insta360ImageViewer stitchActivity = activityWeakReference.get();
            if (stitchActivity != null && !isCancelled()) {
                // Start HDR stitching
                stitchActivity.mIsStitchHDRSuccessful = StitchUtils.generateHDR(stitchActivity.mWorkWrapper, stitchActivity.mHDROutputPath);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            Insta360ImageViewer stitchActivity = activityWeakReference.get();
            if (stitchActivity != null && !isCancelled()) {
                stitchActivity.showGenerateResult();
            }
            mDialog.dismiss();
        }
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
                                dialog.dismiss();
                                finish();
                            }
                        }
                    });
        }
    }
}