package social.tsu.android.utils

import social.tsu.android.data.local.entity.Post


fun List<Post>.updateWith(post: Post): List<Post> {
    val idx = indexOfFirst {
        return@indexOfFirst it.id == post.id
    }
    return toMutableList().also {
        if (idx != -1) {
            it.removeAt(idx)
            it.add(idx, post)
        }
    }
}
