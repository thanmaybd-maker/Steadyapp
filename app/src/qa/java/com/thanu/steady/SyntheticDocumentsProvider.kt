package com.thanu.steady

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/** Disposable QA provider: never packaged in the owner APK. */
class SyntheticDocumentsProvider : ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri) = "application/octet-stream"
    override fun query(uri: Uri,projection: Array<out String>?,selection: String?,args: Array<out String>?,sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri,values: ContentValues?): Uri? = null
    override fun update(uri: Uri,values: ContentValues?,selection: String?,args: Array<out String>?) = 0
    override fun delete(uri: Uri,selection: String?,args: Array<out String>?) = 0
    override fun openFile(uri: Uri,mode: String): ParcelFileDescriptor {
        val name = uri.lastPathSegment
        if(name !in setOf("roundtrip","oversized")) throw FileNotFoundException("Synthetic provider failure")
        val file = File(requireNotNull(context).cacheDir,"qa-document-$name.bin")
        return ParcelFileDescriptor.open(file,ParcelFileDescriptor.parseMode(mode))
    }
}
