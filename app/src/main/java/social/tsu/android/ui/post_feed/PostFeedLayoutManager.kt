package social.tsu.android.ui.post_feed

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler


class PostFeedLayoutManager(ctx: Context) : LinearLayoutManager(ctx) {
    override fun onLayoutChildren(recycler: Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Log.e("PostFeedLayoutManager", "Inconsistency detected")
        }
    }
}