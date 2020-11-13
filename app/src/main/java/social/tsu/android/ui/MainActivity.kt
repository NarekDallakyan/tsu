package social.tsu.android.ui

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.chartboost.sdk.Chartboost
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.mopub.common.MoPub
import com.mopub.common.SdkConfiguration
import dagger.android.AndroidInjection
import dagger.android.support.DaggerAppCompatActivity
import de.hdodenhof.circleimageview.CircleImageView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.Period
import social.tsu.android.NavGraphDirections
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.repository.MessagingRepository
import social.tsu.android.ext.hideKeyboard
import social.tsu.android.helper.*
import social.tsu.android.helper.ads.AdsConstants
import social.tsu.android.network.model.UserProfile
import social.tsu.android.service.*
import social.tsu.android.ui.community.BottomListener
import social.tsu.android.ui.messaging.chats.ChatFragment
import social.tsu.android.ui.messaging.tsu_contacts.TsuContactsFragment
import social.tsu.android.ui.messaging.tsu_contacts.TsuContactsFragmentArgs
import social.tsu.android.ui.notifications.feed.NotificationsViewModel
import social.tsu.android.ui.post.view.draft.PostDraftFragment
import social.tsu.android.ui.post_feed.main.MainFeedFragment
import social.tsu.android.ui.post_feed.main.MainFeedFragmentDirections
import social.tsu.android.utils.*
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class MainActivityDefaultLayout {
    SIGN_UP,
    LOG_IN,
    FEED
}

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class MainActivity : DaggerAppCompatActivity(), NavController.OnDestinationChangedListener,
    UserInfoServiceCallback, BottomListener, LogoutListener {

    private var toolbarLogo: ImageView? = null
    private val TAG = MainActivity::class.java.simpleName
    private var mImageUrl: String? = null
    private var mVideoUrl: String? = null

    private lateinit var feedToolbarButtons: View
    private lateinit var newMessageButton: ImageView

    private lateinit var navController: NavController
    private lateinit var dateFormat: SimpleDateFormat

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var bottomNavigationDivider: View
    private var profileIcon: CircleImageView? = null
    private var livestreamIcon: ImageView? = null
    private var notifyBadge: TextView? = null
    private var toolbarTitle: TextView? = null
    private var toolbar: Toolbar? = null
    private var appBarLayout: AppBarLayout? = null
    private var searchIcon: ImageView? = null
    private var mCalender: Calendar? = null
    private var dspTimer: Disposable? = null
    val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val age = Calendar.getInstance()

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var viewmodelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var userProfileImageService: UserProfileImageService

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    private val mainViewModel by viewModels<MainViewModel> { viewmodelFactory }

    private val adsViewModel by viewModels<AdsSupportViewModel> { viewmodelFactory }

    private val notificationViewModel by viewModels<NotificationsViewModel> { viewmodelFactory }

    @Inject
    lateinit var friendsService: FriendService

    @Inject
    lateinit var messagingRepository: MessagingRepository

    private val userService: UserInfoService by lazy {
        DefaultUserInfoService(this.application as TsuApplication, this)
    }

    private val bottomViewDestinations = setOf(
        R.id.mainFeedFragment,
        R.id.searchFragment,
        R.id.communityFragment
    )
    private val topLevelDestinations = setOf(
        R.id.oldUserEnterEmailFragment,
        R.id.currentUserProfileFragment,
        R.id.recentContactsFragment
    ).plus(bottomViewDestinations)

    private val appBarConfiguration: AppBarConfiguration by lazy {
        AppBarConfiguration(topLevelDestinations)
    }

    private val appBarChangedListener: ActionBarOnDestinationChangedListener by lazy {
        ActionBarOnDestinationChangedListener(this, appBarConfiguration)
    }
    private var isCustomToolbar: Boolean = true


    private fun setupNewPostMenuItem(bnv: BottomNavigationView) {
        bnv.menu.findItem(R.id.postTypesFragment).setOnMenuItemClickListener {
            checkPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                ),
                NEW_POST_PERMISSIONS_REQUEST_CODE
            ) {
                openPostFragment()
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun onStop() {
        super.onStop()
        window.decorView.hideKeyboard(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        window.setBackgroundDrawableResource(android.R.drawable.screen_background_dark)
        setContentView(R.layout.app_bar_main)
        AndroidInjection.inject(this)
        mImageUrl = intent?.getStringExtra(Constants.IMAGE_URI)
        mVideoUrl = intent?.getStringExtra(Constants.VIDEO_URI)

        toolbar = findViewById(R.id.toolbar)
        toolbarTitle = findViewById(R.id.toolbar_title)
        toolbarLogo = findViewById(R.id.toolbar_logo)
        feedToolbarButtons = findViewById(R.id.feed_toolbar_buttons)
        newMessageButton = findViewById(R.id.new_message)
        bottomNavigation = findViewById(R.id.bottom_navbar)
        bottomNavigationDivider = findViewById(R.id.bottom_navbar_divider)
        notifyBadge = findViewById(R.id.notify_badge)
        appBarLayout = findViewById(R.id.main_app_bar_layout)
        livestreamIcon = findViewById(R.id.btn_toolbar_live)
        searchIcon = findViewById(R.id.btn_toolbar_search)

        ImageView(this).apply {
            setImageResource(R.drawable.ic_tsu_plus_button)
            bottomNavigation.getMenuItemViewById(R.id.postTypesFragment)?.let { itemView ->
                itemView.setIconSizeDimen(R.dimen.bottom_profile_icon_size)
                itemView.replaceChildWithView(0, this)
            }
        }

        profileIcon = CircleImageView(this).apply {
            setImageResource(R.drawable.user)
            id = android.R.id.icon
            borderColor = getColor(R.color.bottom_nav_item_active)
            isBorderOverlay = true
            borderWidth = 0
            bottomNavigation.getMenuItemViewById(R.id.currentUserProfileFragment)?.let { itemView ->
                itemView.setIconSizeDimen(R.dimen.bottom_profile_icon_size)
                itemView.replaceChildWithView(0, this)
            }
        }

        setSupportActionBar(toolbar)
        toolbar?.title = ""
        supportActionBar?.setDisplayShowTitleEnabled(false)

        ConsentHelper.shouldShowConsent(this, object : ConsentHelper.Companion.ConsentResult {
            override fun onConsentResult(shouldShowConsentButton: Boolean) {
            }
        })

        navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener(this)


        restoreToolbar()

        bottomNavigation.setupWithNavController(navController)

        setupNewPostMenuItem(bottomNavigation)

        messagingRepository.unreadCount.observe(this, Observer {
            notifyUnreadChatsUpdated(it)
        })

        initAds()

        initRemoteConfig()

        checkLoggedIn()
        initButtons()

        loadCurrentUser()
        dateFormat = SimpleDateFormat("yyyy-MM-dd")

        startTimer()

        //uncomment to test ads integration
//        MediationTestSuite.launch(this)
        if (mImageUrl.isNullOrEmpty().not() || mVideoUrl.isNullOrEmpty().not()) {
            openPostFragmentWithResource()
        }
    }

    private fun startTimer() {
        Observable.interval(0, 3000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : io.reactivex.Observer<Long?> {
                override fun onComplete() {

                }

                override fun onSubscribe(d: Disposable) {
                    dspTimer = d
                }

                override fun onNext(t: Long) {
                    val todayAsString = dateFormat.format(Calendar.getInstance().time)
                    sharedPrefManager.getExclusivePostTime()?.let { tomorrowAsString ->
                        if (tomorrowAsString.equals(todayAsString, true).not()) {
                            sharedPrefManager.setExclusivePostTime("")
                        }
                    }
                    sharedPrefManager.getLaunchTime()?.let { launchtime ->
                        if (launchtime.equals(todayAsString, true).not()) {
                            sharedPrefManager.setLaunchTime(
                                SimpleDateFormat("yyyy-MM-dd").format(
                                    System.currentTimeMillis()
                                )
                            )
                            sharedPrefManager.setSupportPostId("")
                        }
                    }
                }

                override fun onError(e: Throwable) {

                }
            })

    }

    override fun onDestroy() {
        super.onDestroy()
        dspTimer?.dispose()

        draftFiles.forEach {
            val mFile = File(it)
            if (mFile.exists() && mFile.delete()) {
                Log.i("adjnjdnfjdn", it)
            }
        }
    }

    private fun initAds() {
        val sdkConfiguration: SdkConfiguration =
            SdkConfiguration.Builder(AdsConstants.MOPUB_AD_UNIT_ID).build()
        MoPub.initializeSdk(this, sdkConfiguration) {
            Log.i("MOPUB_ADS", "init done")
        }
        adsViewModel.initAds(this)
    }

    override fun onBackPressed() {
        if (Chartboost.onBackPressed())
            return
        else
            super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        if (navController.currentDestination?.id != R.id.recentContactsFragment) {
            messagingRepository.retry()
        }
        // TODO: Replace when livestream API for getting isLive status is available
        setTsuLiveOn(true)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        //Clear memory cache for Glide manually, in case it won't clear it
        //automatically
        when (level) {
            Application.TRIM_MEMORY_RUNNING_CRITICAL, Application.TRIM_MEMORY_COMPLETE -> {
                Glide.get(this).clearMemory()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        when (requestCode) {
            NEW_MESSAGE_PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    val navHostFragment = supportFragmentManager.primaryNavigationFragment
                    val fragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
                    if (fragment is ChatFragment) {
                        fragment.openPostTypesFragment()
                    }
                } else {
                    onPermissionsDenied(permissions, grantResults)
                }
            }
            NEW_POST_PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    openPostFragment()
                } else {
                    onPermissionsDenied(permissions, grantResults)
                }
            }
            NEW_POST_FROM_FEED_PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    val navHostFragment = supportFragmentManager.primaryNavigationFragment
                    val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
                    if (currentFragment is MainFeedFragment) {
                        currentFragment.openPostTypesFragment()
                    } else if (currentFragment is PostDraftFragment) {
                        currentFragment.openPostTypesFragment()
                    }
                } else {
                    onPermissionsDenied(permissions, grantResults)
                }

            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    /**
     * Starts empty post composer via action MainFeedFragmentDirections.showPostTypesFragment()
     */
    private fun openPostFragment() {
        val navController = findNavController(R.id.nav_host_fragment)
        navController.navigate(MainFeedFragmentDirections.showPostTypesFragment("").apply {
            popToDestination = navController.currentDestination?.id ?: R.id.mainFeedFragment
        })
    }

    /**
     * Starts post composer via action MainFeedFragmentDirections.showPostTypesFragment() with either
     * image or video depending on which one is available
     * mImageUrl or mVideoUrl are set in onCreate and are coming from ACTION_SEND intent
     */
    private fun openPostFragmentWithResource() {
        mImageUrl?.let {
            val navController = findNavController(R.id.nav_host_fragment)
            navController.navigate(
                MainFeedFragmentDirections.showPostDraftFragment(
                    null,
                    null,
                    Uri.parse(it)
                ).apply {
                    popToDestination = navController.currentDestination?.id ?: R.id.mainFeedFragment
                })
        }
        mVideoUrl?.let {
            //Start PostDraftFragment with url for wideo that user wanted to share
            val navController = findNavController(R.id.nav_host_fragment)
            navController.navigate(
                MainFeedFragmentDirections.showPostDraftFragment(null, it, null).apply {
                    popToDestination = navController.currentDestination?.id ?: R.id.mainFeedFragment
                })
        }
    }

    private fun onPermissionsDenied(permissions: Array<out String>, grantResults: IntArray) {
        val cameraPermissionIndex = permissions.indexOf(Manifest.permission.CAMERA)
        val messageRes = when {
            cameraPermissionIndex < 0 -> R.string.permission_not_granted
            grantResults[cameraPermissionIndex] != PackageManager.PERMISSION_GRANTED -> {
                R.string.permission_rational_camera
            }
            else -> R.string.permission_not_granted
        }
        snack(messageRes)
    }


    private fun initRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig

        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d("MAIN", "Config params updated: $updated")
                } else {
                    Log.e("MAIN", "Unable to update remote config")
                }
            }
    }

    fun handleSupportTap(subject: String) {

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse(
                "mailto:support@tsu.social?" +
                        "subject=${Uri.encode(subject)}&" +
                        "body="
            )
        }
        startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_us)))
    }


    override fun onSupportNavigateUp(): Boolean {
        appBarConfiguration.let {
            if (navController.navigateUp(it)) return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        try {
            analyticsHelper.onPreSaveState(outState)
            super.onSaveInstanceState(outState)
            analyticsHelper.onPostSaveState(outState)
        }catch (error: Exception){
            error.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//         Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.sample_main, menu)
        return true
    }

    override fun completedGetUserInfo(info: UserProfile?) {
        AuthenticationHelper.update(info)
        if (info != null) {
            bindUserInfo(info.fullName, info.username, info.profilePictureUrl, info.verifiedStatus)
            try {
                if (!info.birthday.isNullOrEmpty()) {

                    val date = formatter.parse(info.birthday) as Date
                    age.time = date
                    val today: LocalDate = LocalDate.now()
                    val birthday: LocalDate = LocalDate.of(
                        age.get(Calendar.YEAR),
                        age.get(Calendar.MONTH),
                        age.get(Calendar.DATE)
                    )
                    val p: Period = Period.between(birthday, today)
                    Log.d("infoiz", "it is p.years:" + p.years + birthday)
                    Log.d("infoiz", "it is info.id:" + info.id)

                    sharedPrefManager.setAge(p.years)

                    Log.d("agevalue", "it is:" + p.years)

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            sharedPrefManager.setUserId(info.id)
            info.createdAtInt?.let {
                sharedPrefManager.setCreatedAt(info.createdAtInt)
            }
        } else {
            updateProfilePhoto(null)
        }
    }

    override fun didErrorWith(message: String) {
    }

    internal fun bindUserInfo(
        fullName: String?,
        username: String?,
        profilePictureUrl: String?,
        verifyStatus: Int?
    ) {
        val content = SpannableString(fullName.plus("  "))
        verifyStatus?.let { user ->
            if (Constants.isVerified(user)) {
                val imageSpan =
                    ImageSpan(this, R.drawable.ic_verified_extra_small)
                content.setSpan(imageSpan, content.length - 1, content.length, 0)
            }
        }
        userProfileImageService.getProfilePicture(profilePictureUrl, false) {
            updateProfilePhoto(it)
        }
    }

    internal fun updateProfilePhoto(drawable: Drawable?) {
        if (drawable != null) {
            profileIcon?.setImageDrawable(drawable)
        } else {
            profileIcon?.setImageResource(R.drawable.user)
        }
    }

    fun loadCurrentUser() {
        AuthenticationHelper.currentUserId?.let { userId ->
            userService.getUserInfo(userId, false)
        }
    }

    private fun initButtons() {
        findViewById<View>(R.id.new_message).setOnClickListener {
            navController.navigate(
                R.id.tsuContactsFragment, TsuContactsFragmentArgs.Builder(
                    TsuContactsFragment.ContactsMode.CHAT
                ).build().toBundle()
            )
        }
        findViewById<View>(R.id.ic_notify).setOnClickListener {
            navController.navigate(R.id.notificationFragment)
        }
        livestreamIcon?.setOnClickListener {
            pbLive?.show()
            GlobalScope.launch {
                val isOnline = NetworkHelper.isOnline()
                withContext(Dispatchers.Main) {
                    pbLive?.hide()
                    if (isOnline)
                        navController.navigate(NavGraphDirections.showLiveFragment())
                    else
                        internetSnack()
                }
            }
        }
        profileIcon?.setOnClickListener {
            findNavController(R.id.nav_host_fragment).navigate(R.id.currentUserProfileFragment)
        }
        searchIcon?.setOnClickListener {
            navController.navigate(R.id.searchFragment)
        }
    }

    fun checkLoggedIn() {
        messagingRepository.refreshCount()
        AuthenticationHelper.currentUserId?.let {
            ConsentHelper.getConsent(this)
        }
    }

    //as I didn't find any usage of this broadcast I'm commenting out whole function, but leaving it here just in case
/*
    /** When key down event is triggered, relay it via local broadcast so fragments can handle it */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val intent = Intent(KEY_EVENT_ACTION).apply { putExtra(KEY_EVENT_EXTRA, keyCode) }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }*/


    companion object {
        const val NEW_POST_PERMISSIONS_REQUEST_CODE = 2002
        const val NEW_POST_FROM_FEED_PERMISSIONS_REQUEST_CODE = 2003
        const val NEW_MESSAGE_PERMISSIONS_REQUEST_CODE = 2004

        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }

        var instance: MainActivity? = null

        // Delete drafts files
        val draftFiles = arrayListOf<String>()

    }

    fun updateNotificationsCount() {
        notificationViewModel.refreshNotifications()
        notificationViewModel.unseenNotificationsCount.observe(this, Observer {
            val manager = NotificationManagerCompat.from(this)
            if (it > 0 && manager.areNotificationsEnabled()) {
                notifyBadge?.show()
                notifyBadge?.text = shortenCountForNotificationBabge(this@MainActivity, it)
            } else {
                notifyBadge?.hide()
            }
        })
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // In a situation when an app was destroyed because of system low memory callback
        // OS will try to restore app from top of task stack (in our case it is MainActivity)
        // these actions won't run StartupActivity as a result user will be unauthenticated
        // and the app configuration won't be fetched
        // force MainActivity to finish will trigger StartupActivity
        Log.d(TAG, "MainActivity finish() call")
        finish()
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {

        Log.d("NAVIGATION", "destination = ${destination.label}")

        analyticsHelper.setCurrentScreen(this, destination.label.toString())

        setupTopLevelConfigurations(destination.id, arguments)

        hideKeyboard()
    }

    private fun setTsuLiveOn(isLive: Boolean) {
        livestreamIcon?.setImageResource(
            if (isLive) R.drawable.ic_livestream_on else R.drawable.ic_livestream_off
        )
    }

    fun hideAppBar() {
        if (appBarLayout?.layoutParams?.height != 0) {
            appBarLayout?.updateLayoutParams<ViewGroup.LayoutParams> {
                height = 0
            }
        }
    }

    fun showAppBar() {
        if (appBarLayout?.layoutParams?.height == 0) {
            appBarLayout?.updateLayoutParams<ViewGroup.LayoutParams> {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
        restoreToolbar()
    }

    private fun setupCustomTitle(@IdRes destinationId: Int, arguments: Bundle?) {
        when (destinationId) {
            R.id.userFriendsFragment -> {
                arguments?.getString("toolbar_title")?.let { title ->
                    toolbarTitle?.show()
                    toolbarTitle?.text = title
                }
            }
            R.id.hashtagGridFragment -> {
                arguments?.getString("hashtag")?.let { title ->
                    toolbarTitle?.show()
                    toolbarTitle?.text = title
                }
            }
            R.id.communityMembersFragment -> {
                toolbarTitle?.show()
                toolbarTitle?.setText(R.string.members)
            }
            R.id.supportsListFragment -> {
                toolbarTitle?.show()
                toolbarTitle?.setText(R.string.toolbar_title_supporters)
            }
            R.id.notificationFragment -> {
                toolbarTitle?.show()
                toolbarTitle?.setText(R.string.notifications)
            }
            R.id.bankAccountFragment -> {
                toolbarTitle?.show()
                toolbarTitle?.setText(R.string.bank_account_title)
            }
            R.id.userSettingsFragment -> {
                toolbarTitle?.show()
                toolbarTitle?.setText(R.string.settings_title)
            }
            R.id.insightsFragment -> {
                toolbarTitle?.show()
                toolbarTitle?.setText(R.string.insights_title)
            }
            R.id.communityCreateFragment -> {
                toolbarTitle?.show()
                toolbarTitle?.setText(R.string.community_update_title)
            }
            R.id.recentContactsFragment -> {
                toolbarTitle?.show()
                toolbarTitle?.text = getString(R.string.messages_screen_title)
            }
            R.id.feedSettingsFragment -> {
                toolbarTitle?.show()
                toolbarTitle?.setText(R.string.feed_type_screen_title)
            }
            R.id.discoveryFeedFragment -> {
                toolbarTitle?.show()
                toolbarTitle?.setText(R.string.discovery_feed_title)
            }
            else -> {
                toolbarTitle?.hide()
            }
        }
    }

    fun setupTopLevelConfigurations(@IdRes destinationId: Int, arguments: Bundle? = null) {
        Handler().post {
            setupCustomTitle(destinationId, arguments)

            profileIcon?.borderWidth = if (destinationId == R.id.currentUserProfileFragment) {
                resources.getDimensionPixelSize(R.dimen.user_item_photo_border)
            } else 0

            when (destinationId) {
                R.id.oldUserEnterEmailFragment -> {
                    showAppBar()
                    feedToolbarButtons.hide()
                    newMessageButton.hide()
                    bottomNavigation.hide()
                    bottomNavigationDivider.hide()
                    navController.graph.startDestination = R.id.oldUserEnterEmailFragment
                }
                R.id.recentContactsFragment -> {
                    showAppBar()
                    feedToolbarButtons.hide()
                    newMessageButton.show()
                    bottomNavigation.show()
                    bottomNavigationDivider.show()
                    navController.graph.startDestination = R.id.recentContactsFragment
                }
                in bottomViewDestinations -> {
                    showAppBar()
                    feedToolbarButtons.show()
                    bottomNavigation.show()
                    bottomNavigationDivider.show()
                    newMessageButton.hide()
                    navController.graph.startDestination = R.id.mainFeedFragment
                }
                R.id.currentUserProfileFragment -> {
                    hideAppBar()
                    feedToolbarButtons.hide()
                    newMessageButton.hide()
                    bottomNavigation.show()
                    bottomNavigationDivider.show()
                    navController.graph.startDestination = R.id.currentUserProfileFragment
                }
                R.id.userProfileFragment -> {
                    hideAppBar()
                    feedToolbarButtons.hide()
                    newMessageButton.hide()
                    bottomNavigation.show()
                    bottomNavigationDivider.show()
                    navController.graph.startDestination = R.id.currentUserProfileFragment
                }
                R.id.postTypesFragment,
                R.id.livestreamFragment -> {
                    hideAppBar()
                    feedToolbarButtons.hide()
                    newMessageButton.hide()
                    bottomNavigation.hide()
                    bottomNavigationDivider.hide()
                }
                R.id.hashtagGridFragment,
                R.id.communityFeedFragment,
                R.id.userFriendsFragment,
                R.id.tsuContactsFragment,
                R.id.singlePostFragment,
                R.id.communityFragment -> {
                    showAppBar()
                    feedToolbarButtons.hide()
                    newMessageButton.hide()
                    bottomNavigation.show()
                    bottomNavigationDivider.show()
                }
                else -> {
                    showAppBar()
                    feedToolbarButtons.hide()
                    newMessageButton.hide()
                    bottomNavigation.hide()
                    bottomNavigationDivider.hide()
                }
            }
        }
    }

    fun setCustomToolbarWithDrawer(toolbar: Toolbar) {
        isCustomToolbar = true
        applyToolbar(toolbar)
        appBarChangedListener.remove()
        appBarChangedListener.showDrawerIcon = true
        appBarChangedListener.apply(navController)
    }

    fun setCustomToolbar(toolbar: Toolbar?) {
        isCustomToolbar = true
        applyToolbar(toolbar)
        appBarChangedListener.remove()
        appBarChangedListener.showDrawerIcon = false
        appBarChangedListener.apply(navController)
    }

    fun restoreToolbar() {
        if (isCustomToolbar) {
            isCustomToolbar = false
            applyToolbar(this.toolbar)
            appBarChangedListener.remove()
            appBarChangedListener.showDrawerIcon = false
            appBarChangedListener.apply(navController)
        }
    }

    fun applyToolbar(toolbar: Toolbar?) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.title = ""
    }

    fun isPreviousFragmentNavStartFragment(): Boolean {
        val backStackCount =
            supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.backStackEntryCount
                ?: 0
        return backStackCount <= 1
    }

    private fun notifyUnreadChatsUpdated(value: Int) {
        if (value == 0) {
            bottomNavigation.removeBadge(R.id.recentContactsFragment)
        } else {
            bottomNavigation.getOrCreateBadge(R.id.recentContactsFragment)?.let { badge ->
                badge.number = value
                badge.backgroundColor = getColor(R.color.tsu_primary)
                badge.badgeTextColor = Color.WHITE
            }
        }
    }

    override fun communityClick() {
        Log.d("BOTTOM_LISTENER", "click")

        val item: MenuItem = bottomNavigation.menu.findItem(R.id.communityFragment)
        item.isChecked = true
    }

    override fun messageClick() {

        val item: MenuItem = bottomNavigation.menu.findItem(R.id.recentContactsFragment)
        item.isChecked = true
    }

    override fun onLogOutSuccess() {
        logOut()
    }

    private fun logOut() {
        val navController = findNavController(R.id.nav_host_fragment)
        mainViewModel.logoutUser()
        snack(getString(R.string.logout_success_message))
        navController.graph.startDestination = R.id.oldUserOnBoardingFragment
        navController.navigate(R.id.showLoginFragment)
    }
}

/**
 * Checks if any of requested permissions are not granted and asks for them if needed
 * If all permissions are already granted - calls action callback
 * @param permissions Permissions that needs to be granted
 * @param requestCode Request code that will be used to start permissions request dialog
 * @param action callback that will be called immediately if all of the requested permissions are
 * already granted
 */
private fun Activity.checkPermissions(
    permissions: Array<String>,
    requestCode: Int,
    action: () -> Unit
) {
    val listPermissionsNeeded = arrayListOf<String>()

    for (index in permissions.indices) {
        val result = ContextCompat.checkSelfPermission(applicationContext, permissions[index])
        if (result != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(permissions[index])
        }
    }

    if (listPermissionsNeeded.isNotEmpty()) {
        ActivityCompat.requestPermissions(
            this,
            listPermissionsNeeded.toTypedArray(),
            requestCode
        )
    } else {
        action()
    }

}

/**
 * Extension that allows handling permission result inside of calling fragment instead of activity
 */
fun Fragment.checkPermissionsInFragment(
    permissions: Array<String>,
    requestCode: Int,
    action: () -> Unit
) {
    val listPermissionsNeeded = arrayListOf<String>()

    for (index in permissions.indices) {
        val result = ContextCompat.checkSelfPermission(requireContext(), permissions[index])
        if (result != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(permissions[index])
        }
    }

    if (listPermissionsNeeded.isNotEmpty()) {
        requestPermissions(
            listPermissionsNeeded.toTypedArray(),
            requestCode
        )
    } else {
        action()
    }

}

/**
 * Convenience method. Calls Activity.checkPermissions()
 *
 * @see Activity.checkPermissions
 */
fun Fragment.checkPermissions(
    permissions: Array<String>,
    requestCode: Int,
    action: () -> Unit
) {
    requireActivity().checkPermissions(permissions, requestCode, action)
}

fun Activity.snack(message: String) {
    val rootView: View? = this.window.decorView.findViewById(R.id.bottom_navbar)
    rootView?.getSnackBar(message)?.show()

}

fun Activity.isInternetAvailable(): Boolean {
    return NetworkHelper.isNetworkAvailable(this)
}

fun Activity.internetSnack() {
    NetworkHelper.openInternetDialog(this)
}

fun Activity.showKeyboard() {
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
}

fun Activity.snack(@StringRes messageRes: Int) {
    val rootView: View? = this.window.decorView.findViewById(R.id.bottom_navbar)
    rootView?.getSnackBar(getString(messageRes))?.show()

}

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Activity.setWindowResizeable() {
    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
}

fun Activity.setWindowDefaultState() {
    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
}

interface LogoutListener {
    fun onLogOutSuccess()
}