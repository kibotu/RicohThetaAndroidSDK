package com.exozet.ricohtheta.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.exozet.ricohtheta.Theta
import com.exozet.ricohtheta.cameras.ThetaS
import com.exozet.ricohtheta.cameras.ThetaV
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val TAG = Theta::class.java.simpleName

    var disposable: CompositeDisposable? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        disposable = CompositeDisposable()

        show360Player()

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Theta.createWirelessConnection(this)
        }

        takePhoto.setOnClickListener {
            Theta.takePicture()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError { throwable -> Log.e(TAG, "Throwable ${throwable.message}") }
                    .subscribe { result -> Log.i(TAG, "snapshot taken $result") }
        }
    }

    private fun show360Player() {
        disposable?.add(RxPermissions(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe { granted ->
                    if (granted) {
                        startActivity(Intent(this, BitmapActivity::class.java))
                        finish()
                    } else {
                        // Oups permission denied
                    }
                }
        )
    }

    override fun onResume() {
        super.onResume()
        Theta.onResume()

        with(Theta) {
            addCamera(ThetaS)
            addCamera(ThetaV)

            findConnectedCamera("192.168.1.1")
            startLiveView(jpegView)
        }
    }

    override fun onStop() {
        super.onStop()

        Theta.onStop()
    }

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }
}
