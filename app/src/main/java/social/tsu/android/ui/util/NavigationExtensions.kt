/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package social.tsu.android.ui.util

import android.content.Intent
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.core.util.set
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import social.tsu.android.R


interface BottomViewFragment {
    fun onFragmentReselected()
}

/**
 * Manages the various graphs needed for a [BottomNavigationView].
 *
 * This sample is a workaround until the Navigation Component supports multiple back stacks.
 */
fun BottomNavigationView.setupWithNavController(
    destinationIds: Collection<Int>,
    navGraphId: Int,
    fragmentManager: FragmentManager,
    containerId: Int,
    intent: Intent,
    listener: NavController.OnDestinationChangedListener
): LiveData<NavController> {

    // Map of tags
    val graphIdToTagMap = SparseArray<String>()
    // Result. Mutable live data with the selected controlled
    val selectedNavController = MutableLiveData<NavController>()

    var firstFragmentGraphId = 0

    // First create a NavHostFragment for each NavGraph ID
    destinationIds.forEachIndexed { index, graphId ->
        val fragmentTag = getFragmentTag(index)

        // If the Nav Host fragment exists, return it
        val existingFragment = fragmentManager.findFragmentByTag(fragmentTag) as NavHostFragment?

        // Find or create the Navigation host fragment
        val navHostFragment = existingFragment ?: createNavHostFragment(
            fragmentManager,
            fragmentTag,
            navGraphId,
            graphId,
            containerId
        )

        if (index == 0) {
            firstFragmentGraphId = graphId
        }

        // Save to the map
        graphIdToTagMap[graphId] = fragmentTag

        // Attach or detach nav host fragment depending on whether it's the selected item.
        if (existingFragment == null) {
            if (this.selectedItemId == graphId) {
                // Update livedata with the selected graph
                selectedNavController.value = navHostFragment.navController
                attachNavHostFragment(fragmentManager, navHostFragment, index == 0)
            } else {
                detachNavHostFragment(fragmentManager, navHostFragment)
            }
        } else {
            try {
                navHostFragment.navController.graph
            } catch (e: IllegalStateException) {
                setNavGraph(navHostFragment, navGraphId, graphId)
                navHostFragment.navController.addOnDestinationChangedListener(listener)
            }
        }
        navHostFragment.navController.addOnDestinationChangedListener(listener)
    }

    // Now connect selecting an item with swapping Fragments
    var selectedItemTag = graphIdToTagMap[this.selectedItemId]
    val firstFragmentTag = graphIdToTagMap[firstFragmentGraphId]
    var isOnFirstFragment = selectedItemTag == firstFragmentTag

    // When a navigation item is selected
    setOnNavigationItemSelectedListener { item ->
        // Don't do anything if the state is state has already been saved.
        if (fragmentManager.isStateSaved) {
            false
        } else {
            val newlySelectedItemTag = graphIdToTagMap[item.itemId]
            // Pop everything above the first fragment (the "fixed start destination")
            fragmentManager.popBackStack(
                firstFragmentTag,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            val selectedFragment = fragmentManager.findFragmentByTag(newlySelectedItemTag)
                    as? NavHostFragment ?: return@setOnNavigationItemSelectedListener false

            // Exclude the first fragment tag because it's always in the back stack.
            if (firstFragmentTag != newlySelectedItemTag) {
                // Commit a transaction that cleans the back stack and adds the first fragment
                // to it, creating the fixed started destination.
                fragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.nav_default_enter_anim,
                        R.anim.nav_default_exit_anim,
                        R.anim.nav_default_pop_enter_anim,
                        R.anim.nav_default_pop_exit_anim
                    )
                    .attach(selectedFragment)
                    .setPrimaryNavigationFragment(selectedFragment)
                    .apply {
                        // Detach all other Fragments
                        graphIdToTagMap.forEach { _, fragmentTagIter ->
                            if (fragmentTagIter != newlySelectedItemTag) {
                                fragmentManager.findFragmentByTag(firstFragmentTag)?.let {
                                    detach(it)
                                }
                            }
                        }
                    }
                    .addToBackStack(firstFragmentTag)
                    .setReorderingAllowed(true)
                    .commit()
            }
            selectedItemTag = newlySelectedItemTag
            isOnFirstFragment = selectedItemTag == firstFragmentTag
            selectedNavController.value = selectedFragment.findNavController()
            true
        }
    }

    // Optional: on item reselected, pop back stack to the destination of the graph
    setupItemReselected(graphIdToTagMap, fragmentManager)

    // Handle deep link
    setupDeepLinks(destinationIds, fragmentManager, intent)

    // Finally, ensure that we update our BottomNavigationView when the back stack changes
    fragmentManager.addOnBackStackChangedListener {
        if (!isOnFirstFragment && !fragmentManager.isOnBackStack(firstFragmentTag)) {
            this.selectedItemId = firstFragmentGraphId
        }

        // Reset the graph if the currentDestination is not valid (happens when the back
        // stack is popped after using the back button).
        selectedNavController.value?.let { controller ->
            if (controller.currentDestination == null) {
                controller.navigate(controller.graph.id)
            }
        }
    }
    return selectedNavController
}

private fun BottomNavigationView.setupDeepLinks(
    destinationIds: Collection<Int>,
    fragmentManager: FragmentManager,
    intent: Intent
) {
    destinationIds.forEachIndexed { index, startDestinationId ->
        val fragmentTag = getFragmentTag(index)

        // Find or create the Navigation host fragment
        val navHostFragment = fragmentManager.findFragmentByTag(fragmentTag) as NavHostFragment?
            ?: return@forEachIndexed
        // Handle Intent
        if (navHostFragment.navController.handleDeepLink(intent)
            && selectedItemId != navHostFragment.navController.graph.id
        ) {
            this.selectedItemId = navHostFragment.navController.graph.id
        }
    }
}

private fun BottomNavigationView.setupItemReselected(
    graphIdToTagMap: SparseArray<String>,
    fragmentManager: FragmentManager
) {
    setOnNavigationItemReselectedListener { item ->
        val newlySelectedItemTag = graphIdToTagMap[item.itemId]
        val selectedFragment = fragmentManager.findFragmentByTag(newlySelectedItemTag)
                as NavHostFragment
        val fragment = selectedFragment.childFragmentManager.primaryNavigationFragment
        if (fragment is BottomViewFragment) {
            fragment.onFragmentReselected()
        }
    }
}

private fun detachNavHostFragment(
    fragmentManager: FragmentManager,
    fragment: Fragment
) {
    fragmentManager.beginTransaction()
        .detach(fragment)
        .commitNow()
}

private fun attachNavHostFragment(
    fragmentManager: FragmentManager,
    fragment: Fragment,
    isPrimaryNavFragment: Boolean
) {
    fragmentManager.beginTransaction()
        .attach(fragment)
        .apply {
            if (isPrimaryNavFragment) {
                setPrimaryNavigationFragment(fragment)
            }
        }
        .commitNow()

}

private fun createNavHostFragment(
    fragmentManager: FragmentManager,
    fragmentTag: String,
    navGraphId: Int,
    startDestinationId: Int,
    containerId: Int
): NavHostFragment {
    // Otherwise, create it and return it.
    val navHostFragment = NavHostFragment()
    fragmentManager.beginTransaction()
        .add(containerId, navHostFragment, fragmentTag)
        .commitNow()
    setNavGraph(navHostFragment, navGraphId, startDestinationId)
    return navHostFragment
}

private fun setNavGraph(
    fragment: NavHostFragment,
    navGraphId: Int,
    startDestinationId: Int
) {
    val navGraph = fragment.navController.navInflater.inflate(navGraphId)
    navGraph.startDestination = startDestinationId
    fragment.navController.graph = navGraph
}

private fun FragmentManager.isOnBackStack(backStackName: String): Boolean {
    val backStackCount = backStackEntryCount
    for (index in 0 until backStackCount) {
        if (getBackStackEntryAt(index).name == backStackName) {
            return true
        }
    }
    return false
}

private fun getFragmentTag(index: Int) = "bottomNavigation#$index"
