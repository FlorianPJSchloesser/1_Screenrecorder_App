package floschlo.screencast.container;

import android.util.Log;

import floschlo.screencast.BuildConfig;

/**
 * Created by Florian on 24.12.2015.
 */
public class VideoDataContainer {

    public final static String TAG = VideoDataContainer.class.getSimpleName();

    protected String mVideoTitle;
    protected String mVideoLength;
    protected String mVideoDate;
    protected String mVideoPath;
    protected String mVideoSize;

    public String getVideoTitle() {
        return mVideoTitle;
    }

    public String getVideoLength() {
        return mVideoLength;
    }

    public String getVideoDate() {
        return mVideoDate;
    }

    public String getVideoPath() {
        return mVideoPath;
    }

    public String getVideoSize() {
        return mVideoSize;
    }

    public VideoDataContainer (String videoTitle, String videoLength, String videoDate, String videoPath, String videoSize) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "VideoDataContainer: Creating video data container");

        mVideoTitle = videoTitle;
        mVideoLength = videoLength;
        mVideoDate = videoDate;
        mVideoPath = videoPath;
        mVideoSize = videoSize;
    }

}
