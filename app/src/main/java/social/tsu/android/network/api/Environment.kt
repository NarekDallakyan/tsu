package social.tsu.android.network.api

enum class ProjectEnvironment {
    unknown,
    local,
    test,
    fargate,
    prd
}

object Environment {

    var key: String = ""
        get() =
            when(current) {
                ProjectEnvironment.prd -> "Lb1GVRNi6Qh1Mk7QXOTq9hVMES5pBzi7IiN0Yhw2"
                ProjectEnvironment.test -> "lOCYqriNEq68UwhmYYyFr8UksdVn1pua21gdZDp6"
                else -> "lOCYqriNEq68UwhmYYyFr8UksdVn1pua21gdZDp6"
            }

    var notificationsKey: String = ""
        get() =
            when (current) {
                ProjectEnvironment.prd -> "hE1qxZKZLMaK4O8cq7Nm11LGO9VwR8Ly6RAuXWWY"
                ProjectEnvironment.test -> "X9ZIf8cEGdaPUZgpdy6Yn1gStQhmVbuz6VxqFEom"
                else -> "X9ZIf8cEGdaPUZgpdy6Yn1gStQhmVbuz6VxqFEom"
            }

    var liveStreamChatsKey: String = ""
        get() =
            when (current) {
                ProjectEnvironment.prd -> "2gbxxf5725uk"
                ProjectEnvironment.test -> "2gbxxf5725uk"
                else -> "2gbxxf5725uk"
            }


    var current: ProjectEnvironment = ProjectEnvironment.unknown
        get() = this.projectEnvironment

    private var projectEnvironment: ProjectEnvironment = ProjectEnvironment.unknown

    fun set(projectEnvironment: ProjectEnvironment) {
        this.projectEnvironment = projectEnvironment
    }

}