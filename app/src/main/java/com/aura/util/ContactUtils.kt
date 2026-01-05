package com.aura.util

import android.content.Context
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isStarredContact(phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrEmpty()) return false
        
        // This is a simplified check. Real implementation would Normalize phone numbers.
        // Also requires READ_CONTACTS permission.
        
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.STARRED)
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val selectionArgs = arrayOf("%$phoneNumber%")
        
        return checkStarred(uri, projection, selection, selectionArgs)
    }

    fun isSignificantContact(name: String): Boolean {
        if (name.isEmpty()) return false
        
        // Check exact display name match for starred contacts
        val uri = ContactsContract.Contacts.CONTENT_URI
        val projection = arrayOf(ContactsContract.Contacts.STARRED)
        val selection = "${ContactsContract.Contacts.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(name)
        
        return checkStarred(uri, projection, selection, selectionArgs)
    }

    private fun checkStarred(uri: android.net.Uri, projection: Array<String>, selection: String, selectionArgs: Array<String>): Boolean {
        try {
            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val starred = it.getInt(0)
                    return starred == 1
                }
            }
        } catch (e: Exception) {
            // Permission might not be granted
            return false
        }
        return false
    }
}
