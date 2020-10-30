package social.tsu.android.network.model

import com.squareup.moshi.Json

data class AppConfiguration(
    val data: AppConfigurationData
)

data class AppConfigurationData(
    @field:Json(name = "min_ios_version")
    val miniOSVersion: String,
    @field:Json(name = "min_android_version")
    val minAndroidVersion: String,
    @field:Json(name = "welcome_images_bucket_url")
    val welcomeImageBucketUrl: String?,
    @field:Json(name = "min_amount_to_redeem")
    val minRedeemValue: Float,
    @field:Json(name = "max_tsupports_per_day")
    val maxSupportsPerDay: Int
)