package social.tsu.android.ui.user_list

import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import social.tsu.android.R
import social.tsu.android.helper.Constants
import social.tsu.android.network.model.UserProfile


interface UserViewHolderCallback {
    fun didUserClick(userProfile: UserProfile)
}

abstract class BaseUserViewHolder(
    itemView: View,
    private val callback: UserViewHolderCallback?
) : RecyclerView.ViewHolder(itemView) {

    private val userPhoto: CircleImageView = itemView.findViewById(R.id.user_item_photo)
    private val userName: TextView = itemView.findViewById(R.id.user_item_name)

    protected var currentItem: UserProfile? = null


    init {
        itemView.setOnClickListener {
            currentItem?.let { callback?.didUserClick(it) }
        }
    }

    open fun bind(item: UserProfile) {

        this.currentItem = item
        val input = SpannableString(item.fullName.plus(" "))
        if (Constants.isVerified(item.verifiedStatus)) {
            val imageSpan = ImageSpan(userName.context, R.drawable.ic_verified_extra_small)
            input.setSpan(imageSpan, input.length - 1, input.length, 0)
        }
        userName.text = input

        Glide.with(itemView)
            .load(item.profilePictureUrl)
            .override(200)
            .error(R.drawable.user)
            .into(userPhoto)
    }

}