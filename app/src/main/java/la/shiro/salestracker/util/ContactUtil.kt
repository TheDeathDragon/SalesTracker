package la.shiro.salestracker.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import la.shiro.salestracker.SalesTrackerApplication
import la.shiro.salestracker.config.TAG

object ContactUtil {
    private fun insertRawContact(contentResolver: ContentResolver): Long? {
        val contentValues = ContentValues()
        val rawContactUri: Uri? =
            contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, contentValues)
        return rawContactUri?.let { ContentUris.parseId(it) }
    }

    fun addContact(name: String, phoneNumber: String) {

        val contentResolver: ContentResolver =
            SalesTrackerApplication.getAppContext().contentResolver

        if (contactExists(name, phoneNumber, contentResolver)) {
            Log.d(TAG, "ContactUtil --> Contact $name $phoneNumber exists, skip adding")
            return
        }
        val rawContactId = insertRawContact(contentResolver) ?: return
        val contentValues = ContentValues()
        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
        contentValues.put(
            ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
        )
        contentValues.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
        contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)
        contentValues.clear()
        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
        contentValues.put(
            ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
        )
        contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
        contentValues.put(
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
        )
        contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)
        Log.d(TAG, "ContactUtil --> Contact $name $phoneNumber added")
    }

    private fun contactExists(
        name: String, phoneNumber: String, contentResolver: ContentResolver
    ): Boolean {
        val projection: Array<String> = arrayOf(ContactsContract.Contacts._ID)
        val selection =
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ? AND ${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
        val selectionArgs = arrayOf(name, phoneNumber)
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        val contactExists = (cursor != null) && cursor.moveToFirst()
        cursor?.close()
        Log.d(
            TAG,
            "ContactUtil --> Contact exists: $contactExists , name: $name, phoneNumber: $phoneNumber"
        )
        return contactExists
    }
}