package com.exozet.ricohtheta.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.exozet.ricohtheta.Theta
import com.exozet.ricohtheta.cameras.ThetaS
import com.exozet.ricohtheta.cameras.ThetaV
import com.exozet.ricohtheta.internal.network.ImageData
import com.exozet.threehundredsixtyplayer.loadImage
import com.exozet.threehundredsixtyplayer.parseAssetFile
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_bitmap.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class BitmapActivity : AppCompatActivity() {

    private val TAG = Theta::class.java.simpleName
    private var latestFileId :String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bitmap)

        val sample1 = "interior_example.jpg"
        val sample2 = "equirectangular.jpg"
        var current = sample2

        var image1: Bitmap? = null
        var image2: Bitmap? = null
        var current2: Bitmap?

        lateinit var savedPhoto : Bitmap

        sample1.parseAssetFile().loadImage(this) {
            image1 = it
            threeHundredSixtyView.bitmap = it
        }

        sample2.parseAssetFile().loadImage(this) {
            image2 = it
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Theta.createWirelessConnection(this)
        }

        show.setOnClickListener {
            threeHundredSixtyView.bitmap = savedPhoto
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

            Theta.takePicture()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError { throwable -> Log.e(TAG, "Throwable ${throwable.message}") }
                    .subscribe {
                        result -> latestFileId = result
                        Log.i(TAG, "snapshot taken $result")
                        Theta.startLiveView(threeHundredSixtyView)
                        transfer.isEnabled = true
                        snapshot.isEnabled = true
                        delete_button.isEnabled = true
                    }
        }

        transfer.isEnabled = false
        transfer.setOnClickListener{
            Log.i(TAG, "start file transfer of $latestFileId ...")
            transfer.isEnabled = false
            snapshot.isEnabled = false
            delete_button.isEnabled = false

            latestFileId?.let {id ->
                Theta.transfer(id)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnError { throwable -> Log.e(TAG, "Throwable ${throwable.message}") }
                        //.doOnNext{ i -> Log.e(TAG, "onNext ${i.rawData.size}")}
                        .doOnSuccess { result ->
                            Log.i(TAG, "file transfered $result")
                            val filename = id.substringAfter("/")

                            //todo: use rx
                            Thread{
                                val photoFile = Theta.saveExternal(this , result, "/exozet Ricoh sdk/", filename)
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
            }
        }

        delete_button.isEnabled = false
        delete_button.setOnClickListener {
            delete_button.isEnabled = false

            latestFileId?.let { id ->
                Theta.deleteOnCamera(id)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess {result ->
                        if(result){
                            val filename = id.substringAfter("/")

                            Theta.deleteExternal(this, "/exozet Ricoh sdk/", filename)

                        }
                    }
                    .subscribe()
            }
        }

        thumbnail.setOnClickListener {
            latestFileId?.let { id ->
                Theta.getThumbnail(id)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess { result->
                        thumb.setImageBitmap(result)
                    }
                    .subscribe()
            }
        }

    }

    override fun onPause() {
        super.onPause()

        Theta.onPause()
    }

    override fun onResume() {
        super.onResume()

        Theta.onResume()

        with(Theta) {
            addCamera(ThetaS)
            addCamera(ThetaV)

            findConnectedCamera("192.168.1.1")
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .retryWhen { o -> o.delay(500, TimeUnit.MILLISECONDS)}
                    .doOnSuccess{
                        startLiveView(threeHundredSixtyView)
                    }.subscribe()

        }
    }
}