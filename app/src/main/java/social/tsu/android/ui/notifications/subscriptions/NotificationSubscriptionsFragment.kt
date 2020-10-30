package social.tsu.android.ui.notifications.subscriptions


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.AndroidSupportInjection
import social.tsu.android.R
import social.tsu.android.data.local.entity.TsuSubscriptionTopic
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.util.RecyclerSectionItemDecoration
import social.tsu.android.utils.snack
import javax.inject.Inject


class NotificationSubscriptionsFragment : Fragment(),
    NotificationSubscriptionsAdapter.ViewHolderActions {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<NotificationSubscriptionViewModel> { viewModelFactory }

    private val notificationAdapter by lazy {
        NotificationSubscriptionsAdapter(this)
    }

    private val sectionCallback = object : RecyclerSectionItemDecoration.SectionCallback {
        override fun isSection(position: Int): Boolean {
            if (position == 0) { // don't add a section to all notifications
                return false
            }
            val currentItem = notificationAdapter.alData.get(position)
            val previousItem = notificationAdapter.alData.get(position - 1)
            return currentItem.category != previousItem.category
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return if (position == 0) "" else notificationAdapter.alData.get(position).category
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(
            R.layout.notification,
            container, false
        )
        val viewManager = LinearLayoutManager(context)

        val sectionItemDecoration = RecyclerSectionItemDecoration(
            resources.getDimensionPixelSize(R.dimen.header),
            true,
            resources.getDimensionPixelSize(R.dimen.margin_default_small),
            sectionCallback
        )

        view.findViewById<RecyclerView>(R.id.notifications).apply {
            layoutManager = viewManager
            adapter = notificationAdapter
            addItemDecoration(sectionItemDecoration)
        }

        viewModel.loadState.observe(viewLifecycleOwner, Observer(this::handleLoadState))

        viewModel.myNotificationSubscriptions.observe(viewLifecycleOwner, Observer {
            val alData = ArrayList<TsuSubscriptionTopic>()
            it.forEach { item ->
                alData.add(item)
            }

            if (alData.isNotEmpty()) {

                var alCheck = true
                for (i in 0 until alData.size) {
                    if (alData[i].name.equals("all_notifications", true)
                            .not() && alData[i].subscribed.not()
                    ) {
                        alCheck = false
                        break
                    }
                }
                alData[0].subscribed = alCheck
            }
            notificationAdapter.addData(alData)
        })

        return view
    }

    private fun handleLoadState(loadState: Data<Boolean>) {
        if (loadState is Data.Error) {
            loadState.throwable.message?.let {
                snack(it)
            }
        }
    }

    override fun onSubscriptionStatusChanged(item: TsuSubscriptionTopic, isSubscribed: Boolean) {
        if (requireActivity().isInternetAvailable())
            viewModel.setNewSubscriptionStatus(item, isSubscribed)
        else
            requireActivity().internetSnack()
    }
}


