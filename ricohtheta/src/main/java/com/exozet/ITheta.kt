package com.exozet

import android.graphics.Bitmap
import com.exozet.ricohtheta.Theta
import com.exozet.ricohtheta.cameras.ICamera
import com.exozet.ricohtheta.internal.network.ImageData
import com.exozet.threehundredsixtyplayer.ThreeHundredSixtyPlayer
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit


/**
 * Public Interface
 */
interface ITheta {

    val isConnected: Boolean

    val connectedCameras: List<ICamera>

    fun addCamera(camera: ICamera): Theta

    fun findCameras(): Observable<ICamera>

    fun disconnect(): Observable<ICamera>

    fun getThumbnail(fileId: String): Single<Bitmap>

    fun takePicture(): Observable<String>

    fun startLivePreview(view: ThreeHundredSixtyPlayer): Observable<Unit>

    fun startLivePreview(delay: Long = 0, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Observable<Bitmap>

    fun transfer(fileId: String): Single<ImageData>

    fun deleteOnCamera(filename: String): Observable<Boolean>
}