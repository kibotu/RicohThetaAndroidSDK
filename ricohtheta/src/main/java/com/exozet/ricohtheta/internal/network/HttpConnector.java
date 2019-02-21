package com.exozet.ricohtheta.internal.network;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;

public interface HttpConnector {

    public enum ShootResult {
        SUCCESS, FAIL_CAMERA_DISCONNECTED, FAIL_STORE_FULL, FAIL_DEVICE_BUSY
    }

    public class CameraNotFoundException extends Exception{

    }

    public InputStream getLivePreview() throws IOException, JSONException;

    public ShootResult takePicture(HttpEventListener listener);
    public ImageData getImage(String fileId, HttpDownloadListener listener);

    public DeviceInfo getDeviceInfo();

    public String connect();
}
