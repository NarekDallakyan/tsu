package social.tsu.android.network.model

import com.squareup.moshi.Json
import social.tsu.android.data.local.entity.Post


data class HashtagResponse(

    val data: List<Post>,

    @field:Json(name = "next_page")
    val nextPage: Int?
)