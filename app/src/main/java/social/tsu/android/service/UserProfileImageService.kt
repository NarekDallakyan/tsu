package social.tsu.android.service

import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.TsuApplication
import social.tsu.android.helper.UserProfileImageCache
import social.tsu.android.network.api.HostEndpoint
import social.tsu.android.network.api.HostProvider

abstract class UserProfileImageService: DefaultService() {

    companion object {
        val MISSING_IMAGE_VALUES =
            arrayOf("/assets/user.png", "/cover_pictures/original/missing.png")
    }

    abstract fun getProfilePicture(
        key: String?,
        ignoringCache: Boolean,
        handler: ImageFetchHandler?
    )

    abstract fun getCoverPicture(key: String?, ignoringCache: Boolean, handler: ImageFetchHandler?)

}

typealias ImageFetchHandler = (Drawable?) -> Unit

class DefaultUserProfileImageService(private val application: TsuApplication): UserProfileImageService() {

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override val tag: String
        get() = "DefaultUserProfileImageSerivce"

    private val cache: UserProfileImageCache = UserProfileImageCache

    override fun getProfilePicture(
        key: String?,
        ignoringCache: Boolean,
        handler: ImageFetchHandler?
    ) {
        // TODO temporary solution until backend fix
        //  key.contains("user.png") || key.contains("missing.png")
        if (key.isNullOrEmpty() || key.contains("user.png") || key.contains("missing.png")) {
            handler?.invoke(null)
        } else {
            if (!ignoringCache) {
                fetchImageFromCache(key)?.let {
                    handler?.invoke(it)
                    return
                }
            }

            fetchImage(key, handler)
            fetchOtherVersionOfProfilePhoto(key)
        }
    }

    override fun getCoverPicture(
        key: String?,
        ignoringCache: Boolean,
        handler: ImageFetchHandler?
    ) {
        // TODO temporary solution until backend fix
        //  key.contains("user.png") || key.contains("missing.png")
        if (key.isNullOrEmpty() || key.contains("user.png") || key.contains("missing.png")) {
            handler?.invoke(null)
        } else {
            if (!ignoringCache) {
                fetchImageFromCache(key)?.let {
                    handler?.invoke(it)
                    return
                }
            }

            fetchImage(key, handler)
        }
    }

    private fun fetchImage(key: String, handler: ImageFetchHandler?) {
        val url = formatUrl(key)

        Glide.with(application)
            .asDrawable()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d(tag, "Failed to load image at $key")
                    handler?.invoke(null)
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    resource?.let {
                        cacheImage(key, it)
                        handler?.invoke(it)
                    } ?: run {
                        handler?.invoke(null)
                    }
                    return true
                }
            })
            .load(url)
            .timeout(6000)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(object : CustomTarget<Drawable>() {
                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {

                }
            })
    }

    private fun fetchOtherVersionOfProfilePhoto(key: String) {
        if (key.contains("square", false)) {
            val largeKey = key.replace("square", "large", false)
            fetchImage(largeKey, null)
        } else if (key.contains("large", false)) {
            val largeKey = key.replace("large", "square", false)
            fetchImage(largeKey, null)
        }
    }

    private fun cacheImage(key: String, image: Drawable) {
        cache[key] = image
    }

    private fun fetchImageFromCache(key: String): Drawable? {
        return cache[key]
    }

    private fun formatUrl(part: String): Uri {
        if (part.startsWith("/")){
            return Uri.parse("${HostProvider.host(HostEndpoint.image)}${part}")
        }

        return Uri.parse(part)
    }

}
