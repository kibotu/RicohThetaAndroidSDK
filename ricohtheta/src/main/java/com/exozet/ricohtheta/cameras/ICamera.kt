package com.exozet.ricohtheta.cameras

import com.exozet.ricohtheta.internal.network.HttpConnector

interface ICamera {

    val deviceInfoName: String

    var httpConnector: HttpConnector?

    val isConnected: Boolean

    fun connection(ip4Address: String): HttpConnector
}