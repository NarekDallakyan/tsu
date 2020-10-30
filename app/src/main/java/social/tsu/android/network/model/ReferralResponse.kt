package social.tsu.android.network.model

import com.squareup.moshi.Json

data class ReferralResponse (
    @field:Json(name = "referral_link")
    val referralLink: String
)

