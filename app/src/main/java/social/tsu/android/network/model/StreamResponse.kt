package social.tsu.android.network.model

import com.squareup.moshi.Json

/**
 * {
 * data={
 * stream={
 * id=f051bca9-7627-40c6-855a-467438be878f,
 * upload_url=https://tsu-staging-stream.s3.amazonaws.com/posts/streams/f051bca9-7627-40c6-855a-467438be878f/video_part_00001/video_file?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIA6CYTXBNCUEL6PCW2%2F20200228%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20200228T064752Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=9305407ec3c1f440b6a5f9802e0408c88e8ed14eb4eaaf76423aa7539e1f7396}}}
 */
data class StreamResponse(
    val stream: StreamUploadEndpoint
)
data class StreamUploadEndpoint(
    val id: String,
    @field:Json(name = "upload_url")
    val uploadUrl: String
)
