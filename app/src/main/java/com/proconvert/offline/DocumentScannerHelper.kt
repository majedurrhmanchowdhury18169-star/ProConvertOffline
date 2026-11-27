package com.proconvert.offline

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class DocumentItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val size: Long,
    val mimeType: String
)

object DocumentScannerHelper {
    fun fetchAllDocuments(context: Context): List<DocumentItem> {
        val documentList = mutableListOf<DocumentItem>()
        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?"
        val selectionArgs = arrayOf("application/pdf", "image/%")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "Unknown"
                val size = cursor.getLong(sizeCol)
                val mimeType = cursor.getString(mimeCol) ?: ""
                val contentUri = ContentUris.withAppendedId(collection, id)

                if (size > 0) {
                    documentList.add(DocumentItem(id, name, contentUri, size, mimeType))
                }
            }
        }
        return documentList
    }
}
