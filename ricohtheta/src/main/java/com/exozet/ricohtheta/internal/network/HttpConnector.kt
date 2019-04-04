package com.exozet.ricohtheta.internal.network

import android.graphics.Bitmap
import org.json.JSONException

import java.io.IOException
import java.io.InputStream

interface HttpConnector {

    @Throws(IOException::class, JSONException::class)
    fun getLivePreview(): InputStream

    val deviceInfo: DeviceInfo

    enum class ShootResult {
        SUCCESS, FAIL_CAMERA_DISCONNECTED, FAIL_STORE_FULL, FAIL_DEVICE_BUSY
    }

    class CameraNotFoundException : Exception()

    fun takePicture(listener: HttpEventListener): ShootResult

    fun getImage(fileId: String, listener: HttpDownloadListener): ImageData

    fun getThumb(fileId: String): Bitmap

    fun connect(): String

    fun disconnect(): Boolean?

    fun deleteFile(deletedFileId: String, listener: HttpEventListener)
}