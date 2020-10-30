package social.tsu.android.adapters

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.R
import social.tsu.android.TsuApplication

class CommentInputViewHolder(private val application: TsuApplication, itemView: View) : RecyclerView.ViewHolder(itemView) {

    var commentTextView: TextView? = itemView.findViewById(R.id.comment_default_input_textview)

    fun updateWith(emoji: String) {
        itemView.findViewById<TextView>(R.id.comment_default_input_textview)?.text = emoji
    }

}