package social.tsu.camerarecorder.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;


public class PortraitFrameLayout extends FrameLayout {

    public PortraitFrameLayout(Context context) {
        super(context);
    }

    public PortraitFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PortraitFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        setMeasuredDimension(width, width / 9 * 16);
    }
}
