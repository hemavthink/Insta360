package com.example.insta360.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.insta360.MyApp;
import com.arashivision.sdkmedia.player.image.ImageParamsBuilder;
import com.arashivision.sdkmedia.player.image.InstaImagePlayerView;
import com.arashivision.sdkmedia.stitch.StitchUtils;
import com.arashivision.sdkmedia.work.WorkWrapper;
import com.example.insta360.R;

import java.lang.ref.WeakReference;

public class StitchActivity extends AppCompatActivity {
    private static final String WORK_URLS = "CAMERA_FILE_PATH";
    public static final String COPY_DIR = MyApp.getInstance().getCacheDir() + "/hdr_source";
    private WorkWrapper mWorkWrapper;
    private String mOutputPath = MyApp.getInstance().getFilesDir() + "/hdr_generate/generate.jpg";
    private StitchTask mStitchTask;

    private InstaImagePlayerView mImagePlayerView;

    public static void launchActivity(Context context, String[] urls) {
        Intent intent = new Intent(context, StitchActivity.class);
        intent.putExtra(WORK_URLS, urls);
        context.startActivity(intent);
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stitch);
        setTitle(R.string.stitch_toolbar_title);



        String[] urls = getIntent().getStringArrayExtra(WORK_URLS);
        if (urls == null) {
            finish();
            Toast.makeText(this, R.string.play_toast_empty_path, Toast.LENGTH_SHORT).show();
            return;
        }
        mWorkWrapper = new WorkWrapper(urls);
        bindViews();

        // 初始显示无HDR效果的图片
        // Initial display image effect without HDR stitching
        showGenerateResult(false);
    }

    private void bindViews() {
        mImagePlayerView = findViewById(R.id.player_image);
        mImagePlayerView.setLifecycle(getLifecycle());
    }

    private void startGenerate() {
        mStitchTask = new StitchTask(this);
        mStitchTask.execute();
    }

    private void showGenerateResult(boolean successful) {
        ImageParamsBuilder builder = new ImageParamsBuilder()
                // 如果HDR合成成功，则将其文件路径设置为播放参数
                // If HDR stitching is successful then set it as the playback proxy
                .setUrlForPlay(successful ? mOutputPath : null);
        mImagePlayerView.prepare(mWorkWrapper, builder);
        mImagePlayerView.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mStitchTask != null) {
            mStitchTask.cancel(true);
        }
        mImagePlayerView.destroy();
    }

    private static class StitchTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<StitchActivity> activityWeakReference;
        private MaterialDialog mDialog;

        private StitchTask(StitchActivity activity) {
            super();
            activityWeakReference = new WeakReference<>(activity);
            mDialog = new MaterialDialog.Builder(activity)
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
            StitchActivity stitchActivity = activityWeakReference.get();
            if (stitchActivity != null && !isCancelled()) {
                // Start HDR stitching
                return StitchUtils.generateHDR(stitchActivity.mWorkWrapper, stitchActivity.mOutputPath);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            StitchActivity stitchActivity = activityWeakReference.get();
            if (stitchActivity != null && !isCancelled()) {
                stitchActivity.showGenerateResult(result);
            }
            mDialog.dismiss();
        }
    }

}
