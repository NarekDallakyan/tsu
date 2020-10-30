package social.tsu.android.ui.new_post.library


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_media_grid.*
import kotlinx.android.synthetic.main.fragment_post_types.*
import social.tsu.android.R
import social.tsu.android.ui.post.view.PostTypesFragment

class MediaGridFragment : Fragment(){

    private val args: MediaGridFragmentArgs by navArgs()

    private val title by lazy {
        args.title
    }

    private val mediaList by lazy {
        args.mediaList
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_media_grid, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().post_types_toolbar?.title = title

        val adapter = LibraryMediaAdapter{
            val imageUri = if (it.isImage()) it.uri else null
            val videoUri = if(it.isVideo()) it.uri.toString() else null

            (requireParentFragment().requireParentFragment() as PostTypesFragment).next(
                videoContentUri = videoUri,
                photoUri = imageUri
            )
        }

        adapter.submitNewList(mediaList.toList())

        media_grid_recycler.adapter = adapter

    }

}