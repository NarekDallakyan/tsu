package social.tsu.android.service

import android.graphics.drawable.Drawable
import android.net.Uri
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import io.reactivex.Single
import social.tsu.android.RxSchedulers
import java.io.File
import javax.inject.Inject

class VideoThumbnailService @Inject constructor(private val glideRequestManager: RequestManager,
                                                private val schedulers: RxSchedulers
) {


    fun fetchThumbnailForVideoFile(videoFile: File): Single<Drawable> {
        return Single.create<Drawable> {  emitter ->
            if (videoFile.exists()) {
                glideRequestManager.load(videoFile).into(object : CustomTarget<Drawable>() {
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onResourceReady( resource: Drawable,
                                                  transition: Transition<in Drawable>? ) {
                        emitter.onSuccess(resource)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        emitter.onError(Error("Unable to load thumbnail for image $videoFile"))
                    }
                })

            } else {
                emitter.onError(Error("file $videoFile does NOT exist"))
            }

        }.observeOn(schedulers.io())
    }

    fun fetchThumbnailForVideoUri(uri: Uri): Single<Drawable> {
        return Single.create<Drawable> {  emitter ->
            glideRequestManager.load(uri).into(object : CustomTarget<Drawable>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                }

                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    emitter.onSuccess(resource)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    emitter.onError(Error("Unable to load thumbnail for image $uri"))
                }
            })


        }.observeOn(schedulers.io())
    }


}
