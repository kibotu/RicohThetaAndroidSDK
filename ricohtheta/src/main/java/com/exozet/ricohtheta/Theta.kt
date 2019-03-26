package com.exozet.ricohtheta

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import com.exozet.ricohtheta.cameras.ICamera
import com.exozet.ricohtheta.internal.network.HttpConnector
import com.exozet.ricohtheta.internal.network.HttpDownloadListener
import com.exozet.ricohtheta.internal.network.HttpEventListener
import com.exozet.ricohtheta.internal.network.ImageData
import com.exozet.ricohtheta.internal.view.MJpegInputStream
import com.exozet.ricohtheta.internal.view.MJpegView
import com.exozet.threehundredsixtyplayer.ThreeHundredSixtyPlayer
import io.reactivex.Observable.fromCallable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit


class Theta {

    private val cameras = ArrayList<ICamera>()

    private var camera: ICamera? = null

    private var ipAddress: String = ""

    private var repaintObserver: Disposable? = null

    fun addCamera(camera: ICamera): Theta {
        cameras.add(camera)
        return this
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun createWirelessConnection(activity: Activity) {
        val cm = activity.application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
        request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

        cm.registerNetworkCallback(request.build(), object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network?) {
                super.onAvailable(network)
                when {
                    Build.VERSION.SDK_INT >= 23 -> {
                        cm.bindProcessToNetwork(network)
                    }
                    else -> {
                        ConnectivityManager.setProcessDefaultNetwork(network)
                    }
                }
            }
        })
    }

    fun findConnectedCamera(ipAddress: String): Single<ICamera> {
        this.ipAddress = ipAddress
        this.camera = null

        return Single.create { emitter ->
            cameras.forEach {
                try {
                    val model = it.connection(ipAddress).deviceInfo.model

                    if (model.isNotEmpty() && model.compareTo(it.deviceInfoName) == 0) {
                        camera = it
                    }
                } catch (t: Throwable) {
                    emitter.onError(t)
                }
            }

            if (camera != null) {
                emitter.onSuccess(camera!!)
            } else {
                emitter.onError(HttpConnector.CameraNotFoundException())
            }
        }
    }

    fun disconnect(connector: HttpConnector) {
        camera?.disconnect()
    }

    val isConnected: Boolean
        get() = camera?.isConnected == true

    fun takePicture(): Single<String> {
        camera?.let {
            stopLiveView()
            val camera = it.connection(ipAddress)

            var captureId = ""

            return Single.create { emitter ->
                camera.takePicture(object : HttpEventListener {
                    override fun onCheckStatus(newStatus: Boolean) {}

                    override fun onObjectChanged(latestCapturedFileId: String?) {

                        latestCapturedFileId?.let { id ->
                            captureId = id
                        }
                    }

                    override fun onCompleted() {
                        emitter.onSuccess(captureId)
                    }

                    override fun onError(errorMessage: String?) {
                        emitter.onError(Throwable(errorMessage))
                    }
                })
            }
        }

        return Single.error(HttpConnector.CameraNotFoundException())
    }


    fun startLiveView(view: MJpegView) = fromCallable {
        camera?.let {
            Log.i(TAG, "startLiveView")
            getJPEGStream(it)
        }
    }.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError { throwable -> Log.e(TAG, "Throwable ${throwable.message}") }
        .retryWhen { o -> o.delay(500, TimeUnit.MILLISECONDS) }
        .subscribe {
            Log.i(TAG, "startLiveView subscribed")
            view.setSource(it)
        }

    fun startLiveView(view: ThreeHundredSixtyPlayer) = fromCallable {
        camera?.let {
            Log.i(TAG, "startLiveView")
            getJPEGStream(it)
        }
    }.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError { throwable -> Log.e(TAG, "Throwable ${throwable.message}") }
        .retryWhen { it.delay(500, TimeUnit.MILLISECONDS) }
        .subscribe {
            Log.i(TAG, "startLiveView subscribed")
            observeBitmapUpdate(view, it)
        }

    private fun getJPEGStream(camera: ICamera): MJpegInputStream? = try {
        val httpConnector = camera.connection(ipAddress)
        val stream = httpConnector.livePreview
        MJpegInputStream(stream)
    } catch (e: Exception) {
        Log.e(TAG, e.toString())
        null
    }

    private fun observeBitmapUpdate(view: ThreeHundredSixtyPlayer, stream: MJpegInputStream?) {
        var bitmap: Bitmap? = null

        Log.i(TAG, "observeBitmapUpdate()")

        repaintObserver = fromCallable {
            try {
                bitmap?.recycle()
                bitmap = stream?.readMJpegFrame()
                view.bitmap = bitmap
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { throwable -> Log.e(TAG, "Throwable ${throwable.message}") }
            .repeatWhen { o -> o.delay(40, TimeUnit.MILLISECONDS) }
            .subscribe {
                Log.i(TAG, "painted ${Calendar.getInstance().timeInMillis} $it")
            }
    }

    fun stopLiveView() {
        repaintObserver?.dispose()
    }

    fun transfer(fileId: String): Single<ImageData> {
        camera?.let {
            stopLiveView()
            val camera = it.connection(ipAddress)

            var totalFileSize = 0L
            var fileSize = 0L

            return Single.create { emitter ->
                val imageData = camera.getImage(fileId, object : HttpDownloadListener {
                    override fun onTotalSize(totalSize: Long) {
                        totalFileSize = totalSize
                    }

                    override fun onDataReceived(size: Int) {
                        fileSize += size
                    }
                })

                emitter.onSuccess(imageData)
            }
        }

        return Single.error(HttpConnector.CameraNotFoundException())
    }


    fun getThumbnail(fileId: String): Single<Bitmap> {
        camera?.let {
            val camera = it.connection(ipAddress)

            return Single.create { emitter ->
                val imageData = camera.getThumb(fileId)

                emitter.onSuccess(imageData)
            }
        }

        return Single.error(HttpConnector.CameraNotFoundException())
    }

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

    fun deleteOnCamera(filename: String): Single<Boolean> {
        camera?.let {
            val camera = it.connection(ipAddress)

            return Single.create { emitter ->
                camera.deleteFile(filename, object : HttpEventListener {
                    override fun onCheckStatus(newStatus: Boolean) {
                    }

                    override fun onObjectChanged(latestCapturedFileId: String?) {
                    }

                    override fun onCompleted() {
                        emitter.onSuccess(true)
                    }

                    override fun onError(errorMessage: String?) {
                        emitter.onError(Throwable(errorMessage))
                    }
                })
            }
        }

        return Single.error(HttpConnector.CameraNotFoundException())
    }

    companion object {
        internal val Any.TAG: String
            get () = this::class.java.simpleName
    }
}