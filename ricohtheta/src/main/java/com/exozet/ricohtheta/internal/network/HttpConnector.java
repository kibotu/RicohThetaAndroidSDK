package com.exozet.ricohtheta.internal.network;

import android.graphics.Bitmap;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;

public interface HttpConnector {

    enum ShootResult {
        SUCCESS, FAIL_CAMERA_DISCONNECTED, FAIL_STORE_FULL, FAIL_DEVICE_BUSY
    }

    class CameraNotFoundException extends Exception {

    }

    InputStream getLivePreview() throws IOException, JSONException;

    ShootResult takePicture(HttpEventListener listener);

    ImageData getImage(String fileId, HttpDownloadListener listener);

    Bitmap getThumb(String fileId);

    DeviceInfo getDeviceInfo();

    String connect();

    void deleteFile(String deletedFileId, HttpEventListener listener);
}