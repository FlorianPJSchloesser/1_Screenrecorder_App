package floschlo.screencast.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import floschlo.screencast.BuildConfig;
import floschlo.screencast.R;
import floschlo.screencast.adapter.DetailContentAdapter;
import floschlo.screencast.background.LoadVideoMetaDataTask;
import floschlo.screencast.background.LoadVideoThumbnailTask;
import floschlo.screencast.container.VideoDataContainer;

/**
 * Created by Florian on 18.01.2016.
 */
public class VideoDetailActivity extends AppCompatActivity implements View.OnClickListener, LoadVideoMetaDataTask.OnMetaDataLoadedListener {

    public final static String TAG = VideoDetailActivity.class.getSimpleName();

    public final static String EXTRA_PATH = VideoDetailActivity.class.getCanonicalName() + ".EXTRA_PATH";

    /* VIEWS */
    private Toolbar mToolbar;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private ImageView mVideoThumbnailView;
    private FloatingActionButton mFloatingActionButton;
    private RecyclerView mContentRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        findViews();
        prepareRecycler();
        applyListeners();
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        startLoadingVideoData(getIntent().getStringExtra(EXTRA_PATH));
    }

    private void prepareRecycler() {
        mContentRecycler.setHasFixedSize(true);
        mContentRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    private void applyContentRecyclerAdapter(VideoDataContainer videoDataContainer) {
        mContentRecycler.setAdapter(new DetailContentAdapter(videoDataContainer));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_rename:
                startRenameAction();
                return true;
        }
        return false;
    }

    private void startRenameAction() {

        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setSingleLine(true);
        new AlertDialog.Builder(this).setTitle("Rename").setMessage("Plase enter new name of file").setPositiveButton("Apply", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File file = new File(getIntent().getStringExtra(EXTRA_PATH));
                file.renameTo(new File(file.getParentFile() + "/" + editText.getText().toString() + ".mp4"));
            }
        }).setNegativeButton("Cancel", null).setView(editText).show();
    }

    private void startLoadingVideoData(String path) {
        new LoadVideoMetaDataTask(this).execute(new File(path));
        new LoadVideoThumbnailTask(path, new LoadVideoThumbnailTask.OnVideoThumbnailLoadListener() {
            @Override
            public void onThumbnailLoad(Bitmap thumbnail) {
                mVideoThumbnailView.setImageBitmap(thumbnail);
            }
        }).execute();
    }

    private void findViews() {
        mToolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.detail_collapsing);
        mVideoThumbnailView = (ImageView) findViewById(R.id.detail_thumbnail);
        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.detail_fab);
        mContentRecycler = (RecyclerView) findViewById(R.id.detail_recycler);
    }

    private void applyListeners() {
        mFloatingActionButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mFloatingActionButton)) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "playVideo: Start playing video from detail");

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(getIntent().getStringExtra(EXTRA_PATH))), "video/mpeg");
            // Verify it resolves
            PackageManager packageManager = this.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
            boolean isIntentSafe = activities.size() > 0;

            // Start an activity if it's safe
            if (isIntentSafe) {
                startActivity(intent);
            }
        }
    }

    @Override
    public void onVideoMetaDataLoaded(ArrayList<VideoDataContainer> dataList) {
        if (dataList.size() < 1) {
            if(BuildConfig.DEBUG) {
                Log.e(TAG, "onVideoMetaDataLoaded: dataList has no items");
            }
        } else {
            mCollapsingToolbarLayout.setTitle(dataList.get(0).getVideoTitle());
            applyContentRecyclerAdapter(dataList.get(0));
        }
    }
}
