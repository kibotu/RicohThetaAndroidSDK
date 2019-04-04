package com.exozet.ricohtheta.cameras

import com.exozet.ricohtheta.internal.network.HttpConnector

interface ICamera {

    val deviceInfoName: String

    val ip4Address: String

    var httpConnector: HttpConnector?

    var isConnected: Boolean
}