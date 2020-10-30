package social.tsu.android.data.local.models

import social.tsu.android.data.local.entity.TsuSubscriptionTopic


class TsuSubscriptionResponse {
    lateinit var category: String
    lateinit var topics: List<TsuSubscriptionTopic>
}

class TsuNotificationSubscriptions {
    lateinit var subscriptions: ArrayList<TsuSubscriptionResponse>
    var email_digests: Boolean = false
}