package social.tsu.android.ui.new_post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_post_visibility.*
import social.tsu.android.R
import social.tsu.android.utils.findParentNavController
import social.tsu.android.viewModel.SharedViewModel


class PostVisibilityFragment : Fragment() {

    private var sharedViewModel: SharedViewModel? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_post_visibility, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Init view models
        initViewModels()
        // Init on clicks
        initOnClicks()
    }

    private fun initViewModels() {

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    private fun initOnClicks() {
        // Listen close layout clicked
        back_button_fragment?.setOnClickListener {
            sharedViewModel!!.select(false)
            findParentNavController().popBackStack(R.id.postDraftFragment, false)
        }
    }
}