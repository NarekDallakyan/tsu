package social.tsu.android.ui.new_post.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.media_library_grid_item.view.*
import social.tsu.android.R

class LibraryMediaAdapter(private val onItemClick: (media:LibraryMedia)->Unit) : RecyclerView.Adapter<LibraryMediaAdapter.LibraryMediaViewHolder>(){

    private var mediaList = listOf<LibraryMedia>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryMediaViewHolder {
        return LibraryMediaViewHolder.create(parent,onItemClick)
    }

    override fun getItemCount(): Int = mediaList.size


    override fun onBindViewHolder(holder: LibraryMediaViewHolder, position: Int) {
        holder.bindFolder(mediaList[position])
    }

    fun submitNewList(newList:List<LibraryMedia>){
        mediaList = newList
        notifyDataSetChanged()
    }

    class LibraryMediaViewHolder(view: View, private val onItemClick: (media:LibraryMedia)->Unit): RecyclerView.ViewHolder(view){

        private val mediaImageView = itemView.media_grid_imageview
        private val videoIndicatorIcon = itemView.media_video_icon

        fun bindFolder(media: LibraryMedia){

            Glide.with(mediaImageView)
                .load(media.uri)
                .centerCrop()
                .into(mediaImageView)

            videoIndicatorIcon.visibility = if(media.isVideo()) View.VISIBLE else View.GONE

            mediaImageView.setOnClickListener { onItemClick.invoke(media) }
        }

        companion object{

            fun create(parent: ViewGroup, onItemClick: (media:LibraryMedia)->Unit):LibraryMediaViewHolder{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.media_library_grid_item, parent, false)
                return LibraryMediaViewHolder(view, onItemClick)
            }
        }
    }

}