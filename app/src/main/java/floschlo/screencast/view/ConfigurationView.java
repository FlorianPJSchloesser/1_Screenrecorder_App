package floschlo.screencast.view;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import floschlo.screencast.BuildConfig;
import floschlo.screencast.R;

/**
 * Created by Florian on 03.02.2016.
 */
public class ConfigurationView extends RelativeLayout implements View.OnClickListener{

    public final static String TAG = ConfigurationView.class.getSimpleName();

    /* VIEWS */
    private Toolbar mToolbar;
    private Button mApplyConfigurationButton;
    private Button mCancelConfigurationButton;
    private Spinner mBitrateSpinner;
    private Spinner mEncoderSpinner;
    private Spinner mOutputFormatSpinner;
    private EditText mFpsEditText;
    private View mSpacer;

    /* CONFIGURATION VALUES */
    private int[] mBitrateValues;
    private int[] mEncoderValues;
    private int[] mOutputFormatValues;

    /* RUNTIME VALUES */
    private int mRevealX;
    private int mRevealY;

    public interface ConfigurationActionReceiver {
        void onCancelled();
        void onApplied();
    }

    public ConfigurationView(Context context) {
        super(context);
        init();
    }

    public ConfigurationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConfigurationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(23)
    public ConfigurationView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mRevealX = 0;
        mRevealY = 0;
        inflate(getContext(), R.layout.widget_video_configuration, this);
        loadValues(getResources());
        assignViews();
        prepareToolbar();
        prepareSpinner();
        applyListeners();
        setVisibility(INVISIBLE);
    }

    private void prepareToolbar() {
        mToolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });
        mToolbar.setNavigationIcon(R.drawable.ic_action_back);
        mToolbar.setTitle(R.string.title_toolbar_video_configuration);
    }

    private void assignViews () {
        mToolbar = (Toolbar) findViewById(R.id.configuration_toolbar);
        mApplyConfigurationButton = (Button) findViewById(R.id.button_apply);
        mCancelConfigurationButton = (Button) findViewById(R.id.button_cancel);
        mBitrateSpinner = (Spinner) findViewById(R.id.bitrate_spinner);
        mEncoderSpinner = (Spinner) findViewById(R.id.encoder_spinner);
        mOutputFormatSpinner = (Spinner) findViewById(R.id.output_fromat_spinner);
        mFpsEditText = (EditText) findViewById(R.id.fps_edit);
        mSpacer = findViewById(R.id.spacer);
    }

    private void applyListeners() {
        mApplyConfigurationButton.setOnClickListener(this);
        mCancelConfigurationButton.setOnClickListener(this);
        mSpacer.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mApplyConfigurationButton) {
            applyConfiguration();
            hide();
        } else if (v == mCancelConfigurationButton) {
            hide();
        } else if (v == mSpacer) {
            hide();
        }
    }

    /**
     * Applies the selected configuration to preferences.
     */
    private void applyConfiguration() {

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "applyConfiguration: mBitrateSpinner.getSelectedItemPosition()==" + mBitrateSpinner.getSelectedItemPosition());
            Log.d(TAG, "applyConfiguration: mEncoderSpinner.getSelectedItemPosition()==" + mEncoderSpinner.getSelectedItemPosition());
            Log.d(TAG, "applyConfiguration: mOutputFormatSpinner.getSelectedItemPosition()==" + mOutputFormatSpinner.getSelectedItemPosition());
        }


        //Read selected configuration.
        int bitrate = mBitrateValues[mBitrateSpinner.getSelectedItemPosition()];
        int encoder = mEncoderValues[mEncoderSpinner.getSelectedItemPosition()];
        int outputFormat = mOutputFormatValues[mOutputFormatSpinner.getSelectedItemPosition()];
        int fps = Integer.parseInt(mFpsEditText.getText().toString());

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "applyConfiguration: bitrate==" + bitrate);
            Log.d(TAG, "applyConfiguration: encoder==" + encoder);
            Log.d(TAG, "applyConfiguration: outputFormat==" + outputFormat);
            Log.d(TAG, "applyConfiguration: fps==" + fps);
        }

        //Apply the selected configuration to preferences.
        android.preference.PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit()
                .putInt("video_bitrate", bitrate)
                .putInt("video_encoder", encoder)
                .putInt("video_output_format", outputFormat)
                .putInt("video_framerate", fps)
                .apply();
    }



    private void restoreConfiguration() {
        SharedPreferences sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
        int storedBitrate = sharedPreferences.getInt("video_bitrate", 1);
        int storedEncoder = sharedPreferences.getInt("video_encoder", 0);
        int storedOutputFormat = sharedPreferences.getInt("video_output_format", -1);
        int storedFps = sharedPreferences.getInt("video_framerate", 30);

        for (int i = 0; i < mBitrateValues.length; i++) {
            if (mBitrateValues[i] == storedBitrate)
                mBitrateSpinner.setSelection(i, true);
            Log.w(TAG, "restoreConfiguration: lksjhgriuehrgoiejrogiejhrogi ");
        }
        for (int i = 0; i < mEncoderValues.length; i++) {
            if (mEncoderValues[i] == storedEncoder)
                mEncoderSpinner.setSelection(i, true);
        }
        for (int i = 0; i < mOutputFormatValues.length; i++) {
            if (mOutputFormatValues[i] == storedOutputFormat)
                mOutputFormatSpinner.setSelection(i, true);
        }

        mFpsEditText.setText("" + storedFps);
    }

    /**
     * Creates adapters and applies them to the spinners.
     */
    private void prepareSpinner() {

        ArrayAdapter<CharSequence> mBitrateAdapter = ArrayAdapter.createFromResource(getContext(), R.array.prefs_entries_video_bitrate, R.layout.simple_list_item);
        ArrayAdapter<CharSequence> mEncoderAdapter = ArrayAdapter.createFromResource(getContext(), R.array.prefs_entries_video_encoder, R.layout.simple_list_item);
        ArrayAdapter<CharSequence> mOutputFormatAdapter = ArrayAdapter.createFromResource(getContext(), R.array.prefs_entries_output_format, R.layout.simple_list_item);

        mBitrateSpinner.setAdapter(mBitrateAdapter);
        mEncoderSpinner.setAdapter(mEncoderAdapter);
        mOutputFormatSpinner.setAdapter(mOutputFormatAdapter);
    }

    /**
     * Loads preferences values from resources.
     *
     * @param resources
     */
    private void loadValues(Resources resources) {
        mBitrateValues = resources.getIntArray(R.array.prefs_video_bitrate);
        mEncoderValues = resources.getIntArray(R.array.prefs_video_encoder);
        mOutputFormatValues = resources.getIntArray(R.array.prefs_output_format);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loadValues: mBitrateValues.length==" + mBitrateValues.length);
            Log.d(TAG, "loadValues: mEncoderValues.length==" + mEncoderValues.length);
            Log.d(TAG, "loadValues: mOutputFormatValues.length==" + mOutputFormatValues.length);
        }
    }

    public void show () {
        show(null);
    }

    public void show(View revealSource) {
        if (!isShown()) {
            restoreConfiguration();
            if (revealSource != null) {
                mRevealX = (int) (revealSource.getX() + (revealSource.getWidth() / 2));
                mRevealY = (int) (revealSource.getY() + (revealSource.getHeight() / 2));
            }
            Animator animator = ViewAnimationUtils.createCircularReveal(this,
                    mRevealX,
                    mRevealY,
                    0,
                    Math.max(this.getWidth(), this.getHeight()));
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {

                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.start();
        }
    }

    public void hide() {
        if (isShown()) {
            Animator animator = ViewAnimationUtils.createCircularReveal(this,
                    mRevealX,
                    mRevealY,
                    Math.max(this.getWidth(), this.getHeight()),
                    0);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.start();
        }
    }
}
