package social.tsu.trimmer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;

import java.text.DecimalFormat;

import iknow.android.utils.DateUtil;
import iknow.android.utils.UnitConverter;
import social.tsu.trimmer.R;
import social.tsu.trimmer.features.trim.VideoTrimmerUtil;

public class RangeSeekBarView extends View {
    public static final int INVALID_POINTER_ID = 255;
    public static final int ACTION_POINTER_INDEX_MASK = 0x0000ff00, ACTION_POINTER_INDEX_SHIFT = 8;
    private static final String TAG = RangeSeekBarView.class.getSimpleName();
    private static final int TextPositionY = UnitConverter.dpToPx(7);
    private static final int paddingTop = UnitConverter.dpToPx(10);
    private final Paint mVideoTrimTimePaintL = new Paint();
    private final Paint mVideoTrimTimePaintR = new Paint();
    private final Paint mShadow = new Paint();
    private final float padding = 0;
    private int mActivePointerId = INVALID_POINTER_ID;
    private long mMinShootTime = VideoTrimmerUtil.MIN_SHOOT_DURATION;
    private double absoluteMinValuePrim, absoluteMaxValuePrim;
    private double normalizedMinValue = 0d;
    private double normalizedMaxValue = 1d;
    private double normalizedMinValueTime = 0d;
    private double normalizedMaxValueTime = 1d;
    private int mScaledTouchSlop;
    private Bitmap thumbImageLeft;
    private Bitmap thumbImageRight;
    private Bitmap thumbPressedImage;
    private Paint paint;
    private Paint rectPaint;
    private int thumbWidth;
    private float thumbHalfWidth;
    private long mStartPosition = 0;
    private long mEndPosition = 0;
    private float thumbPaddingTop = 0;
    private boolean isTouchDown;
    private float mDownMotionX;
    private boolean mIsDragging;
    private Thumb pressedThumb;
    private boolean isMin;
    private double min_width = 1;
    private boolean notifyWhileDragging = false;
    private OnRangeSeekBarChangeListener mRangeSeekBarChangeListener;
    private int whiteColorRes = getContext().getResources().getColor(R.color.white);

    private float firstPointer = 0;
    private float secondPointer = 0;

    private Canvas mCanvas;
    private int w;
    private int h;

    public RangeSeekBarView(Context context) {
        super(context);
    }

    public RangeSeekBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RangeSeekBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RangeSeekBarView(Context context, long absoluteMinValuePrim, long absoluteMaxValuePrim) {
        super(context);
        this.absoluteMinValuePrim = absoluteMinValuePrim;
        this.absoluteMaxValuePrim = absoluteMaxValuePrim;
        setFocusable(true);
        setFocusableInTouchMode(true);
        init();
    }

    private void init() {
        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        thumbImageLeft = BitmapFactory.decodeResource(getResources(), R.drawable.left);
        thumbImageRight = BitmapFactory.decodeResource(getResources(), R.drawable.right);
        int width = thumbImageLeft.getWidth();
        int height = thumbImageLeft.getHeight();
        int newWidth = UnitConverter.dpToPx(11);
        int newHeight = UnitConverter.dpToPx(55);
        float scaleWidth = newWidth * 1.0f / width;
        float scaleHeight = newHeight * 1.0f / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        thumbImageLeft = Bitmap.createBitmap(thumbImageLeft, 0, 0, width, height, matrix, true);
        thumbImageRight = Bitmap.createBitmap(thumbImageRight, 0, 0, width, height, matrix, true);
        thumbPressedImage = thumbImageLeft;
        thumbWidth = newWidth;
        thumbHalfWidth = thumbWidth / 2;
        int shadowColor = getContext().getResources().getColor(R.color.shadow_color);
        mShadow.setAntiAlias(true);
        mShadow.setColor(shadowColor);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setStyle(Paint.Style.FILL);
        rectPaint.setColor(whiteColorRes);

        mVideoTrimTimePaintL.setStrokeWidth(3);
        mVideoTrimTimePaintL.setARGB(255, 51, 51, 51);
        mVideoTrimTimePaintL.setTextSize(28);
        mVideoTrimTimePaintL.setAntiAlias(true);
        mVideoTrimTimePaintL.setColor(whiteColorRes);
        mVideoTrimTimePaintL.setTextAlign(Paint.Align.LEFT);

        mVideoTrimTimePaintR.setStrokeWidth(3);
        mVideoTrimTimePaintR.setARGB(255, 51, 51, 51);
        mVideoTrimTimePaintR.setTextSize(28);
        mVideoTrimTimePaintR.setAntiAlias(true);
        mVideoTrimTimePaintR.setColor(whiteColorRes);
        mVideoTrimTimePaintR.setTextAlign(Paint.Align.RIGHT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 300;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        int height = 120;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        }
        setMeasuredDimension(width, height);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mCanvas = mCanvas;
        float bg_middle_left = 0;
        float bg_middle_right = getWidth() - getPaddingRight();
        float rangeL = normalizedToScreen(normalizedMinValue);
        float rangeR = normalizedToScreen(normalizedMaxValue);
        Rect leftRect = new Rect((int) bg_middle_left, getHeight(), (int) rangeL, 0);
        Rect rightRect = new Rect((int) rangeR, getHeight(), (int) bg_middle_right, 0);
        canvas.drawRect(leftRect, mShadow);
        canvas.drawRect(rightRect, mShadow);

        drawThumb(normalizedToScreen(normalizedMaxValue), false, canvas, false, leftRect, rightRect);
        drawThumb(normalizedToScreen(normalizedMinValue), false, canvas, true, leftRect, rightRect);
        drawVideoTrimTimeText(canvas);
    }

    private void drawThumb(float screenCoord, boolean pressed, Canvas canvas, boolean isLeft, Rect leftRect, Rect rightRect) {

        if (isLeft) {
            firstPointer = screenCoord;
        } else {
            secondPointer = screenCoord - thumbWidth;
        }

        canvas.drawBitmap(pressed ? thumbPressedImage : (isLeft ? thumbImageLeft : thumbImageRight), screenCoord - (isLeft ? 0 : thumbWidth), paddingTop,
                paint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                        18,
                        getResources().getDisplayMetrics()));
        textPaint.setTextAlign(Paint.Align.CENTER);

        String intervalText = getVideoTrimDuration() + "s";

        float x = ((secondPointer - firstPointer) / 2) + firstPointer;
        float y = (int) ((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));
        Paint rectangularPoint = new Paint();
        rectangularPoint.setColor(Color.BLACK);
        rectangularPoint.setAlpha(90);
        rectangularPoint.setStrokeWidth(150);
        //canvas.drawPoint(x, y, rectangularPoint);
        //canvas.drawBitmap(thumbImageLeft, (x - thumbImageLeft.getWidth()) / 2, (h - thumbImageLeft.getHeight()) / 2, rectangularPoint);
        canvas.drawText(intervalText, x, y, textPaint);
    }

    public long getVideoTrimDuration() {
        return mEndPosition - mStartPosition;
    }

    private void drawVideoTrimTimeText(Canvas canvas) {
        String leftThumbsTime = DateUtil.convertSecondsToTime(mStartPosition);
        String rightThumbsTime = DateUtil.convertSecondsToTime(mEndPosition);
        canvas.drawText(leftThumbsTime, normalizedToScreen(normalizedMinValue), TextPositionY, mVideoTrimTimePaintL);
        canvas.drawText(rightThumbsTime, normalizedToScreen(normalizedMaxValue), TextPositionY, mVideoTrimTimePaintR);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.w = w;
        this.h = h;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isTouchDown) {
            return super.onTouchEvent(event);
        }
        if (event.getPointerCount() > 1) {
            return super.onTouchEvent(event);
        }

        if (!isEnabled()) return false;
        if (absoluteMaxValuePrim <= mMinShootTime) {
            return super.onTouchEvent(event);
        }
        int pointerIndex;
        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
                pointerIndex = event.findPointerIndex(mActivePointerId);
                mDownMotionX = event.getX(pointerIndex);
                firstPointer = mDownMotionX;
                pressedThumb = evalPressedThumb(mDownMotionX);
                if (pressedThumb == null) return super.onTouchEvent(event);
                setPressed(true);
                onStartTrackingTouch();
                trackTouchEvent(event);
                attemptClaimDrag();
                if (mRangeSeekBarChangeListener != null) {
                    mRangeSeekBarChangeListener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_DOWN, isMin,
                            pressedThumb);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (pressedThumb != null) {
                    if (mIsDragging) {
                        trackTouchEvent(event);
                    } else {
                        // Scroll to follow the motion event
                        pointerIndex = event.findPointerIndex(mActivePointerId);
                        final float x = event.getX(pointerIndex);
                        if (Math.abs(x - mDownMotionX) > mScaledTouchSlop) {
                            setPressed(true);
                            invalidate();
                            onStartTrackingTouch();
                            trackTouchEvent(event);
                            attemptClaimDrag();
                        }
                    }
                    if (notifyWhileDragging && mRangeSeekBarChangeListener != null) {
                        mRangeSeekBarChangeListener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_MOVE,
                                isMin, pressedThumb);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }

                invalidate();
                if (mRangeSeekBarChangeListener != null) {
                    mRangeSeekBarChangeListener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_UP, isMin,
                            pressedThumb);
                }
                pressedThumb = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = event.getPointerCount() - 1;
                mDownMotionX = event.getX(index);
                firstPointer = mDownMotionX;
                mActivePointerId = event.getPointerId(index);
                invalidate();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
            default:
                break;
        }
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mDownMotionX = ev.getX(newPointerIndex);
            secondPointer = mDownMotionX;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private void trackTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) return;
        Log.e(TAG, "trackTouchEvent: " + event.getAction() + " x: " + event.getX());
        final int pointerIndex = event.findPointerIndex(mActivePointerId);
        float x = 0;
        try {
            x = event.getX(pointerIndex);
        } catch (Exception e) {
            return;
        }
        if (Thumb.MIN.equals(pressedThumb)) {
            setNormalizedMinValue(screenToNormalized(x, 0));
        } else if (Thumb.MAX.equals(pressedThumb)) {
            setNormalizedMaxValue(screenToNormalized(x, 1));
        }
    }

    private double screenToNormalized(float screenCoord, int position) {
        int width = getWidth();
        if (width <= 2 * padding) {
            // prevent division by zero, simply return 0.
            return 0d;
        } else {
            isMin = false;
            double current_width = screenCoord;
            float rangeL = normalizedToScreen(normalizedMinValue);
            float rangeR = normalizedToScreen(normalizedMaxValue);
            double min = mMinShootTime / (absoluteMaxValuePrim - absoluteMinValuePrim) * (width - thumbWidth * 2);

            if (absoluteMaxValuePrim > 5 * 60 * 1000) {
                DecimalFormat df = new DecimalFormat("0.0000");
                min_width = Double.parseDouble(df.format(min));
            } else {
                min_width = Math.round(min + 0.5d);
            }
            if (position == 0) {
                if (isInThumbRangeLeft(screenCoord, normalizedMinValue, 0.5)) {
                    return normalizedMinValue;
                }

                float rightPosition = (getWidth() - rangeR) >= 0 ? (getWidth() - rangeR) : 0;
                double left_length = getValueLength() - (rightPosition + min_width);

                if (current_width > rangeL) {
                    current_width = rangeL + (current_width - rangeL);
                } else if (current_width <= rangeL) {
                    current_width = rangeL - (rangeL - current_width);
                }

                if (current_width > left_length) {
                    isMin = true;
                    current_width = left_length;
                }

                if (current_width < thumbWidth * 2 / 3) {
                    current_width = 0;
                }

                double resultTime = (current_width - padding) / (width - 2 * thumbWidth);
                normalizedMinValueTime = Math.min(1d, Math.max(0d, resultTime));
                double result = (current_width - padding) / (width - 2 * padding);
                return Math.min(1d, Math.max(0d, result));
            } else {
                if (isInThumbRange(screenCoord, normalizedMaxValue, 0.5)) {
                    return normalizedMaxValue;
                }

                double right_length = getValueLength() - (rangeL + min_width);
                if (current_width > rangeR) {
                    current_width = rangeR + (current_width - rangeR);
                } else if (current_width <= rangeR) {
                    current_width = rangeR - (rangeR - current_width);
                }

                double paddingRight = getWidth() - current_width;

                if (paddingRight > right_length) {
                    isMin = true;
                    current_width = getWidth() - right_length;
                    paddingRight = right_length;
                }

                if (paddingRight < thumbWidth * 2 / 3) {
                    current_width = getWidth();
                    paddingRight = 0;
                }

                double resultTime = (paddingRight - padding) / (width - 2 * thumbWidth);
                resultTime = 1 - resultTime;
                normalizedMaxValueTime = Math.min(1d, Math.max(0d, resultTime));
                double result = (current_width - padding) / (width - 2 * padding);
                return Math.min(1d, Math.max(0d, result));
            }
        }
    }

    private int getValueLength() {
        return (getWidth() - 2 * thumbWidth);
    }

    private Thumb evalPressedThumb(float touchX) {
        Thumb result = null;
        boolean minThumbPressed = isInThumbRange(touchX, normalizedMinValue, 2);
        boolean maxThumbPressed = isInThumbRange(touchX, normalizedMaxValue, 2);
        if (minThumbPressed && maxThumbPressed) {
            result = (touchX / getWidth() > 0.5f) ? Thumb.MIN : Thumb.MAX;
        } else if (minThumbPressed) {
            result = Thumb.MIN;
        } else if (maxThumbPressed) {
            result = Thumb.MAX;
        }
        return result;
    }

    private boolean isInThumbRange(float touchX, double normalizedThumbValue, double scale) {
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue)) <= thumbHalfWidth * scale;
    }

    private boolean isInThumbRangeLeft(float touchX, double normalizedThumbValue, double scale) {
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue) - thumbWidth) <= thumbHalfWidth * scale;
    }

    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    void onStartTrackingTouch() {
        mIsDragging = true;
    }

    void onStopTrackingTouch() {
        mIsDragging = false;
    }

    public void setMinShootTime(long min_cut_time) {
        this.mMinShootTime = min_cut_time;
    }

    private float normalizedToScreen(double normalizedCoord) {
        return (float) (getPaddingLeft() + normalizedCoord * (getWidth() - getPaddingLeft() - getPaddingRight()));
    }

    private double valueToNormalized(long value) {
        if (0 == absoluteMaxValuePrim - absoluteMinValuePrim) {
            return 0d;
        }
        return (value - absoluteMinValuePrim) / (absoluteMaxValuePrim - absoluteMinValuePrim);
    }

    public void setStartEndTime(long start, long end) {
        this.mStartPosition = start / 1000;
        this.mEndPosition = end / 1000;
    }

    public void setNormalizedMinValue(double value) {
        normalizedMinValue = Math.max(0d, Math.min(1d, Math.min(value, normalizedMaxValue)));
        invalidate();
    }

    public void setNormalizedMaxValue(double value) {
        normalizedMaxValue = Math.max(0d, Math.min(1d, Math.max(value, normalizedMinValue)));
        invalidate();
    }

    public long getSelectedMinValue() {
        return normalizedToValue(normalizedMinValueTime);
    }

    public void setSelectedMinValue(long value) {
        if (0 == (absoluteMaxValuePrim - absoluteMinValuePrim)) {
            setNormalizedMinValue(0d);
        } else {
            setNormalizedMinValue(valueToNormalized(value));
        }
    }

    public long getSelectedMaxValue() {
        return normalizedToValue(normalizedMaxValueTime);
    }

    public void setSelectedMaxValue(long value) {
        if (0 == (absoluteMaxValuePrim - absoluteMinValuePrim)) {
            setNormalizedMaxValue(1d);
        } else {
            setNormalizedMaxValue(valueToNormalized(value));
        }
    }

    private long normalizedToValue(double normalized) {
        return (long) (absoluteMinValuePrim + normalized * (absoluteMaxValuePrim - absoluteMinValuePrim));
    }

    public boolean isNotifyWhileDragging() {
        return notifyWhileDragging;
    }

    public void setNotifyWhileDragging(boolean flag) {
        this.notifyWhileDragging = flag;
    }

    public void setTouchDown(boolean touchDown) {
        isTouchDown = touchDown;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable("SUPER", super.onSaveInstanceState());
        bundle.putDouble("MIN", normalizedMinValue);
        bundle.putDouble("MAX", normalizedMaxValue);
        bundle.putDouble("MIN_TIME", normalizedMinValueTime);
        bundle.putDouble("MAX_TIME", normalizedMaxValueTime);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcel) {
        final Bundle bundle = (Bundle) parcel;
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
        normalizedMinValue = bundle.getDouble("MIN");
        normalizedMaxValue = bundle.getDouble("MAX");
        normalizedMinValueTime = bundle.getDouble("MIN_TIME");
        normalizedMaxValueTime = bundle.getDouble("MAX_TIME");
    }

    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener listener) {
        this.mRangeSeekBarChangeListener = listener;
    }

    public enum Thumb {
        MIN, MAX
    }

    public interface OnRangeSeekBarChangeListener {
        void onRangeSeekBarValuesChanged(RangeSeekBarView bar, long minValue, long maxValue, int action, boolean isMin, Thumb pressedThumb);
    }
}
