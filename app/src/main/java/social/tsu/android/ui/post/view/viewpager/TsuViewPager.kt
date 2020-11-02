package social.tsu.android.ui.post.view.viewpager

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class TsuViewPager(private var mContext: Context, attributeSet: AttributeSet) :
    ViewPager(mContext, attributeSet) {

    private var mIsEnable: Boolean = true
    fun enableSwiping(enable: Boolean) {
        this.mIsEnable = enable
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return mIsEnable && super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return mIsEnable && super.onTouchEvent(event)
    }
}