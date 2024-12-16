package com.exozet.ricohtheta.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.exozet.ricohtheta.Theta
import com.exozet.ricohtheta.app.databinding.ActivityBitmapBinding
import com.exozet.ricohtheta.cameras.*
import com.exozet.threehundredsixtyplayer.loadImage
import com.exozet.threehundredsixtyplayer.parseAssetFile
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


class BitmapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBitmapBinding

    private var latestFileId: String? = null

    val theta by lazy {
        Theta()
            .addCamera(ThetaS())
            .addCamera(ThetaV())
            .addCamera(ThetaSC())
            .addCamera(ThetaSC2())
    }

    var subscription: CompositeDisposable = CompositeDisposable()

    var livePreviewStream: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBitmapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        subscription = CompositeDisposable()

        binding.setup()
    }

    fun ActivityBitmapBinding.setup() {

        val sample1 = "interior_example.jpg"
        val sample2 = "equirectangular.jpg"
        var current = sample2

        var image1: Bitmap? = null
        var image2: Bitmap? = null
        var current2: Bitmap?

        var savedPhoto: Bitmap? = null

        findCamera {
            startLivePreview()
            Timber.v("connected: ${it.deviceInfoName}")
        }

        sample1.parseAssetFile().loadImage(this@BitmapActivity) {
            image1 = it
            threeHundredSixtyView.bitmap = it
        }

        sample2.parseAssetFile().loadImage(this@BitmapActivity) {
            image2 = it
        }

        show.setOnClickListener {
            threeHundredSixtyView.bitmap = savedPhoto
        }

        closeConnectionButton.setOnClickListener {
            disposeLivePreview()
        }

        startConnectionButton.setOnClickListener {
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

            Timber.v("current=$current")

            threeHundredSixtyView.bitmap = current2
        }

        snapshot.setOnClickListener {
            Timber.v("take snapshot...")
            snapshot.isEnabled = false

            subscription += theta.takePicture()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { Timber.v("Throwable ${it.message}") }
                .subscribe { result ->
                    latestFileId = result
                    Timber.v("snapshot taken $result")
                    //theta.startLivePreview(threeHundredSixtyView).addTo(subscription)
                    transfer.isEnabled = true
                    snapshot.isEnabled = true
                    deleteButton.isEnabled = true
                }
        }

        transfer.isEnabled = false
        transfer.setOnClickListener {
            Timber.v("start file transfer of $latestFileId ...")
            transfer.isEnabled = false
            snapshot.isEnabled = false
            deleteButton.isEnabled = false

            latestFileId?.let { id ->
                theta.transfer(id)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError { throwable -> Timber.e(throwable) }
                    //.doOnNext{ i -> Log.e(TAG, "onNext ${i.rawData.size}")}
                    .doOnSuccess { result ->
                        Timber.v("file transfered $result")
                        val filename = id.substringAfter("/")

                        //todo: use rx
                        Thread {
                            val photoFile =
                                theta.saveExternal(
                                    this@BitmapActivity,
                                    result,
                                    "/exozet Ricoh sdk/",
                                    filename
                                )
                            val filePath = photoFile.path
                            savedPhoto = BitmapFactory.decodeFile(filePath)
                        }.start()

                        Timber.v("file saved $filename with result $result")
                        transfer.isEnabled = true
                        snapshot.isEnabled = true
                        deleteButton.isEnabled = true
                        show.isEnabled = true
                    }
                    .subscribe()
                    .addTo(subscription)
            }
        }

        deleteButton.isEnabled = false
        deleteButton.setOnClickListener {
            deleteButton.isEnabled = false

            latestFileId?.let { id ->
                theta.deleteOnCamera(id)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (it) {
                            val filename = id.substringAfter("/")

                            theta.deleteExternal(
                                this@BitmapActivity,
                                "/exozet Ricoh sdk/",
                                filename
                            )

                        }
                    }, { it.printStackTrace() })
            }
        }

        thumbnail.setOnClickListener {

            threeHundredSixtyView.vrLibrary?.renderer?.takeScreenshot {

                Timber.v("[takeScreenshot] bitmap width=${it.width} height=${it.height}")

                runOnUiThread {

                    val layoutParams = thumb.layoutParams as ConstraintLayout.LayoutParams
                    layoutParams.dimensionRatio = "${it.width}:${it.height}"
                    thumb.layoutParams = layoutParams

                    Glide.with(this@BitmapActivity)
                        .load(it)
                        .into(thumb)
                }
            }
        }
    }

    private fun startLivePreview() {
        disposeLivePreview()
        livePreviewStream = theta.startLivePreview(binding.threeHundredSixtyView)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { Timber.e(it) }
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
        subscription += theta.findCameras()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                onComplete(it)
            }, {
                it.printStackTrace()
            })
    }
}

operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    add(disposable)
}

fun Disposable.addTo(compositeDisposable: CompositeDisposable): Disposable =
    apply { compositeDisposable.add(this) }