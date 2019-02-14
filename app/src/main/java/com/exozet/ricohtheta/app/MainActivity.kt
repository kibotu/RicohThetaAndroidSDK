package com.exozet.ricohtheta.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.exozet.ricohtheta.Theta
import com.exozet.ricohtheta.cameras.ThetaS
import com.exozet.ricohtheta.cameras.ThetaV
import kotlinx.android.synthetic.main.activity_main.*
import android.net.Network
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.content.Context.CONNECTIVITY_SERVICE
import android.os.Build
import android.annotation.TargetApi
import android.content.Context


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        forceConnectToWifi()



    }

    override fun onResume() {
        super.onResume()

        with(Theta){
            addCamera(ThetaS)
            addCamera(ThetaV)

            val camera = findConnectedCamera()

            startLiveView(jpegView)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun forceConnectToWifi() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (info != null && info.isAvailable) {
                val builder = NetworkRequest.Builder()
                builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                val requestedNetwork = builder.build()

                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)

                        ConnectivityManager.setProcessDefaultNetwork(network)
                        invalidateOptionsMenu()
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)

                        invalidateOptionsMenu()
                    }
                }

                cm.registerNetworkCallback(requestedNetwork, callback)
            }
        } else {
            invalidateOptionsMenu()
        }
    }



}
