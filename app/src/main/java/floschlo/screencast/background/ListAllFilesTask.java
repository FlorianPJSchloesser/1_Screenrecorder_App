package floschlo.screencast.background;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import floschlo.screencast.BuildConfig;

/**
 * Created by Florian on 25.01.2016.
 */
public class ListAllFilesTask extends AsyncTask<Void, Void, File[]> {

    public final static String TAG = ListAllFilesTask.class.getSimpleName();

    private File mParentDir;
    private OnFilesListedListener mListener;

    public ListAllFilesTask(File parentDir, OnFilesListedListener onFilesListedListener) {
        mParentDir = parentDir;
        mListener = onFilesListedListener;
    }

    @Override
    protected File[] doInBackground(Void... params) {
        return getAllVideoFiles(mParentDir);
    }

    @Override
    protected void onPostExecute(File[] files) {
        mListener.onFilesListed(files);
    }

    /**
     * Find all video files in screencast folder.
     * @return Array holdng all found file objects.
     */
    private File[] getAllVideoFiles(File parentDir) {

        if (BuildConfig.DEBUG)
            Log.i(TAG, "getAllVideoFiles: Listing all items loaded from directory.");

        if (BuildConfig.DEBUG)
            Log.d(TAG, "getAllVideoFiles: parentDir is '" + parentDir.getPath() + "'");

        ArrayList<File> videoFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
        if (files != null && files.length > 0) {
            for (int i = files.length - 1; i >= 0; i--) {
                File currentFiles = files[i];
                if (currentFiles.isDirectory()) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "getAllVideoFiles: Found a folder!");
                } else {
                    //If the current file is actually a file, we have to check for the name extension.
                    if (currentFiles.getName().endsWith(".mp4")) {
                        videoFiles.add(currentFiles);
                        if (BuildConfig.DEBUG)
                            Log.i(TAG, "getAllVideoFiles: Added file '" + currentFiles.getName() + "'");
                    }
                }
            }
        }
        File[] fileArray = new File[videoFiles.size()];
        fileArray = videoFiles.toArray(fileArray);
        return fileArray;
    }

    public interface OnFilesListedListener {
        void onFilesListed (File[] files);
    }
}
