package social.tsu.android.helper

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavGraph
import androidx.navigation.NavOptions


fun NavController.navigateSafe(directions: NavDirections, navOptions: NavOptions?) {
    val action = (currentDestination ?: graph).getAction(directions.actionId) ?: return
    var destId = action.destinationId
    val dest = graph.findNode(destId)
    if (dest is NavGraph) {
        // Action destination is a nested graph, which isn't a real destination.
        // The real destination is the start destination of that graph so resolve it.
        destId = dest.startDestination
    }
    if (currentDestination?.id != destId) {
        navigate(directions, navOptions)
    }
}