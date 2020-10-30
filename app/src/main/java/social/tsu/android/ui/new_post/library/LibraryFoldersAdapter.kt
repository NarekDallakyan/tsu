package social.tsu.android.ui.new_post.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.media_library_folder_item.view.*
import social.tsu.android.R

class LibraryFoldersAdapter(private val onItemClick:(folder:LibraryFolder)->Unit)
    : RecyclerView.Adapter<LibraryFoldersAdapter.LibraryFolderViewHolder>(){

    private var foldersList = listOf<LibraryFolder>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryFolderViewHolder {
        return LibraryFolderViewHolder.create(parent, onItemClick)
    }

    override fun getItemCount(): Int = foldersList.size


    override fun onBindViewHolder(holder: LibraryFolderViewHolder, position: Int) {
        holder.bindFolder(foldersList[position])
    }

    fun submitNewList(newList:List<LibraryFolder>){
        foldersList = newList
        notifyDataSetChanged()
    }

    class LibraryFolderViewHolder(view: View,
                                  private val onItemClick:(folder:LibraryFolder)->Unit): RecyclerView.ViewHolder(view){
        private val nameTextView = itemView.folder_name_textview
        private val mediaCountTextView = itemView.folder_media_count_textview
        private val thumbnailImageView = itemView.folder_thumbnail_imageview

        fun bindFolder(folder: LibraryFolder){
            nameTextView.text = folder.folderName
            mediaCountTextView.text = folder.mediaContent.size.toString()

            Glide.with(thumbnailImageView)
                .load(folder.mediaContent[0].uri)
                .centerCrop()
                .into(thumbnailImageView)

            itemView.setOnClickListener { onItemClick.invoke(folder) }
        }

        companion object{

            fun create(parent: ViewGroup, onItemClick:(folder:LibraryFolder)->Unit):LibraryFolderViewHolder{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.media_library_folder_item, parent, false)
                return LibraryFolderViewHolder(view,onItemClick)
            }
        }
    }
}