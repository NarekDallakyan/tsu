package social.tsu.android.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.viewModel.CommentInputViewModel

interface CommentDefaultInputAdapterDelegate {
    fun didTapOnEmoji(value: String)
}

class CommentDefaultInputAdapter(private val application: TsuApplication, private val delegate: CommentDefaultInputAdapterDelegate): RecyclerView.Adapter<CommentInputViewHolder>() {

    private val viewModel = CommentInputViewModel()

    override fun getItemCount(): Int {
        return viewModel.numberOfDefaultEmojis
    }

    override fun onBindViewHolder(holder: CommentInputViewHolder, position: Int) {
        holder.updateWith(viewModel.emoji(position))
        holder.commentTextView?.setOnClickListener {
            delegate.didTapOnEmoji(holder.commentTextView?.text.toString())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentInputViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_default_input_layout, parent, false)
        return CommentInputViewHolder(application, view)
    }

}