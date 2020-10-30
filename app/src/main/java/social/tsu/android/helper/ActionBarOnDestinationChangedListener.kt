package social.tsu.android.helper

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.customview.widget.Openable
import androidx.navigation.FloatingWindow
import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import androidx.navigation.NavDestination
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import social.tsu.android.R
import java.lang.ref.WeakReference
import java.util.regex.Pattern


class ActionBarOnDestinationChangedListener(
    activity: AppCompatActivity,
    configuration: AppBarConfiguration
) : OnDestinationChangedListener {

    var showDrawerIcon = false

    private var mActivity: AppCompatActivity = activity
    private var mTopLevelDestinations: Set<Int> = configuration.topLevelDestinations
    private var mOpenableLayoutWeakReference: WeakReference<Openable>? = null
    private var mArrowDrawable: DrawerArrowDrawable? = null
    private var mAnimator: ValueAnimator? = null

    private var navControllerWeakReference: WeakReference<NavController>? = null

    init {
        val openableLayout = configuration.openableLayout
        mOpenableLayoutWeakReference = if (openableLayout != null) {
            WeakReference(openableLayout)
        } else {
            null
        }
    }

    fun apply(navController: NavController) {
        navControllerWeakReference?.get()?.removeOnDestinationChangedListener(this)
        navControllerWeakReference = WeakReference(navController)
        navController.addOnDestinationChangedListener(this)
    }

    fun remove() {
        navControllerWeakReference?.get()?.removeOnDestinationChangedListener(this)
        navControllerWeakReference?.clear()
        navControllerWeakReference = null
    }

    private fun setTitle(title: CharSequence?) {
        mActivity.supportActionBar?.title = title
    }

    private fun setNavigationIcon(icon: Drawable?, @StringRes contentDescription: Int) {
        val actionBar = mActivity.supportActionBar
        if (icon == null) {
            actionBar?.setDisplayHomeAsUpEnabled(false)
        } else {
            actionBar?.setDisplayHomeAsUpEnabled(true)
            mActivity.drawerToggleDelegate?.setActionBarUpIndicator(icon, contentDescription)
        }
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination, arguments: Bundle?
    ) {
        if (destination is FloatingWindow) {
            return
        }
        val openableLayout = mOpenableLayoutWeakReference?.get()
        if (mOpenableLayoutWeakReference != null && openableLayout == null) {
            controller.removeOnDestinationChangedListener(this)
            return
        }
        val label = destination.label
        if (label != null) {
            // Fill in the data pattern with the args to build a valid URI
            val title = StringBuffer()
            val fillInPattern = Pattern.compile("\\{(.+?)\\}")
            val matcher = fillInPattern.matcher(label)
            while (matcher.find()) {
                val argName = matcher.group(1)
                if (arguments != null && arguments.containsKey(argName)) {
                    matcher.appendReplacement(title, "")
                    title.append(arguments[argName].toString())
                } else {
                    throw IllegalArgumentException(
                        "Could not find " + argName + " in "
                                + arguments + " to fill label " + label
                    )
                }
            }
            matcher.appendTail(title)
            setTitle(title)
        }
        val isTopLevelDestination = matchDestinations(destination)
        if (openableLayout == null && !showDrawerIcon && isTopLevelDestination) {
            setNavigationIcon(null, 0)
        } else {
            setActionBarUpIndicator((openableLayout != null || showDrawerIcon) && isTopLevelDestination)
        }
    }

    private fun setActionBarUpIndicator(showAsDrawerIndicator: Boolean) {
        var animate = true
        if (mArrowDrawable == null) {
            mArrowDrawable = DrawerArrowDrawable(mActivity)
            // We're setting the initial state, so skip the animation
            animate = false
        }
        setNavigationIcon(
            mArrowDrawable,
            if (showAsDrawerIndicator) {
                R.string.nav_app_bar_open_drawer_description
            } else {
                R.string.nav_app_bar_navigate_up_description
            }
        )
        val endValue = if (showAsDrawerIndicator) 0f else 1f
        if (animate) {
            val startValue = mArrowDrawable!!.progress
            mAnimator?.cancel()
            mAnimator = ObjectAnimator.ofFloat(
                mArrowDrawable, "progress",
                startValue, endValue
            )?.also { it.start() }
        } else {
            mArrowDrawable?.progress = endValue
        }
    }

    private fun matchDestinations(destination: NavDestination): Boolean {
        var currentDestination: NavDestination? = destination
        do {
            if (mTopLevelDestinations.contains(currentDestination?.id)) {
                return true
            }
            currentDestination = currentDestination?.parent
        } while (currentDestination != null)
        return false
    }

}