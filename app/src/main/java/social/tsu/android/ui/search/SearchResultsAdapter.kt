package social.tsu.android.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import social.tsu.android.R
import social.tsu.android.network.model.Group
import social.tsu.android.network.model.HashTag
import social.tsu.android.network.model.MentionUser
import social.tsu.android.network.model.SearchUser
import social.tsu.android.network.model.diffutil.SearchDiffCallback


interface OnItemClickListener<T> {
    fun onItemClicked(
        item: View, data: Any
    )
}

class SearchResultsAdapter(
    private val itemClickListener: OnItemClickListener<Any>
) : RecyclerView.Adapter<SearchResultViewHolder>() {

    private val results = mutableListOf<Any>()

    suspend fun updateSearchResults(newResult: List<Any>) = withContext(Dispatchers.IO) {
        val oldList = ArrayList(results)
        val diffResult = DiffUtil.calculateDiff(SearchDiffCallback(oldList, newResult))
        withContext(Dispatchers.Main) {
            if (isActive) {
                results.clear()
                results.addAll(newResult)
                diffResult.dispatchUpdatesTo(this@SearchResultsAdapter)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = when (viewType) {

            SearchFragment.SEARCH_TYPE_MENTION -> inflater.inflate(
                R.layout.search_result_user,
                parent,
                false
            )

            SearchFragment.SEARCH_TYPE_USERS -> inflater.inflate(
                R.layout.search_result_user,
                parent,
                false
            )
            SearchFragment.SEARCH_TYPE_GROUPS -> inflater.inflate(
                R.layout.search_result_group,
                parent,
                false
            )
            SearchFragment.SEARCH_TYPE_HASHTAGS -> inflater.inflate(
                R.layout.search_result_hashtag,
                parent,
                false
            )
            else -> inflater.inflate(R.layout.search_result_group, parent, false)
        }

        return SearchResultViewHolder(
            view,
            itemClickListener
        )
    }

    override fun getItemCount(): Int {
        return results.size
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemViewType(position: Int): Int {
        return when (results[position]) {
            is MentionUser -> SearchFragment.SEARCH_TYPE_MENTION
            is SearchUser -> SearchFragment.SEARCH_TYPE_USERS
            is Group -> SearchFragment.SEARCH_TYPE_GROUPS
            is HashTag -> SearchFragment.SEARCH_TYPE_HASHTAGS
            else -> SearchFragment.SEARCH_TYPE_ANY
        }
    }
}