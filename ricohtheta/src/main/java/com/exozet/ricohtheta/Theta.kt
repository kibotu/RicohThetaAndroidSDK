package com.exozet.ricohtheta

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import com.exozet.ITheta
import com.exozet.ricohtheta.cameras.ICamera
import com.exozet.ricohtheta.internal.network.HttpConnector
import com.exozet.ricohtheta.internal.network.HttpDownloadListener
import com.exozet.ricohtheta.internal.network.HttpEventListener
import com.exozet.ricohtheta.internal.network.ImageData
import com.exozet.ricohtheta.internal.view.MJpegInputStream
import com.exozet.threehundredsixtyplayer.ThreeHundredSixtyPlayer
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit


class Theta : ITheta {

    private val cameras = ArrayList<ICamera>()

    // region ITheta

    override val isConnected
        get() = cameras.any { it.isConnected }

    override val connectedCameras
        get() = cameras.filter { it.isConnected }

    override fun addCamera(camera: ICamera): Theta {
        cameras.add(camera)
        return this
    }

    override fun findCameras() = Observable.create<ICamera> { emitter ->

        val cameras = cameras.mapNotNull { camera ->
            
            if (camera.deviceInfoName != camera.httpConnector!!.deviceInfo.model) {
                return@mapNotNull
            }

            camera.httpConnector?.deviceInfo?.also {
                camera.isConnected = true
                emitter.onNext(camera)
                emitter.onComplete()
                return@create
            }
        }

        if (cameras.isEmpty())
            emitter.onError(HttpConnector.CameraNotFoundException())
    }

    override fun disconnect() = Observable.create<ICamera> { emitter ->

        val cameras = cameras.mapNotNull { camera ->

            camera.httpConnector?.disconnect()?.also {
                emitter.onNext(camera)
            }
        }

        if (cameras.isEmpty())
            emitter.onError(HttpConnector.CameraNotFoundException())
    }

    override fun getThumbnail(fileId: String) = Single.create<Bitmap> { emitter ->

        if (connectedCameras.isEmpty()) {
            emitter.onError(HttpConnector.CameraNotFoundException())
            return@create
        }

        try {
            val bitmap = connectedCameras.first().httpConnector?.getThumb(fileId)
            emitter.onSuccess(bitmap!!)
        } catch (e: Exception) {
            emitter.onError(e)
        }
    }

    override fun takePicture() = Observable.create<String> { emitter ->

        if (connectedCameras.isEmpty()) {
            emitter.onError(HttpConnector.CameraNotFoundException())
            return@create
        }

        connectedCameras.first().httpConnector?.takePicture(object : HttpEventListener {

            override fun onCheckStatus(newStatus: Boolean) = Unit

            override fun onObjectChanged(latestCapturedFileId: String?) = emitter.onNext(latestCapturedFileId ?: "")

            override fun onCompleted() = emitter.onComplete()

            override fun onError(errorMessage: String?) = emitter.onError(Throwable(errorMessage))
        })
    }

    override fun startLivePreview(view: ThreeHundredSixtyPlayer) = startLivePreview()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .unsubscribeOn(Schedulers.io())
        .map {
            view.bitmap = it
        }

    private var livePreview: MJpegInputStream? = null

    private fun cleanupLivePreview() {
        try {
            livePreview?.close()
            livePreview = null

        } catch (e: Exception) {
            e.log()
        }
    }

    override fun startLivePreview(delay: Long, timeUnit: TimeUnit) = Observable.create<Bitmap> { emitter ->

        if (emitter.isDisposed) {
            return@create
        }

        if (connectedCameras.isEmpty()) {
            emitter.onError(HttpConnector.CameraNotFoundException())
            return@create
        }

        try {
            val startTime = System.currentTimeMillis()
            val httpConnector = connectedCameras.first().httpConnector
            if (livePreview == null) {
                livePreview = httpConnector?.getLivePreview()?.asJPEGStream()
            }
            livePreview?.let {


                if (!emitter.isDisposed) {
                    try {
                        val image = it.readMJpegFrame()
                        emitter.onNext(image)
                    } catch (e: Exception) {
                        log("exception readMjepgFrame ${e.message}")
                    }
                    log("reading bitmap ${System.currentTimeMillis() - startTime}")
                }
            }

            if (!emitter.isDisposed)
                emitter.onComplete()

        } catch (e: Exception) {
            log("startLivePreview $e")
            if (!emitter.isDisposed)
                emitter.onError(e)
        }
    }.doOnDispose {
        cleanupLivePreview()
    }.doOnError {
        it.log()
    }.repeat()

    fun InputStream.asJPEGStream(): MJpegInputStream? = try {
        MJpegInputStream(this)
    } catch (e: Exception) {
        loge(e.toString())
        null
    }

    override fun transfer(fileId: String) = Single.create<ImageData> { emitter ->

        if (connectedCameras.isEmpty()) {
            emitter.onError(HttpConnector.CameraNotFoundException())
            return@create
        }

        try {
            val imageData = connectedCameras.first().httpConnector?.getImage(fileId, object : HttpDownloadListener {

                var totalFileSize = 0L

                var fileSize = 0L

                override fun onTotalSize(totalSize: Long) {
                    totalFileSize = totalSize
                }

                override fun onDataReceived(size: Int) {
                    fileSize += size
                }
            })

            emitter.onSuccess(imageData!!)

        } catch (e: Exception) {
            emitter.onError(e)
        }
    }

    override fun deleteOnCamera(filename: String) = Observable.create<Boolean> { emitter ->

        if (connectedCameras.isEmpty()) {
            emitter.onError(HttpConnector.CameraNotFoundException())
            return@create
        }

        try {
            connectedCameras.first().httpConnector?.deleteFile(filename, object : HttpEventListener {
                override fun onCheckStatus(newStatus: Boolean) = Unit

                override fun onObjectChanged(latestCapturedFileId: String?) = emitter.onNext(latestCapturedFileId != null)

                override fun onCompleted() = emitter.onComplete()

                override fun onError(errorMessage: String?) = emitter.onError(Throwable(errorMessage))
            })

        } catch (e: Exception) {
            emitter.onError(e)
        }
    }

    // endregion

    fun saveExternal(context: Context, imageData: ImageData, folderName: String, fileName: String): File {
        val path = Environment.getExternalStorageDirectory().toString()
        var fOutputStream: OutputStream? = null
        val file = File("$path/$folderName/", fileName)

        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }

        try {
            fOutputStream = FileOutputStream(file)

            val bmp = BitmapFactory.decodeByteArray(imageData.rawData, 0, imageData.rawData.count())
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fOutputStream)

            fOutputStream.flush()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            fOutputStream!!.close()
        }

        return file
    }

    fun deleteExternal(context: Context, folderName: String, fileName: String): File {
        val path = Environment.getExternalStorageDirectory().toString()
        val file = File("$path/$folderName/", fileName)

        if (file.exists()) {
            file.delete()
        }

        return file
    }
}