package social.tsu.android.network.model

/*
{
    "error": true,
    "message": "You have already followed this user."
}
 */
data class FollowResponse(val error: Boolean = false, val message: String)

