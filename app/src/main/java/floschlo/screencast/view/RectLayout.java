package floschlo.screencast.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import floschlo.screencast.R;

/**
 * Advanced RelativeLayout always having same height as width.
 */
public class RectLayout extends RelativeLayout {

    public final static String TAG = RectLayout.class.getSimpleName();

    public RectLayout(Context context) {
        super(context);
    }

    public RectLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RectLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RectLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }
}
