package floschlo.screencast.background;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import floschlo.screencast.BuildConfig;
import floschlo.screencast.container.VideoDataContainer;

/**
 * Created by Florian on 20.12.2015.
 */
public class LoadVideoMetaDataTask extends AsyncTask<File, Void, ArrayList<VideoDataContainer>> {

    public final static String TAG = LoadVideoMetaDataTask.class.getSimpleName();

    private OnMetaDataLoadedListener mListener;

    public LoadVideoMetaDataTask(OnMetaDataLoadedListener listener) {
        mListener = listener;
    }

    @Override
    protected ArrayList<VideoDataContainer> doInBackground(File... files) {

        ArrayList<VideoDataContainer> data = new ArrayList<>();

        for (File file : files) {

            if (isCancelled()) {
                return data;
            }

            if (file.exists() && file.isFile()) {

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(file.getPath());
                VideoDataContainer dataContainer = new VideoDataContainer(
                        file.getName(),
                        convertVideoLength(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)),
                        convertVideoDate(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)),
                        file.getPath(),
                        convertFileSize(file.length())


                );
                data.add(dataContainer);
                if (BuildConfig.DEBUG)
                    Log.i(TAG, "doInBackground: Data added");
            }
        }

        return data;
    }



    private String convertFileSize(long length) {

        double mb = length / 1048576;

        return mb + "MB";

    }

    @Override
    protected void onPostExecute(ArrayList<VideoDataContainer> dataContainers) {
        mListener.onVideoMetaDataLoaded(dataContainers);
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

    private String convertVideoLength(String strMillis) {
        int millis = Integer.parseInt(strMillis);
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    public interface OnMetaDataLoadedListener {

        void onVideoMetaDataLoaded(ArrayList<VideoDataContainer> dataList);

    }
}
