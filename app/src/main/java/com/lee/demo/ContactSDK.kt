package com.lee.demo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.provider.ContactsContract
import android.util.Log
import com.lee.withpermission.PermissionManager
import com.lee.withpermission.WithPermissionTask

/**
 * Pretending that some module which need contacts info
 */
class ContactSDK {
    companion object {
        val TAG = "ContactSDK"

        fun readContacts(activity: Activity) {
            // request contacts permission
            PermissionManager.withPermission(
                activity,
                object : WithPermissionTask(arrayOf(Manifest.permission.READ_CONTACTS)) {
                    @SuppressLint("NewApi")
                    override fun onGrant() {
                        val info = arrayOf(
                            ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.DISPLAY_NAME
                        )
                        val cursor = activity.contentResolver.query(
                            ContactsContract.Contacts.CONTENT_URI,
                            info,
                            null,
                            null
                        )
                        while (cursor?.moveToNext()!!) {
                            val id =
                                cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                            val name =
                                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                            val phonesCursor = activity.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + id,
                                null,
                                null
                            )
                            phonesCursor?.moveToFirst()
                            val phoneNumber = phonesCursor?.getString(0)
                            phonesCursor?.close()

                            Log.d(
                                TAG,
                                "contact name: $name, phone number: $phoneNumber"
                            )
                        }
                        cursor.close()
                    }
                })
        }

    }
}