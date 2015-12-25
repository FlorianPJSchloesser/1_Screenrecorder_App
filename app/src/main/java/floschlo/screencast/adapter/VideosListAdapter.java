package floschlo.screencast.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import floschlo.screencast.BuildConfig;
import floschlo.screencast.R;
import floschlo.screencast.activity.MainActivity;
import floschlo.screencast.container.VideoDataContainer;

/**
 * Created by Florian on 24.12.2015.
 */
public class VideosListAdapter extends RecyclerView.Adapter<VideosListAdapter.VideoCardViewHolder> {

    public final static String TAG = VideosListAdapter.class.getSimpleName();

    private Context mContext;

    private ArrayList<VideoDataContainer> mVideoDataList;

    public VideosListAdapter(Context context) {
        mContext = context;
    }

    public void setData (ArrayList<VideoDataContainer> data) {
        mVideoDataList = data;
    }

    public ArrayList<VideoDataContainer> getData () {
        return mVideoDataList;
    }

    @Override
    public VideoCardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater lLayoutInflater = LayoutInflater.from(parent.getContext());
        VideoCardViewHolder videoViewHolder = new VideoCardViewHolder(lLayoutInflater.inflate(R.layout.layout_videocard, parent, false));

        return videoViewHolder;
    }

    @Override
    public void onBindViewHolder(VideoCardViewHolder holder, int position) {
        holder.applyVideoDataContainer(mVideoDataList.get(position));
        applyVideoCardListeners(holder, mVideoDataList.get(position));
    }

    @Override
    public int getItemCount() {
        if (mVideoDataList != null) {
            return mVideoDataList.size();
        }
        return 0;
    }

    private void applyVideoCardListeners(final VideoCardViewHolder holder, VideoDataContainer videoDataContainer) {
        holder.mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setDataAndType(Uri.fromFile(new File(mVideoDataList.get(holder.getAdapterPosition()).getVideoPath())), "video/mpeg");

                Intent chooser = Intent.createChooser(intent, "Share with");
                mContext.startActivity(chooser);
            }
        });

        holder.mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "playVideo: Start playing video from card");

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(mVideoDataList.get(holder.getAdapterPosition()).getVideoPath())), "video/mpeg");
                // Verify it resolves
                PackageManager packageManager = mContext.getPackageManager();
                List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
                boolean isIntentSafe = activities.size() > 0;

                // Start an activity if it's safe
                if (isIntentSafe) {
                    mContext.startActivity(intent);
                }
            }
        });

        holder.mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.setAction(MainActivity.ACTION_FILE_DELETE);
                intent.putExtra(MainActivity.EXTRA_LIST_POSITION, holder.getAdapterPosition());
                mContext.startActivity(intent);
            }
        });
    }

    public class VideoCardViewHolder extends RecyclerView.ViewHolder {

        int mId = -1;

        View mRoot;
        ImageView mVideoThumbnailImage;
        TextView mTitleView;
        TextView mDateView;
        TextView mLengthView;
        TextView mSizeView;
        Button mPlayButton;
        Button mShareButton;
        Button mDeleteButton;

        public VideoCardViewHolder(View itemView) {
            super(itemView);
            mRoot = itemView.findViewById(R.id.root);
            mPlayButton = (Button) itemView.findViewById(R.id.btn_play);
            mShareButton = (Button) itemView.findViewById(R.id.btn_share);
            mDeleteButton = (Button) itemView.findViewById(R.id.btn_delete);
            mVideoThumbnailImage = (ImageView) itemView.findViewById(R.id.videocard_thumbnail);
            mTitleView = (TextView) itemView.findViewById(R.id.videocard_title);
            mDateView = (TextView) itemView.findViewById(R.id.videocard_date);
            mLengthView = (TextView) itemView.findViewById(R.id.videocard_length);
            mSizeView = (TextView) itemView.findViewById(R.id.videocard_size);
        }

        public void applyVideoDataContainer (VideoDataContainer videoDataContainer) {
            mTitleView.setText(videoDataContainer.getVideoTitle());
            mLengthView.setText(videoDataContainer.getVideoLength());
            mDateView.setText(videoDataContainer.getVideoDate());
            mSizeView.setText(videoDataContainer.getVideoSize());
        }
    }
}
