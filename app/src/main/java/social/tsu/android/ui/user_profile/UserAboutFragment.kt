package social.tsu.android.ui.user_profile

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.text.set
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerFragment
import social.tsu.android.R
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.showUserProfile
import social.tsu.android.network.model.UserProfile
import social.tsu.android.network.model.UserProfileParams
import social.tsu.android.utils.URLSpanNoUnderline
import social.tsu.android.utils.openUrl
import social.tsu.android.utils.show
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


class UserAboutFragment() : DaggerFragment(), AboutUserActionCallback {

    private var userId: Int = 0

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory


    private val profileViewModel by viewModels<UserProfileViewModel>(
        ownerProducer = { requireParentFragment() }, factoryProducer = { viewModelFactory }
    )

    private lateinit var recyclerView: RecyclerView

    private val aboutAdapter: AboutUserAdapter by lazy {
        AboutUserAdapter(this)
    }

    val args by navArgs<UserAboutFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindUserInfo(args.profileInfo)

        val viewManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        recyclerView = view.findViewById<RecyclerView>(R.id.about_recycler).apply {
            layoutManager = viewManager
            adapter = aboutAdapter
        }

//        profileViewModel.isBlock.observe(viewLifecycleOwner, Observer {
//            if (it) {
//                panelAbout.hide()
//            } else
//                panelAbout.show()
//
//        })
    }

    //Generate list of items that will represent all info about user
    private fun bindUserInfo(userProfile: UserProfileParams) {

        val ownProfile = AuthenticationHelper.currentUserId == userProfile.id

        val items = ArrayList<AboutItem>()
        //Uncomment below for real values and comment out addMockData() call to remove mock data from list
        createInfoItems(items, userProfile)

        aboutAdapter.updateItems(items)
    }

    private fun createInfoItems(
        items: java.util.ArrayList<AboutItem>,
        userProfile: UserProfileParams
    ) {

        val ownProfile = AuthenticationHelper.currentUserId == userProfile.id

        items.add(AboutUserHeader(getString(R.string.about_profile_intro), ownProfile))
        var bio = userProfile.bio ?: ""
        if (bio.isEmpty()) {
            items.add(AboutEmptyUserItem(0, getString(R.string.empty_bio)))
        } else {
            items.add(AboutUserDescription(bio))
        }
        items.add(AboutUserDivider)

        items.add(
            AboutUserHeader(
                getString(R.string.about_basic_info),
                ownProfile
            )
        )
        var basicInfoEmpty = true
        if (!userProfile.namePronunciation.isNullOrEmpty()) {
            items.add(AboutUserItem(R.drawable.user_nav_header, getString(R.string.basic_info_name_pronunciation), userProfile.namePronunciation))
            basicInfoEmpty = false
        }
        if (!userProfile.birthday.isNullOrEmpty()) {
            //TODO: Uncomment birthdate when privacy toggle (to turn visibility of birthdate on/off) is in place.
           // items.add(setBirthdate(userProfile))
            basicInfoEmpty = false
        }
        if (basicInfoEmpty) {
            items.add(AboutEmptyUserItem(R.drawable.user_nav_header, getString(R.string.empty_info)))
        }
        items.add(AboutUserDivider)

        items.add(AboutUserHeader(getString(R.string.about_relationship), ownProfile))
        items.add(setRelationship(userProfile))
        items.add(AboutUserDivider)

        items.add(AboutUserHeader(getString(R.string.about_contact_info), ownProfile))
        addSocialLinks(items, userProfile)
        items.add(AboutUserDivider)
    }

    private fun addSocialLinks(
        items: java.util.ArrayList<AboutItem>,
        userProfile: UserProfileParams
    ) {
        var hasContacts = false
        val site = bindWebsite(userProfile.website)
        if (site != null) {
            items.add(site)
            hasContacts = true
        }
        if (!userProfile.instagram.isNullOrEmpty()) {
            items.add(setMediaItem(
                R.drawable.ic_user_profile_instagram,
                R.string.about_profile_instagram,
                "https://www.instagram.com/",
                userProfile.instagram
            ))
            hasContacts = true
        }

        if (!userProfile.facebook.isNullOrEmpty()) {
            items.add(setMediaItem(
                R.drawable.ic_user_profile_facebook,
                R.string.about_profile_facebook,
                "https://www.facebook.com/",
                userProfile.facebook
            ))
            hasContacts = true
        }

        if (!userProfile.youtube.isNullOrEmpty()) {
            items.add(setMediaItem(
                R.drawable.ic_user_profile_youtube,
                R.string.about_profile_youtube,
                "https://www.youtube.com/",
                userProfile.youtube
            ))
            hasContacts = true
        }

        if (!hasContacts) {
            items.add(AboutEmptyUserItem(R.drawable.ic_phone, getString(R.string.empty_info)))
        }
    }

    private fun setBirthdate(userProfile: UserProfileParams): AboutItem {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = userProfile.birthday?.let { formatter.parse(it) }

        val prettyFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val value = if (date != null) prettyFormat.format(date) else ""

        return AboutUserItem(
            R.drawable.ic_calendar,
            getString(R.string.about_info_birthday),
            value
        )
    }

    private fun setMediaItem(
        @DrawableRes iconRes: Int,
        @StringRes nameRes: Int,
        link: String,
        value: String
    ): AboutItem {
        return AboutUserItem(iconRes, getString(nameRes), value) {
            context?.openUrl("$link$value")
        }
    }

    /*
    private fun addMockData(items: java.util.ArrayList<AboutItem>, isOwnProfile: Boolean) {


        items.add(AboutUserHeader(getString(R.string.about_profile_intro), isOwnProfile))
        items.add(AboutUserDescription("Just stylish guy"))
        items.add(AboutUserDivider)

        items.add(
            AboutUserHeader(
                getString(R.string.about_basic_info),
                isOwnProfile,
                object : AboutUserHeaderListener {
                    override fun onEditClicked() {
                        findNavController().navigate(R.id.basicInfoFragment)
                    }
                })
        )
        items.add(AboutUserItem(R.drawable.user_nav_header, "Name Pronunciation", "Gene Figueroa"))
        items.add(AboutUserItem(R.drawable.ic_calendar, "Birthdate", "Aug 04, 1988"))
        items.add(AboutUserItem(R.drawable.ic_home, "Hometown", "JSetauket, New York"))
        items.add(AboutUserDivider)

        items.add(AboutUserHeader(getString(R.string.about_relationship), isOwnProfile))
        items.add(AboutUserItem(R.drawable.ic_like, "Relationship", "Single"))
        items.add(AboutUserDivider)

        items.add(AboutUserHeader(getString(R.string.about_contact_info), isOwnProfile))
        items.add(AboutUserItem(R.drawable.ic_home, "Phone Number", "185-442-9114"))
        items.add(AboutUserItem(R.drawable.ic_website, "Email", "c-owens@gmail.com"))
        items.add(AboutUserDivider)

        items.add(AboutUserHeader(getString(R.string.about_places_lived), isOwnProfile))
        items.add(AboutUserItem(R.drawable.ic_lived, "Current City", "Setauket, New York"))
        items.add(AboutUserItem(R.drawable.ic_lived, "Lived", "Redding, Connecticut"))
        items.add(AboutUserDivider)

        items.add(AboutUserHeader(getString(R.string.about_work), isOwnProfile))
        items.add(AboutUserItem(R.drawable.ic_working, "Working", "Norton"))
        items.add(AboutUserItem(R.drawable.ic_working, "Worked", "Reddott co"))
        items.add(AboutUserDivider)

        items.add(
            AboutUserHeader(
                getString(R.string.about_top_friends),
                isOwnProfile,
                object : AboutUserHeaderListener {
                    override fun onEditClicked() {
                        findNavController().navigate(R.id.topFriendsFragment)
                    }
                })
        )
        items.add(AboutUserItem(R.drawable.user_nav_header, "Friend", "Jennifer Reid"))
        items.add(AboutUserItem(R.drawable.user_nav_header, "Friend", "Song Bao"))
        items.add(AboutUserItem(R.drawable.user_nav_header, "Friend", "Ren Delan"))
        items.add(
            AboutUserHeader(
                getString(R.string.about_interests),
                isOwnProfile,
                object : AboutUserHeaderListener {
                    override fun onEditClicked() {
                        findNavController().navigate(R.id.interestsFragment)
                    }
                })
        )
        items.add(AboutUserDivider)
    }*/

    private fun setRelationship(userProfile: UserProfileParams): AboutItem {
        val status = userProfile.relationshipStatus

        if (status != null && status.isNotBlank()) {
            val name = userProfile.relationshipWith
            if (!name.isNullOrBlank()) {
                val text = getString(
                    R.string.user_info_relationship_to_full,
                    status,
                    name
                )
                val startIdx = text.indexOf(name)
                val spannable = SpannableString(text)
                val context = this.context
                if (startIdx >= 0 && context != null) {
                    spannable[startIdx, startIdx + name.length] = UsernameClickableSpan(
                        context, userProfile.relationshipWithId
                    )
                }

                return AboutUserItem(
                    R.drawable.ic_like,
                    getString(R.string.about_relationship),
                    spannable
                )
            } else {
                return AboutUserItem(
                    R.drawable.ic_like,
                    getString(R.string.about_relationship),
                    status
                )
            }
        }
        return AboutEmptyUserItem(R.drawable.ic_like, getString(R.string.empty_info))
    }

    private fun setAboutBio(value: UserProfile) {
        /*  user_info_about_label.show()
          user_info_about.show()
          if (!value.bio.isNullOrBlank()) {
              user_info_about.text = value.bio
              user_info_about.setTextColor(
                  ContextCompat.getColor(
                      user_info_about.context,
                      R.color.text_description
                  )
              )
          } else {
              if (userId == AuthenticationHelper.currentUserId) {
                  user_info_about.setText(R.string.user_info_about_no_value_my)
              } else {
                  user_info_about.text =
                      getString(R.string.user_info_about_no_value, value.firstname, value.lastname)
              }
              user_info_about.setTextColor(ContextCompat.getColor(user_info_about.context, R.color.text_no_info_value))
          }*/
    }

    private fun setMediaButton(textView: ImageButton, value: String?, link: String) {
        val context = textView.context ?: return
        if (!value.isNullOrBlank()) {
            textView.show()
            textView.setOnClickListener {
                context.openUrl("$link$value")
            }
        }
    }

    private fun bindWebsite(website: String?): AboutItem? {
        if (website != null && website.isNotBlank()) {
            val text = SpannableString(website)
            val urlMatcher = Patterns.WEB_URL.matcher(website)
            while (urlMatcher.find()) {
                text.setSpan(
                    URLSpanNoUnderline(requireContext(), urlMatcher.group()),
                    urlMatcher.start(),
                    urlMatcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return AboutUserItem(
                R.drawable.ic_email,
                getString(R.string.about_user_site),
                text.toString()
            ) {
                val value = if (!website.startsWith("http")) "https://$website" else website
                context?.openUrl(value)
            }
        } else {
            return null
        }
    }

    private inner class UsernameClickableSpan(
        private val context: Context,
        private val userId: Int?
    ) : ClickableSpan() {
        override fun onClick(widget: View) {
            if (userId != null) {
                findNavController().showUserProfile(userId)
            }
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ds.underlineThickness =
                    context.resources.getDimension(R.dimen.user_info_underline_width)
                ds.underlineColor = ContextCompat.getColor(
                    context,
                    R.color.text_info_value_underline
                )
            }
            ds.color = Color.WHITE
        }
    }


}