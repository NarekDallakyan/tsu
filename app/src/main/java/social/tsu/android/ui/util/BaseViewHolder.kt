package social.tsu.android.ui.util

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder(itemView:View): RecyclerView.ViewHolder(itemView){
    abstract fun <T> bind(item: T)
}