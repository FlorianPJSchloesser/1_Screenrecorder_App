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
import android.support.v4.widget.SwipeRefreshLayout;
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
import java.util.ArrayList;
import java.util.Date;

import floschlo.screencast.BuildConfig;
import floschlo.screencast.R;
import floschlo.screencast.adapter.VideosListAdapter;
import floschlo.screencast.background.LoadVideoMetaDataTask;
import floschlo.screencast.container.VideoDataContainer;
import floschlo.screencast.notifications.RecordingFinishedNotification;
import floschlo.screencast.notifications.RecordingNotification;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public final static String TAG = MainActivity.class.getSimpleName();

    /* INTENTS */
    public final static String ACTION_REC_STOP = MainActivity.class.getCanonicalName() + ".ACTION_REC_STOP";
    public final static String ACTION_REC_DISCARD = MainActivity.class.getCanonicalName() + ".ACTION_REC_DISCARD";
    public final static String ACTION_FILE_DELETE = MainActivity.class.getCanonicalName() + ".ACTION_FILE_DELETE";

    /* EXTRA */
    public final static String EXTRA_LIST_POSITION = "extra_list_position";

    /* VIEWS */
    private View mRoot;
    private FloatingActionButton mStartRecordFab;
    private RecyclerView mRecyclerView;

    /* REQUEST CODES */
    private static final int REQUEST_ASK_SCREEN_CAPTURE_ACCESS = 1;

    /* RECORD VALUES */
    private int mScreenDensity;
    private int mDisplayWidth;
    private int mDisplayHeight;

    /* RECORD CLASSES */
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mProjectionManager;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mVirtualDisplay;

    private boolean mIsRecording;
    private File mCurrentVideoFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        initializeViews();

        calculateDisplayMetrics();

        mMediaRecorder = new MediaRecorder();
        mProjectionManager = (MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);

        mMediaProjectionCallback = new MediaProjectionCallback();
        mIsRecording = false;

    }

    private void initializeViews() {
        //Find views in layout
        mRoot = findViewById(R.id.root);
        mStartRecordFab = (FloatingActionButton) findViewById(R.id.record_fab);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);

        //Prepare them
        mStartRecordFab.setOnClickListener(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new VideosListAdapter(this));
    }

    @Override
    protected void onStart() {
        super.onStart();

        new LoadVideoMetaDataTask(new LoadVideoMetaDataTask.OnMetaDataLoadedListener() {
            @Override
            public void onVideoMetaDataLoaded(ArrayList<VideoDataContainer> dataList) {
                ((VideosListAdapter) mRecyclerView.getAdapter()).setData(dataList);
                mRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }).execute(getAllVideoFiles());

    }

    private File[] getAllVideoFiles() {

        if (BuildConfig.DEBUG)
            Log.d(TAG, "getAllVideoFiles: Listing all items loaded from directory.");

        File parentDir = new File(Environment.getExternalStorageDirectory().getPath() + "/Movies/Screencasts/");

        if (BuildConfig.DEBUG)
            Log.d(TAG, "getAllVideoFiles: parentDir is '" + parentDir.getPath() + "'");

        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
        if (files != null) {
            for (int i = files.length - 1; i >= 0; i--) {
                File file = files[i];
                if (file.isDirectory()) {
                    if (BuildConfig.DEBUG) Log.w(TAG, "getAllVideoFiles: found a folder!");
                } else {
                    if (file.getName().endsWith(".mp4")) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "getAllVideoFiles: Added file '" + file.getName() + "'");
                        inFiles.add(file);
                    }
                }
            }
        }
        File [] inFilesArray = new File[inFiles.size()];
        for (int i = 0; i < inFiles.size(); i++) {
            inFilesArray[i] = inFiles.get(i);
        }
        return inFilesArray;
    }

    private void calculateDisplayMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        Point size = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);
        display.getSize(size);
        mScreenDensity = metrics.densityDpi;
        mDisplayWidth = size.x;
        mDisplayHeight = size.y;
        
        if (BuildConfig.DEBUG)
            Log.d(TAG, "calculateDisplayMetrics: Calculated metrics. mScreenDensitiy==" + mScreenDensity
                    + ", mDisplayWidth==" + mDisplayWidth
                    + ", mDisplayHeight==" + mDisplayHeight);
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
            mIsRecording = false;
            return;
        }
        // TODO: 20.12.2015 Implement delete intent
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private void startScreenRecording() {
        RecordingFinishedNotification.cancel(this);
        shareScreen();
    }

    private void stopScreenRecording(boolean keep) {
        if (keep) {
            addVideoToMediaStore(mCurrentVideoFile);
        } else {
            mCurrentVideoFile.delete();
        }
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        Log.v(TAG, "Recording Stopped");
        stopScreenSharing();

        new LoadVideoMetaDataTask(new LoadVideoMetaDataTask.OnMetaDataLoadedListener() {
            @Override
            public void onVideoMetaDataLoaded(ArrayList<VideoDataContainer> dataList) {
                ((VideosListAdapter) mRecyclerView.getAdapter()).getData().add(0, dataList.get(0));
                mRecyclerView.getAdapter().notifyItemInserted(0);
                mRecyclerView.getLayoutManager().scrollToPosition(0);
            }
        }).execute(mCurrentVideoFile);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater lMenuInflater = new MenuInflater(this);
        if (BuildConfig.DEBUG)
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
    protected void onNewIntent(final Intent intent) {
        Log.d(TAG, "onNewIntent: receiving new intent with action: " + intent.getAction());
        if (intent.getAction() == ACTION_REC_STOP) {
            Snackbar.make(findViewById(R.id.root), R.string.snackbar_recording_finished, Snackbar.LENGTH_LONG).show();
            RecordingNotification.cancel(this);
            RecordingFinishedNotification.createNotification(getActivity(), mCurrentVideoFile);
            stopScreenRecording(true);
        } else if (intent.getAction().equals(ACTION_REC_DISCARD)) {
            stopScreenRecording(false);
            RecordingNotification.cancel(this);
        } else if (intent.getAction().equals(ACTION_FILE_DELETE)) {
            int itemPosition = intent.getIntExtra(EXTRA_LIST_POSITION, -1);
            deleteVideo(itemPosition);

        } else {
            super.onNewIntent(intent);
        }
    }

    private void deleteVideo(final int itemPosition) {
        final VideoDataContainer videoDataContainer = ((VideosListAdapter) mRecyclerView.getAdapter()).getData().get(itemPosition);
        ((VideosListAdapter) mRecyclerView.getAdapter()).getData().remove(itemPosition);
        mRecyclerView.getAdapter().notifyItemRemoved(itemPosition);
        Snackbar.make(mRoot, R.string.snackbak_file_deleted, Snackbar.LENGTH_LONG).setAction(R.string.action_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((VideosListAdapter) mRecyclerView.getAdapter()).getData().add(itemPosition, videoDataContainer);
                mRecyclerView.getAdapter().notifyItemInserted(itemPosition);
            }
        }).setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                if (event != DISMISS_EVENT_ACTION) {
                    new File(videoDataContainer.getVideoPath()).delete();
                }
            }
        }).show();
    }


    @Override
    public void onClick(View v) {

        if (v==mStartRecordFab) {
            RecordingNotification.createNotification(this);
            mCurrentVideoFile = null;
            initRecorder();
            prepareRecorder();
            startScreenRecording();
        }

    }

    public Context getActivity() {
        return this;
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (mIsRecording) {
                mIsRecording = false;
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "onStop: Recording stopped");
            }
            mMediaProjection = null;
            stopScreenSharing();
            if (BuildConfig.DEBUG)
                Log.d(TAG, "onStop: Media projection stopped");
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

        if (BuildConfig.DEBUG)
            Log.d(TAG, "initRecorder: Initialize screen recording");

        mCurrentVideoFile = generateNewVideoFile();

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncodingBitRate(1024 * 1000);
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

    private void addVideoToMediaStore(File file) {

        ContentValues values = new ContentValues();

        values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mpeg");
        values.put(MediaStore.MediaColumns.DATA, file.getPath());

        getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

    }
}
