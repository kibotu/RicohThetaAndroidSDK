package com.exozet.ricohtheta.cameras

import com.exozet.ricohtheta.internal.network.HttpConnector

interface ICamera{
    fun connection(ipAddress : String) : HttpConnector

    val deviceInfoName: String


}