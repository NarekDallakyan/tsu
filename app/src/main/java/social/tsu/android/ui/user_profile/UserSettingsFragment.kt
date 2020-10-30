package social.tsu.android.ui.user_profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment
import social.tsu.android.R
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.ConsentHelper
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.view.SettingsItemView
import social.tsu.android.utils.AppVersion
import social.tsu.android.utils.applyTo

class UserSettingsFragment : DaggerFragment(), View.OnClickListener {

    private lateinit var selectAccountItem: SettingsItemView
    private lateinit var selectPersonalizedAds: SettingsItemView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.user_settings_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectAccountItem = view.findViewById(R.id.settings_select_account)
        selectAccountItem.valueText = AuthenticationHelper.currentUserFullName

        selectPersonalizedAds = view.findViewById(R.id.settings_personalized_ads)

        view.findViewById<TextView>(R.id.settings_version).text = getString(R.string.drawer_item_version, AppVersion.versionName)
        view.findViewById<TextView>(R.id.settings_build).text = getString(R.string.drawer_item_build, AppVersion.versionCode)

        ConsentHelper.shouldShowConsent.observe(viewLifecycleOwner, Observer {
            selectPersonalizedAds.isVisible = it
        })

        applyTo(
            view.findViewById<SettingsItemView>(R.id.settings_account),
            view.findViewById<SettingsItemView>(R.id.settings_bank),
            view.findViewById<SettingsItemView>(R.id.settings_analytics),
            view.findViewById<SettingsItemView>(R.id.settings_invite),
            selectAccountItem,
            view.findViewById<SettingsItemView>(R.id.settings_notifications),
            view.findViewById<SettingsItemView>(R.id.settings_terms_of_use),
            view.findViewById<SettingsItemView>(R.id.settings_payments_policy),
            view.findViewById<SettingsItemView>(R.id.settings_privacy_policy),
            view.findViewById<SettingsItemView>(R.id.settings_support),
            selectPersonalizedAds,
            view.findViewById<SettingsItemView>(R.id.settings_sign_out)
        )
    }

    override fun onClick(v: View?) {
        val activity = this.activity as? MainActivity
        when (v?.id) {
            R.id.settings_account -> findNavController().navigate(R.id.editAccountFragment)
            R.id.settings_bank -> findNavController().navigate(R.id.bankAccountFragment)
            R.id.settings_analytics -> findNavController().navigate(R.id.insightsFragment)
            R.id.settings_invite -> findNavController().navigate(R.id.inviteFriendsFragment)
            R.id.settings_select_account -> {}
            R.id.settings_notifications -> findNavController().navigate(R.id.notificationSubscriptionsFragment)
            R.id.settings_terms_of_use -> findNavController().navigate(R.id.termsOfService)
            R.id.settings_payments_policy -> findNavController().navigate(R.id.paymentPolicy)
            R.id.settings_privacy_policy -> findNavController().navigate(R.id.privacyPolicy)
            R.id.settings_support -> findNavController().navigate(R.id.support)
            R.id.settings_personalized_ads -> ConsentHelper.getConsent(requireActivity(), true)
            R.id.settings_sign_out -> activity?.onLogOutSuccess()
        }
    }

}