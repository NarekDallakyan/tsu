package social.tsu.android.ui.messaging.tsu_contacts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import kotlinx.android.synthetic.main.load_state_item.view.*
import social.tsu.android.R
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.util.BaseViewHolder
import social.tsu.android.ui.util.RetryCallback

class LoadStateViewHolder private constructor(itemView: View, retryCallback: RetryCallback) : BaseViewHolder(itemView) {

    private val retryButton = itemView.retry_loading_button
    private val errorTextView = itemView.error_message_textView
    private val loadingProgressBar = itemView.loading_progress_bar
    
    init {
        retryButton.setOnClickListener {retryCallback.retry() }
    }

    override fun <T> bind(item: T) {
        val loadState = item as Data<*>

        //loading and retry
        retryButton.visibility = if (loadState is Data.Error) View.VISIBLE else View.GONE

        //error message
        errorTextView.visibility = if (loadState is Data.Error) View.VISIBLE else View.GONE
        if (loadState is Data.Error) {
            val errMsg = loadState.throwable.message?:""
            errorTextView.text = loadState.throwable.message
            if(errMsg.contains("No") && errMsg.contains("found")){
                itemView.retry_loading_button.visibility = View.GONE
            }
        }

        loadingProgressBar.visibility = if (loadState is Data.Loading) View.VISIBLE else View.GONE

    }

    companion object {

        fun create(parent: ViewGroup, retryCallback: RetryCallback): LoadStateViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.load_state_item, parent, false)
            return LoadStateViewHolder(view, retryCallback)
        }
    }

}
