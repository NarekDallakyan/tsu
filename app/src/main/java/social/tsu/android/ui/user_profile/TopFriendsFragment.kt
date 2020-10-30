package social.tsu.android.ui.user_profile

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.R

class TopFriendsFragment : Fragment() {

    lateinit var recyclerView: RecyclerView
    private val adapter = TopFriendsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_top_friends, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_menu, menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val friends = ArrayList<TopFriendModel>()
        friends.add(TopFriendModel(0, "Jennifer Reid", "", true))
        friends.add(TopFriendModel(0, "Song Bao", "", true))
        friends.add(TopFriendModel(0, "Ren Delan", "", false))
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        adapter.setFriends(friends)
    }
}