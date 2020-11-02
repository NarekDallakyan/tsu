package social.tsu.android.ui.new_post.library

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_media_library.*
import kotlinx.android.synthetic.main.fragment_post_types.*
import social.tsu.android.R
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.post.view.PostTypesFragment
import social.tsu.android.utils.snack
import javax.inject.Inject

class MediaLibraryFragment : Fragment() {

    @Inject
    lateinit var viewmodelFactory: ViewModelProvider.Factory

    private val libraryViewModel by viewModels<MediaLibraryViewModel> { viewmodelFactory }

    private val adapter = LibraryFoldersAdapter {

        val mBundle = Bundle()
        mBundle.putSerializable("postTypeFragment", postTypeFragment)
        mBundle.putParcelableArrayList("mediaContentList", ArrayList<Parcelable>(it.mediaContent))
        mBundle.putString("folderName", it.folderName)
        findNavController().navigate(R.id.mediaGridFragment, mBundle)

        /*findNavController().navigate(
            MediaLibraryFragmentDirections.next(
                it.mediaContent.toTypedArray(),
                it.folderName
            )
        )*/
    }

    private var postTypeFragment: PostTypesFragment? = null

    override fun onStart() {
        super.onStart()
        val mainActivity = requireActivity() as? MainActivity
        mainActivity?.supportActionBar?.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AndroidSupportInjection.inject(this)
        requireActivity().post_types_toolbar?.title = getString(R.string.select_media_file)
        return inflater.inflate(R.layout.fragment_media_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Get argument data
        getArgumentData()
        media_folder_recycler.adapter = adapter

        try {
            libraryViewModel.allowVideo = postTypeFragment!!.allowVideo
        } catch (e: IllegalStateException) {
            Log.e("Library", "", e)
        }
        libraryViewModel.mediaLibraryFolders.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Data.Success -> adapter.submitNewList(it.data)
                is Data.Error -> snack("Unable to load library")
                else -> {
                }
            }
        })

    }

    private fun getArgumentData() {

        if (arguments == null) return

        postTypeFragment =
            requireArguments().getSerializable("postTypeFragment") as? PostTypesFragment
    }

}