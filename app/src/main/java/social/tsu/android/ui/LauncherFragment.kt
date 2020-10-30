package social.tsu.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import social.tsu.android.R
import social.tsu.android.utils.updateLoginStatus

class LauncherFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(
            R.layout.launcher,
            container, false
        )


        view.findViewById<Button>(R.id.create_account).setOnClickListener {
            findNavController().navigate(R.id.showSignupFragment)
        }

        view.findViewById<Button>(R.id.login).setOnClickListener {
            findNavController().navigate(R.id.action_launcherFragment_to_loginFragment)
        }

        view.findViewById<Button>(R.id.clear_login).setOnClickListener {
            clearStoredCredentials()
            findNavController().navigate(R.id.showSignupFragment)
        }

        view.findViewById<Button>(R.id.user_feed).setOnClickListener {
            findNavController().navigate(LauncherFragmentDirections
                .actionLauncherFragmentToUserFeedFragment().setShowCompose(true))
        }

        view.findViewById<Button>(R.id.user_feed_full).setOnClickListener {
            findNavController().navigate(LauncherFragmentDirections
                .actionLauncherFragmentToUserFeedFragment().setShowCompose(false))
        }

        view.findViewById<Button>(R.id.search).setOnClickListener {
            findNavController().navigate(R.id.action_launcherFragment_to_searchFragment)
        }

        return view
    }

    fun clearStoredCredentials() {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "PreferencesFilename",
            masterKeyAlias,
            activity?.applicationContext!!,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit()?.apply{
            remove("LOGIN_USER")
            remove("LOGIN_PASS")
            remove("AUTH_TOKEN")
            remove("USER_ID")
            commit()
        }

        updateLoginStatus()

    }
}


