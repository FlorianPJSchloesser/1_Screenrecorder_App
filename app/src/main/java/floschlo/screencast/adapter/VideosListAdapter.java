package floschlo.screencast.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import floschlo.screencast.BuildConfig;
import floschlo.screencast.R;
import floschlo.screencast.activity.MainActivity;
import floschlo.screencast.activity.VideoDetailActivity;
import floschlo.screencast.background.LoadVideoThumbnailTask;
import floschlo.screencast.container.VideoDataContainer;
import floschlo.screencast.utils.IntentUtils;

/**
 * Created by Florian on 24.12.2015.
 */
public class VideosListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public final static String TAG = VideosListAdapter.class.getSimpleName();

    private final static int TYPE_CARD = 0;
    private final static int TYPE_FOOTER = 1;

    private Context mContext;

    private HashMap<Integer, LoadVideoThumbnailTask> mTasksList;

    private ArrayList<VideoDataContainer> mVideoDataList;

    private int mIdCount;

    public VideosListAdapter(Context context) {
        mContext = context;
        mTasksList = new HashMap<>();
        mIdCount = 1;
    }

    public void setData (ArrayList<VideoDataContainer> data) {
        mVideoDataList = data;
    }

    public ArrayList<VideoDataContainer> getData () {
        return mVideoDataList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater lLayoutInflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_CARD:
                VideoCardViewHolder videoViewHolder = new VideoCardViewHolder(lLayoutInflater.inflate(R.layout.layout_videocard, parent, false));
                videoViewHolder.mId = mIdCount;
                mIdCount++;
                return videoViewHolder;
            case TYPE_FOOTER:
                FooterViewHolder footerViewHolder = new FooterViewHolder(lLayoutInflater.inflate(R.layout.layout_footer, parent, false));
                return footerViewHolder;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof VideoCardViewHolder) {
            ((VideoCardViewHolder)holder).reset();
            ((VideoCardViewHolder)holder).applyVideoDataContainer(mVideoDataList.get(position));
            applyVideoCardListeners(((VideoCardViewHolder)holder), mVideoDataList.get(position));
            startLoadThumbnail(((VideoCardViewHolder)holder));
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).mTextView.setText(
                    mContext.getResources().getQuantityString(R.plurals.footer_video_count, mVideoDataList.size())
            );
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < mVideoDataList.size()) {
            return TYPE_CARD;
        } else {
            return TYPE_FOOTER;
        }
    }

    @Override
    public int getItemCount() {
        if (mVideoDataList != null) {
            return mVideoDataList.size() + 1;
        }
        return 0;
    }

    private void startLoadThumbnail (final VideoCardViewHolder holder) {
        if (mTasksList.containsKey(holder.mId)) {
            mTasksList.get(holder.mId).cancel(true);
            mTasksList.remove(holder.mId);
        }
        LoadVideoThumbnailTask task = new LoadVideoThumbnailTask(mVideoDataList.get(holder.mId).getVideoPath(), new LoadVideoThumbnailTask.OnVideoThumbnailLoadListener() {
            @Override
            public void onThumbnailLoad(Bitmap thumbnail) {
                holder.setThumbnail(thumbnail);
                mTasksList.remove(holder.mId);
            }
        });
        mTasksList.put(holder.mId, task);
        task.execute();
    }

    private void applyVideoCardListeners(final VideoCardViewHolder holder, VideoDataContainer videoDataContainer) {
        holder.mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri videoUri = Uri.fromFile(new File(mVideoDataList.get(holder.getAdapterPosition()).getVideoPath()));
                IntentUtils.shareViedeo(mContext, videoUri);
            }
        });

        holder.mRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, VideoDetailActivity.class);
                intent.putExtra(VideoDetailActivity.EXTRA_PATH, mVideoDataList.get(holder.getAdapterPosition()).getVideoPath());

                mContext.startActivity(intent);
            }
        });

        holder.mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "playVideo: Start playing video from card");
                Uri videoUri = Uri.fromFile(new File(mVideoDataList.get(holder.getAdapterPosition()).getVideoPath()));
                IntentUtils.playVideo(mContext, videoUri);
            }
        });

        holder.mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.setAction(MainActivity.ACTION_FILE_DELETE);
                intent.putExtra(MainActivity.EXTRA_LIST_POSITION, holder.getAdapterPosition());
                intent.putExtra(MainActivity.EXTRA_REAL_LIST_POSITION, holder.getLayoutPosition());
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
        ProgressBar mProgress;

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
            mProgress = (ProgressBar) itemView.findViewById(R.id.videocard_progress);

            reset();
        }

        public void setThumbnail (Bitmap bitmap) {
            mVideoThumbnailImage.setImageBitmap(bitmap);
        }

        public void applyVideoDataContainer (VideoDataContainer videoDataContainer) {

            mProgress.setVisibility(View.GONE);
            mTitleView.setVisibility(View.VISIBLE);
            mLengthView.setVisibility(View.VISIBLE);
            mDateView.setVisibility(View.VISIBLE);
            mSizeView.setVisibility(View.VISIBLE);
            mPlayButton.setEnabled(true);
            mShareButton.setEnabled(true);
            mDeleteButton.setEnabled(true);

            mTitleView.setText(videoDataContainer.getVideoTitle());
            mLengthView.setText(videoDataContainer.getVideoLength());
            mDateView.setText(videoDataContainer.getVideoDate());
            mSizeView.setText(videoDataContainer.getVideoSize());
        }

        public void reset () {
            mVideoThumbnailImage.setImageResource(R.drawable.ic_placeholder_thumbnail);
            mProgress.setVisibility(View.VISIBLE);
            mTitleView.setVisibility(View.GONE);
            mLengthView.setVisibility(View.GONE);
            mDateView.setVisibility(View.GONE);
            mSizeView.setVisibility(View.GONE);
            mPlayButton.setEnabled(false);
            mShareButton.setEnabled(false);
            mDeleteButton.setEnabled(false);
        }
    }

    public class FooterViewHolder extends RecyclerView.ViewHolder {

        TextView mTextView;

        public FooterViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.footer_text);
        }
    }
}
