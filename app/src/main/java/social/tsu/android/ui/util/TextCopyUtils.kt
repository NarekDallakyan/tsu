package social.tsu.android.ui.util

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat

object TextCopyUtils {

    fun textGestureDetectorCompat(context: Context, textView: TextView?) {

        var mDetector = GestureDetectorCompat(context, GestureDetector.SimpleOnGestureListener())

        mDetector.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                return false
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return false
            }
        })

        textView?.setOnTouchListener(View.OnTouchListener { v, event ->
            mDetector.onTouchEvent(event)
            false
        })

    }

}