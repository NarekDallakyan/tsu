package social.tsu.android.ui.new_post.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_color_gradient.view.*
import social.tsu.android.R

class ColorGradientAdapter : RecyclerView.Adapter<ColorGradientAdapter.ColorViewHolder>() {

    private var alColor = ArrayList<Int>()

    var onItemClick: ((color: Int) -> Unit)? = null

    init {
        alColor = ArrayList()
        alColor.add(R.drawable.gradient_one)
        alColor.add(R.drawable.gradient_two)
        alColor.add(R.drawable.gradient_three)
        alColor.add(R.drawable.gradient_four)
        alColor.add(R.drawable.gardient_five)
        alColor.add(R.drawable.gradient_six)

    }

    class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        return ColorViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_color_gradient, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.itemView.bgColor.setImageResource(alColor[position])
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(alColor[position])
        }

    }

    override fun getItemCount(): Int {
        return alColor.size
    }
}