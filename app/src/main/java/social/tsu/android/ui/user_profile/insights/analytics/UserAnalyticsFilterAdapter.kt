package social.tsu.android.ui.user_profile.insights.analytics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import social.tsu.android.R
import java.text.NumberFormat

class UserAnalyticsFilterAdapter(private val itemsList: MutableList<UserAnalyticsFilterItem> = mutableListOf(), val clickListener: (FilterType, Int) -> Unit) : RecyclerView.Adapter<UserAnalyticsFilterAdapter.FilterItemViewHolder>() {

    override fun getItemCount() = itemsList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_analytics_filter, parent, false)
        return FilterItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterItemViewHolder, position: Int) {
        holder.bind(itemsList[position], position)
    }

    fun setActiveItem(position: Int){
        itemsList.forEach { it.isSelected = false }
        itemsList[position].isSelected = true
        notifyDataSetChanged()
    }

    fun submitList(newItemsList: List<UserAnalyticsFilterItem>) {
        itemsList.clear()
        itemsList.addAll(newItemsList)
        notifyDataSetChanged()
    }

    inner class FilterItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.icon_imageView)
        private val counter: TextView = view.findViewById(R.id.counter_textView)
        private val label: TextView = view.findViewById(R.id.label_textView)

        fun bind(item: UserAnalyticsFilterItem, position: Int) {
            icon.setImageResource(item.icon)
            when (item.icon) {
                R.drawable.ic_eye_open -> icon.imageTintList = ContextCompat.getColorStateList(view.context, R.color.tsu_yellow)
                R.drawable.ic_like -> icon.imageTintList = ContextCompat.getColorStateList(view.context, R.color.tsu_red)
                R.drawable.ic_comment -> icon.imageTintList = ContextCompat.getColorStateList(view.context, R.color.tsu_blue)
                R.drawable.ic_share -> icon.imageTintList = ContextCompat.getColorStateList(view.context, R.color.tsu_cine)
                else -> icon.imageTintList = ContextCompat.getColorStateList(view.context, R.color.white)
            }
            counter.text = NumberFormat.getIntegerInstance().format(item.counterValue)

            label.text = item.label
            if (item.isSelected)
                label.setTextColor(ContextCompat.getColorStateList(view.context, R.color.analytics_filter_item_text_active))
            else
                label.setTextColor(ContextCompat.getColorStateList(view.context, R.color.analytics_filter_item_text))

            view.setOnClickListener {
                clickListener(item.filterType, position)
                setActiveItem(position)
            }

            (view as MaterialCardView).isChecked = item.isSelected
        }
    }
}
