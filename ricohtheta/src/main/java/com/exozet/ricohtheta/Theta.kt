package com.exozet.ricohtheta

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.net.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.exozet.ricohtheta.cameras.ICamera
import com.exozet.ricohtheta.cameras.ThetaS
import com.exozet.ricohtheta.internal.network.HttpConnector
import com.exozet.ricohtheta.internal.network.HttpEventListener
import com.exozet.ricohtheta.internal.view.MJpegInputStream
import com.exozet.ricohtheta.internal.view.MJpegView
import com.exozet.threehundredsixtyplayer.ThreeHundredSixtyPlayer
import io.reactivex.Observable
import io.reactivex.Observable.fromCallable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.util.concurrent.TimeUnit


object Theta {


    private val cameras = ArrayList<ICamera>()
    private val TAG = Theta::class.java.simpleName
    private var currentCamera : ICamera? = null
    private var ipAddress : String = ""

    @JvmStatic
    fun addCamera(camera : ICamera){
        cameras.add(camera)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun createWirelessConnection(activity : Activity)
    {
        val cm = activity.application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
        request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

        cm.registerNetworkCallback(request.build(), object : ConnectivityManager.NetworkCallback(){
            override fun onAvailable(network: Network?) {
                super.onAvailable(network)
                when{
                    Build.VERSION.SDK_INT >= 23 -> {
                        cm?.bindProcessToNetwork(network)
                    }
                    // 21..22 = Lollipop
                    Build.VERSION.SDK_INT in 21..22 -> {
                        ConnectivityManager.setProcessDefaultNetwork(network)
                    }
                }
            }
        })
    }

    fun onResume(){
    }

    fun onStop(){
    }


    fun findConnectedCamera(ipAddress : String){

        this.ipAddress = ipAddress

        cameras.forEach{
            //it.connect()
        }

        //TODO
        currentCamera = ThetaS
    }

    fun disconnect(connector: HttpConnector){

        currentCamera?.let {
            //TODO
        }
    }


    val isConnected: Boolean
        get() {
            return true
        }

    fun takePicture() : Single<String> {
        currentCamera?.let{
            val camera = it.connect(ipAddress)

            return Single.create { emitter->
                camera.takePicture(object : HttpEventListener{
                    override fun onCheckStatus(newStatus: Boolean) {}

                    override fun onObjectChanged(latestCapturedFileId: String?) {
                        emitter.onSuccess(latestCapturedFileId!!)
                    }

                    override fun onCompleted() {}

                    override fun onError(errorMessage: String?) {
                        emitter.onError(Throwable(errorMessage))
                    }
                })
            }
        }

        return Single.error(Throwable("currentCamera does not exist"))
    }//(Observable machen -> Stream + delete image )
    //fun getThumbnail() : Uri{ return Uri.EMPTY}

    fun startLiveView(view: MJpegView) {

        fromCallable {
            currentCamera?.let {

                var success = false
                var mjis : MJpegInputStream? = null

                while (!success){
                    try {
                        val camera = it.connect(ipAddress)
                        val stream = camera.livePreview
                        mjis = MJpegInputStream(stream)
                        success = true
                    }catch (e: IOException){
                        Thread.sleep(500)
                        Log.e(TAG, e.toString())
                        success = false
                    }
                }
                mjis
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe{
                view.setSource(it)
            }
    }

    fun startLiveView(view: ThreeHundredSixtyPlayer){

        fromCallable {
            currentCamera?.let {

                var success = false
                var mjis : MJpegInputStream? = null

                while (!success){
                    try {
                        val camera = it.connect(ipAddress)
                        val stream = camera.livePreview
                        mjis = MJpegInputStream(stream)
                        success = true
                    }catch (e: IOException){
                        Thread.sleep(500)
                        Log.e(TAG, e.toString())
                        success = false
                    }
                }
                mjis
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { throwable -> Log.e(TAG, "Throwable ${throwable.message}") }
            .subscribe{runUpdateBitmapLoop(view, it)
           }
    }

    private fun runUpdateBitmapLoop(view: ThreeHundredSixtyPlayer, stream: MJpegInputStream?) {

        var bitmap: Bitmap? = null

        fromCallable {
            try{
                bitmap = stream?.readMJpegFrame()
                view.bitmap = bitmap
                //bitmap.recycle()
            }
            catch (e : Exception){
                Log.e(TAG, e.toString())
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { throwable -> Log.e(TAG, "Throwable ${throwable.message}") }
            .repeatWhen{ o -> o.delay(100, TimeUnit.MILLISECONDS) }
            .subscribe{
               // bitmap?.recycle()
                Log.i(TAG,"painted ${it.toString()}")
            }
    }

    fun stopLiveView() {}
}