package social.tsu.android.network.model

import com.squareup.moshi.Json
import social.tsu.android.data.local.entity.Post


data class DiscoveryFeedResponse(

    val data: List<Post>,

    @field:Json(name = "next_page")
    val nextPage: DiscoveryPagination?
)

data class DiscoveryPagination(val curated: Int?, val popular: Int?)