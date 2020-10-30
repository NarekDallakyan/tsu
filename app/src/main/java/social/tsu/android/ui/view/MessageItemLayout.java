package social.tsu.android.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import social.tsu.android.R;

public class MessageItemLayout extends RelativeLayout {
    private TextView viewPartMessage;
    private View viewPartTime;

    private TypedArray a;

    private RelativeLayout.LayoutParams viewPartMessageLayoutParams;
    private int viewPartMessageWidth;
    private int viewPartMessageHeight;

    private RelativeLayout.LayoutParams viewPartTimeLayoutParams;
    private int viewPartTimeWidth;
    private int viewPartTimeHeight;


    public MessageItemLayout(Context context) {
        super(context);
    }

    public MessageItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        a = context.obtainStyledAttributes(attrs, R.styleable.MessageItemLayout, 0, 0);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        try {
            viewPartMessage = (TextView) this.findViewById(a.getResourceId(R.styleable.MessageItemLayout_viewPartMessage, -1));
            viewPartTime = this.findViewById(a.getResourceId(R.styleable.MessageItemLayout_viewPartTime, -1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (viewPartMessage == null || viewPartTime == null || widthSize <= 0) {
            return;
        }

        int availableWidth = widthSize - getPaddingLeft() - getPaddingRight();
        int availableHeight = heightSize - getPaddingTop() - getPaddingBottom();

        viewPartMessageLayoutParams = (LayoutParams) viewPartMessage.getLayoutParams();
        viewPartMessageWidth = viewPartMessage.getMeasuredWidth() + viewPartMessageLayoutParams.leftMargin + viewPartMessageLayoutParams.rightMargin;
        viewPartMessageHeight = viewPartMessage.getMeasuredHeight() + viewPartMessageLayoutParams.topMargin + viewPartMessageLayoutParams.bottomMargin;

        viewPartTimeLayoutParams = (LayoutParams) viewPartTime.getLayoutParams();
        viewPartTimeWidth = viewPartTime.getMeasuredWidth() + viewPartTimeLayoutParams.leftMargin + viewPartTimeLayoutParams.rightMargin;
        viewPartTimeHeight = viewPartTime.getMeasuredHeight() + viewPartTimeLayoutParams.topMargin + viewPartTimeLayoutParams.bottomMargin;

        int viewPartMainLineCount = viewPartMessage.getLineCount();
        float viewPartMainLastLineWitdh = viewPartMainLineCount > 0 ? viewPartMessage.getLayout().getLineWidth(viewPartMainLineCount - 1) : 0;

        widthSize = getPaddingLeft() + getPaddingRight();
        heightSize = getPaddingTop() + getPaddingBottom();

        if (viewPartMainLineCount > 1 && !(viewPartMainLastLineWitdh + viewPartTimeWidth >= viewPartMessage.getMeasuredWidth())) {
            widthSize += viewPartMessageWidth;
            heightSize += viewPartMessageHeight;
        } else if (viewPartMainLineCount > 1 && (viewPartMainLastLineWitdh + viewPartTimeWidth >= availableWidth)) {
            widthSize += viewPartMessageWidth;
            heightSize += viewPartMessageHeight + viewPartTimeHeight;
        } else if (viewPartMainLineCount == 1 && (viewPartMessageWidth + viewPartTimeWidth >= availableWidth)) {
            widthSize += viewPartMessage.getMeasuredWidth();
            heightSize += viewPartMessageHeight + viewPartTimeHeight;
        } else {
            widthSize += viewPartMessageWidth + viewPartTimeWidth;
            heightSize += viewPartMessageHeight;
        }

        this.setMeasuredDimension(widthSize, heightSize);
        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (viewPartMessage == null || viewPartTime == null) {
            return;
        }

        viewPartMessage.layout(
                getPaddingLeft(),
                getPaddingTop(),
                viewPartMessage.getWidth() + getPaddingLeft(),
                viewPartMessage.getHeight() + getPaddingTop());

        viewPartTime.layout(
                right - left - viewPartTimeWidth - getPaddingRight(),
                bottom - top - getPaddingBottom() - viewPartTimeHeight,
                right - left - getPaddingRight(),
                bottom - top - getPaddingBottom());
    }
}