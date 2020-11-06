package social.tsu.android.ui.post.view.preview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.vh_font_item.view.*
import social.tsu.android.R
import social.tsu.android.ui.post.model.FontModel

class FontsAdapter : RecyclerView.Adapter<FontsAdapter.FontsViewHolder>() {

    private var fontModelList = arrayListOf<FontModel>()
    private var itemClickListener: ((position: Int, itemModel: FontModel) -> Unit)? = null

    class FontsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun onBind(data: FontModel) {

            val fontImage = itemView.fontImage
            val fontBorderLayout = itemView.fontBorderLayout
            if (data.needBorder) {
                fontBorderLayout.background = ContextCompat.getDrawable(
                    itemView.context,
                    R.drawable.ic_rectangular_border_white
                )
            } else {
                fontBorderLayout.background = null
            }
            fontImage.setImageResource(data.iconResource)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontsViewHolder {

        val trimView = LayoutInflater.from(parent.context)
            .inflate(R.layout.vh_font_item, parent, false)
        return FontsViewHolder(trimView)
    }

    override fun onBindViewHolder(holder: FontsViewHolder, position: Int) {
        holder.onBind(fontModelList[position])
    }

    override fun getItemCount(): Int = fontModelList.size

    fun submitList(fontModelList: ArrayList<FontModel>) {
        this.fontModelList = fontModelList
        notifyDataSetChanged()
    }

    fun getData() = fontModelList

    fun addItemClickListener(itemClickListener: (position: Int, itemModel: FontModel) -> Unit) {
        this.itemClickListener = itemClickListener
    }
}