package social.tsu.android.ui.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes

class LitAnimationHelper {


    fun moveToX(view: View, mDuration: Long, finalXCoordinate: Float) {

        ObjectAnimator.ofFloat(view, "translationX", finalXCoordinate).apply {
            duration = mDuration
            start()
        }
    }

    fun loadAnimation(@AnimRes resource: Int, view: View, context: Context?) {

        context?.let {
            val animation = AnimationUtils.loadAnimation(context, resource)
            view.startAnimation(animation)
        }
    }

    fun showWithAlpha(v: View, duration: Long, endListener: (() -> Unit)? = null) {

        v.alpha = 0f
        v.visibility = View.VISIBLE

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        v.animate()
            .alpha(1f)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    v.clearAnimation()
                    endListener?.let {
                        it()
                    }
                }
            })
    }

    fun changeAlpha(v: View, duration: Long, hide: Boolean, endListener: (() -> Unit)? = null) {

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        val alpha = if (hide) {
            v.alpha = 1f
            0f
        } else {
            v.alpha = 0f
            1f
        }

        v.animate()
            .alpha(alpha)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    v.clearAnimation()
                    endListener?.let {
                        it()
                    }
                }
            })
    }

    fun hideWithAlpha(
        v: View,
        duration: Long,
        invisible: Boolean = false,
        endListener: (() -> Unit)? = null
    ) {

        v.alpha = 1f

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        v.animate()
            .alpha(0f)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    v.clearAnimation()
                    if (!invisible) {
                        v.visibility = View.GONE
                    } else {
                        v.visibility = View.INVISIBLE
                    }
                    endListener?.let {
                        it()
                    }
                }
            })
    }

    fun rotation(view: View, value: Float, finish: () -> Unit) {

        view.animate()
            .rotation(value)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {

                }

                override fun onAnimationEnd(animation: Animator?) {
                    view.clearAnimation()
                    finish()
                }

                override fun onAnimationCancel(animation: Animator?) {

                }

                override fun onAnimationStart(animation: Animator?) {

                }

            })
    }
}