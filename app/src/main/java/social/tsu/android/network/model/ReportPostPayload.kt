package social.tsu.android.network.model

import com.squareup.moshi.Json

data class ReportPostPayload (
    @field:Json(name = "post_id")
    val postId: Long,
    val type: Int)

