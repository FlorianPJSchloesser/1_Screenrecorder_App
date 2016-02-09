package floschlo.screencast.background;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;

/**
 * Created by Florian on 28.12.2015.
 */
public class LoadVideoThumbnailTask extends AsyncTask<Void, Void, Bitmap> {

    public final static String TAG = LoadVideoThumbnailTask.class.getSimpleName();

    private OnVideoThumbnailLoadListener mListener;
    private String mPath;

    public LoadVideoThumbnailTask(String path, OnVideoThumbnailLoadListener listener) {
        mPath = path;
        mListener = listener;
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        if (!isCancelled()) {
            return ThumbnailUtils.createVideoThumbnail(mPath, MediaStore.Video.Thumbnails.MINI_KIND);
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null && !isCancelled())
            mListener.onThumbnailLoad(bitmap);
    }

    public interface OnVideoThumbnailLoadListener {
        void onThumbnailLoad(Bitmap thumbnail);
    }
}
