package social.tsu.android.helper

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import social.tsu.android.BuildConfig
import social.tsu.android.R
import java.io.File
import java.util.*

class PickerUtils {

    companion object {
        const val PICK_IMAGE_CHOOSER_REQUEST_CODE = 266

        fun startPickImageChooser(context: Context, fragment: Fragment) {
            fragment.startActivityForResult(
                getPickImageChooserIntent(context),
                PICK_IMAGE_CHOOSER_REQUEST_CODE
            )
        }

        fun getPickImageChooserIntent(context: Context): Intent? {
            return getPickImageChooserIntent(
                context, context.getString(R.string.photo), false, true
            )
        }

        fun isExplicitCameraPermissionRequired(context: Context): Boolean {
            return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasPermissionInManifest(
                context,
                "android.permission.CAMERA"
            )
                    && (context.checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED))
        }

        fun hasPermissionInManifest(
            context: Context, permissionName: String
        ): Boolean {
            val packageName = context.packageName
            try {
                val packageInfo = context.packageManager
                    .getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                val declaredPermisisons = packageInfo.requestedPermissions
                if (declaredPermisisons != null && declaredPermisisons.size > 0) {
                    for (p in declaredPermisisons) {
                        if (p.equals(permissionName, ignoreCase = true)) {
                            return true
                        }
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
            }
            return false
        }


        fun getPickImageChooserIntent(
            context: Context,
            title: CharSequence?,
            includeDocuments: Boolean,
            includeCamera: Boolean
        ): Intent? {
            val allIntents = ArrayList<Intent>()
            val packageManager = context.packageManager

            // collect all camera intents if Camera permission is available
            if (!isExplicitCameraPermissionRequired(context) && includeCamera) {
                allIntents.addAll(getCameraIntents(context, packageManager))
            }
            var galleryIntents: List<Intent> =
                getGalleryIntents(
                    packageManager,
                    Intent.ACTION_GET_CONTENT,
                    includeDocuments
                )
            if (galleryIntents.size == 0) {
                // if no intents found for get-content try pick intent action (Huawei P9).
                galleryIntents = getGalleryIntents(
                    packageManager,
                    Intent.ACTION_PICK,
                    includeDocuments
                )
            }
            allIntents.addAll(galleryIntents)
            val target: Intent
            /*    if (allIntents.isEmpty()) {
          target = new Intent();
        } else {
          target = allIntents.get(allIntents.size() - 1);
          allIntents.remove(allIntents.size() - 1);
        }*/target = Intent(Intent.ACTION_PICK)
            target.type = "image/*"

            // Create a chooser from the main  intent
            val chooserIntent = Intent.createChooser(target, title)

            // Add all other intents
            chooserIntent.putExtra(
                Intent.EXTRA_INITIAL_INTENTS, allIntents.toTypedArray<Parcelable>()
            )
            return chooserIntent
        }


        fun getGalleryIntents(
            packageManager: PackageManager, action: String, includeDocuments: Boolean
        ): List<Intent> {
            val intents: MutableList<Intent> = ArrayList()
            val galleryIntent =
                if (action === Intent.ACTION_GET_CONTENT) Intent(action) else Intent(
                    action,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
            galleryIntent.type = "image/*"
            val listGallery =
                packageManager.queryIntentActivities(galleryIntent, 0)
            for (res in listGallery) {
                val intent = Intent(galleryIntent)
                intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
                intent.setPackage(res.activityInfo.packageName)
                intents.add(intent)
            }

            // remove documents intent
            if (!includeDocuments) {
                for (intent in intents) {
                    if (intent.component?.className
                        == "com.android.documentsui.DocumentsActivity"
                    ) {
                        intents.remove(intent)
                        break
                    }
                }
            }
            return intents
        }

        fun getCameraIntents(
            context: Context, packageManager: PackageManager
        ): List<Intent> {
            val allIntents: MutableList<Intent> = ArrayList()

            // Determine Uri of camera image to  save.
            val outputFileUri: Uri? =
                getCaptureImageOutputUri(context)
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val listCam =
                packageManager.queryIntentActivities(captureIntent, 0)
            for (res in listCam) {
                val intent = Intent(captureIntent)
                intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
                intent.setPackage(res.activityInfo.packageName)
                if (outputFileUri != null) {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
                }
                allIntents.add(intent)
            }
            return allIntents
        }

        fun getCaptureImageOutputUri(context: Context): Uri? {
            var outputFileUri: Uri? = null
            val getImage = context.externalCacheDir
            if (getImage != null) {
                outputFileUri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    File(getImage.path, "pickImageResult.jpeg")
                )
            }
            return outputFileUri
        }

        fun getPickImageResultUri(
            context: Context,
            data: Intent?
        ): Uri? {
            var isCamera = true
            if (data != null && data.data != null) {
                val action = data.action
                isCamera = action != null && action == MediaStore.ACTION_IMAGE_CAPTURE
            }
            return if (isCamera || data!!.data == null) getCaptureImageOutputUri(
                context
            ) else data.data
        }
    }



}