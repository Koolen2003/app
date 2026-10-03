package com.example.helpdesk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

fun uriToCompressedBase64(context: Context, uri: Uri, maxWidth: Int = 800, quality: Int = 55): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val scale = maxWidth.toFloat() / original.width
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, maxWidth, (original.height * scale).toInt(), true)
        } else original

        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()

        Base64.encodeToString(bytes, Base64.DEFAULT)
    } catch (e: Exception) {
        null
    }
}

fun base64ToBitmap(base64: String): Bitmap? {
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}