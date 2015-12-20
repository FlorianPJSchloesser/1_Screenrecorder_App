package floschlo.screencast.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import floschlo.screencast.BuildConfig;
import floschlo.screencast.R;
import floschlo.screencast.background.LoadVideoMetaDataTask;

/**
 * Created by Florian on 19.12.2015.
 */
public class AllVideosAdapter extends RecyclerView.Adapter<AllVideosAdapter.VideoViewHolder>{

    public final static String TAG = AllVideosAdapter.class.getSimpleName();

    ArrayList<File> mAllVideos;

    Activity mActivity;

    public AllVideosAdapter (Activity activity) {

        mActivity = activity;

        mAllVideos = getAllVideoFiles(activity);

    }

    @Override
    public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater lLayoutInflater = LayoutInflater.from(parent.getContext());
        return new VideoViewHolder(lLayoutInflater.inflate(R.layout.layout_videocard, parent, false));

    }

    @Override
    public void onBindViewHolder(final VideoViewHolder holder, final int position) {

        holder.mTitleView.setText("Loading...");
        holder.mDateView.setText("--.--.-- (--:--)");
        holder.mLengthView.setText("--:--:--");
        holder.mVideoThumbnailImage.setImageResource(R.mipmap.ic_launcher);

        new LoadVideoMetaDataTask(mAllVideos.get(position), new LoadVideoMetaDataTask.OnMetaDataLoadedListener() {
            @Override
            public void onVideoThumbnailLoaded(Bitmap videoThumbnail, String videoTitle, String videoDate, String videoLength) {
                holder.mTitleView.setText(videoTitle);
                holder.mDateView.setText(videoDate);
                holder.mLengthView.setText(videoLength);
                holder.mVideoThumbnailImage.setImageBitmap(videoThumbnail);
            }
        }).execute();

        holder.mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playVideo(position);
            }
        });

        holder.mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareVideo(position);
            }
        });

    }

    private void shareVideo(int pPosition) {
        // FIXME: 20.12.2015 Failure on intent start
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setDataAndType(Uri.parse(mAllVideos.get(pPosition).getPath()), "video/mpeg");
        mActivity.startActivity(intent);
    }

    private void playVideo(int pPosition) {
        // FIXME: 20.12.2015 Failure on intent start
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(mAllVideos.get(pPosition).getPath()), "video/*");
        mActivity.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return mAllVideos.size();
    }

    private ArrayList<File> getAllVideoFiles(Context pContext) {

        File parentDir = new File(Environment.getExternalStorageDirectory().getPath() + "/Movies/Screencasts/");

        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if (BuildConfig.DEBUG) Log.w(TAG, "getAllVideoFiles: found a folder!");
            } else {
                if(file.getName().endsWith(".mp4")){
                    inFiles.add(file);
                }
            }
        }
        return inFiles;
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder {

        View mRoot;
        ImageView mVideoThumbnailImage;
        TextView mTitleView;
        TextView mDateView;
        TextView mLengthView;
        Button mPlayButton;
        Button mShareButton;
        Button mDeleteButton;

        public VideoViewHolder(View itemView) {
            super(itemView);
            mRoot = itemView.findViewById(R.id.root);
            mPlayButton = (Button) itemView.findViewById(R.id.btn_play);
            mShareButton = (Button) itemView.findViewById(R.id.btn_share);
            mDeleteButton = (Button) itemView.findViewById(R.id.btn_delete);
            mVideoThumbnailImage = (ImageView) itemView.findViewById(R.id.videocard_thumbnail);
            mTitleView = (TextView) itemView.findViewById(R.id.videocard_title);
            mDateView = (TextView) itemView.findViewById(R.id.videocard_date);
            mLengthView = (TextView) itemView.findViewById(R.id.videocard_length);
        }
    }
}
