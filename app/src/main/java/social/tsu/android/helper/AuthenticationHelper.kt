package social.tsu.android.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.getstream.sdk.chat.Chat
import io.getstream.chat.android.client.errors.ChatError
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.socket.InitConnectionListener
import social.tsu.android.R
import social.tsu.android.data.local.models.ChatUserData
import social.tsu.android.network.api.Environment
import social.tsu.android.network.model.CreateAccountResponsePayload
import social.tsu.android.network.model.UserProfile
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.MainActivityDefaultLayout

object AuthenticationHelper {

    var authToken: String? = null
    var currentUserId: Int? = null
    var currentUsername: String? = null
    var currentUserFullName: String? = null

    fun clear() {
        authToken = null
        currentUserId = null
        currentUsername = null
        currentUserFullName = null
    }

    fun update(data: CreateAccountResponsePayload?) {
        authToken = data?.authToken
        currentUserId = data?.id
        currentUsername = data?.username
        currentUserFullName = data?.fullName
    }

    fun update(data: UserProfile?) {
        currentUsername = data?.username
        currentUserFullName = data?.fullName
    }

    fun loginInfo(context: Activity): Pair<String, String> {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "PreferencesFilename",
            masterKeyAlias,
            context.applicationContext!!,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val user = sharedPreferences.getString("LOGIN_USER", "")
        val pass = sharedPreferences.getString("LOGIN_PASS", "")

        user?.let { mUser ->
            pass?.let { mPass ->
                return Pair(mUser, mPass)
            }
        }

        return Pair("", "")
    }

    fun goToLogin(context:Context){
        Toast.makeText(
            context,
            context.getString(R.string.session_expired_message),
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra("default_layout", MainActivityDefaultLayout.LOG_IN)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun setupLiveChatsUser(
        context: Context,
        chatUserData: ChatUserData,
        token: String,
        onSuccess: () -> Unit = {},
        onFailure: (errMsg: String) -> Unit = {}
    ) {
        try {
            val chat = Chat.Builder(Environment.liveStreamChatsKey, context)
                .logLevel(ChatLogLevel.ALL)
                .build()

            val client = chat.client

            val user = User(chatUserData.username)
            user.extraData = chatUserData.getExtraData()

            client.setUser(user, token, object : InitConnectionListener() {
                override fun onSuccess(data: ConnectionData) {
                    Log.i("AuthenticationService", "setUpChatUser completed")
                    onSuccess.invoke()
                }

                override fun onError(error: ChatError) {
                    Log.e("AuthenticationService", "setUpChatUser onError: $error")
                    onFailure.invoke(error.localizedMessage ?: error.message ?: "unknown error")
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    val KEY_USERNAME = "LOGIN_USER"
    val KEY_PASSWORD = "LOGIN_PASS"
}

fun runIfUserIsAuthenticated(block: (userId: Int) -> Unit) {
    AuthenticationHelper.currentUserId?.let {
        block.invoke(it)
    }
}