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

class MediaGridFragment : Fragment() {

    private var mediaContentList: ArrayList<LibraryMedia>? = null
    private var folderName: String? = null

    private val args: MediaGridFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_media_grid, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Get argument data
        getArgumentData()
        requireActivity().post_types_toolbar?.title = folderName

        val adapter = LibraryMediaAdapter {
            val imageUri = if (it.isImage()) it.uri else null
            val videoUri = if (it.isVideo()) it.uri.toString() else null

            (PostTypesFragment.instance()).next(
                videoContentUri = videoUri,
                photoUri = imageUri,
                fromGrid = true
            )
        }

        mediaContentList?.let {
            adapter.submitNewList(it.toList())
            media_grid_recycler.adapter = adapter
        }
    }

    private fun getArgumentData() {

        if (arguments == null) return

        mediaContentList =
            requireArguments().getParcelableArrayList<LibraryMedia>("mediaContentList")
        folderName = requireArguments().getString("folderName")
    }

}