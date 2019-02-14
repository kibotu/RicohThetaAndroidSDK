package com.exozet.ricohtheta

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import com.exozet.ricohtheta.cameras.ICamera
import com.exozet.ricohtheta.cameras.ThetaS
import com.exozet.ricohtheta.internal.network.HttpConnector
import com.exozet.ricohtheta.internal.view.MJpegInputStream
import com.exozet.ricohtheta.internal.view.MJpegView
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.io.InputStream

object Theta {

    private val cameras = ArrayList<ICamera>()

    private val MAX_RETRY_COUNTS = 20
    private val TAG = Theta::class.java.simpleName

    private var currentCamera : ICamera? = null

    @JvmStatic
    fun addCamera(camera : ICamera){
        cameras.add(camera)
    }

    fun findConnectedCamera(){
        cameras.forEach{
            //it.connect()
        }

        //TODO
        currentCamera = ThetaS
    }

    fun connect(ip4Address: String){

        currentCamera?.let {
            it.connect(ip4Address)
        }
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

    fun takePicture(): Uri {
        return Uri.EMPTY
    }//(Observable machen -> Stream + delete image )
    //fun getThumbnail() : Uri{ return Uri.EMPTY}

    open fun startLiveView(view: MJpegView) {

        Observable.fromCallable {
            currentCamera?.let {

                var success = false
                var mjis : MJpegInputStream? = null

                while (!success){
                    try {
                        val camera = it.connect("192.168.1.1")
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
                    //view.play()
                }
    }
    fun stopLiveView() {}
}