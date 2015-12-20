package floschlo.screencast.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import floschlo.screencast.BuildConfig;
import floschlo.screencast.R;
import floschlo.screencast.adapter.AllVideosAdapter;
import floschlo.screencast.notifications.RecordingNotification;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public final static String TAG = MainActivity.class.getSimpleName();

    /* INTENTS */
    public final static String ACTION_REC_STOP = MainActivity.class.getCanonicalName() + ".ACTION_REC_STOP";
    public final static String ACTION_REC_DISCARD = MainActivity.class.getCanonicalName() + ".ACTION_REC_DISCARD";

    /* VIEWS */
    private FloatingActionButton mStartRecordFab;
    private RecyclerView mRecyclerView;

    /* REQUEST CODES */
    private static final int REQUEST_ASK_SCREEN_CAPTURE_ACCESS = 1;

    /* RECORD VALUES */
    private int mScreenDensity;
    private int mDisplayWidth;// = 480;
    private int mDisplayHeight;// = 640;

    /* RECORD CLASSES */
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    private MediaProjectionManager mProjectionManager;

    private boolean mRecording;
    private File mCurrentVideoFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mStartRecordFab = (FloatingActionButton) findViewById(R.id.record_fab);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);

        mStartRecordFab.setOnClickListener(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new AllVideosAdapter(this));

        prepareRecordValues();

        mMediaRecorder = new MediaRecorder();

        mProjectionManager = (MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);

        mMediaProjectionCallback = new MediaProjectionCallback();
        mRecording = false;

    }

    private void prepareRecordValues() {
        DisplayMetrics metrics = new DisplayMetrics();
        Point size = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);
        display.getSize(size);
        mScreenDensity = metrics.densityDpi;
        mDisplayWidth = size.x;
        mDisplayHeight = size.y;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_ASK_SCREEN_CAPTURE_ACCESS) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            mRecording = false;
            return;
        }
        // TODO: 20.12.2015 Implement delete intent
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private void startScreenRecording() {
        shareScreen();
    }

    private void stopScreenRecording(boolean keep) {
        if (keep) {
            addVideoToMediaServer(mCurrentVideoFile);
        } else {
            mCurrentVideoFile.delete();
        }
        mCurrentVideoFile = null;
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        Log.v(TAG, "Recording Stopped");
        stopScreenSharing();
        mRecyclerView.setAdapter(new AllVideosAdapter(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater lMenuInflater = new MenuInflater(this);
        lMenuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                openSetting();
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent: receiving new intent with action: " + intent.getAction());
        if (intent.getAction() == ACTION_REC_STOP) {
            Snackbar.make(findViewById(R.id.root), "Recording stopped", Snackbar.LENGTH_LONG).show();
            stopScreenRecording(true);
            RecordingNotification.cancel(this);
        } else if (intent.getAction().equals(ACTION_REC_DISCARD)) {
            stopScreenRecording(false);
            RecordingNotification.cancel(this);
        } else {
            super.onNewIntent(intent);
        }
    }


    @Override
    public void onClick(View v) {

        RecordingNotification.createNotification(this);
        initRecorder();
        prepareRecorder();
        startScreenRecording();

    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (mRecording) {
                mRecording = false;
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                if (BuildConfig.DEBUG)
                    Log.v(TAG, "Recording Stopped");
            }
            mMediaProjection = null;
            stopScreenSharing();
            if (BuildConfig.DEBUG)
                Log.i(TAG, "MediaProjection Stopped");
        }
    }

    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_ASK_SCREEN_CAPTURE_ACCESS);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        //mMediaRecorder.release();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                mDisplayWidth, mDisplayHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }

    private void prepareRecorder() {
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    /**
     * Makes everything ready for recording.
     */
    private void initRecorder() {

        mCurrentVideoFile = generateNewVideoFile();

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mDisplayWidth, mDisplayHeight);
        mMediaRecorder.setOutputFile(mCurrentVideoFile.getPath());
    }

    /**
     * Generates file name and creates directory if not exist.
     *
     * @return File for saving video data
     */
    private File generateNewVideoFile() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "generateNewVideoFile: generating File");
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date now = new Date();
        String fileName = "Screencast_" + formatter.format(now) + ".mp4";
        String pathName = Environment.getExternalStorageDirectory().getPath() + "/Movies/Screencasts/";
        new File(pathName).mkdirs();
        return new File(pathName + fileName);
    }

    private void openSetting() {
        Intent openSettingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(openSettingsIntent);
    }

    private void addVideoToMediaServer (File file) {
        ContentValues values = new ContentValues();

        values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mpeg");
        values.put(MediaStore.MediaColumns.DATA, file.getPath());

        getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }
}
