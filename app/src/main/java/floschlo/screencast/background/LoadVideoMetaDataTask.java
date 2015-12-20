package floschlo.screencast.background;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;

import java.io.File;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

/**
 * Created by Florian on 20.12.2015.
 */
public class LoadVideoMetaDataTask extends AsyncTask<Void, Void, MediaMetadataRetriever> {

    public final static String TAG = LoadVideoMetaDataTask.class.getSimpleName();

    private File mVideoFile;
    private OnMetaDataLoadedListener mListener;

    public LoadVideoMetaDataTask(File videoFile, OnMetaDataLoadedListener listener) {
        mVideoFile = videoFile;
        mListener = listener;
    }

    @Override
    protected MediaMetadataRetriever doInBackground(Void... params) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mVideoFile.getPath());
        return retriever;
    }

    @Override
    protected void onPostExecute(MediaMetadataRetriever retriever) {
        Bitmap videoThumbnail = null;
        String videoTitle = "";// TODO: 20.12.2015 Find way to get video title
        String videoDate = "";
        String videoLength = "";

        videoThumbnail = retriever.getFrameAtTime();// TODO: 20.12.2015 Performance issue (perhaps this method belongs in doInBackground?)
        videoLength = convertVideoLength(Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
        videoDate = convertVideoDate(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));

        retriever.release();

        mListener.onVideoThumbnailLoaded(videoThumbnail, videoTitle, videoDate, videoLength);

    }

    private String convertVideoDate(String rawDate) {
        String[] splitResult = rawDate.split("T");
        String calendarDate = "";
        String timeDate = "";
        try {
            calendarDate = new SimpleDateFormat("dd.MM.yyyy").format(new SimpleDateFormat("yyyyMMdd").parse(splitResult[0]));
            timeDate = new SimpleDateFormat("HH:mm:ss").format(new SimpleDateFormat("HHmmss").parse(splitResult[1].substring(0, splitResult[1].lastIndexOf('.'))));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return calendarDate + " (" + timeDate + ")";
    }

    private String convertVideoLength(int millis) {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    public interface OnMetaDataLoadedListener {

        void onVideoThumbnailLoaded(Bitmap videoThumbnail, String videoTitle, String videoDate, String videoLength);

    }
}
