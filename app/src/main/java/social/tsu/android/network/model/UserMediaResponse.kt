package social.tsu.android.network.model

import social.tsu.android.data.local.entity.Post

data class UserMediaResponse(
    val data: List<Post>,
    val next_page: Int?
)



