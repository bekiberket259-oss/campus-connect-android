package com.campusconnect.marketplace.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun compressImage(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val maxDimension = 1080
            val width = originalBitmap.width
            val height = originalBitmap.height
            val newWidth: Int
            val newHeight: Int

            if (width > height) {
                newWidth = maxDimension
                newHeight = (height * (maxDimension.toFloat() / width)).toInt()
            } else {
                newHeight = maxDimension
                newWidth = (width * (maxDimension.toFloat() / height)).toInt()
            }

            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)

            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}