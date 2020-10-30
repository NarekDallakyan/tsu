package social.tsu.android.workmanager.workers

import android.app.Application
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.work.*
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.data.local.dao.PostFeedDao
import social.tsu.android.data.local.entity.Post
import social.tsu.android.network.api.CommunityApi
import social.tsu.android.network.api.PostApi
import social.tsu.android.network.api.StreamApi
import social.tsu.android.network.api.UploadVideoService
import social.tsu.android.network.model.CreateCommunityPostPayload
import social.tsu.android.network.model.CreatePostPayload
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.NotificationService
import social.tsu.android.service.VideoThumbnailService
import social.tsu.android.workmanager.WorkerKey
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Upload worker that takes video file and uploads it to our server in specific way.
 *  Unlike images, videos are large and are treated differently. They need to be uploaded as Streams
 *  with following algo:
 *  - create steam on server using StreamApi
 *  - upload video to server using id of a stream obtained on previous step
 *  - make a post using stream id
 */
class UploadVideoWorker
@Inject
constructor(
    private val application: Application,
    workerParameters: WorkerParameters,
    private val schedulers: RxSchedulers,
    private val streamApi: StreamApi,
    private val postApi: PostApi,
    private val postFeedDao: PostFeedDao,
    private val communityApi: CommunityApi,
    private val videoThumbnailService: VideoThumbnailService,
    private val uploadVideoService: UploadVideoService,
    private val notificationService: NotificationService
) : RxWorker(application, workerParameters) {

    lateinit var thumbnail: Drawable
    lateinit var streamId: String

    @Module
    abstract class Builder {
        @Binds
        @IntoMap
        @WorkerKey(UploadVideoWorker::class)
        abstract fun bindUploadVideoWorker(worker: UploadVideoWorker): RxWorker
    }

    companion object {
        private var privacy: Int = 0
        const val DATA_VIDEO_FILE = "videoFile"
        const val DATA_VIDEO_CONTENT_URI = "videoContentUri"
        const val DATA_AUTH_TOKEN = "authToken"
        const val DATA_POST_CONTENT = "postContent"
        const val DATA_COMMUNITY_ID = "communityId"

        fun start(
            videoFile: String,
            postContent: String,
            communityId: Int = -1,
            privacy: Int = Post.PRIVACY_PUBLIC
        ) {
            this.privacy = privacy

            val data = Data.Builder()
                .putString(DATA_VIDEO_FILE, videoFile)
                .putString(DATA_POST_CONTENT, postContent)
                .putInt(DATA_COMMUNITY_ID, communityId)
                .build()

            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

            val workReq = OneTimeWorkRequestBuilder<UploadVideoWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .setInitialDelay(2, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance().enqueue(workReq)
        }

        /**
         * Uplpad file as a stream
         * After successful upload will make post in the feed or in community if correct communityId
         * was provided
         *
         * @param contentPath Path to file to be uploaded
         * @param communityId Id of the community that user is posting to, optional
         * @param privacy Post privacy
         * @param postContent Textual content of the post
         */
        fun start(
            contentPath: Uri,
            postContent: String,
            communityId: Int = -1,
            privacy: Int = Post.PRIVACY_PUBLIC
        ) {
            this.privacy = privacy

            val data = Data.Builder()
                .putString(DATA_VIDEO_CONTENT_URI, contentPath.toString())
                .putString(DATA_POST_CONTENT, postContent)
                .putInt(DATA_COMMUNITY_ID, communityId)
                .build()

            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

            val workReq = OneTimeWorkRequestBuilder<UploadVideoWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .setInitialDelay(2, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance().enqueue(workReq)
        }
    }

    private val progressSubject = PublishSubject.create<Int>()
    private val compositeDisposable = CompositeDisposable()


    override fun onStopped() {
        Log.i("UploadTask", "shutting down")
        super.onStopped()
        compositeDisposable.dispose()
    }

    override fun createWork(): Single<Result> {
        val postContent = inputData.getString(DATA_POST_CONTENT) ?: ""
        val communityId = inputData.getInt(DATA_COMMUNITY_ID, -1)


        val notificationId = 100

        compositeDisposable += progressSubject.debounce(10L, TimeUnit.MILLISECONDS)
            .subscribe({ progress ->
                Log.d("UploadTaks", "Progress = $progress")
                notificationService.sendProgressNotification(
                    notificationId,
                    thumbnail, application.getString(R.string.upload_video_text),
                    progress, 100
                )
            }, {

            }, {
                Log.d("UploadTask", "Progress = done! ")
                notificationService.sendProgressNotification(
                    notificationId,
                    thumbnail, application.getString(R.string.upload_video_complete_text),
                    100, 100
                )
            })

        if (inputData.hasKeyWithValueOfType<String>(DATA_VIDEO_FILE)) {
            val videoPath =
                inputData.getString(DATA_VIDEO_FILE) ?: return Single.just(Result.failure())
            val videoFile = File(videoPath)
            return uploadFile(notificationId, postContent, videoFile, communityId)
        } else {
            val contentPath = Uri.parse(inputData.getString(DATA_VIDEO_CONTENT_URI))
                ?: return Single.just(Result.failure())
            return uploadFile(notificationId, postContent, contentPath, communityId)
        }

    }

    private fun uploadFile(
        notificationId: Int,
        postContent: String,
        contentPath: Uri,
        communityId: Int
    ): Single<Result> {
        return videoThumbnailService.fetchThumbnailForVideoUri(contentPath)
            .subscribeOn(schedulers.io())
            .doOnSuccess {
                thumbnail = it
                notificationService.sendProgressNotification(
                    notificationId, it!!,
                    application.getString(R.string.upload_video_text)
                )
            }.flatMap {
                streamApi.createStream()

            }.map {
                it.body()!!

            }.flatMap { response ->
                streamId = response.data.stream.id

                val inputStream =
                    applicationContext.contentResolver.openInputStream(contentPath)!!

                val outputDir = applicationContext.cacheDir // context being the Activity pointer
                val outputFile = File.createTempFile("prefix", "extension", outputDir)

                val out = outputFile.outputStream()
                inputStream.copyTo(out)

                inputStream.close()
                out.close()



                uploadVideoService.uploadVideoFile(
                    response.data.stream.id,
                    response.data.stream.uploadUrl, outputFile, progressSubject
                )

            }.doOnError {
                Log.e("UploadTask", "unable to upload stream", it)
                Result.failure()

            }.flatMap {
                Log.d("UploadTask", "upload response = ${it.code}")
                val postPayload = CreatePostPayload(postContent, streamId, privacy = privacy)
                if (communityId == -1) {
                    postApi.createPost(postPayload)
                } else {
                    communityApi.createPost(communityId, CreateCommunityPostPayload(postPayload))
                }

            }.doOnError {
                Log.e("UploadTask", "unable to create post", it)
                Result.failure()

            }.map {
                Log.i("UploadTask", "post response ${it.code()} ${it.body()}")
//                Toast.makeText(
//                    applicationContext,
//                    application.getString(R.string.upload_video_complete_text),
//                    Toast.LENGTH_SHORT
//                ).show()
                Result.success()
            }
    }

    private fun uploadFile(
        notificationId: Int,
        postContent: String,
        videoFile: File,
        communityId: Int
    ): Single<Result> {
        return videoThumbnailService.fetchThumbnailForVideoFile(videoFile)
            .subscribeOn(schedulers.io())
            .doOnSuccess {
                thumbnail = it
                notificationService.sendProgressNotification(
                    notificationId, it!!,
                    application.getString(R.string.upload_video_text)
                )
            }.flatMap {
                streamApi.createStream()

            }.map {
                it.body()!!

            }.flatMap { response ->
                streamId = response.data.stream.id

                uploadVideoService.uploadVideoFile(
                    response.data.stream.id,
                    response.data.stream.uploadUrl, videoFile, progressSubject
                )

            }.doOnError {
                Log.e("UploadTask", "unable to upload stream", it)
                Result.failure()

            }.flatMap {
                Log.d("UploadTask", "upload response = ${it.code}")
                val postPayload = CreatePostPayload(postContent, streamId, privacy = privacy)
                if (communityId == -1) {
                    postApi.createPost(postPayload)
                } else {
                    communityApi.createPost(communityId, CreateCommunityPostPayload(postPayload))
                }
            }.doOnError {
                Log.e("UploadTask", "unable to create post", it)
                Result.failure()

            }.observeOn(schedulers.main()).map {
                Log.i("UploadTask", "post response ${it.code()} ${it.body()}")
//                Toast.makeText(
//                    applicationContext,
//                    application.getString(R.string.upload_video_complete_text),
//                    Toast.LENGTH_SHORT
//                ).show()
                Result.success()
            }
    }

    private fun generateNotificationId(videoFile: String): Int {
        return videoFile.hashCode()
    }
}