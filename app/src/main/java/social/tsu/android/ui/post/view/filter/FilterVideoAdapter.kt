package social.tsu.android.ui.post.view.filter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import jp.co.cyberagent.android.gpuimage.GPUImage
import kotlinx.android.synthetic.main.vh_trim_video.view.*
import social.tsu.android.R
import social.tsu.android.ext.show
import social.tsu.android.ui.post.model.FilterVideoModel
import social.tsu.android.utils.hide
import social.tsu.camerarecorder.widget.Filters

class FilterVideoAdapter : RecyclerView.Adapter<FilterVideoAdapter.TrimVideoViewHolder>() {

    private var trimVideoModelList = arrayListOf<FilterVideoModel>()
    private var itemClickListener: ((position: Int, itemModel: FilterVideoModel) -> Unit)? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrimVideoViewHolder {

        val trimView = LayoutInflater.from(parent.context)
            .inflate(R.layout.vh_trim_video, parent, false)
        return TrimVideoViewHolder(trimView)
    }

    override fun onBindViewHolder(holder: TrimVideoViewHolder, position: Int) {

        val itemModel = trimVideoModelList[position]
        holder.onBind(itemModel, itemClickListener)
    }

    override fun getItemCount(): Int = trimVideoModelList.size

    fun submitList(filterVideoModelList: ArrayList<FilterVideoModel>) {
        this.trimVideoModelList = filterVideoModelList
        notifyDataSetChanged()
    }

    fun getData() = trimVideoModelList

    fun addItemClickListener(itemClickListener: (position: Int, itemModel: FilterVideoModel) -> Unit) {
        this.itemClickListener = itemClickListener
    }

    class TrimVideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun onBind(
            itemModel: FilterVideoModel,
            itemClickListener: ((position: Int, itemModel: FilterVideoModel) -> Unit)?
        ) {

            val chooseLayer = itemView.chooseLayer
            val filterText = itemView.filterNameText

            itemView.profile_image.setImageBitmap(itemModel.bitmaps)

            itemView.setOnClickListener {
                itemClickListener?.let {
                    it(adapterPosition, itemModel)
                }
            }

            filterText.text = (itemModel.filterObject as Filters).value

            if (!itemModel.isSelected) {
                chooseLayer.hide()
            } else {
                chooseLayer.show(animate = true)
            }
        }
    }
}