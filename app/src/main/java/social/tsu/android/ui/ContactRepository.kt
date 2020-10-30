package social.tsu.android.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import androidx.core.database.getStringOrNull
import javax.inject.Inject

class ContactRepository @Inject constructor(val application: Application) {
    companion object {
        const val TAG = "ContactRepository"

        fun fetchContactPhoto(contentResolver: ContentResolver, contactId: Long): ByteArray? {
            val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            val photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)

            val cursor = contentResolver.query(
                photoUri,
                arrayOf(ContactsContract.Contacts.Photo.PHOTO),
                null, null, null
            ) ?: return null

            if (cursor.moveToFirst()) {
                val photoBytes = cursor.getBlob(0)
                cursor.close()
                return photoBytes
            } else {

                cursor.close()
                return null
            }
        }
    }

    private val contentResolver = application.contentResolver

    @SuppressLint("InlinedApi")
    private val PROJECTION: Array<out String> = arrayOf(
        ContactsContract.Data._ID,
        // The primary display name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            ContactsContract.Data.DISPLAY_NAME_PRIMARY
        else
            ContactsContract.Data.DISPLAY_NAME,

        // The contact's _ID, to construct a content URI
        ContactsContract.Data.CONTACT_ID,

        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Email.ADDRESS,
        ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
//         The contact's LOOKUP_KEY, to construct a content URI
//        ContactsContract.Data.LOOKUP_KEY
    )

    sealed class ContactMethod {
        data class Email(val id: Int, val emailAddress: String) : ContactMethod()
        data class Phone(val id: Int, val number: String) : ContactMethod()
        object None : ContactMethod()
    }

    data class ContactItem(
        val contactId: Int,
        val displayName: String,
        val contactMethods: List<ContactMethod>
    )

    private val SELECTION: String =
        "${ContactsContract.Data.MIMETYPE} = '${ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE}' OR ${ContactsContract.Data.HAS_PHONE_NUMBER}"

//    private val SELECTION: String = "1=1"

    fun queryContacts(includeNone: Boolean = false): List<ContactItem> {

        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,  // The content URI of the words table
            PROJECTION,                       // The columns to return for each row
            SELECTION,                  // Either null, or the word the user entered
            null,                    // Either empty, or the string the user entered
            null                         // The sort order for the returned rows
        )

        val contacts = mutableListOf<ContactItem>()
        when (cursor?.count) {
            null -> {
                Log.d(TAG, "Nothing")
            }
            0 -> {
                Log.d(TAG, "Zero")
            }
            else -> {
                Log.d(TAG, "got data ${cursor.count}")

                cursor.apply {
                    while (moveToNext()) {
                        val id = getInt(0)
                        val displayName = getString(1)
                        val contactId = getInt(2)
                        val phoneNumber = getStringOrNull(3)
                        val emailAddress = getStringOrNull(4)
                        val normalizedNumber = getStringOrNull(5)

//                        Log.d(TAG, "$displayName:  Phone = $phoneNumber")
                        if ((emailAddress != null) && android.util.Patterns.EMAIL_ADDRESS.matcher(
                                emailAddress
                            ).matches()
                        ) {
                            val method = ContactMethod.Email(id, emailAddress)
                            val contact = ContactItem(contactId, displayName, listOf(method))

                            Log.d(TAG, "The contact = $contact")
                            contacts.add(contact)
                        }
//                        if ((normalizedNumber != null) && normalizedNumber.matches(Regex("\\+\\d+"))){
                        if ((normalizedNumber != null) && android.util.Patterns.PHONE.matcher(
                                normalizedNumber
                            ).matches()
                        ) {

                            val method = ContactMethod.Phone(id, normalizedNumber)
                            val contact = ContactItem(contactId, displayName, listOf(method))

                            contacts.add(contact)
                            Log.d(TAG, "The contact = $contact")
                        }

                        if ((emailAddress == null) && (normalizedNumber == null)) {
                            val contact =
                                ContactItem(contactId, displayName, listOf(ContactMethod.None))

                            Log.d(TAG, "The contact = $contact")
                            if (includeNone) {
                                contacts.add(contact)

                            }
                        }
                    }
                }

                cursor.close()
            }
        }

        return contacts
    }
}
