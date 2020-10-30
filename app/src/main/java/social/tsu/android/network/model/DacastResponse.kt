package social.tsu.android.network.model

data class DacastResponse(
    val code : Int,
    val token: String
)

// {"code":200,"token":"?hdnea=st=1592396467~exp=1592396587~acl=\/i\/secure\/148510\/148510*~hmac=0a7d36b83f5568e84e8e10f57b4180b77ef5c84b8e49ce1277e114da4ce8d816","expiration":null}

data class HLSStream(
    val hls: String
)