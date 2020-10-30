package social.tsu.android.ui.post_feed.feed_type

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.fragment.findNavController
import org.w3c.dom.Text
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.service.SharedPrefManager
import javax.inject.Inject

class FeedTypeFragment: Fragment() {

    private lateinit var chronoSelector: ImageView
    private lateinit var trendSelector: ImageView

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_feed_type, container, false)

        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        view.findViewById<TextView>(R.id.chrono_text_view).setOnClickListener {
            sharedPrefManager.setFeedType(SharedPrefManager.MAIN_FEED_TYPE_CHRONO)
            val navOptions = NavOptions.Builder().setPopUpTo(R.id.mainFeedFragment, true).build()
            findNavController().navigate(R.id.mainFeedFragment, null, navOptions)
        }

        view.findViewById<TextView>(R.id.trending_text_view).setOnClickListener {
            sharedPrefManager.setFeedType(SharedPrefManager.MAIN_FEED_TYPE_TREND)
            val navOptions = NavOptions.Builder().setPopUpTo(R.id.mainFeedFragment, true).build()
            findNavController().navigate(R.id.mainFeedFragment, null, navOptions)
        }

        chronoSelector = view.findViewById(R.id.chrono_selector)
        trendSelector = view.findViewById(R.id.trending_selector)

        when (sharedPrefManager.getFeedType() ?: SharedPrefManager.MAIN_FEED_TYPE_CHRONO) {
            SharedPrefManager.MAIN_FEED_TYPE_CHRONO -> {
                chronoSelector.visibility = View.VISIBLE
                trendSelector.visibility = View.INVISIBLE
                sharedPrefManager.setFeedType(SharedPrefManager.MAIN_FEED_TYPE_CHRONO)
            }
            SharedPrefManager.MAIN_FEED_TYPE_TREND -> {
                chronoSelector.visibility = View.INVISIBLE
                trendSelector.visibility = View.VISIBLE
                sharedPrefManager.setFeedType(SharedPrefManager.MAIN_FEED_TYPE_TREND)
            }
        }

        return view
    }

}