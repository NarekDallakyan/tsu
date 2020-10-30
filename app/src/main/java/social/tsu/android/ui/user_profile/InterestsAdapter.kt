package social.tsu.android.ui.user_profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.R

class InterestsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val TYPE_ITEM = 0
        val TYPE_BUTTON = 1
    }

    private var data = ArrayList<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            TYPE_ITEM -> InterestViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_interest, parent, false))
            TYPE_BUTTON -> ButtonViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_interest_add, parent, false))
            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)) {
            TYPE_BUTTON -> {
                (holder as ButtonViewHolder).bind(this)
            }
            TYPE_ITEM -> {
                (holder as InterestViewHolder).bind(data[position], position)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size + 1
    }

    fun setInterests(list: List<String>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    fun addInterest(interest: String) {
        data.add(interest)
        notifyItemInserted(data.size - 1)
    }

    override fun getItemViewType(position: Int): Int {
        return if (data.size == position) TYPE_BUTTON else TYPE_ITEM
    }
}

class InterestViewHolder(view : View) : RecyclerView.ViewHolder(view) {
    fun bind(interest: String, position: Int) {
        itemView.findViewById<TextView>(R.id.label).setText(itemView.resources.getString(R.string.interest_label, position + 1))
        itemView.findViewById<EditText>(R.id.interest).setText(interest)
    }
}

class ButtonViewHolder(view : View) : RecyclerView.ViewHolder(view) {
    fun bind(interestsAdapter: InterestsAdapter) {
        itemView.setOnClickListener {
            interestsAdapter.addInterest("")
        }
    }
}