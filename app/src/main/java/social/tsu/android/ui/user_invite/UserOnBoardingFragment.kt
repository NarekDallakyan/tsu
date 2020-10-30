package social.tsu.android.ui.user_invite


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_user_onboarding.*
import kotlinx.android.synthetic.main.user_onboarding_slide.view.*
import kotlinx.coroutines.delay
import social.tsu.android.R
import social.tsu.android.utils.hide
import social.tsu.android.utils.onBackPressed
import social.tsu.android.utils.show


// When class was extended by NoAppBarFragment toolbar was blinking or appeared
class OldUserOnBoardingFragment : Fragment() {

    val args by navArgs<OldUserOnBoardingFragmentArgs>()
    private val titles by lazy { resources.getStringArray(R.array.on_boarding_titles) }
    private val messages by lazy { resources.getStringArray(R.array.on_boarding_messages) }
    private val images = arrayOf(
        R.drawable.user_onboarding_1,
        R.drawable.user_onboarding_2,
        R.drawable.user_onboarding_3,
        R.drawable.user_onboarding_4,
        R.drawable.user_onboarding_5,
        R.drawable.user_onboarding_6,
        R.drawable.user_onboarding_7
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenResumed {
            delay(1000)
            setUpCarouselView()
        }
        onBackPressed(true) {
            findNavController().popBackStack(
                R.id.mainFeedFragment,
                false
            )
        }

    }

    private fun setUpCarouselView() {
        thank_you_text.hide()
        carousel_view.show()
        val onBoardingItems = arrayListOf<OnBoardingItem>()
        titles.forEachIndexed { index, _ ->
            val item =
                //eLeaving the below block in for now if the UI changes again.
                /*if (index == 0) {
                OnBoardingItem(
                    titles[index],
                    null,
                    messages[index],
                    images[index],
                    index == titles.size - 1
                )
            } else*/ if (index == titles.size - 1 && args.oldUserDetails != null) {
                OnBoardingItem(
                    null,
                    null,
                    getString(R.string.old_user_last_slide_message), null,
                    true
                )
            } else {
                OnBoardingItem(
                    null,
                    titles[index],
                    messages[index],
                    images[index],
                    index == titles.size - 1
                )
            }
            onBoardingItems.add(item)
        }
        carousel_view.setViewListener {

            val slideItem = onBoardingItems[it]
            val customView = layoutInflater.inflate(R.layout.user_onboarding_slide, null)
            slideItem.imageRes?.let { res -> customView.slide_image?.setImageResource(res) }

            customView.on_boarding_top_title?.text = slideItem.title
            customView.on_boarding_subtitle?.text = slideItem.subtitle
            customView.on_boarding_text?.text = slideItem.message

            if (slideItem.isFinalSlide) {
                customView.edit_profile_button?.show()
                customView.done_button?.show()
            } else {
                customView.edit_profile_button?.hide()
                customView.done_button?.hide()
            }

            customView.edit_profile_button?.setOnClickListener {
                goToProfileEdit()
            }

            customView.done_button?.setOnClickListener {
                findNavController().popBackStack(
                    R.id.mainFeedFragment,
                    false
                )
            }
            return@setViewListener customView
        }
        carousel_view.pageCount = titles.size
    }

    private fun goToProfileEdit() {
        findNavController().navigate(
            OldUserOnBoardingFragmentDirections.actionOldUserOnBoardingFragmentToProfileEditFragment()
                .apply {
                    oldUserDetails = args.oldUserDetails
                    isNewUser = true
                })
    }
}

data class OnBoardingItem(
    val title: String?, val subtitle: String?,
    val message: String, @DrawableRes val imageRes: Int?,
    val isFinalSlide: Boolean = false
)