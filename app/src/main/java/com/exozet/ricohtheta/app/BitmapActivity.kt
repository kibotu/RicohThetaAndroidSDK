package com.exozet.ricohtheta.app

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.exozet.ricohtheta.Theta
import com.exozet.ricohtheta.cameras.ThetaS
import com.exozet.ricohtheta.cameras.ThetaV
import com.exozet.threehundredsixtyplayer.loadImage
import com.exozet.threehundredsixtyplayer.parseAssetFile
import kotlinx.android.synthetic.main.activity_bitmap.*
import kotlinx.android.synthetic.main.activity_main.*

class BitmapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bitmap)

        val sample1 = "interior_example.jpg"
        val sample2 = "equirectangular.jpg"
        var current = sample2

        var image1: Bitmap? = null
        var image2: Bitmap? = null
        var current2: Bitmap?

        sample1.parseAssetFile().loadImage(this) {
            image1 = it
            threeHundredSixtyView.bitmap = it
        }

        sample2.parseAssetFile().loadImage(this) {
            image2 = it
        }

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
    }

    override fun onResume() {
        super.onResume()

        Theta.onResume()

        with(Theta) {
            addCamera(ThetaS)
            addCamera(ThetaV)

            findConnectedCamera("192.168.1.1")
            startLiveView(threeHundredSixtyView)
        }
    }
}