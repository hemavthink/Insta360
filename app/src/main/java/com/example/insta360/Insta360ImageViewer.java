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

import java.lang.ref.WeakReference;

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

        mWorkWrapper = new WorkWrapper(urls);
        mImagePlayerView = findViewById(R.id.player_image);
         mStitchTask = new StitchTask(this);
         mStitchTask.execute();
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
}