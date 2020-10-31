package social.tsu.android.ui.post.view.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_post_result.*
import social.tsu.android.R
import social.tsu.android.ui.MainActivity
import social.tsu.android.utils.findParentNavController
import social.tsu.android.viewModel.SharedViewModel


class PostResultFragment : Fragment() {

    var sharedViewModel: SharedViewModel? = null

    // Arguments data
    private var filePath: String? = null
    private var fromScreenType: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_post_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Get Arguments
        getArgumentData()
        // Init view models
        initViewModels()
        // Listen on click listeners
        initOnClicks()
        Toast.makeText(requireContext(), filePath, Toast.LENGTH_LONG).show()
    }

    private fun initOnClicks() {

        closeLayout_id.setOnClickListener {
            sharedViewModel!!.select(false)
            findParentNavController().popBackStack(R.id.postTypesFragment, false)
        }
    }

    private fun initViewModels() {

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()
        val mainActivity = requireActivity() as? MainActivity
        mainActivity?.supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        val mainActivity = requireActivity() as? MainActivity
        mainActivity?.supportActionBar?.show()
    }

    private fun getArgumentData() {

        if (arguments == null) return

        filePath = requireArguments().getString("filePath")
        fromScreenType = requireArguments().getInt("fromScreenType")
    }
}