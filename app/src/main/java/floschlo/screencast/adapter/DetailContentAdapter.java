package floschlo.screencast.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import floschlo.screencast.R;
import floschlo.screencast.container.VideoDataContainer;

/**
 * Created by Florian on 19.01.2016.
 */
public class DetailContentAdapter extends RecyclerView.Adapter<DetailContentAdapter.ContentItemViewHolder> {

    public final static String TAG = DetailContentAdapter.class.getSimpleName();

    private VideoDataContainer mVideoDataContainer;

    public DetailContentAdapter(VideoDataContainer videoDataContainer) {
        mVideoDataContainer = videoDataContainer;
    }

    @Override
    public ContentItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ContentItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_detail_content, parent, false));
    }

    @Override
    public void onBindViewHolder(ContentItemViewHolder holder, int position) {
        switch (position) {
            case 0:
                recordDateHolder(holder);
                break;
            case 1:
                recordLengthHolder(holder);
                break;
            case 2:
                recordSizeHolder(holder);
                break;
        }
    }

    private void recordSizeHolder(ContentItemViewHolder holder) {
        holder.mTitleView.setText("Size: ");
        holder.mValueView.setText(mVideoDataContainer.getVideoSize());
    }

    private void recordLengthHolder(ContentItemViewHolder holder) {
        holder.mTitleView.setText("Length: ");
        holder.mValueView.setText(mVideoDataContainer.getVideoLength());
    }

    private void recordDateHolder(ContentItemViewHolder holder) {
        holder.mTitleView.setText("Date: ");
        holder.mValueView.setText(mVideoDataContainer.getVideoDate());
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public class ContentItemViewHolder extends RecyclerView.ViewHolder {

        TextView mTitleView;
        TextView mValueView;

        public ContentItemViewHolder(View itemView) {
            super(itemView);
            mTitleView = (TextView) itemView.findViewById(R.id.listitem_title);
            mValueView = (TextView) itemView.findViewById(R.id.listitem_value);
        }
    }
}
