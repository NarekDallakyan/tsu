package social.tsu.android.ui.new_post.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_color.view.*
import social.tsu.android.R

class ColorAdapter : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    private var alColor = ArrayList<Int>()

    var onItemClick: ((color: Int) -> Unit)? = null

    init {
        alColor = ArrayList()
        alColor.add(R.color.tsu_red)
        alColor.add(R.color.tsu_yellow)
        alColor.add(R.color.tsu_green)
        alColor.add(R.color.tsu_blue)
        alColor.add(R.color.tsu_brown)
        alColor.add(R.color.tsu_dark_green)
        alColor.add(R.color.tsu_maroon)
        alColor.add(R.color.tsu_dark_yellow)
        alColor.add(R.color.tsu_light_blue)
        alColor.add(R.color.tsu_pink)
        alColor.add(R.color.tsu_grey)
        alColor.add(R.color.tsu_white)
    }

    class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        return ColorViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_color, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.itemView.bgColor.setCardBackgroundColor(
            ContextCompat.getColor(
                holder.itemView.context,
                alColor[position]
            )
        )
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(alColor[position])
        }

    }

    override fun getItemCount(): Int {
        return alColor.size
    }
}