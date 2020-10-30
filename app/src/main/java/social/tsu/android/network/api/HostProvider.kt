package social.tsu.android.network.api

enum class HostEndpoint {
    api,
    image,
    video,
    notification
}

object HostProvider {
    private val environment: Environment = Environment
    private val localHostPort: Int = 3000

    val apiHost: String by lazy {
        when(environment.current) {
            ProjectEnvironment.prd -> "https://api.tsuprod.com"
            ProjectEnvironment.local -> "http://0.0.0.0"
            ProjectEnvironment.test -> "https://api.utvstage.com"
            ProjectEnvironment.fargate -> "http://matt-fargate-lb-1020943446.us-east-1.elb.amazonaws.com"
            else -> ""
        }
    }

    val imageHost: String by lazy {
        when(environment.current) {
            ProjectEnvironment.prd -> "https://images.tsuprod.com"
            ProjectEnvironment.local -> "http://0.0.0.0"
            ProjectEnvironment.test -> "https://images.utvstage.com"
            ProjectEnvironment.fargate -> "http://d3jibqhzv1fm2e.cloudfront.net"
            else -> ""
        }
    }

    val videoHost: String by lazy {
        when(environment.current) {
            ProjectEnvironment.prd -> "https://cdn-stream-prod.tsuprod.com"
            else -> "https://cdn-stream-staging.utvstage.com"
        }
    }

    val notificationsHost: String by lazy {
        when(environment.current) {
            ProjectEnvironment.prd -> "https://notifications.tsuprod.com"
            ProjectEnvironment.test -> "https://notifications.utvstage.com"
            else -> ""
        }
    }

    fun host(type: HostEndpoint): String {
        return when(type) {
            HostEndpoint.api -> apiHost
            HostEndpoint.image -> imageHost
            HostEndpoint.video -> videoHost
            HostEndpoint.notification -> notificationsHost
        }
    }

}