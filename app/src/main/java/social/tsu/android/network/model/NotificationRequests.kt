package social.tsu.android.network.model

data class TokenUpdateRequest(val device_type:String, val device_token:String)

data class UpdateSubscriptionRequest(val topics: Map<String, Boolean>?=null, val email_digests: Boolean?=null)

data class MarkSeenRequest(val until:Long)

data class MarkReadRequest(val id: Long)