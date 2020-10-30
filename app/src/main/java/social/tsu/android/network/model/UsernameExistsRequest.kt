package social.tsu.android.network.model

//data class User(val username: String)
data class UsernameExistsRequest( val user: User )
data class UsernameExistsResponse(val result: Boolean)
