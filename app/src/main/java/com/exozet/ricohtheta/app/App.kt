package com.exozet.ricohtheta.app

import android.os.StrictMode
import androidx.multidex.MultiDexApplication
import com.squareup.leakcanary.LeakCanary


class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {

            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectCustomSlowCalls()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyDeath()
                    .build())

            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectActivityLeaks()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build())

            if (LeakCanary.isInAnalyzerProcess(this)) {
                // This process is dedicated to LeakCanary for heap analysis.
                // You should not init your app in this process.
                return
            }
            LeakCanary.install(this)
        }
    }
}