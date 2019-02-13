package com.exozet.ricohtheta.internal.network;

public abstract class HttpConnector {

    public enum ShootResult {
        SUCCESS, FAIL_CAMERA_DISCONNECTED, FAIL_STORE_FULL, FAIL_DEVICE_BUSY
    }

}
