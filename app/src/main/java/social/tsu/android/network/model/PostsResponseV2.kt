package social.tsu.android.network.model

import social.tsu.android.data.local.entity.Post

data class PostsResponseV2 (val sources: List<PostV2>,
                            val next_page: Int?)

data class PostV2 (val type: String, val posts: List<Post>)
