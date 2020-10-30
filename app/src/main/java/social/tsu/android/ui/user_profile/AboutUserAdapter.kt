package social.tsu.android.ui.user_profile


import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_profile_about.view.*
import kotlinx.android.synthetic.main.item_profile_header.view.*
import social.tsu.android.*
import social.tsu.android.adapters.CommentsLoadingViewHolder
import social.tsu.android.helper.TSUTextTokenizingHelper

import social.tsu.android.ui.model.*
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import kotlin.collections.ArrayList


private const val TYPE_ITEM = 0
private const val TYPE_HEADER = 1
private const val TYPE_ITEM_EMPTY = 2
private const val TYPE_DESCRIPTION = 3
private const val TYPE_DIVIDER = 4

open class AboutUserAdapter(
    private val actionCallback: AboutUserActionCallback?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = ArrayList<AboutItem>()


    override fun getItemCount(): Int {
        return items.size
    }

    fun updateItems(newItems: List<AboutItem>){
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AboutUserItem -> TYPE_ITEM
            is AboutUserDescription -> TYPE_DESCRIPTION
            is AboutUserDivider -> TYPE_DIVIDER
            is AboutUserHeader -> TYPE_HEADER
            else -> TYPE_ITEM_EMPTY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_profile_about, parent, false)
                AboutItemViewHolder(view)
            }
            TYPE_ITEM_EMPTY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_profile_about_empty, parent, false)
                AboutItemEmptyViewHolder(view)
            }
            TYPE_DESCRIPTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_profile_about_description, parent, false)
                AboutDescriptionItemViewHolder(view)
            }
            TYPE_DIVIDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_profile_about_divider, parent, false)
                AboutDividerItemViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_profile_header, parent, false)
                AboutHeaderViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AboutItemViewHolder -> {
                val item = items[position] as AboutUserItem
                holder.updateWithItem(item)
            }
            is AboutDescriptionItemViewHolder -> {
                val item = items[position] as AboutUserDescription
                holder.updateWithItem(item)
            }
            is AboutItemEmptyViewHolder -> {
                val item = items[position] as AboutEmptyUserItem
                holder.updateWithItem(item)
            }
            is AboutHeaderViewHolder -> {
                val item = items[position] as AboutUserHeader
                holder.updateWithItem(item)
            }
        }
    }
}

interface AboutUserActionCallback {

}

class AboutItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


    private val title = itemView.about_item_title
    private val icon = itemView.about_item_icon
    private val text = itemView.about_item_text


    fun updateWithItem(item: AboutUserItem) {
        title.text = item.title
        if (item.icon > 0) {
            icon.show()
            icon.setImageResource(item.icon)
        } else {
            icon.hide()
        }
        text.text = item.text
        text.movementMethod =  if (item.text is Spannable) {
            LinkMovementMethod.getInstance()
        } else {
            null
        }
        itemView.setOnClickListener {
            item.onClick()
        }
    }

}

class AboutItemEmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


    private val icon = itemView.about_item_icon
    private val text = itemView.about_item_text


    fun updateWithItem(item: AboutEmptyUserItem) {
        text.text = item.text
        if (item.icon > 0) {
            icon.show()
            icon.setImageResource(item.icon)
        } else {
            icon.hide()
        }
    }
}

class AboutDividerItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class AboutDescriptionItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val text = itemView.about_item_text

    fun updateWithItem(item: AboutUserDescription) {
        text.text = tokenize(item.text)
        text.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun tokenize(fullText: String?): SpannableString? {
        if (fullText == null) return null
        val input = SpannableString(fullText)
        return TSUTextTokenizingHelper.tokenize(itemView.context, input, {
            // TODO: Add username click
        }, { hashtag ->
        })
    }

}

class AboutHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val title = itemView.about_item_header
    private val edit = itemView.about_item_edit

    fun updateWithItem(item: AboutUserHeader) {
        title.text = item.header
        if(item.ownProfile){
            edit.show()
        } else {
            edit.hide()
        }
        if (item.listener == null) {
            edit.hide()
        } else {
            edit.show()
            edit.setOnClickListener { item.listener.onEditClicked() }
        }
    }
}

interface AboutItem {
    fun getType(): ItemType
}

enum class ItemType { ITEM, HEADER, DESCRIPTION, DIVIDER }

object AboutUserDivider : AboutItem {
    override fun getType(): ItemType {
        return ItemType.DIVIDER
    }
}

data class AboutUserDescription(val text: String) : AboutItem {
    override fun getType(): ItemType {
        return ItemType.DESCRIPTION
    }
}

data class AboutUserItem(
    val icon: Int = 0,
    val title: String,
    val text: CharSequence,
    val onClick: () -> Unit = {}
) : AboutItem {
    override fun getType(): ItemType {
        return ItemType.ITEM
    }
}

data class AboutEmptyUserItem(val icon: Int = 0, val text: String) : AboutItem {
    override fun getType(): ItemType {
        return ItemType.ITEM
    }
}

data class AboutUserHeader(val header: String, val ownProfile: Boolean = false, val listener: AboutUserHeaderListener? = null): AboutItem {
    override fun getType(): ItemType {
        return ItemType.HEADER
    }
}

interface AboutUserHeaderListener {
    fun onEditClicked()
}
