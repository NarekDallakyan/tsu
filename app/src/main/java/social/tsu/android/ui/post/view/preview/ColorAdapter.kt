package social.tsu.android.ui.post.view.preview

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.vh_color_item.view.*
import social.tsu.android.R
import social.tsu.android.ui.post.model.ColorModel
import java.util.*

class ColorAdapter : RecyclerView.Adapter<ColorAdapter.ColorsViewHolder>() {

    private var colorModelList = arrayListOf<ColorModel>()
    private var itemClickListener: ((position: Int, itemModel: ColorModel) -> Unit)? = null

    class ColorsViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        fun onBind(
            data: ColorModel,
            itemClickListener: ((position: Int, itemModel: ColorModel) -> Unit)?
        ) {

            val colorLayout = itemView.colorLayout
            val rnd = Random()
            val color: Int = Color.argb(
                255,
                rnd.nextInt(256),
                rnd.nextInt(256),
                rnd.nextInt(256)
            )
            colorLayout.setBackgroundColor(color)
            data.color = color
            itemView.setOnClickListener {
                if (itemClickListener != null) {
                    itemClickListener(adapterPosition, data)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorsViewHolder {

        val trimView = LayoutInflater.from(parent.context)
            .inflate(R.layout.vh_color_item, parent, false)
        return ColorsViewHolder(trimView)
    }

    override fun onBindViewHolder(holder: ColorsViewHolder, position: Int) {
        holder.onBind(colorModelList[position], itemClickListener)
    }

    override fun getItemCount(): Int = colorModelList.size

    fun submitList(colorModelList: ArrayList<ColorModel>) {
        this.colorModelList = colorModelList
        notifyDataSetChanged()
    }

    fun getData() = colorModelList

    fun addItemClickListener(itemClickListener: (position: Int, itemModel: ColorModel) -> Unit) {
        this.itemClickListener = itemClickListener
    }
}