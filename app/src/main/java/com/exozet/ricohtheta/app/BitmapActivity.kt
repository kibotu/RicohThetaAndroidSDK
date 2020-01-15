package com.exozet.ricohtheta.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.exozet.ricohtheta.Theta
import com.exozet.ricohtheta.cameras.*
import com.exozet.threehundredsixtyplayer.loadImage
import com.exozet.threehundredsixtyplayer.parseAssetFile
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_bitmap.*
import java.util.concurrent.TimeUnit


class BitmapActivity : AppCompatActivity() {

    private val TAG = BitmapActivity::class.java.simpleName
    private var latestFileId: String? = null

    val theta by lazy {
        Theta().addCamera(ThetaS()).addCamera(ThetaV()).addCamera(ThetaSC()).addCamera(ThetaSC2())
    }

    var subscription: CompositeDisposable = CompositeDisposable()

    var livePreviewStream: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bitmap)

        subscription = CompositeDisposable()

        val sample1 = "interior_example.jpg"
        val sample2 = "equirectangular.jpg"
        var current = sample2

        var image1: Bitmap? = null
        var image2: Bitmap? = null
        var current2: Bitmap?

        var savedPhoto: Bitmap? = null

        findCamera {
            startLivePreview()
            Log.v( TAG,"connected: ${it.deviceInfoName}")
        }

        sample1.parseAssetFile().loadImage(this) {
            image1 = it
            threeHundredSixtyView.bitmap = it
        }

        sample2.parseAssetFile().loadImage(this) {
            image2 = it
        }

        show.setOnClickListener {
            threeHundredSixtyView.bitmap = savedPhoto
        }

        close_connection_button.setOnClickListener {
            disposeLivePreview()
        }

        start_connection_button.setOnClickListener {
            findCamera {
                startLivePreview()
            }
        }

        show.isEnabled = false

        next.setOnClickListener {

            if (current == sample1) {
                current = sample2
                current2 = image2
            } else {
                current = sample1
                current2 = image1
            }

            Log.v("ThreeHundredSixty", "current=$current")

            threeHundredSixtyView.bitmap = current2
        }

        snapshot.setOnClickListener {
            Log.i(TAG, "take snapshot...")
            snapshot.isEnabled = false

            theta.takePicture()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { Log.e(TAG, "Throwable ${it.message}") }
                .subscribe { result ->
                    latestFileId = result
                    Log.i(TAG, "snapshot taken $result")
                    //theta.startLivePreview(threeHundredSixtyView).addTo(subscription)
                    transfer.isEnabled = true
                    snapshot.isEnabled = true
                    delete_button.isEnabled = true
                }
                .addTo(subscription)
        }

        transfer.isEnabled = false
        transfer.setOnClickListener {
            Log.i(TAG, "start file transfer of $latestFileId ...")
            transfer.isEnabled = false
            snapshot.isEnabled = false
            delete_button.isEnabled = false

            latestFileId?.let { id ->
                theta.transfer(id)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError { throwable -> Log.e(TAG, "Throwable ${throwable.message}") }
                    //.doOnNext{ i -> Log.e(TAG, "onNext ${i.rawData.size}")}
                    .doOnSuccess { result ->
                        Log.i(TAG, "file transfered $result")
                        val filename = id.substringAfter("/")

                        //todo: use rx
                        Thread {
                            val photoFile = theta.saveExternal(this, result, "/exozet Ricoh sdk/", filename)
                            val filePath = photoFile.path
                            savedPhoto = BitmapFactory.decodeFile(filePath)
                        }.start()

                        Log.i(TAG, "file saved $filename with result $result")
                        transfer.isEnabled = true
                        snapshot.isEnabled = true
                        delete_button.isEnabled = true
                        show.isEnabled = true
                    }
                    .subscribe()
                    .addTo(subscription)
            }
        }

        delete_button.isEnabled = false
        delete_button.setOnClickListener {
            delete_button.isEnabled = false

            latestFileId?.let { id ->
                theta.deleteOnCamera(id)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (it) {
                            val filename = id.substringAfter("/")

                            theta.deleteExternal(this, "/exozet Ricoh sdk/", filename)

                        }
                    }, { it.printStackTrace() })
            }
        }

        thumbnail.setOnClickListener {

            threeHundredSixtyView.vrLibrary?.renderer?.takeScreenshot {

                Log.v( TAG, "[takeScreenshot] bitmap width=${it.width} height=${it.height}")

                runOnUiThread {
                    thumb.setImageBitmap(it)
                }
            }
        }
    }

    private fun startLivePreview() {
        disposeLivePreview()
        livePreviewStream = theta.startLivePreview(threeHundredSixtyView)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError{Log.e(TAG, "fail ${it.message}")}
            .subscribe({}, { it.printStackTrace() })
    }


    override fun onDestroy() {
        if (!subscription.isDisposed) {
            subscription.dispose()
        }
        disposeLivePreview()
        super.onDestroy()
    }

    private fun disposeLivePreview() {
        if (livePreviewStream?.isDisposed == false) {
            livePreviewStream?.dispose()
        }
    }

    private fun findCamera(onComplete: (ICamera) -> Unit) {
        theta.findCameras()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                onComplete(it)
            }, {
                it.printStackTrace()
            }).addTo(subscription)
    }
}