package social.tsu.android.ui.util


import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.app_bar_main.*
import social.tsu.android.ui.MainActivity

abstract class NoAppBarFragment : Fragment() {

    protected abstract val showAppBarOnDestroyView: Boolean

    private var appBarHeight = 0
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        appBarHeight = activity?.main_app_bar_layout?.height ?: 0
        hideAppBar()
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onDestroyView() {
        if (showAppBarOnDestroyView) {
            showAppBar()
        }
        super.onDestroyView()
    }


    private fun showAppBar() {
        val myActivity = requireActivity()
        if (myActivity is MainActivity) {
            myActivity.showAppBar()
        }
    }

    private fun hideAppBar() {
        val myActivity = requireActivity()
        if (myActivity is MainActivity) {
            myActivity.hideAppBar()
        }
    }


}