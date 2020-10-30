package social.tsu.android.ui.new_post.adapter

import android.graphics.Typeface
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_font.view.*
import social.tsu.android.R

class FontAdapter : RecyclerView.Adapter<FontAdapter.ColorViewHolder>() {

    private var alFont = ArrayList<Int>()
    private var alName = ArrayList<String>()

    var onItemClick: ((font: Int) -> Unit)? = null

    init {
        alFont = ArrayList()
        alFont.add(R.font.abel)
        alFont.add(R.font.abril_fatface)
        alFont.add(R.font.alef)
        alFont.add(R.font.anton)
        alFont.add(R.font.architects_daughter)
        alFont.add(R.font.bangers)
        alFont.add(R.font.caveat)
        alFont.add(R.font.dancing_script)
        alFont.add(R.font.fredoka_one)
        alFont.add(R.font.indie_flower)
        alFont.add(R.font.londrina_solid)
        alFont.add(R.font.open_sans)
        alFont.add(R.font.pacifico)
        alFont.add(R.font.permanenet_marker)
        alFont.add(R.font.poller_one)
        alFont.add(R.font.satisfy)

        alName = ArrayList()
        alName.add("Abel")
        alName.add("Abril Fatface")
        alName.add("Alef")
        alName.add("Anton")
        alName.add("Architects Daughter")
        alName.add("Bangers")
        alName.add("Caveat")
        alName.add("Dancing Script")
       // alName.add("Fredoka One")
        //alName.add("Indie Flower")
        alName.add("Londrina Solid")
        alName.add("Open Sans")
        alName.add("Pacifico")
        alName.add("Permanenet Marker")
        alName.add("Poller One")
        alName.add("Satisfy")

    }

    class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        return ColorViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_font, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        ResourcesCompat.getFont(
            holder.itemView.context, alFont[position],
            object : ResourcesCompat.FontCallback() {
                override fun onFontRetrieved(typeface: Typeface) {
                    holder.itemView.tvFont.typeface = typeface
                    holder.itemView.tvFont.text = alName[position]
                }

                override fun onFontRetrievalFailed(reason: Int) {
                }
            }, Handler(

            )
        )
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(alFont[position])
        }

    }

    override fun getItemCount(): Int {
        return alName.size
    }
}