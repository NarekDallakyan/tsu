package social.tsu.android.ui.post.view.preview

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.vh_font_item.view.*
import social.tsu.android.R
import social.tsu.android.ui.post.model.FontModel
import social.tsu.android.utils.hide
import social.tsu.android.utils.show

class FontsAdapter : RecyclerView.Adapter<FontsAdapter.FontsViewHolder>() {

    private var fontModelList = arrayListOf<FontModel>()
    private var itemClickListener: ((position: Int, itemModel: FontModel) -> Unit)? = null

    class FontsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun onBind(
            data: FontModel,
            itemClickListener: ((position: Int, itemModel: FontModel) -> Unit)?
        ) {

            val fontLayout = itemView.fontBorderLayout
            val fontText = itemView.fontImage

            val iconLayout = itemView.iconLayout
            val iconImage = itemView.iconImage

            when (data.itemType) {
                FontModel.ItemType.FONT -> {

                    iconLayout.hide()
                    fontLayout.show()
                    fontText.text = data.text
                    fontText.typeface = data.font

                    if (data.isSelected) {

                        fontLayout.setBackgroundDrawable(
                            ContextCompat.getDrawable(
                                itemView.context,
                                R.drawable.font_white_border
                            )
                        )
                        fontText.setTextColor(Color.WHITE)
                    } else {

                        fontLayout.setBackgroundDrawable(
                            ContextCompat.getDrawable(
                                itemView.context,
                                R.drawable.font_grey_border
                            )
                        )
                        fontText.setTextColor(Color.GRAY)
                    }
                }

                FontModel.ItemType.ALIGN -> {
                    iconLayout.show()
                    fontLayout.hide()
                    iconImage.setImageResource(data.icon!!)

                }

                FontModel.ItemType.WATERMARK -> {

                    iconLayout.show()
                    fontLayout.hide()
                    iconImage.setImageResource(data.icon!!)
                }
            }

            itemView.setOnClickListener {

                if (itemClickListener != null) {

                    when (data.itemType) {

                        FontModel.ItemType.WATERMARK -> {
                            val currentWatermarkStatus = data.watermark
                            data.watermark = !currentWatermarkStatus

                            if (data.watermark) {
                                iconImage.setImageResource(R.drawable.ic_watermark_on)
                            } else {
                                iconImage.setImageResource(R.drawable.ic_watermark_off)
                            }
                        }

                        FontModel.ItemType.ALIGN -> {

                            when (data.align) {
                                0 -> {
                                    data.align = 1
                                    iconImage.setImageResource(R.drawable.ic_text_align_center)
                                }
                                1 -> {
                                    data.align = 2
                                    iconImage.setImageResource(R.drawable.ic_text_align_right)
                                }
                                2 -> {
                                    data.align = 0
                                    iconImage.setImageResource(R.drawable.ic_text_align_left)
                                }
                            }
                        }
                        else -> {
                        }
                    }

                    itemClickListener(adapterPosition, data)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontsViewHolder {

        val trimView = LayoutInflater.from(parent.context)
            .inflate(R.layout.vh_font_item, parent, false)
        return FontsViewHolder(trimView)
    }

    override fun onBindViewHolder(holder: FontsViewHolder, position: Int) {
        holder.onBind(fontModelList[position], itemClickListener)
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