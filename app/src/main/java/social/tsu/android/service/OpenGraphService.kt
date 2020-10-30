package social.tsu.android.service

import android.util.Log
import io.umehara.ogmapper.OgMapper
import io.umehara.ogmapper.jsoup.JsoupOgMapperFactory
import kotlinx.coroutines.*
import social.tsu.android.data.local.entity.Post
import social.tsu.android.data.local.entity.PostPreview
import social.tsu.android.helper.PostPreviewCache
import social.tsu.android.utils.LinkExtractor
import java.net.URL
import kotlin.coroutines.CoroutineContext


interface OpenGraphService {

    fun info(post: Post, handler: (PostPreview?) -> Unit)
    fun onDestroy()

}

class DefaultOpenGraphService : OpenGraphService, CoroutineScope {

    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.IO

    private val cache = PostPreviewCache

    private val ogMapper: OgMapper by lazy {
        JsoupOgMapperFactory().build()
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
    }

    override fun info(post: Post, handler: (PostPreview?) -> Unit) {
        if (post.preview != null) {
            handler(post.preview)
            return
        }

        val postLink = post.getLinks().firstOrNull() ?: run {
            handler(null)
            return
        }

        cache[postLink]?.let { preview ->
            post.preview = preview
            handler(preview)
            return
        }

        launch {
            val preview = extractLinkPreview(postLink)
            if (preview != null) {
                cache[postLink] = preview
            }
            post.preview = preview
            withContext(Dispatchers.Main) {
                handler(preview)
            }
        }
    }

    private fun extractLinkPreview(link: String): PostPreview? {
        try {
            val host = LinkExtractor.extractHost(link)
            val ogTags = ogMapper.process(URL(link))
            val imageUrl = ogTags?.image?.toString()
            return PostPreview(
                link,
                host,
                ogTags?.title,
                ogTags?.description,
                imageUrl
            )
        } catch (e : Exception) {
            Log.e("LinkExtractor", "Unable to extract link", e)
        }
        return null
    }

}