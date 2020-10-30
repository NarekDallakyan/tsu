package social.tsu.android.network.model

data class ResetPasswordRequest (
    val email:String,
    val password:String,
    val code:String
)