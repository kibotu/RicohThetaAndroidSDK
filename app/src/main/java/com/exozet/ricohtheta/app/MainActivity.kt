package com.exozet.ricohtheta.app

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.net.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.exozet.freedomplayer.FreedomPlayerActivity
import com.exozet.freedomplayer.Parameter
import com.exozet.ricohtheta.Theta
import com.exozet.ricohtheta.cameras.ThetaS
import com.exozet.ricohtheta.cameras.ThetaV
import com.exozet.sequentialimageplayer.parseAssetFile
import com.exozet.threehundredsixty.player.ThreeHundredSixtyPlayer
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val TAG = Theta::class.java.simpleName

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            Theta.createWirelessConnection(this)
        }

        takePhoto.setOnClickListener {
            Theta.takePicture()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError { throwable -> Log.e(TAG, "Throwable ${throwable.message}") }
                    .subscribe{result -> Log.i(TAG,"snapshot taken $result")}
        }
    }

    private fun show360Player(filename: String) {
        RxPermissions(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe { granted ->
                    if (granted) {
                        initFreedomPlayer(parseAssetFile(filename))
                    } else {
                        // Oups permission denied
                    }
                }
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

    fun initFreedomPlayer(threeHundredSixtyUri: Uri) {

        FreedomPlayerActivity.startActivity(this, Parameter(
                startPlayer = FreedomPlayerActivity.THREE_HUNDRED_SIXTY_PLAYER, // SEQUENTIAL_IMAGE_PLAYER, THREE_HUNDRED_SIXTY_PLAYER, default: FreedomPlayerActivity.SEQUENTIAL_IMAGE_PLAYER
                // threeHundredSixtyUri = parseAssetFile("equirectangular.jpg"), // load local asset file
                threeHundredSixtyUri = threeHundredSixtyUri,  // load local asset file
//                threeHundredSixtyUri = Uri.parse("https://storage.googleapis.com/preview-mobile-de/default/0001/01/b66f74bd094963f1b07b297508cfdeae1262cc7f.json"), // load using interior.json
                projectionMode = ThreeHundredSixtyPlayer.PROJECTION_MODE_SPHERE, // PROJECTION_MODE_SPHERE, PROJECTION_MODE_MULTI_FISH_EYE_HORIZONTAL, PROJECTION_MODE_MULTI_FISH_EYE_VERTICAL
                interactionMode = ThreeHundredSixtyPlayer.INTERACTIVE_MODE_MOTION_WITH_TOUCH, // INTERACTIVE_MODE_TOUCH, INTERACTIVE_MODE_MOTION, INTERACTIVE_MODE_MOTION_WITH_TOUCH, default: INTERACTIVE_MODE_MOTION_WITH_TOUCH
                showControls = false, // shows autoPlay and motion buttons, default false
                sequentialImageUris = (1 until 192).map { parseAssetFile(String.format("stabilized/out%03d.png", it)) }.toTypedArray(), // load from list of local files
                // sequentialImageUri = Uri.parse("https://storage.googleapis.com/preview-mobile-de/default/0001/01/7eb02f09747a624a50d3d287d2354610251ec2ad.json"), // load using exterior.json
                autoPlay = true, // default: true
                fps = 17, // [1:60] default: 30
                playBackwards = false, // default: false
                zoomable = true, // default: true
                translatable = true, // default: true
                swipeSpeed = 0.8f, // default 1f
                blurLetterbox = true // default: true
        ))
    }
}
