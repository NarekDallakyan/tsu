package social.tsu.android.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import social.tsu.android.LegacyUserPostsAdapter
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.UserPostsAdapterActionCallback
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.showUserProfile
import social.tsu.android.service.SharedPrefManager
import social.tsu.android.utils.openUrl
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


abstract class LegacyBaseFeedFragment<T : BaseFeedViewModel> : Fragment(),
    UserPostsAdapterActionCallback {

    private var lastBottomDialog: BottomSheetDialog? = null

    private val TAG = this::class.java.simpleName

    protected abstract val postsAdapter: LegacyUserPostsAdapter
    protected abstract val viewModel: T

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    override fun didTapLikeOn(post: Post) {

        if (post.has_liked == true) {
            viewModel.unlike(post)
        } else {
            viewModel.like(post)
        }
    }

    override fun didTapShowLikes(post: Post) {
        findNavController().navigate(
            R.id.likesListFragment,
            bundleOf("postId" to post.originalPostId)
        )
    }

    override fun didTapMoreOptions(post: Post) {
        showExtraOptionsOn(post)
    }

    override fun didTapCommentOn(post: Post) {
        findNavController().navigate(R.id.commentsFragment, bundleOf("postId" to post.id))
    }

    override fun didTapOnUser(userId: Long) {
        findNavController().showUserProfile(userId.toInt())
    }

    override fun didTapOnGroup(groupId: Int) {
        findNavController().navigate(
            R.id.communityFeedFragment, bundleOf(
                "groupId" to groupId
            )
        )
    }

    override fun didTapLink(link: String) {
        context?.openUrl(link)
    }

    protected fun showExtraOptionsOn(post: Post, blocked: Boolean = false) {
        if (lastBottomDialog?.isShowing == true) return

        if (post.user_id == AuthenticationHelper.currentUserId) {
            if (post.is_share && post.shared_id != null) {
                showSharedExtraDialog(post)
            } else {
                showUserPostExtraDialog(post)
            }
        } else if (post.has_shared == true && post.shared_id != null) {
            showSharedExtraDialog(post)
        } else {
            // No need to implement this now, it will be covered in ts-673 and ts-557
            showPostExtraDialog(post, blocked)
        }
    }

    private fun showPostExtraDialog(@LayoutRes layoutRes: Int): BottomSheetDialog {
        val sheetView = activity?.layoutInflater?.inflate(layoutRes, null)
        val actionSheet = BottomSheetDialog(context as Context)
        actionSheet.setContentView(sheetView as View)
        actionSheet.show()
        actionSheet.setOnDismissListener {
            lastBottomDialog = null
        }

        sheetView.findViewById<View>(R.id.dialog_cancel_button)?.setOnClickListener {
            actionSheet.dismiss()
        }
        lastBottomDialog = actionSheet
        return actionSheet
    }

    private fun showPostExtraDialog(post: Post, blocked: Boolean = false) {
        val actionSheet = showPostExtraDialog(R.layout.dialog_bottom_other_post_more_actions)

        actionSheet.findViewById<View>(R.id.dialog_report_button)?.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Reason")
            builder.setItems(resources.getStringArray(R.array.report_reasons_array)) { _, which ->
                viewModel.report(post.id, which + 1)
            }

            builder.create().show()

            actionSheet.dismiss()
        }
        actionSheet.findViewById<TextView>(R.id.dialog_block_text)?.text =
            if (blocked) getString(R.string.unblock_user) else getString(R.string.block_user)
        actionSheet.findViewById<View>(R.id.dialog_block_button)?.setOnClickListener {
            if (blocked) {
                viewModel.unblock(post.user_id)
            } else {
                viewModel.block(post.user_id)
            }
            actionSheet.dismiss()
        }
    }

    private fun showUserPostExtraDialog(post: Post) {
        val actionSheet = showPostExtraDialog(R.layout.dialog_bottom_post_more_actions)

        actionSheet.findViewById<View>(R.id.dialog_edit_button)?.setOnClickListener {
            Navigation.findNavController(view as View).navigate(
                R.id.editPostFragment,
                bundleOf("postId" to post.id)
            )
            actionSheet.dismiss()
        }

        actionSheet.findViewById<View>(R.id.dialog_delete_button)?.setOnClickListener {
            showConfirmDeleteAlert(post)
            actionSheet.dismiss()
        }
    }

    private fun showSharedExtraDialog(post: Post) {
        val actionSheet = showPostExtraDialog(R.layout.dialog_bottom_shared_post_more_actions)

        actionSheet.findViewById<View>(R.id.dialog_unshare_button)?.setOnClickListener {
            viewModel.unshare(post)
            actionSheet.dismiss()
        }
    }

    private fun showConfirmDeleteAlert(post: Post) {
        val builder = AlertDialog.Builder(context as Context)
        builder.setTitle(getString(R.string.delete))
        builder.setMessage(getString(R.string.areYouSureDeletePost))

        builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
            viewModel.delete(post.id)
            if (sharedPrefManager.getExclusivePostTime().isNullOrEmpty()
                    .not() && post.privacy == Post.PRIVACY_EXCLUSIVE
            ) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd")
                sharedPrefManager.getExclusivePostTime()?.let { tomorrowAsString ->
                    val todayAsString = dateFormat.format(Calendar.getInstance().time)
                    if (tomorrowAsString.equals(todayAsString, true)) {
                        sharedPrefManager.setExclusivePostTime("")
                    }
                }
            }
            dialog.dismiss()
        }

        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

}