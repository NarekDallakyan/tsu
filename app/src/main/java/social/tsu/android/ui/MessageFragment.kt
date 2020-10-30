package social.tsu.android.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import social.tsu.android.R

class MessageFragment: Fragment() {

    private var shouldSendToFeed: Boolean = false

    private val args: MessageFragmentArgs by navArgs()

    companion object {
        const val KEY_MESSAGE = "message"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.success_page,
            container, false
        )

        view.findViewById<TextView>(R.id.message).text = args.message

        shouldSendToFeed = arguments?.let {
            it.getBoolean("shouldSendToFeed")
        } ?: false

        return view
    }

    override fun onResume() {
        super.onResume()
        if (shouldSendToFeed) {
            Handler().postDelayed({
                val intent = Intent(this.context, MainActivity::class.java)
                intent.putExtra("default_layout", MainActivityDefaultLayout.FEED)
                startActivity(intent)
            }, 2000)
        }
    }
}
