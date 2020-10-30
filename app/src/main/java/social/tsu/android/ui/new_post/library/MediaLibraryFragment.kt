package social.tsu.android.ui.new_post.library

import android.os.Bundle
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
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.new_post.PostTypesFragment
import social.tsu.android.utils.snack
import javax.inject.Inject

class MediaLibraryFragment : Fragment(){

    @Inject
    lateinit var viewmodelFactory: ViewModelProvider.Factory

    private val libraryViewModel by viewModels<MediaLibraryViewModel> { viewmodelFactory }

    private val adapter = LibraryFoldersAdapter{
        findNavController().navigate(MediaLibraryFragmentDirections.next(it.mediaContent.toTypedArray(),it.folderName))
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

        media_folder_recycler.adapter = adapter

        try {
            val parentFragment = requireParentFragment().requireParentFragment() as PostTypesFragment
            libraryViewModel.allowVideo = parentFragment.allowVideo
        } catch (e: IllegalStateException) {
            Log.e("Library", "", e)
        }
        libraryViewModel.mediaLibraryFolders.observe(viewLifecycleOwner, Observer {
            when(it){
                is Data.Success -> adapter.submitNewList(it.data)
                is Data.Error -> snack("Unable to load library")
            }
        })

    }

}