package floschlo.screencast.activity;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.GridLayoutManager;
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
import android.Manifest;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import floschlo.screencast.BuildConfig;
import floschlo.screencast.R;
import floschlo.screencast.adapter.VideosListAdapter;
import floschlo.screencast.background.ListAllFilesTask;
import floschlo.screencast.background.LoadVideoMetaDataTask;
import floschlo.screencast.container.VideoDataContainer;
import floschlo.screencast.notifications.RecordingFinishedNotification;
import floschlo.screencast.notifications.RecordingNotification;
import floschlo.screencast.utils.IntentUtils;
import floschlo.screencast.view.ConfigurationView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public final static String TAG = MainActivity.class.getSimpleName();

    /* INTENTS */
    public final static String ACTION_REC_STOP = MainActivity.class.getCanonicalName() + ".ACTION_REC_STOP";
    public final static String ACTION_REC_DISCARD = MainActivity.class.getCanonicalName() + ".ACTION_REC_DISCARD";
    public final static String ACTION_FILE_DELETE = MainActivity.class.getCanonicalName() + ".ACTION_FILE_DELETE";

    /* EXTRA */
    public final static String EXTRA_LIST_POSITION = "extra_list_position";
    public final static String EXTRA_REAL_LIST_POSITION = "extra_real_list_position";

    /* VIEWS */
    private View mRootView;
    private FloatingActionButton mStartRecordFabView;
    private RecyclerView mRecyclerView;
    private View mNoStoragePermissionView;
    private Button mGrantStorageButtonView;
    private ConfigurationView mConfigurationPanel;

    /* REQUEST CODES */
    private static final int REQUEST_ASK_SCREEN_CAPTURE_ACCESS = 1;
    private static final int REQUEST_START_RECORDING = 2;
    private static final int REQUEST_LIST_VIDEOS = 3;

    /* RECORD VALUES */
    private int mScreenDensity;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private boolean mIsRecording;
    private File mCurrentVideoFile;

    /* RECORD CLASSES */
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mProjectionManager;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mVirtualDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Prepare activity basics
        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        //Initialize all views in this activity
        assignViews();

        applyTransculentStatusMargins();

        //Collect values needed for recording
        calculateDisplayMetrics();

    }

    private void applyTransculentStatusMargins() {
        findViewById(R.id.statusbar_scrim).setMinimumHeight(getStatusBarHeight());
        findViewById(R.id.statusbar_scrim).invalidate();
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onNewIntent(final Intent intent) {
        Log.d(TAG, "onNewIntent: receiving new intent with action: " + intent.getAction());
        if (intent.getAction() == ACTION_REC_STOP) {
            Snackbar.make(mRootView, R.string.snackbar_recording_finished, Snackbar.LENGTH_LONG).setAction(R.string.action_play, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    IntentUtils.playVideo(getActivity(), Uri.fromFile(mCurrentVideoFile));
                }
            }).show();
            RecordingNotification.cancel(this);
            RecordingFinishedNotification.createNotification(getActivity(), mCurrentVideoFile);
            stopScreenRecording(true);
        } else if (intent.getAction().equals(ACTION_REC_DISCARD)) {
            stopScreenRecording(false);
            RecordingNotification.cancel(this);
        } else if (intent.getAction().equals(ACTION_FILE_DELETE)) {
            deleteVideoFromList(intent);
        } else {
            super.onNewIntent(intent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetMediaProjection();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case REQUEST_START_RECORDING:
                boolean allowed = true;
                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i] == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            if (BuildConfig.DEBUG)
                                Log.w(TAG, "onRequestPermissionsResult: External storage permission denied!");
                            allowed = false;
                        }
                    }
                }
                if (allowed) startScreenRecording();
                break;
            case REQUEST_LIST_VIDEOS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mNoStoragePermissionView.setVisibility(View.INVISIBLE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                    loadVideos();
                } else {
                    showSnackbar(R.string.snackbar_cannot_list_videos);
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_ASK_SCREEN_CAPTURE_ACCESS:
                if (resultCode == RESULT_OK) {
                    mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
                    mMediaProjection.registerCallback(mMediaProjectionCallback, null);
                    mVirtualDisplay = createVirtualDisplay();
                    mMediaRecorder.start();
                } else if (resultCode == RESULT_CANCELED) {
                    showSnackbar(R.string.snackbar_permission_denied);
                    mIsRecording = false;
                } else {
                    if (BuildConfig.DEBUG)
                        Log.w(TAG, "onActivityResult: requestCode == REQUEST_ASK_SCREEN_CAPTURE_ACCESS but resultCode is " + resultCode);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(this);
        menuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_video_configuration:
                //openVideoConfiguration();

                mConfigurationPanel.show(findViewById(R.id.action_video_configuration));
            default:
                return false;
        }
    }

    @Override
    public void onClick(View v) {

        if (v == mStartRecordFabView) {
            if (mIsRecording) {

            } else {
                startScreenRecording();
            }
        } else if (v == mGrantStorageButtonView) {
            ActivityCompat.requestPermissions((Activity) getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_LIST_VIDEOS);
        }
    }

    private void loadVideos() {
        File parentDir = new File(Environment.getExternalStorageDirectory().getPath() + "/Movies/Screencasts/");

        new ListAllFilesTask(parentDir, new ListAllFilesTask.OnFilesListedListener() {

            @Override
            public void onFilesListed(File[] files) {
                new LoadVideoMetaDataTask(new LoadVideoMetaDataTask.OnMetaDataLoadedListener() {
                    @Override
                    public void onVideoMetaDataLoaded(ArrayList<VideoDataContainer> dataList) {
                        ((VideosListAdapter) mRecyclerView.getAdapter()).setData(dataList);
                        mRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                }).execute(files);
            }
        }).execute();
    }

    /**
     * Resets media projection
     */
    private void resetMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        } else {
            if (BuildConfig.DEBUG)
                Log.w(TAG, "resetMediaProjection: Tried to reset media projection but mMediaProjection was null!");
        }
    }

    /**
     * Finds views in layout and assigns them to variables.
     */
    private void assignViews() {
        //Find views in layout
        mRootView = findViewById(R.id.root);
        mStartRecordFabView = (FloatingActionButton) findViewById(R.id.record_fab);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);
        mNoStoragePermissionView = findViewById(R.id.storage_root);
        mGrantStorageButtonView = (Button) findViewById(R.id.storage_button_grant_permission);
        mConfigurationPanel = (ConfigurationView) findViewById(R.id.configuration_panel);

        //Prepare them
        mStartRecordFabView.setOnClickListener(this);
        mGrantStorageButtonView.setOnClickListener(this);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float density = getResources().getDisplayMetrics().density;
        if (metrics.widthPixels / density < 600) {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mRecyclerView.setAdapter(new VideosListAdapter(this));
        } else {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
            mRecyclerView.setLayoutManager(gridLayoutManager);
            mRecyclerView.setAdapter(new VideosListAdapter(this));
        }

        if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            mNoStoragePermissionView.setVisibility(View.INVISIBLE);
            loadVideos();
        } else {
            mRecyclerView.setVisibility(View.INVISIBLE);
        }

    }

    /**
     * Calculates the display metrics.
     */
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

    /**
     * Starts screen recording
     */
    @TargetApi(23)
    private void startScreenRecording() {

        if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) || !checkPermission(Manifest.permission.RECORD_AUDIO)) {

            if (BuildConfig.DEBUG)
                Log.w(TAG, "startScreenRecording: Cannot start screen recording yet. Asking for permissions...");

            String[] permissionRequests;

            if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionRequests = new String[]{Manifest.permission.RECORD_AUDIO};
            } else {
                permissionRequests = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
            }

            requestPermissions(permissionRequests, REQUEST_START_RECORDING);

        } else {

            if (BuildConfig.DEBUG)
                Log.i(TAG, "startScreenRecording: Starting screen recording...");

            if (mMediaRecorder == null) {
                //This class manages to record media into files
                mMediaRecorder = new MediaRecorder();
            }

            if (mProjectionManager == null) {
                //This class manages projection of the screen content
                mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            }

            if (mMediaProjectionCallback == null) {
                //This class receives status updates for the recording
                mMediaProjectionCallback = new MediaProjectionCallback();
            }

            mIsRecording = true;
            mCurrentVideoFile = null;
            initRecorder();
            prepareRecorder();
            RecordingFinishedNotification.cancel(this);
            shareScreen();
            RecordingNotification.createNotification(this);

        }
    }

    /**
     * Stops screen recording
     *
     * @param keep
     */
    private void stopScreenRecording(boolean keep) {
        mIsRecording = false;
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        stopScreenSharing();

        if (keep) {
            addVideoToMediaStore(mCurrentVideoFile);
            new LoadVideoMetaDataTask(new LoadVideoMetaDataTask.OnMetaDataLoadedListener() {
                @Override
                public void onVideoMetaDataLoaded(ArrayList<VideoDataContainer> dataList) {
                    ((VideosListAdapter) mRecyclerView.getAdapter()).getData().add(0, dataList.get(0));
                    mRecyclerView.getAdapter().notifyItemInserted(0);
                    mRecyclerView.getLayoutManager().scrollToPosition(0);
                }
            }).execute(mCurrentVideoFile);
        } else {
            mCurrentVideoFile.delete();
        }
    }

    /**
     * Removes video from list and deletes file if the "Undo" action hasn't been activated.
     *
     * @param deleteIntent
     */
    private void deleteVideoFromList(Intent deleteIntent) {

        final int itemPosition = deleteIntent.getIntExtra(EXTRA_REAL_LIST_POSITION, -1);
        final int realItemPosition = deleteIntent.getIntExtra(EXTRA_LIST_POSITION, -1);
        final VideoDataContainer videoDataContainer = ((VideosListAdapter) mRecyclerView.getAdapter()).getData().get(itemPosition);
        ((VideosListAdapter) mRecyclerView.getAdapter()).getData().remove(itemPosition);
        mRecyclerView.getAdapter().notifyItemRemoved(itemPosition);
        Snackbar.make(mRootView, R.string.snackbak_file_deleted, Snackbar.LENGTH_LONG).setAction(R.string.action_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((VideosListAdapter) mRecyclerView.getAdapter()).getData().add(itemPosition, videoDataContainer);
                mRecyclerView.getAdapter().notifyItemInserted(itemPosition);
                mRecyclerView.scrollToPosition(realItemPosition);
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


        //Load video configuration
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isAudioEnabled = sharedPreferences.getBoolean("microphone_audio_recording", true) && checkPermission(Manifest.permission.RECORD_AUDIO);
        int bitrate = sharedPreferences.getInt("video_bitrate", 1);
        int videoEncoder = sharedPreferences.getInt("video_encoder", 2);
        int outputFormat = sharedPreferences.getInt("video_output_format", -1);
        int fps = sharedPreferences.getInt("video_framerate", 30);

        if (outputFormat == -1) {
            outputFormat = 2;
        }

        //Generate new file to store the video
        mCurrentVideoFile = generateNewVideoFile(outputFormat);

        if (isAudioEnabled)
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(outputFormat);
        mMediaRecorder.setVideoEncoder(videoEncoder);
        if (isAudioEnabled)
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncodingBitRate(bitrate * 1000 * 1024);
        mMediaRecorder.setVideoFrameRate(fps);
        mMediaRecorder.setVideoSize(mDisplayWidth, mDisplayHeight);
        mMediaRecorder.setOutputFile(mCurrentVideoFile.getPath());
    }

    /**
     * Generates file name and creates directory if not exist.
     *
     * @return File for saving video data
     */
    private File generateNewVideoFile(int outputFormat) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "generateNewVideoFile: generating File");
        }
        String extension = generateExtensionFromOutputFormat(outputFormat);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date now = new Date();
        String fileName = "Screencast_" + formatter.format(now) + extension;
        String pathName = Environment.getExternalStorageDirectory().getPath() + "/Movies/Screencasts/";
        new File(pathName).mkdirs();
        File file = new File(pathName + fileName);
        if (!file.exists()) {
            if (BuildConfig.DEBUG)
                Log.w(TAG, "generateNewVideoFile: Cannot create video file!");
        }
        return file;
    }

    private String generateExtensionFromOutputFormat(int outputFormat) {
        /*
        public static final int THREE_GPP = 1;
        public static final int MPEG_4 = 2; */
        String[] outputArray = new String[]{".mp4", ".3gp", ".mp4"};
        return outputArray[outputFormat];
    }

    private void addVideoToMediaStore(File file) {

        ContentValues values = new ContentValues();

        values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mpeg");
        values.put(MediaStore.MediaColumns.DATA, file.getPath());

        getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

    }

    /**
     * Shows a snack bar with given string as message and LENGTH_SHORT.
     *
     * @param stringResourceId The id of string ressource.
     */
    private void showSnackbar(int stringResourceId) {
        Snackbar.make(mRootView, getResources().getString(stringResourceId), Snackbar.LENGTH_SHORT).show();
    }

    private boolean checkPermission(String permission) {
        if (Build.VERSION.SDK_INT >= 23) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }
}
